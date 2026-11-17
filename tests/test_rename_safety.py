#!/usr/bin/env python3
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
"""Synthetic-only regression tests: never use personal media or external dirs."""
from __future__ import annotations

import csv
import logging
import os
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import rename_camera_prefixes as renamer

LOGGER = logging.getLogger("vision-test")


def seed(folder: Path, names: dict[str, bytes]) -> None:
    for name, blob in names.items():
        (folder / name).write_bytes(blob)


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def test_dryrun_manifest_is_planned_and_undo_ignores_it(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_001.jpg": b"image"})
    report = renamer.build_plan(tmp_path)
    manifest = tmp_path / "preview.csv"
    renamer.write_manifest(report, manifest)
    assert read_rows(manifest)[0]["status"] == "planned"
    count, errors = renamer.undo_from_manifest(manifest, LOGGER, dry_run=False)
    assert (count, errors) == (0, [])
    assert (tmp_path / "IMG_001.jpg").read_bytes() == b"image"
    assert not (tmp_path / "001.jpg").exists()


def test_manifest_tracks_actual_execution_and_undo(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_ok.jpg": b"A", "VID_b.jpg": b"B"})
    report = renamer.build_plan(tmp_path)
    renamer.execute_plan(report, LOGGER)
    manifest = tmp_path / "actual.csv"
    renamer.write_manifest(report, manifest)
    assert {r["status"] for r in read_rows(manifest)} == {"renamed"}
    count, errors = renamer.undo_from_manifest(manifest, LOGGER, dry_run=False)
    assert (count, errors) == (2, [])
    assert (tmp_path / "IMG_ok.jpg").read_bytes() == b"A"
    assert (tmp_path / "VID_b.jpg").read_bytes() == b"B"


def test_chain_execution_vacates_destination_first_and_preserves_bytes(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_VID_a.jpg": b"first", "VID_a.jpg": b"second"})
    report = renamer.build_plan(tmp_path)
    assert len(report.planned) == 2
    renamer.execute_plan(report, LOGGER)
    assert report.renamed == 2
    assert (tmp_path / "VID_a.jpg").read_bytes() == b"first"
    assert (tmp_path / "a.jpg").read_bytes() == b"second"
    manifest = tmp_path / "chain.csv"
    renamer.write_manifest(report, manifest)
    restored, errors = renamer.undo_from_manifest(manifest, LOGGER, dry_run=False)
    # Reverse manifest/source-name order is NOT inherently dependency-safe;
    # this assertion pins that requirement rather than silently accepting
    # accidental success in one ordering.
    assert restored == 2, errors
    assert (tmp_path / "IMG_VID_a.jpg").read_bytes() == b"first"
    assert (tmp_path / "VID_a.jpg").read_bytes() == b"second"


def test_destination_appears_after_plan_does_not_overwrite(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"source"})
    report = renamer.build_plan(tmp_path)
    (tmp_path / "a.jpg").write_bytes(b"outside")
    renamer.execute_plan(report, LOGGER)
    assert report.renamed == 0
    assert report.planned[0].status == "skipped"
    assert (tmp_path / "a.jpg").read_bytes() == b"outside"
    assert (tmp_path / "IMG_a.jpg").read_bytes() == b"source"


def test_execute_status_tracks_partial_result(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"A", "IMG_b.jpg": b"B"})
    report = renamer.build_plan(tmp_path)
    (tmp_path / "a.jpg").write_bytes(b"EXTERNAL")
    renamer.execute_plan(report, LOGGER)
    manifest = tmp_path / "mixed.csv"
    renamer.write_manifest(report, manifest)
    rows = {row["original_name"]: row["status"] for row in read_rows(manifest)}
    assert rows == {"IMG_a.jpg": "skipped", "IMG_b.jpg": "renamed"}
    assert (tmp_path / "a.jpg").read_bytes() == b"EXTERNAL"
    count, errors = renamer.undo_from_manifest(manifest, LOGGER, dry_run=False)
    assert count == 1 and not errors
    assert (tmp_path / "IMG_b.jpg").read_bytes() == b"B"


def test_undo_does_not_overwrite_preexisting_source(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"source"})
    report = renamer.build_plan(tmp_path)
    renamer.execute_plan(report, LOGGER)
    manifest = tmp_path / "undo.csv"
    renamer.write_manifest(report, manifest)
    (tmp_path / "IMG_a.jpg").write_bytes(b"unrelated")
    count, errors = renamer.undo_from_manifest(manifest, LOGGER, dry_run=False)
    assert count == 0 and errors
    assert (tmp_path / "IMG_a.jpg").read_bytes() == b"unrelated"
    assert (tmp_path / "a.jpg").read_bytes() == b"source"


