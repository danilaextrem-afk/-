import struct
import unittest
from repair_normal_tga import repair_bytes, BAD, NEUTRAL

def tga(pixels, width=2, height=2):
    header = struct.pack("<BBBHHBHHHHBB", 0, 0, 2, 0, 0, 0, 0, 0, width, height, 24, 0x20)
    return header + b"".join(pixels)

class RepairTests(unittest.TestCase):
    def test_replaces_only_bad_pixels(self):
        other = bytes((255, 180, 100))
        src = tga([BAD, NEUTRAL, other, BAD])
        fixed, n = repair_bytes(src)
        self.assertEqual(n, 2)
        self.assertEqual(fixed[:18], src[:18])
        self.assertEqual(fixed[18:], NEUTRAL + NEUTRAL + other + NEUTRAL)
    def test_idempotent(self):
        fixed, _ = repair_bytes(tga([BAD]*4))
        self.assertEqual(repair_bytes(fixed), (fixed, 0))
    def test_rejects_truncated_payload(self):
        with self.assertRaises(ValueError): repair_bytes(tga([BAD]*3))
    def test_rejects_wrong_format(self):
        src = bytearray(tga([BAD]*4)); src[2] = 10
        with self.assertRaises(ValueError): repair_bytes(bytes(src))
    def test_rejects_header(self):
        with self.assertRaises(ValueError): repair_bytes(b"bad")

if __name__ == "__main__": unittest.main()
