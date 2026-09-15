#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""make_fixtures.py — generate tiny synthetic device-test fixtures.

Pure standard library (struct, zlib, binascii). Writes:
  app/assets/fixtures/ve_exif.jpg   (~350B JPEG, hand-built EXIF)
  app/assets/fixtures/ve_plain.png  (16x16 RGB, no EXIF)
  app/assets/fixtures/ve_minimal.mp4 (ftyp+moov/mvhd+mdat, no tracks)

Every blob is self-verified by re-parsing before writing. Re-run any time:
  C:\\venv-hub\\venv\\Scripts\\python.exe tools\\make_fixtures.py
"""

from __future__ import annotations

import binascii
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "assets" / "fixtures"

MAKE = b"SYNTHETIC\x00"
MODEL = b"SAMPLE CAM\x00"
DATETIME = b"2026:09:14 11:05:23\x00"


def _entry(tag: int, typ: int, count: int, value: bytes) -> bytes:
    assert len(value) == 4
    return struct.pack("<HHI4s", tag, typ, count, value)


def build_jpeg() -> bytes:
    dt = bytes(DATETIME)
    exp = struct.pack("<II", 1, 120)
    lat = struct.pack("<IIIIII", 44, 1, 58, 1, 4008, 100)
    lon = struct.pack("<IIIIII", 93, 1, 15, 1, 54, 1)
    ifd0_size = 2 + 12 * 5 + 4
    exif_size = 2 + 12 * 4 + 4
    gps_size = 2 + 12 * 5 + 4
    make_off = 8 + ifd0_size
    model_off = make_off + len(MAKE)
    exif_off = model_off + len(MODEL)
    dt_off = exif_off + exif_size
    exp_off = dt_off + len(dt)
    gps_off = exp_off + len(exp)
    lat_off = gps_off + gps_size
    lon_off = lat_off + len(lat)

    ifd0 = struct.pack("<H", 5)
    ifd0 += _entry(0x010F, 2, len(MAKE), struct.pack("<I", make_off))
    ifd0 += _entry(0x0110, 2, len(MODEL), struct.pack("<I", model_off))
    ifd0 += _entry(0x0112, 3, 1, struct.pack("<HH", 6, 0))
    ifd0 += _entry(0x8769, 4, 1, struct.pack("<I", exif_off))
    ifd0 += _entry(0x8825, 4, 1, struct.pack("<I", gps_off))
    ifd0 += struct.pack("<I", 0)

    exif = struct.pack("<H", 4)
    exif += _entry(0x9003, 2, len(dt), struct.pack("<I", dt_off))
    exif += _entry(0x829A, 5, 1, struct.pack("<I", exp_off))
    exif += _entry(0xA002, 4, 1, struct.pack("<I", 3000))
    exif += _entry(0xA003, 4, 1, struct.pack("<I", 4000))
    exif += struct.pack("<I", 0)

    gps = struct.pack("<H", 5)
    gps += _entry(0x0000, 1, 4, bytes((2, 3, 0, 0)))
    gps += _entry(0x0001, 2, 2, b"N\x00\x00\x00")
    gps += _entry(0x0002, 5, 3, struct.pack("<I", lat_off))
    gps += _entry(0x0003, 2, 2, b"W\x00\x00\x00")
    gps += _entry(0x0004, 5, 3, struct.pack("<I", lon_off))
    gps += struct.pack("<I", 0)

    tiff = b"II" + struct.pack("<HI", 42, 8) + ifd0
    assert len(tiff) == make_off
    tiff += bytes(MAKE) + bytes(MODEL)
    assert len(tiff) == exif_off
    tiff += exif + dt + exp
    assert len(tiff) == gps_off
    tiff += gps + lat + lon
    app1 = b"Exif\x00\x00" + tiff
    sof0 = (
        b"\xff\xc0"
        + struct.pack(">H", 17)
        + bytes((8,))
        + struct.pack(">HH", 4000, 3000)
        + bytes((3, 1, 0x22, 0, 2, 0x11, 1, 3, 0x11, 1))
    )
    return (
        b"\xff\xd8"
        + b"\xff\xe1"
        + struct.pack(">H", len(app1) + 2)
        + app1
        + sof0
        + b"\xff\xd9"
    )


def _ifd_map(tiff: bytes, ifd_off: int) -> dict[int, int]:
    (count,) = struct.unpack_from("<H", tiff, ifd_off)
    out: dict[int, int] = {}
    for i in range(count):
        off = ifd_off + 2 + 12 * i
        (tag,) = struct.unpack_from("<H", tiff, off)
        out[tag] = off
    return out


def _ascii(tiff: bytes, entry_off: int) -> bytes:
    typ, count, val = struct.unpack_from("<HI4s", tiff, entry_off + 2)
    assert typ == 2
    if count <= 4:
        return val[:count]
    (off,) = struct.unpack("<I", val)
    return tiff[off : off + count]


def _rational(tiff: bytes, entry_off: int, index: int) -> float:
    (off,) = struct.unpack_from("<I", tiff, entry_off + 8)
    num, den = struct.unpack_from("<II", tiff, off + 8 * index)
    return num / den


def verify_jpeg(blob: bytes) -> None:
    assert blob[:2] == b"\xff\xd8" and blob[-2:] == b"\xff\xd9"
    pos = 2
    seen = set()
    sof_dims: tuple[int, int] | None = None
    while pos < len(blob) - 1:
        assert blob[pos] == 0xFF
        marker = blob[pos + 1]
        seen.add(marker)
        if marker == 0xD9:
            break
        (seg_len,) = struct.unpack_from(">H", blob, pos + 2)
        if marker == 0xC0:
            (prec, height, width) = struct.unpack_from(">BHH", blob, pos + 4)
            assert prec == 8
            sof_dims = (width, height)
        pos += 2 + seg_len
    assert 0xE1 in seen and sof_dims == (3000, 4000)
    assert blob[2:4] == b"\xff\xe1"
    (seg_len,) = struct.unpack_from(">H", blob, 4)
    app1 = blob[6 : 4 + seg_len]
    assert app1[:6] == b"Exif\x00\x00"
    tiff = app1[6:]
    assert tiff[:4] == b"II*\x00"
    (ifd0_off,) = struct.unpack_from("<I", tiff, 4)
    tags = _ifd_map(tiff, ifd0_off)
    assert set(tags) == {0x010F, 0x0110, 0x0112, 0x8769, 0x8825}
    assert _ascii(tiff, tags[0x010F]) == MAKE
    assert _ascii(tiff, tags[0x0110]) == MODEL
    (orient,) = struct.unpack_from("<H", tiff, tags[0x0112] + 8)
    assert orient == 6
    (exif_off,) = struct.unpack_from("<I", tiff, tags[0x8769] + 8)
    emap = _ifd_map(tiff, exif_off)
    assert set(emap) == {0x9003, 0x829A, 0xA002, 0xA003}
    assert _ascii(tiff, emap[0x9003]) == DATETIME
    assert _rational(tiff, emap[0x829A], 0) == 1 / 120
    (px,) = struct.unpack_from("<I", tiff, emap[0xA002] + 8)
    (py,) = struct.unpack_from("<I", tiff, emap[0xA003] + 8)
    assert (px, py) == (3000, 4000)
    (gps_off,) = struct.unpack_from("<I", tiff, tags[0x8825] + 8)
    gmap = _ifd_map(tiff, gps_off)
    assert _ascii(tiff, gmap[0x0001]) == b"N\x00"
    assert _ascii(tiff, gmap[0x0003]) == b"W\x00"
    lat = (
        _rational(tiff, gmap[0x0002], 0)
        + _rational(tiff, gmap[0x0002], 1) / 60
        + _rational(tiff, gmap[0x0002], 2) / 3600
    )
    lon = (
        _rational(tiff, gmap[0x0004], 0)
        + _rational(tiff, gmap[0x0004], 1) / 60
        + _rational(tiff, gmap[0x0004], 2) / 3600
    )
    assert abs(lat - 44.9778) < 0.0001
    assert abs(lon - 93.2650) < 0.0001


def _chunk(typ: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + typ + data + struct.pack(
        ">I", binascii.crc32(typ + data) & 0xFFFFFFFF
    )


def build_png() -> bytes:
    raw = bytearray()
    for y in range(16):
        raw.append(0)
        for x in range(16):
            raw.extend((x * 16, y * 16, 128))
    ihdr = struct.pack(">IIBBBBB", 16, 16, 8, 2, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + _chunk(b"IHDR", ihdr)
        + _chunk(b"IDAT", zlib.compress(bytes(raw)))
        + _chunk(b"IEND", b"")
    )


def verify_png(blob: bytes) -> None:
    assert blob[:8] == b"\x89PNG\r\n\x1a\n"
    pos, seen = 8, set()
    while pos < len(blob):
        (length,) = struct.unpack_from(">I", blob, pos)
        typ = blob[pos + 4 : pos + 8]
        data = blob[pos + 8 : pos + 8 + length]
        (crc,) = struct.unpack_from(">I", blob, pos + 8 + length)
        assert binascii.crc32(typ + data) & 0xFFFFFFFF == crc
        seen.add(typ)
        if typ == b"IHDR":
            (w, h) = struct.unpack_from(">II", data, 0)
            assert (w, h) == (16, 16)
        if typ == b"IDAT":
            raw = zlib.decompress(data)
            assert len(raw) == 16 * (1 + 16 * 3)
        pos += 12 + length
    assert seen == {b"IHDR", b"IDAT", b"IEND"}


def _box(typ: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", 8 + len(payload)) + typ + payload


def build_mp4() -> bytes:
    ftyp = _box(b"ftyp", b"isom" + struct.pack(">I", 0) + b"isom")
    mvhd = struct.pack(">I", 0)
    mvhd += struct.pack(">II", 0, 0)
    mvhd += struct.pack(">I", 1000)
    mvhd += struct.pack(">I", 8340)
    mvhd += struct.pack(">I", 0x00010000)
    mvhd += struct.pack(">H", 0x0100) + b"\x00\x00" + b"\x00" * 10
    mvhd += struct.pack(">9I", *([0] * 9))
    mvhd += b"\x00" * 24 + struct.pack(">I", 2)
    moov = _box(b"moov", _box(b"mvhd", mvhd))
    mdat = _box(b"mdat", b"\x00\x00\x00\x00")
    return ftyp + moov + mdat


def verify_mp4(blob: bytes) -> None:
    boxes: dict[bytes, bytes] = {}
    pos = 0
    while pos < len(blob):
        (size,) = struct.unpack_from(">I", blob, pos)
        typ = blob[pos + 4 : pos + 8]
        boxes.setdefault(typ, blob[pos + 8 : pos + size])
        pos += size
    assert boxes[b"ftyp"][:4] == b"isom"
    assert set(boxes) == {b"ftyp", b"moov", b"mdat"}
    mvhd = boxes[b"moov"][8:]
    (timescale, duration) = struct.unpack_from(">II", mvhd, 12)
    assert (timescale, duration) == (1000, 8340)


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    jpeg, png, mp4 = build_jpeg(), build_png(), build_mp4()
    verify_jpeg(jpeg)
    verify_png(png)
    verify_mp4(mp4)
    (OUT / "ve_exif.jpg").write_bytes(jpeg)
    (OUT / "ve_plain.png").write_bytes(png)
    (OUT / "ve_minimal.mp4").write_bytes(mp4)
    print(f"fixtures: jpg={len(jpeg)}B png={len(png)}B mp4={len(mp4)}B -> {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
