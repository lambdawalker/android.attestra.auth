"""Canonical repository registry and deterministic GitHub dropdown generation."""
import argparse
import json
import os
from pathlib import Path
import re
import urllib.parse
import yaml

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIG = ROOT / 'publishing/repositories.yml'
WORKFLOWS = ('publish-auth.yml', 'finalize-auth.yml')
ID = re.compile(r'[a-z][a-z0-9-]{0,62}')

class UniqueLoader(yaml.SafeLoader):
    pass

def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if not isinstance(key, str) or key in result:
            raise ValueError('Repository configuration has a duplicate or non-string key')
        result[key] = loader.construct_object(value_node, deep=deep)
    return result

UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)

def load_config(path=DEFAULT_CONFIG):
    try:
        document = yaml.load(Path(path).read_text(encoding='utf-8'), Loader=UniqueLoader)
    except yaml.YAMLError as error:
        raise ValueError('Invalid repository YAML') from error
    if not isinstance(document, dict) or set(document) != {'repositories'}:
        raise ValueError('Configuration must contain only a repositories mapping')
    entries = document['repositories']
    if not isinstance(entries, dict) or not entries:
        raise ValueError('Configure at least one repository')
    environments = set()
    for name, entry in entries.items():
        if not ID.fullmatch(name) or not isinstance(entry, dict) or set(entry) != {'environment', 'publisher'}:
            raise ValueError('Each repository needs a safe stable ID, environment and publisher')
        environment = entry['environment']
        if not isinstance(environment, str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,100}', environment):
            raise ValueError('Environment names must use letters, digits, underscores or hyphens')
        if environment.casefold() in environments:
            raise ValueError('Each repository must use its own environment')
        environments.add(environment.casefold())
        if entry['publisher'] not in ('central', 'jitpack'):
            raise ValueError('Only Central and public JitPack publishers are supported')
        if (entry['publisher'] == 'central') != (name == 'maven-central'):
            raise ValueError('Keep the maven-central ID for Central provenance; other IDs use maven')
        if (entry['publisher']=='jitpack') != (name=='jitpack'):raise ValueError('Use the jitpack ID for JitPack')
    if 'maven-central' not in entries:
        raise ValueError('Keep maven-central for existing release history and workflow defaults')
    return entries

def validate_url(value):
    url = urllib.parse.urlsplit(value)
    if (url.scheme != 'https' or not url.hostname or url.username or url.password
            or url.query or url.fragment or re.search(r'[\s<>"`\\]', value)):
        raise ValueError('MAVEN_REPOSITORY_URL must be an HTTPS repository URL without credentials, query or fragment')
    try:
        url.port
    except ValueError as error:
        raise ValueError('Invalid repository URL port') from error
    return value.rstrip('/')

def settings_for(entry):
    if entry['publisher']=='jitpack': return []
    prefix = 'MAVEN_CENTRAL' if entry['publisher'] == 'central' else 'MAVEN_REPOSITORY'
    return [
        {'name': 'MAVEN_REPOSITORY_URL', 'kind': 'variable', 'required': True,
         'default': 'https://repo.maven.apache.org/maven2' if entry['publisher'] == 'central' else ''},
        *[{'name': prefix + suffix, 'kind': 'secret', 'required': True, 'default': ''}
          for suffix in ('_USERNAME', '_PASSWORD')],
        {'name': 'SIGNING_IN_MEMORY_KEY', 'kind': 'secret', 'required': True, 'default': ''},
        {'name': 'SIGNING_IN_MEMORY_KEY_PASSWORD', 'kind': 'secret', 'required': False, 'default': ''},
    ]

def render_dropdown(text, entries):
    pattern = r'(?m)^( +)# BEGIN GENERATED REPOSITORIES\n.*?^\1# END GENERATED REPOSITORIES$'
    replacement = lambda match: (match[1] + '# BEGIN GENERATED REPOSITORIES\n' + match[1]
        + 'options: ' + json.dumps(list(entries)) + '\n' + match[1] + '# END GENERATED REPOSITORIES')
    rendered, count = re.subn(pattern, replacement, text, flags=re.S)
    if count != 1:
        raise ValueError('Expected exactly one repository dropdown marker pair per workflow')
    return rendered

def render_properties(entries):
    lines = ['# Generated from publishing/repositories.yml; run python scripts/publishing_config.py generate.']
    for name, entry in entries.items():
        lines.extend(f'{name}.{field}={entry[field]}' for field in ('environment', 'publisher'))
    return '\n'.join(lines) + '\n'

def generate(check=False, root=ROOT):
    entries = load_config(root / 'publishing/repositories.yml')
    outputs = {root / 'publishing/repositories.properties': render_properties(entries)}
    for name in WORKFLOWS:
        path = root / '.github/workflows' / name
        outputs[path] = render_dropdown(path.read_text(encoding='utf-8'), entries)
    drift = [str(path.relative_to(root)) for path, value in outputs.items()
             if not path.exists() or path.read_text(encoding='utf-8') != value]
    if check and drift:
        raise ValueError('Run python scripts/publishing_config.py generate; outdated files: ' + ', '.join(drift))
    if not check:
        for path, value in outputs.items():
            path.write_text(value, encoding='utf-8')
    return drift

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('generate', 'check', 'resolve', 'preflight'))
    parser.add_argument('--repository', default=os.environ.get('RELEASE_REPOSITORY', 'maven-central'))
    args = parser.parse_args()
    if args.command in ('generate', 'check'):
        generate(check=args.command == 'check')
        return
    entries = load_config()
    if args.repository not in entries:
        parser.error('Unknown repository; add it to publishing/repositories.yml first')
    entry = entries[args.repository]
    if args.command == 'preflight':
        for setting in settings_for(entry):
            if setting['required'] and not os.environ.get(setting['name'], '').strip():
                parser.error('Missing required setting: ' + setting['name'])
        if entry['publisher']!='jitpack': validate_url(os.environ['MAVEN_REPOSITORY_URL'])
        return
    output = '\n'.join(f'{key}={entry[key]}' for key in ('environment', 'publisher')) + '\n'
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as stream:
            stream.write(output)
    else:
        print(output, end='')

if __name__ == '__main__':
    main()
