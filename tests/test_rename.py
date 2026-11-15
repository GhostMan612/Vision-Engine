#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""Tests for rename_camera_prefixes.py and rename_min.py.

Synthetic files only, all in tmp_path. Covers the four audit bugs:
chain renames, dry-run manifest honesty, symlink skip, and exit codes,
plus the rename_min duplicate / chain / missing-dir fixes.
Run: pytest -q tests/
"""

import csv
import importlib.util
import os
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import rename_camera_prefixes as rcp


def _load_min():
    spec = importlib.util.spec_from_file_location("rename_min", ROOT / "rename_min.py")
    mod = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = mod
    spec.loader.exec_module(mod)
    return mod


def _seed(target, names):
    for name in names:
        (target / name).write_bytes(b"SYNTHETIC:" + name.encode())


def _names(target):
    return sorted(p.name for p in target.iterdir() if p.is_file() and not p.is_symlink())


def _read_manifest(path):
    with Path(path).open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


# --- strip ---

def test_strip_leading_only():
    assert rcp.strip_leading_prefix("IMG_foo.jpg") == "foo.jpg"
    assert rcp.strip_leading_prefix("VID_foo.mp4") == "foo.mp4"
    assert rcp.strip_leading_prefix("MY_IMG_foo.jpg") is None
    assert rcp.strip_leading_prefix("img_foo.jpg") is None
    assert rcp.strip_leading_prefix("img_foo.jpg", case_sensitive=False) == "foo.jpg"
    assert rcp.strip_leading_prefix("IMG_") == ""
    assert rcp.strip_leading_prefix("IMG_IMG_x.jpg") == "IMG_x.jpg"


def test_strip_preserves_suffix_verbatim():
    assert rcp.strip_leading_prefix("IMG_20260905_110523079_HDR.jpg") == "20260905_110523079_HDR.jpg"


# --- plan ---

def test_plan_collision_trio(tmp_path):
    _seed(tmp_path, ["IMG_A.jpg", "A.jpg", "IMG_B.jpg", "VID_B.jpg"])
    report = rcp.build_plan(tmp_path)
    assert [op.new_name for op in report.planned] == ["B.jpg"]
    assert report.skipped_collision
    assert any("IMG_A.jpg" in c for c in report.skipped_collision)
    assert any("duplicate target" in c for c in report.skipped_collision)


def test_plan_chain_lists_both(tmp_path):
    _seed(tmp_path, ["IMG_VID_a.jpg", "VID_a.jpg"])
    report = rcp.build_plan(tmp_path)
    assert len(report.planned) == 2
    assert not report.skipped_collision


def test_plan_skips_symlink_to_file(tmp_path):
    target = tmp_path / "real.jpg"
    target.write_bytes(b"real")
    link = tmp_path / "IMG_link.jpg"
    try:
        os.symlink(target, link)
    except (OSError, NotImplementedError):
        pytest.skip("symlinks unavailable here")
    _seed(tmp_path, ["IMG_ok.jpg"])
    report = rcp.build_plan(tmp_path)
    assert report.skipped_symlink == 1
    assert [op.old_name for op in report.planned] == ["IMG_ok.jpg"]


def test_plan_skips_empty_result(tmp_path):
    _seed(tmp_path, ["IMG_", "VID_", "IMG_ok.jpg"])
    report = rcp.build_plan(tmp_path)
    assert report.skipped_empty_result == 2
    assert len(report.planned) == 1


# --- execute: chain ---

def test_execute_chain_renames_both(tmp_path):
    _seed(tmp_path, ["IMG_VID_a.jpg", "VID_a.jpg"])
    code = rcp.main([str(tmp_path), "--execute"])
    assert code == 0
    assert _names(tmp_path) == ["VID_a.jpg", "a.jpg"]
    assert (tmp_path / "a.jpg").read_bytes() == b"SYNTHETIC:VID_a.jpg"
    assert (tmp_path / "VID_a.jpg").read_bytes() == b"SYNTHETIC:IMG_VID_a.jpg"


def test_execute_partial_collision_returns_2(tmp_path):
    _seed(tmp_path, ["IMG_A.jpg", "A.jpg", "IMG_B.jpg"])
    code = rcp.main([str(tmp_path), "--execute"])
    assert code == 2
    assert (tmp_path / "B.jpg").exists()
    assert (tmp_path / "IMG_A.jpg").exists()
    assert (tmp_path / "A.jpg").exists()


def test_dry_run_changes_nothing_and_returns_0_when_clean(tmp_path):
    _seed(tmp_path, ["IMG_a.jpg", "VID_b.mp4", "keep.jpg"])
    code = rcp.main([str(tmp_path)])
    assert code == 0
    assert _names(tmp_path) == ["IMG_a.jpg", "VID_b.mp4", "keep.jpg"]


def test_dry_run_with_collision_returns_2(tmp_path):
    _seed(tmp_path, ["IMG_A.jpg", "A.jpg"])
    assert rcp.main([str(tmp_path)]) == 2
    assert (tmp_path / "IMG_A.jpg").exists()


# --- manifest honesty + undo ---

def test_dry_run_manifest_says_planned(tmp_path):
    _seed(tmp_path, ["IMG_a.jpg"])
    manifest = tmp_path / "dry.csv"
    assert rcp.main([str(tmp_path), "--manifest", str(manifest)]) == 0
    rows = _read_manifest(manifest)
    assert [row["status"] for row in rows] == ["planned"]
    assert (tmp_path / "IMG_a.jpg").exists()


def test_execute_manifest_says_renamed_and_undo_restores(tmp_path):
    _seed(tmp_path, ["IMG_a.jpg", "VID_b.mp4"])
    outdir = tmp_path / "manifests"
    outdir.mkdir()
    manifest = outdir / "run.csv"
    assert rcp.main([str(tmp_path), "--execute", "--manifest", str(manifest)]) == 0
    rows = _read_manifest(manifest)
    assert [row["status"] for row in rows] == ["renamed", "renamed"]
    assert _names(tmp_path) == ["a.jpg", "b.mp4"]
    assert rcp.main([str(tmp_path), "--undo", str(manifest), "--execute"]) == 0
    assert _names(tmp_path) == ["IMG_a.jpg", "VID_b.mp4"]


def test_undo_of_dry_run_manifest_restores_nothing(tmp_path):
    _seed(tmp_path, ["IMG_a.jpg"])
    manifest = tmp_path / "dry.csv"
    rcp.main([str(tmp_path), "--manifest", str(manifest)])
    restored, errors = rcp.undo_from_manifest(manifest, rcp.setup_logging(False, None), dry_run=False)
    assert restored == 0
    assert errors
    assert (tmp_path / "IMG_a.jpg").exists()


def test_missing_dir_returns_1(tmp_path):
    assert rcp.main([str(tmp_path / "nope")]) == 1


# --- rename_min ---

def test_min_duplicate_target_skipped(tmp_path):
    mod = _load_min()
    _seed(tmp_path, ["IMG_A.jpg", "VID_A.jpg"])
    assert mod.main([str(tmp_path), "--execute"]) == 2
    assert (tmp_path / "A.jpg").exists()
    assert (tmp_path / "VID_A.jpg").exists()


def test_min_chain_renames_both(tmp_path):
    mod = _load_min()
    _seed(tmp_path, ["IMG_VID_a.jpg", "VID_a.jpg"])
    assert mod.main([str(tmp_path), "--execute"]) == 0
    assert _names(tmp_path) == ["VID_a.jpg", "a.jpg"]


def test_min_missing_dir_returns_1(tmp_path):
    mod = _load_min()
    assert mod.main([str(tmp_path / "nope")]) == 1


def test_min_dry_run_changes_nothing(tmp_path):
    mod = _load_min()
    _seed(tmp_path, ["IMG_a.jpg"])
    assert mod.main([str(tmp_path)]) == 0
    assert (tmp_path / "IMG_a.jpg").exists()
