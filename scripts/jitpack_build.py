"""JitPack entry point: only canonical module tags may publish one library.

Keep this standalone script compatible with Python 3.5 on JitPack's default
image. Other release tooling runs on the modern Python configured in Actions.
"""
import hashlib
import os
from pathlib import Path
import re
import subprocess
ROOT = Path(__file__).resolve().parents[1]
TAG = re.compile('(auth)[/~]v((?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*))')

def selection(value):
    match = TAG.fullmatch(value)
    if not match:
        raise ValueError('VERSION must be a canonical auth/vX.Y.Z tag (slash or ~)')
    return match.groups()

def run(*args):
    return subprocess.check_output(args, cwd=str(ROOT), universal_newlines=True).strip()

def command(module, version):
    selection('{}/v{}'.format(module, version))
    return ['bash', 'gradlew', '--no-daemon', ':{}:publishToMavenLocal'.format(module), '-PjitpackBuild=true', '-PreleaseModule={}'.format(module), '-PreleaseVersion={}'.format(version)]

def main():
    module, version = selection(os.environ.get('VERSION', ''))
    source = run('git', 'rev-parse', 'HEAD')
    if run('git', 'rev-parse', 'refs/tags/{}/v{}^{{commit}}'.format(module, version)) != source:
        raise ValueError('Canonical tag does not identify checkout')
    if os.environ.get('GIT_COMMIT', source) != source:
        raise ValueError('JitPack GIT_COMMIT differs from checkout')
    subprocess.run(command(module, version), cwd=str(ROOT), check=True)
if __name__ == '__main__':
    main()
