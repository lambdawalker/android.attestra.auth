import json
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import suite


def png(width=2, height=3):
    return b'\x89PNG\r\n\x1a\n' + b'\x00\x00\x00\x0dIHDR' + struct.pack('>II', width, height)


class ScreenshotContracts(unittest.TestCase):
    def test_invalid_or_blank_image_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp)/'x.png'; p.write_bytes(b'not png')
            with self.assertRaises(ValueError): suite.png_info(p)
            p.write_bytes(png(0, 1))
            with self.assertRaises(ValueError): suite.png_info(p)

    def test_candidate_does_not_allow_image_or_input_drift(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); (root/'auth').mkdir(); image=root/'auth/x.png'; image.write_bytes(png())
            data={'inputs':{'sha256':'expected'},'images':[dict(path='auth/x.png',**suite.png_info(image))]}
            with patch.object(suite,'render_inputs',return_value={'sha256':'expected'}):
                suite.validate_manifest(root,data)
                image.write_bytes(png(4,5))
                with self.assertRaises(ValueError):suite.validate_manifest(root,data)
            with patch.object(suite,'render_inputs',return_value={'sha256':'changed'}):
                with self.assertRaises(ValueError):suite.validate_manifest(root,data)

    def test_review_is_required_before_baseline_acceptance(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);(root/'manifest.json').write_text(json.dumps({'status':'candidates-not-approved'}))
            with self.assertRaises(ValueError):suite.accept(root,'')

    def test_restore_without_approval_does_not_generate_references(self):
        with patch.object(suite,'ACCEPTED',Path('/nonexistent/attestra-baseline.json')):
            with self.assertRaises(ValueError):suite.restore()

    def test_missing_required_variant_and_unknown_scenario_fail(self):
        inventory=[{'id':'auth:Fixture.screen','variants':2}]
        with self.assertRaises(ValueError):suite.validate_variants([],inventory)
        with self.assertRaises(ValueError):suite.validate_variants([{'path':'auth/a.png','scenario':'auth:Fixture.screen'}],inventory)
        with self.assertRaises(ValueError):suite.validate_variants([{'path':'auth/a.png','scenario':'unknown'}],inventory)
        suite.validate_variants([{'path':'auth/a.png','scenario':'auth:Fixture.screen'},{'path':'auth/b.png','scenario':'auth:Fixture.screen'}],inventory)

    def test_composable_inventory_and_scenarios_are_complete(self):
        self.assertGreater(len(suite.coverage()), 0)


if __name__=='__main__':unittest.main()