def test_exclusive_manifest_does_not_overwrite(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"source", "existing.csv": b"keep"})
    report = renamer.build_plan(tmp_path)
    with pytest.raises(FileExistsError):
        renamer.write_manifest(report, tmp_path / "existing.csv")
    assert (tmp_path / "existing.csv").read_bytes() == b"keep"


def test_cli_refuses_source_as_manifest(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"source"})
    result = renamer.main([str(tmp_path), "--execute", "--manifest", str(tmp_path / "IMG_a.jpg")])
    assert result == 2
    assert (tmp_path / "IMG_a.jpg").read_bytes() == b"source"


def test_cli_partial_execution_exits_nonzero(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"source"})
    def planned_collision(report, logger):
        (tmp_path / "a.jpg").write_bytes(b"outside")
        return original_execute(report, logger)
    original_execute = renamer.execute_plan
    monkeypatch.setattr(renamer, "execute_plan", planned_collision)
    result = renamer.main([str(tmp_path), "--execute"])
    assert result == 2
    assert (tmp_path / "a.jpg").read_bytes() == b"outside"


@pytest.mark.skipif(os.name == "nt" or not hasattr(os, "symlink"), reason="POSIX symlink test")
def test_symlinks_are_not_sources_or_destinations(tmp_path: Path) -> None:
    seed(tmp_path, {"real.jpg": b"real", "IMG_b.jpg": b"B"})
    (tmp_path / "IMG_link.jpg").symlink_to(tmp_path / "real.jpg")
    (tmp_path / "IMG_dangling.jpg").symlink_to(tmp_path / "missing")
    (tmp_path / "b.jpg").symlink_to(tmp_path / "missing-dest")
    report = renamer.build_plan(tmp_path)
    assert report.skipped_unsupported == 3
    assert report.skipped_no_prefix == 1
    assert not report.planned
    assert (tmp_path / "IMG_link.jpg").is_symlink()
    assert (tmp_path / "IMG_dangling.jpg").is_symlink()
    assert (tmp_path / "b.jpg").is_symlink()


@pytest.mark.skipif(os.name == "nt", reason="POSIX no-clobber primitive")
def test_destination_race_cannot_replace_existing_file(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"A"})
    report = renamer.build_plan(tmp_path)
    original_link = renamer.os.link
    def racing_link(src, dst, **kwargs):
        Path(dst).write_bytes(b"outside")
        return original_link(src, dst, **kwargs)
    monkeypatch.setattr(renamer.os, "link", racing_link)
    renamer.execute_plan(report, LOGGER)
    assert report.planned[0].status == "skipped"
    assert (tmp_path / "a.jpg").read_bytes() == b"outside"
    assert (tmp_path / "IMG_a.jpg").read_bytes() == b"A"


def test_chain_undo_dryrun_reports_both_restorations(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_VID_a.jpg": b"first", "VID_a.jpg": b"second"})
    report = renamer.build_plan(tmp_path)
    renamer.execute_plan(report, LOGGER)
    manifest = tmp_path / "chain_preview.csv"
    renamer.write_manifest(report, manifest)
    restored, errors = renamer.undo_from_manifest(manifest, LOGGER, dry_run=True)
    assert (restored, errors) == (2, [])
    assert (tmp_path / "VID_a.jpg").read_bytes() == b"first"
    assert (tmp_path / "a.jpg").read_bytes() == b"second"


def test_cli_no_runnable_plan_reports_collision(tmp_path: Path) -> None:
    seed(tmp_path, {"IMG_a.jpg": b"first", "a.jpg": b"second"})
    result = renamer.main([str(tmp_path), "--execute"])
    assert result == 2
    assert (tmp_path / "IMG_a.jpg").read_bytes() == b"first"
    assert (tmp_path / "a.jpg").read_bytes() == b"second"
