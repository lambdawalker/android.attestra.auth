import tempfile
from pathlib import Path
import json
import unittest
from unittest.mock import patch
import build


class SiteContracts(unittest.TestCase):
    def test_raw_links_stay_in_selected_language_and_scope(self):
        text='[Guide](concepts.md#resources)\n\n```text\n[x](missing.md)\n```\n`[x](missing.md)`'
        out=build.raw_markdown(text,'docs/agents/index.md','es',{'docs/agents/concepts.md':''})
        self.assertIn('/raw/es/main/docs/agents/concepts.md#resources',out)
        self.assertIn('```text\n[x](missing.md)\n```',out)
        self.assertIn('`[x](missing.md)`',out)

    def test_spanish_relative_links_map_to_same_canonical_page(self):
        out=build.resolve('docs/es/agents/index.md','quickstart.md','es',{'docs/agents/quickstart.md':''})
        self.assertIn('/es/main/docs/agents/quickstart.html',out)
        out=build.resolve('docs/es/agents/index.md','../IMPORT.md','es',{'IMPORT.md':''})
        self.assertIn('/es/main/IMPORT.html',out)

    def test_raw_html_images_are_rebased_without_touching_fences(self):
        text='<img src="../assets/screenshots/email-start-light.png" alt="sample">\n```html\n<img src="missing.png">\n```'
        out=build.raw_markdown(text,'docs/agents/index.md','en',{})
        self.assertIn('/assets/docs/assets/screenshots/email-start-light.png',out)
        self.assertIn('<img src="missing.png">',out)

    def test_missing_local_target_fails(self):
        with self.assertRaises(ValueError):build.resolve('README.md','missing.md','en',{})

    def test_source_and_agent_navigation_are_distinct(self):
        out=build.raw_markdown('[x](https://github.com/a/b/blob/main/docs/index.md)','README.md','en',{})
        self.assertIn('https://raw.githubusercontent.com/a/b/main/docs/index.md',out)

    def test_stale_translation_falls_back_to_same_english_text(self):
        english='# English\n\nCurrent contract.'
        text,missing=build.translated('missing-guide.md',english,'es')
        self.assertTrue(missing);self.assertEqual(english,text)

    def test_fresh_translations_preserve_code_fences(self):
        docs=build.sources()
        manifest=json.loads((build.ROOT/'sites/translations.json').read_text())
        self.assertGreaterEqual(len(manifest),10)
        for path in manifest:
            text,fallback=build.translated(path,docs[path],'es')
            self.assertFalse(fallback,path)
            self.assertNotEqual(text,docs[path])

    def test_no_fictional_stable_release(self):
        catalog=json.loads((build.ROOT/'sites/versions.json').read_text())
        self.assertEqual([],catalog['releases'])
        self.assertEqual('development',catalog['channels'][0]['kind'])

    def test_unknown_release_cannot_silently_render_main(self):
        catalog=json.loads((build.ROOT/'sites/versions.json').read_text())
        catalog['releases']=[{'version':'1.0.0'}]
        with self.assertRaises(ValueError):build.validate_catalog(catalog)

    def test_stale_real_translation_keeps_current_canonical_scope(self):
        path='docs/agents/api.md';english=build.sources()[path]
        with patch.object(build,'sha',return_value='not-the-reviewed-hash'):
            text,fallback=build.translated(path,english,'es')
        self.assertTrue(fallback);self.assertEqual(english,text)


if __name__=='__main__':unittest.main()
