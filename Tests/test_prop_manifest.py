import json
import subprocess
import sys
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory


class PropManifestTest(unittest.TestCase):
    def test_manifest_inventory(self):
        source = Path(__file__).resolve().parents[1] / "Tools/validate_original_props.py"
        with TemporaryDirectory() as temp:
            root = Path(temp)
            script = root / "Tools" / source.name
            script.parent.mkdir()
            script.write_bytes(source.read_bytes())
            manifest = root / "Content/Art/OriginalProps/asset_manifest.json"
            manifest.parent.mkdir(parents=True)
            (root / "triangle.obj").write_text(
                "v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nusemtl stone\nf 1/1 2/1 3/1\n"
            )
            asset = dict(id="triangle", file="triangle.obj", vertices=3, triangles=1)
            for count, entries, valid in [(4, [], False), (0, [], False),
                                          (2, [asset], False), (1, [asset], True)]:
                with self.subTest(count=count, entries=len(entries)):
                    manifest.write_text(json.dumps(dict(number_of_assets=count, artifacts=entries)))
                    result = subprocess.run([sys.executable, str(script)], capture_output=True)
                    self.assertEqual(result.returncode == 0, valid, result.stderr)


if __name__ == "__main__":
    unittest.main()
