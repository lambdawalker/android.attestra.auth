"""Render installation examples only from the latest confirmed module identities."""
import re
from urllib.parse import urlsplit
CENTRAL = 'https://repo.maven.apache.org/maven2'
GOOGLE = 'https://dl.google.com/dl/android/maven2'
MODULES = {'auth': 'attestra-auth'}

def _safe(value, pattern, name):
    if not isinstance(value, str) or not re.fullmatch(pattern, value):
        raise ValueError(f'Invalid installation {name}')
    return value

def _coordinate(group, artifact):
    _safe(group, '[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+', 'group')
    _safe(artifact, '[A-Za-z0-9_][A-Za-z0-9_.-]*', 'artifact')

def _version(value):
    _safe(value, '(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)', 'version')
    return tuple(map(int, value.split('.')))

def _url(value):
    _safe(value, 'https://[A-Za-z0-9._:/%+~-]+', 'repository URL')
    parsed = urlsplit(value)
    if not parsed.hostname or parsed.username or parsed.password:
        raise ValueError('Invalid installation repository URL')
    try:
        parsed.port
    except ValueError as error:
        raise ValueError('Invalid installation repository URL') from error
    return value.rstrip('/')

def _consumer(value):
    return _safe(value, '[A-Za-z0-9_][A-Za-z0-9_.~+-]*', 'consumer version')

def _validate(target, module, record):
    if record.get('module') != module or record.get('repository', 'maven-central') != target:
        raise ValueError('Installation metadata module/repository mismatch')
    _safe(target, '[a-z][a-z0-9-]{0,62}', 'repository')
    _coordinate(record['group'], record['artifact'])
    _version(record['version'])
    _safe(record['source'], '[0-9a-f]{40}', 'source')
    _consumer(record.get('consumer_version', record['version']))
    _url(record.get('repository_url', CENTRAL))
    if target == 'jitpack' and record.get('consumer_version') != f"{module}~v{record['version']}":
        raise ValueError('JitPack consumer version must match the module release')

def _examples(module, record):
    group, artifact = (record['group'], record['artifact'])
    version = record.get('consumer_version', record['version'])
    gav = f'{group}:{artifact}:{version}'
    urls = [GOOGLE, CENTRAL, 'https://jitpack.io', _url(record.get('repository_url', CENTRAL))]
    urls = list(dict.fromkeys(urls))
    extra = [url for url in urls if url not in (GOOGLE, CENTRAL)]
    kotlin_repos = '\n'.join((f'        maven {{ url = uri("{url}") }}' for url in extra))
    groovy_repos = '\n'.join((f"        maven {{ url '{url}' }}" for url in extra))
    kotlin_repos = '\n' + kotlin_repos if kotlin_repos else ''
    groovy_repos = '\n' + groovy_repos if groovy_repos else ''
    maven_repos = '\n'.join((f"  <repository>\n    <id>{('google' if url == GOOGLE else 'central' if url == CENTRAL else f'confirmed-{i}')}</id>\n    <url>{url}</url>\n  </repository>" for i, url in enumerate(urls)))
    return f'''#### Gradle Kotlin DSL\n\nIn `settings.gradle.kts`:\n\n```kotlin\ndependencyResolutionManagement {{\n    repositories {{\n        google()\n        mavenCentral(){kotlin_repos}\n    }}\n}}\n```\n\nIn the app's `build.gradle.kts`:\n\n```kotlin\ndependencies {{\n    implementation("{gav}")\n}}\n```\n\n#### Gradle Groovy DSL\n\nIn `settings.gradle`:\n\n```groovy\ndependencyResolutionManagement {{\n    repositories {{\n        google()\n        mavenCentral(){groovy_repos}\n    }}\n}}\n```\n\nIn the app's `build.gradle`:\n\n```groovy\ndependencies {{\n    implementation '{gav}'\n}}\n```\n\n#### Version catalog\n\nUse the dependency repositories shown above. Add to `gradle/libs.versions.toml`:\n\n```toml\n[libraries]\n{module} = {{ module = "{group}:{artifact}", version = "{version}" }}\n```\n\nThen use this instead of the direct dependency in the app's `build.gradle.kts`:\n\n```kotlin\ndependencies {{\n    implementation(libs.{module})\n}}\n```\n\n#### Maven\n\nAdd these repositories and dependency to `pom.xml`:\n\n```xml\n<repositories>\n{maven_repos}\n</repositories>\n<dependencies>\n  <dependency>\n    <groupId>{group}</groupId>\n    <artifactId>{artifact}</artifactId>\n    <version>{version}</version>\n    <type>aar</type>\n  </dependency>\n</dependencies>\n```'''

def render(records, identities):
    """Return module sections; pending releases cannot displace confirmed releases.

    Equal newest semantic versions must identify the same source across mirrors.
    No network or mutable configuration is consulted here.
    """
    selected = {module: [] for module in MODULES}
    for (target, module), record in records.items():
        if module not in MODULES:
            raise ValueError('Unknown installation module')
        if record is None:
            continue
        if record.get('phase') == 'reserved-before-upload':
            continue
        if record.get('phase') != 'confirmed-public':
            raise ValueError('Installation can only advertise confirmed releases')
        _validate(target, module, record)
        selected[module].append((target, record))
    sections = []
    for module, candidates in selected.items():
        configured = identities[module]
        _coordinate(*configured)
        if not candidates:
            sections.append(f'## {MODULES[module]}\n\nNo confirmed release.')
            continue
        highest = max((_version(record['version']) for _, record in candidates))
        latest = [(target, record) for target, record in candidates if _version(record['version']) == highest]
        if len({record['source'] for _, record in latest}) != 1:
            raise ValueError(f'Conflicting confirmed sources for latest {module} version')
        latest.sort(key=lambda item: (item[0] != 'maven-central', item[0]))
        first = latest[0][1]
        section = f"## {module}: {first['artifact']}\n\nConfirmed version: **{first['version']}**. Source: [{first['source']}](https://github.com/lambdawalker/android.attestra.auth/commit/{first['source']}).\n\nChoose **one** destination below and **one** dependency syntax. Each destination provides this same release; do not add duplicate dependencies."
        for target, record in latest:
            notice = ''
            if target != 'jitpack' and (record['group'], record['artifact']) != configured:
                notice += f'\n\nConfigured next publication: `{configured[0]}:{configured[1]}` — not yet confirmed here. The dependency below remains the last confirmed publication.'
            section += f'\n\n### {target}\n\nRepository: **{target}**.{notice}\n\n{_examples(module, record)}'
        sections.append(section)
    return '\n\n'.join(sections)
