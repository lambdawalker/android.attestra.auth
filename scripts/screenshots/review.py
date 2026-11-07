"""Produce a read-only review of CI screenshot candidates against accepted baselines."""
import argparse
from difflib import unified_diff
from html import escape
import json
from pathlib import Path
import zipfile

from suite import (
    ACCEPTED, ARCHIVE, CANDIDATES, ROOT, coverage, digest, documentation_captures,
    read, validate_manifest, validate_variants,
)


def manifest_patch(path, value):
    relative = path.relative_to(ROOT).as_posix()
    return ''.join(unified_diff(
        path.read_text().splitlines(keepends=True),
        (json.dumps(value, indent=2) + '\n').splitlines(keepends=True),
        f'a/{relative}', f'b/{relative}',
    ))


def review(directory):
    candidates = read(directory / 'manifest.json')
    if candidates['status'] != 'candidates-not-approved':
        raise ValueError('Expected unapproved screenshot candidates')
    validate_variants(candidates['images'], coverage())
    validate_manifest(directory, candidates)
    baseline = read(ACCEPTED)
    if baseline['status'] != 'reviewed-baselines' or digest(ARCHIVE.read_bytes()) != baseline['archive_sha256']:
        raise ValueError('Baseline archive does not match the reviewed manifest')
    before = {image['path']: image['sha256'] for image in baseline['images']}
    after = {image['path']: image['sha256'] for image in candidates['images']}
    changed = sorted(path for path in before.keys() | after.keys() if before.get(path) != after.get(path))
    output = directory / 'review'
    output.mkdir()
    with zipfile.ZipFile(ARCHIVE) as archive:
        for path, sha in before.items():
            if digest(archive.read(path)) != sha:
                raise ValueError('Baseline image hash mismatch: ' + path)
        if changed:
            panels = []
            for index, path in enumerate(changed):
                images = []
                for side, data in [
                    ('Before', archive.read(path) if path in before else None),
                    ('After', (directory / path).read_bytes() if path in after else None),
                ]:
                    name = f'{index}-{side.lower()}.png'
                    if data is not None:
                        (output / name).write_bytes(data)
                    content = f'<img src="{name}" alt="{side} screenshot">' if data is not None else '<p>No image</p>'
                    images.append(f'<figure><figcaption>{side}</figcaption>{content}</figure>')
                panels.append(f'<section><h2>{escape(path)}</h2><div>{"".join(images)}</div></section>')
            (output / 'index.html').write_text(
                '<!doctype html><html lang="en"><meta charset="utf-8"><title>Screenshot changes</title>'
                '<style>section div{display:flex;flex-wrap:wrap}figure{max-width:45%;margin:1em}img{max-width:100%}'
                'h2{overflow-wrap:anywhere}</style><h1>Screenshot changes</h1>' + ''.join(panels),
                encoding='utf-8',
            )
            return f'{len(changed)} screenshot files changed, were added, or were removed. Open review/index.html to compare.'
    if baseline['inputs']['sha256'] == candidates['inputs']['sha256']:
        return 'Screenshot files and render inputs match the approved baselines.'
    updated = dict(candidates, status='reviewed-baselines',
                   reviewed_by='CI verified that every candidate PNG matches the approved baseline archive byte for byte.',
                   archive_sha256=baseline['archive_sha256'])
    docs = ROOT / 'docs/screenshots/documentation.json'
    patch = manifest_patch(ACCEPTED, updated)
    patch += manifest_patch(docs, dict(schemaVersion=1, captures=documentation_captures(updated)))
    (output / 'provenance.patch').write_text(patch, encoding='utf-8')
    return 'All screenshot PNGs match. Render inputs changed. Review and apply review/provenance.patch to refresh the manifests.'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, default=CANDIDATES)
    args = parser.parse_args()
    summary = review(args.directory)
    (args.directory / 'review/summary.txt').write_text(summary + '\n', encoding='utf-8')
    print(summary)
