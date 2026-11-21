import tempfile
import unittest
from pathlib import Path
from audit_collision_obj import audit, load_obj

MESH = "v 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 1\nf 1 2 3\nf 1 2 4\nf 1 3 4\nf 2 3 4\n"

class TestCollisionAudit(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.a = Path(self.tmp.name) / "render.obj"
        self.b = Path(self.tmp.name) / "proxy.obj"
        self.a.write_text(MESH)
        self.b.write_text(MESH)

    def test_matching_proxy(self):
        self.assertFalse(audit(self.a, self.b)["proxy_outside_tolerance"])

    def test_excessive_proxy(self):
        self.b.write_text(MESH.replace("v 1 0 0", "v 2 0 0"))
        self.assertTrue(audit(self.a, self.b)["proxy_outside_tolerance"])

    def test_tolerance(self):
        self.b.write_text(MESH.replace("v 1 0 0", "v 1.005 0 0"))
        self.assertFalse(audit(self.a, self.b, .01)["proxy_outside_tolerance"])

    def test_invalid_face(self):
        self.b.write_text(MESH.replace("f 2 3 4", "f 2 3 9"))
        with self.assertRaises(ValueError):
            load_obj(self.b)

    def test_nan_vertex(self):
        self.b.write_text(MESH.replace("v 1 0 0", "v nan 0 0"))
        with self.assertRaises(ValueError):
            load_obj(self.b)

if __name__ == "__main__":
    unittest.main()
