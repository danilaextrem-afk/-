#!/usr/bin/env python3
"""Synthetic TGA regression checks for invalid tangent-space normals."""
import tempfile
import unittest
from pathlib import Path
from audit_normal_tga import audit_normal_tga
from build_uv_pbr import write_tga

class NormalAuditTest(unittest.TestCase):
    def test_valid_neutral_normal(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/"valid.tga"
            write_tga(p, 2, bytes((128,128,255))*4)
            self.assertTrue(audit_normal_tga(p)["pass"])

    def test_dark_block_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/"invalid.tga"
            write_tga(p, 2, bytes((4,4,4)) + bytes((128,128,255))*3)
            self.assertEqual(audit_normal_tga(p)["invalid_normal_pixels"], 1)

    def test_truncation_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/"broken.tga"
            p.write_bytes(b"abc")
            with self.assertRaises(ValueError):
                audit_normal_tga(p)

if __name__ == "__main__":
    unittest.main()
