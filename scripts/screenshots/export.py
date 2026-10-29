"""Export Compose reference PNGs as a browsable documentation gallery."""
from pathlib import Path
import html
import json
import shutil

ROOT = Path(__file__).resolve().parents[2]


def export(source=ROOT / 'auth/src/screenshotTestDebug/reference',
           output=ROOT / 'docs/generated/screenshots'):
    images = sorted(source.rglob('*.png'))
    if not images:
        raise RuntimeError(f'No rendered screenshots found in {source}')
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    entries = []
    for image in images:
        relative = image.relative_to(source)
        target = output / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(image, target)
        entries.append(relative.as_posix())
    (output / 'manifest.json').write_text(json.dumps(entries, indent=2) + '\n')
    cards = ''.join(f'<figure><a href="{html.escape(p, quote=True)}"><img loading="lazy" src="{html.escape(p, quote=True)}" alt="{html.escape(Path(p).stem, quote=True)}"></a><figcaption>{html.escape(Path(p).stem)}</figcaption></figure>' for p in entries)
    (output / 'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Attestra screen gallery</title><style>body{font:16px system-ui;margin:2rem;background:#eef2f5;color:#18232d}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:24px}figure{margin:0}img{width:100%;height:auto}figcaption{overflow-wrap:anywhere;margin:8px 0 24px}</style><h1>Attestra screen gallery</h1><p>Rendered from synthetic UI fixtures. Camera, system dialogs and backend behavior require device testing.</p><main>' + cards + '</main></html>')
    print(f'Exported {len(entries)} screenshots to {output}')


if __name__ == '__main__':
    export()
