"""Public artifact and identity checks shared by the auth release adapters."""
import hashlib, io, json, os, re, time, tomllib, urllib.error, urllib.request, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
CENTRAL = 'https://repo.maven.apache.org/maven2'
SEMVER = r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)'
SUFFIXES = ('.pom', '.aar', '-sources.jar', '-javadoc.jar', '.module')

def version_key(value):
    if not re.fullmatch(SEMVER, value):
        raise ValueError(f'Expected stable X.Y.Z, got {value!r}')
    return tuple(map(int, value.split('.')))

def fetch(url, missing=False, attempts=3):
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(url, timeout=20) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if error.code == 404 and missing:
                return None
            if error.code != 429 and error.code < 500:
                raise
            if attempt == attempts - 1:
                raise
        except (urllib.error.URLError, TimeoutError):
            if attempt == attempts - 1:
                raise
        time.sleep(2 ** attempt)

def artifact_base(group, artifact):
    return f'{CENTRAL}/{group.replace(".", "/")}/{artifact}'

def published_versions(group, artifact):
    data = fetch(artifact_base(group, artifact) + '/maven-metadata.xml', missing=True)
    if data is None:
        return []
    xml = ET.fromstring(data)
    if xml.tag != 'metadata' or xml.findtext('groupId') != group or xml.findtext('artifactId') != artifact:
        raise ValueError('Invalid Central metadata identity')
    versions = xml.findall('./versioning/versions/version')
    if not versions or any(not node.text for node in versions):
        raise ValueError('Malformed/empty Central metadata')
    return [node.text for node in versions]

def outputs(values):
    text = ''.join(f'{k}={v}\n' for k, v in values.items())
    print(text, end='')
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as stream:
            stream.write(text)
    return values

def verify_pom(data, group, artifact, version):
    xml = ET.fromstring(data)
    for node in xml.iter(): node.tag = node.tag.split('}')[-1]
    if tuple(xml.findtext(k) for k in ('groupId','artifactId','version','packaging')) != (group,artifact,version,'aar'):
        raise ValueError('POM identity/packaging mismatch')
    for path in ('name','description','url','licenses/license/name','licenses/license/url','developers/developer/id','developers/developer/name','scm/url','scm/connection'):
        if not xml.findtext(path): raise ValueError('Missing POM metadata: '+path)
    catalog = tomllib.loads((ROOT/'gradle/libs.versions.toml').read_text())
    deps = xml.findall('dependencies/dependency')
    for name in ('apexfission-permissions','apexfission-card-detector'):
        lib = catalog['libraries'][name]
        expected = (*lib['module'].split(':'),lib['version'])
        if not any(tuple(d.findtext(k) for k in ('groupId','artifactId','version')) == expected for d in deps):
            raise ValueError('Missing pinned dependency: '+name)
    if any(d.findtext('artifactId') in ('app','identity-mock','unspecified') or d.findtext('version') == 'unspecified' for d in deps):
        raise ValueError('Internal/unresolved project dependency in POM')

def verify_artifact(data, suffix, group, artifact, version, model_assets=None):
    if suffix == '.pom': return verify_pom(data,group,artifact,version)
    if suffix == '.module':
        metadata = json.loads(data)
        if tuple(metadata.get('component',{}).get(k) for k in ('group','module','version')) != (group,artifact,version):
            raise ValueError('Gradle metadata identity mismatch')
        if not metadata.get('variants'): raise ValueError('Missing Gradle variants')
        return
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if archive.testzip(): raise ValueError('Corrupt archive')
        names = archive.namelist()
        if suffix == '.aar':
            if not {'AndroidManifest.xml','classes.jar'} <= set(names): raise ValueError('Missing manifest/classes')
            with zipfile.ZipFile(io.BytesIO(archive.read('classes.jar'))) as classes:
                owned = [n for n in classes.namelist() if n.endswith('.class') and n.startswith('com/apexfission/android/attestra/auth/')]
                if not owned: raise ValueError('Missing auth classes')
                if any(n.startswith('com/apexfission/android/attestra/identitymock/') for n in classes.namelist()):
                    raise ValueError('Mock classes must not be published')
                for name in owned:
                    bytecode = classes.read(name)
                    if bytecode[:4] != b'\xca\xfe\xba\xbe' or int.from_bytes(bytecode[6:8],'big') != 61:
                        raise ValueError('Expected Java 17 bytecode')
            models = [n for n in names if n.endswith('.tflite')]
            if not models or any(archive.read(n)[4:8] != b'TFL3' for n in models):
                raise ValueError('Missing/invalid bundled card model assets')
            if model_assets is not None and {n:hashlib.sha256(archive.read(n)).hexdigest() for n in models} != model_assets:
                raise ValueError('Bundled model assets differ from reservation')
        elif suffix == '-sources.jar' and not any(n.endswith('.kt') for n in names):
            raise ValueError('Empty Kotlin sources')
        elif suffix == '-javadoc.jar' and not any(n.endswith(('.md','.html')) for n in names):
            raise ValueError('Empty documentation archive')
