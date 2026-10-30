"""Inventory, candidate generation, explicit review and read-only regression inputs."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SCENARIOS = ROOT / 'docs/screenshots/scenarios.json'
COVERAGE = ROOT / 'docs/screenshots/coverage.json'
ACCEPTED = ROOT / 'docs/screenshots/baselines.json'
ARCHIVE = ROOT / 'docs/screenshots/baselines.zip'
CANDIDATES = ROOT / 'docs/generated/screenshots'
MODULES = ('auth', 'app')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read(path):
    return json.loads(path.read_text())


def render_inputs():
    paths = set()
    for module in ('auth', 'app', 'identity-mock', 'buildSrc'):
        for path in (ROOT / module).rglob('*'):
            if path.is_file() and not any(part in ('build', '.gradle', 'screenshotTestDebug') for part in path.relative_to(ROOT).parts) and path.suffix in ('.kt', '.kts', '.xml'):
                paths.add(path)
    paths.update(ROOT / p for p in ('gradle/libs.versions.toml', 'gradle/wrapper/gradle-wrapper.properties', 'gradle.properties', 'settings.gradle.kts', 'build.gradle.kts', 'docs/screenshots/scenarios.json'))
    files = {p.relative_to(ROOT).as_posix(): digest(p.read_bytes()) for p in sorted(paths)}
    return {'files': files, 'sha256': digest(json.dumps(files, sort_keys=True).encode())}


def png_info(path):
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != b'\x89PNG\r\n\x1a\n' or data[12:16] != b'IHDR':
        raise ValueError(f'Invalid PNG: {path}')
    width, height = struct.unpack('>II', data[16:24])
    if width < 1 or height < 1:
        raise ValueError('Empty image dimensions')
    return {'sha256': digest(data), 'width': width, 'height': height}


def coverage():
    inventory = read(SCENARIOS)['scenarios']
    ids = {s['id'] for s in inventory}
    if len(ids) != len(inventory) or not ids:
        raise ValueError('Missing or duplicate scenario IDs')
    for scenario in inventory:
        text = (ROOT / scenario['fixture']).read_text()
        if not re.search(r'@PreviewTest\s+(?:(?!fun\s).)*?fun\s+' + re.escape(scenario['method']) + r'\b', text, re.S):
            raise ValueError('Missing executable scenario: ' + scenario['id'])
        if scenario['module'] not in MODULES or scenario['variants'] < 1:
            raise ValueError('Invalid scenario module/count')
    discovered = set()
    for module in MODULES + ('identity-mock',):
        for source in (ROOT / module / 'src').rglob('*.kt'):
            if any(x in source.parts for x in ('test', 'androidTest', 'screenshotTest')):
                continue
            for symbol in re.findall(r'@Composable\s+(?:(?:internal|private|public)\s+)?fun\s+(\w+)', source.read_text()):
                if not symbol.endswith('Preview'):
                    discovered.add((source.relative_to(ROOT).as_posix(), symbol))
    records = read(COVERAGE)['composables']
    mapped = {(r['source'], r['symbol']) for r in records}
    if discovered != mapped or len(mapped) != len(records):
        raise ValueError(f'Composable coverage drift. Unmapped: {discovered-mapped}; removed/duplicate: {mapped-discovered}')
    for record in records:
        if record['kind'] == 'nonvisual-runtime':
            if not record['note']:
                raise ValueError('Nonvisual runtime needs rationale')
        elif not record['scenarios'] or not set(record['scenarios']) <= ids:
            raise ValueError('Invalid preview mapping: ' + record['symbol'])
    print(f'Covered {len(records)} composables; {len(inventory)} required scenarios; {sum(s["variants"] for s in inventory)} images')
    return inventory


def candidates():
    inventory = coverage()
    if CANDIDATES.exists():
        shutil.rmtree(CANDIDATES)
    CANDIDATES.mkdir(parents=True)
    images = []
    for module in MODULES:
        source = ROOT / module / 'src/screenshotTestDebug/reference'
        paths = sorted(source.rglob('*.png'))
        if not paths:
            raise ValueError('Zero screenshots for UI module: ' + module)
        for path in paths:
            matching = [s for s in inventory if s['module'] == module and s['id'].split(':')[1].split('.')[0] in path.as_posix() and path.stem.startswith(s['method'] + '_')]
            if len(matching) != 1:
                raise ValueError('Unknown/ambiguous rendered scenario: ' + str(path))
            scenario = matching[0]
            relative = Path(module) / path.relative_to(source)
            dest = CANDIDATES / relative
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, dest)
            images.append(dict(path=relative.as_posix(), scenario=scenario['id'], fixture=scenario['fixture'], capture_kind='component-preview', theme='dark' if '_Dark_' in path.stem else 'light' if '_Light_' in path.stem else scenario.get('theme', 'fixture-default'), viewport_dp=scenario['viewport_dp'], locale=scenario['locale'], font_scale=scenario['font_scale'], **png_info(path)))
    for scenario in inventory:
        actual = sum(i['scenario'] == scenario['id'] for i in images)
        if actual != scenario['variants']:
            raise ValueError(f"Missing required variants for {scenario['id']}: {actual}/{scenario['variants']}")
    manifest = {'schemaVersion': 1, 'status': 'candidates-not-approved', 'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(), 'inputs': render_inputs(), 'toolchain': {'versions_sha256': digest((ROOT/'gradle/libs.versions.toml').read_bytes()), 'renderer': 'Compose screenshot alpha16', 'platform': 'Linux host layoutlib; no device', 'fonts': 'renderer platform fonts', 'clock_animation': 'renderer capture frame; permission autoplay disabled'}, 'images': images}
    (CANDIDATES / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    from export import gallery
    gallery(CANDIDATES, images)
    print(f'Generated {len(images)} candidates; not accepted baselines')


def validate_manifest(directory, manifest):
    if manifest['inputs']['sha256'] != render_inputs()['sha256']:
        raise ValueError('Render inputs changed; render and review new candidates')
    for image in manifest['images']:
        path = Path(image['path'])
        if path.is_absolute() or '..' in path.parts or path.parts[0] not in MODULES:
            raise ValueError('Unsafe image path')
        if png_info(directory / path) != {key: image[key] for key in ('sha256', 'width', 'height')}:
            raise ValueError('Unreviewed image change: ' + str(path))


def accept(directory, reviewer):
    manifest = read(directory / 'manifest.json')
    if manifest['status'] != 'candidates-not-approved' or not reviewer.strip():
        raise ValueError('Explicit review of candidate images is required')
    validate_manifest(directory, manifest)
    with zipfile.ZipFile(ARCHIVE, 'w', zipfile.ZIP_DEFLATED) as archive:
        for image in manifest['images']:
            archive.write(directory / image['path'], image['path'])
    manifest.update(status='reviewed-baselines', reviewed_by=reviewer, archive_sha256=digest(ARCHIVE.read_bytes()))
    ACCEPTED.write_text(json.dumps(manifest, indent=2) + '\n')
    promote_docs(directory, manifest)
    print('Accepted reviewed images. Commit archive, manifest and curated documentation images together.')


def promote_docs(directory, manifest):
    selections = {'emailStart': ('light', 'email-start-light.png'), 'readDocument': ('dark', 'read-document-dark.png'), 'detailsSaved': ('light', 'details-saved-light.png')}
    captures = []
    for method, (theme, name) in selections.items():
        image = next(i for i in manifest['images'] if i['scenario'] == 'auth:DocumentationScreenshotTest.' + method and i['theme'] == theme)
        target = ROOT / 'docs/assets/screenshots' / name
        shutil.copyfile(directory / image['path'], target)
        captures.append(dict(path=target.relative_to(ROOT).as_posix(), image_sha256=image['sha256'], width=image['width'], height=image['height'], scenario=image['scenario'], fixture=image['fixture'], capture_kind=image['capture_kind'], theme=theme, locale='en', source_commit=manifest['source_commit'], render_inputs_sha256=manifest['inputs']['sha256'], caption='Synthetic component preview: '+method, alt='Attestra '+method+' in '+theme+' theme; synthetic fixture data'))
    (ROOT / 'docs/screenshots/documentation.json').write_text(json.dumps({'schemaVersion': 1, 'captures': captures}, indent=2) + '\n')


def restore():
    if not ACCEPTED.exists():
        raise ValueError('No approved baseline. Run the manual candidate workflow, inspect it, then accept explicitly.')
    manifest = read(ACCEPTED)
    if manifest['status'] != 'reviewed-baselines' or digest(ARCHIVE.read_bytes()) != manifest['archive_sha256']:
        raise ValueError('Baseline archive does not match approved manifest')
    if manifest['inputs']['sha256'] != render_inputs()['sha256']:
        raise ValueError('Screenshot inputs changed; candidate review and intentional acceptance required')
    for module in MODULES:
        target = ROOT / module / 'src/screenshotTestDebug/reference'
        if target.exists():
            shutil.rmtree(target)
        target.mkdir(parents=True)
    with zipfile.ZipFile(ARCHIVE) as archive:
        for image in manifest['images']:
            rel = Path(image['path'])
            if rel.is_absolute() or '..' in rel.parts or rel.parts[0] not in MODULES:
                raise ValueError('Unsafe baseline path')
            dest = ROOT / rel.parts[0] / 'src/screenshotTestDebug/reference' / Path(*rel.parts[1:])
            dest.parent.mkdir(parents=True, exist_ok=True)
            data = archive.read(image['path'])
            if digest(data) != image['sha256']:
                raise ValueError('Baseline image hash mismatch')
            dest.write_bytes(data)
    print('Restored approved baseline inputs; validation may now compare without updating them')


def verify_docs():
    path = ROOT / 'docs/screenshots/documentation.json'
    if not path.exists():
        raise ValueError('Missing reviewed documentation provenance')
    for image in read(path)['captures']:
        actual = png_info(ROOT / image['path'])
        if actual != dict(sha256=image['image_sha256'], width=image['width'], height=image['height']):
            raise ValueError('Documentation image/hash/dimensions drift: ' + image['path'])
    print('Documentation screenshot hashes and dimensions verified')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['coverage', 'candidates', 'accept', 'restore', 'verify-docs'])
    parser.add_argument('--directory', type=Path, default=CANDIDATES)
    parser.add_argument('--reviewed-by', default='')
    args = parser.parse_args()
    if args.command == 'accept':
        accept(args.directory, args.reviewed_by)
    else:
        globals()[args.command.replace('-', '_')]()
