"""Browsable candidate gallery; acceptance is a separate suite.py operation."""
from pathlib import Path
import html


def gallery(output, images):
    cards = ''.join(f'<figure><a href="{html.escape(i["path"], quote=True)}"><img loading="lazy" src="{html.escape(i["path"], quote=True)}" alt="{html.escape(i["scenario"], quote=True)}"></a><figcaption>{html.escape(i["scenario"])} · {i["theme"]} · {html.escape(i.get("status", "implemented-ui"))}</figcaption></figure>' for i in images)
    (Path(output) / 'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Attestra screenshot candidates</title><style>body{font:16px system-ui;margin:2rem;background:#eef2f5;color:#18232d}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:24px}figure{margin:0}img{width:100%;height:auto}figcaption{overflow-wrap:anywhere;margin:8px 0 24px}</style><h1>Screenshot candidates — not approved baselines</h1><p>Inspect the images before an explicit acceptance. Synthetic data; no hardware or backend verification.</p><main>' + cards + '</main></html>')


if __name__ == '__main__':
    raise SystemExit('Use python3 scripts/screenshots/suite.py candidates')
