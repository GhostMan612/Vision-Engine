// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.debugoverlay

/**
 * Default checklist (MP8/B160V smoke). This list is the tool's item
 * set — extend/replace per repo to make it global (README.md).
 */
val DefaultItems: List<ChecklistItem> = listOf(
    ChecklistItem(
        id = "launch_offline",
        label = "App launches while airplane mode is on",
        description = "Main app process starts and holds without crashing while airplane mode is on (auto).",
        auto = true,
    ),
    ChecklistItem(
        id = "focus_offline",
        label = "App has foreground focus",
        description = "After the launch check, the main app is the top/resumed activity (auto).",
        auto = true,
    ),
    ChecklistItem(
        id = "viewer_grid_detail",
        label = "Viewer grid renders and detail opens",
        description = "Open the Viewer tab: the 3-column grid of thumbs renders, and tapping a row opens the detail fields (human).",
    ),
    ChecklistItem(
        id = "renamer_preview_execute",
        label = "Renamer preview -> execute -> undo works",
        description = "Preview, execute and undo through the Renamer screen on a real folder (human).",
    ),
    ChecklistItem(
        id = "realshot_dims",
        label = "Real-shot dims are orientation-corrected",
        description = "For each real device-camera shot, the reported dimensions match the raw file's corrected dims (human).",
    ),
    ChecklistItem(
        id = "realshot_unknowns",
        label = "Unknown values render as —",
        description = "Fields with no EXIF/container data render as the em dash — (human).",
    ),
    ChecklistItem(
        id = "realshot_provenance",
        label = "Provenance chips show the source of each field",
        description = "Each detail row shows its provenance chip (EXIF / container / filesystem) (human).",
    ),
    ChecklistItem(
        id = "realshot_thumb",
        label = "Thumbs are correct",
        description = "Photo thumbs ~256px (JPEG-85), video frames <= 512px, JPEG bytes of untouched sources unchanged (human).",
    ),
)
