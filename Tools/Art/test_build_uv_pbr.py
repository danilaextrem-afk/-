#!/usr/bin/env python3
"""Deterministic UV/PBR draft generator contract tests (no UE5 required)."""
from __future__ import annotations
import importlib.util
import struct
import tempfile
import unittest
from pathlib import Path

SAMPLE = """# hand-made two-material triangulated prop
mtllib dummy.mtl
o oak_slab
v 0 0 0
v 1 0 0
v 1 1 0
v 0 1 0
v 0 0 0.1
v 1 0 0.1
v 1 1 0.1
v 0 1 0.1
usemtl oak
f 1 2 3
f 1 3 4
o steel_crest
usemtl steel
f 5 8 7
f 5 7 6
"""

def load_builder():
    source = Path(__file__).resolve().parent / "build_uv_pbr.py"
    spec = importlib.util.spec_from_file_location("razlom_art_builder", source)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

class UVPBRPipelineTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.baker = load_builder()

    def test_triangulated_uv_pbr_atlas_and_header(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "prop.obj"
            source.write_text(SAMPLE, encoding="utf-8")
            result = self.baker.build(source, root / "out", 64)
            self.assertEqual(result["vertices"], 8)
            self.assertEqual(result["triangles"], 4)
            self.assertEqual(result["materials"], ["oak", "steel"])
            self.assertEqual(result["atlas_grid"], [2, 1])
            self.assertGreater(result["uv_vertices"], 0)
            processed = (root / "out" / result["output_obj"]).read_text(encoding="utf-8")
            uv = [line for line in processed.splitlines() if line.startswith("vt ")]
            faces = [line for line in processed.splitlines() if line.startswith("f ")]
            self.assertEqual(len(uv), result["uv_vertices"])
            self.assertEqual(len(faces), 4)
            for line in uv:
                self.assertTrue(all(0.0 <= float(x) <= 1.0 for x in line.split()[1:]))
            self.assertTrue(all(all("/" in point for point in face.split()[1:]) for face in faces))
            for tex_name in result["textures"].values():
                data = (root / "out" / tex_name).read_bytes()
                self.assertEqual(len(data), 18 + 64*64*3)
                header = struct.unpack("<BBBHHBHHHHBB", data[:18])
                self.assertEqual((header[2], header[8], header[9], header[10], header[11]),
                                 (2, 64, 64, 24, 0x20))
            self.assertFalse(result["claims"]["ue5_import_verified"])
            self.assertFalse(result["claims"]["owner_art_approved"])

    def test_rejects_invalid_texture_dimensions(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "prop.obj"
            source.write_text(SAMPLE, encoding="utf-8")
            for size in (0, 70, 8192):
                with self.subTest(size=size):
                    with self.assertRaises(ValueError):
                        self.baker.build(source, root / "out", size)

    def test_rejects_nonexistent_face_vertices(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "bad.obj"
            source.write_text(SAMPLE.replace("f 5 7 6", "f 5 7 700"), encoding="utf-8")
            with self.assertRaises(ValueError):
                self.baker.build(source, root / "out", 64)

    def test_rejects_unrecognized_material_slots(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "bad.obj"
            source.write_text(SAMPLE.replace("usemtl steel", "usemtl mystery"), encoding="utf-8")
            with self.assertRaises(ValueError):
                self.baker.build(source, root / "out", 64)

if __name__ == "__main__":
    unittest.main()
