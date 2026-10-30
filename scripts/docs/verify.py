"""Check the documented host call against the compiled sample, ignoring indentation."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[2]
page=(ROOT/'docs/agents/quickstart.md').read_text()
example=next(x for x in re.findall(r'```kotlin\n(.*?)```',page,re.S) if 'EmailOnboardingHost(' in x)
source=(ROOT/'app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt').read_text()
normalize=lambda s:re.sub(r'\s+','',s)
if normalize(example) not in normalize(source):
    raise SystemExit('Quickstart host call drifted from compiled MainActivity')
print('Quickstart call matches compiled sample source')
