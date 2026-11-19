// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

import java.util.Locale

val defaultPrefixes: List<String> = listOf("IMG_", "VID_")

/**
 * Return [filename] with its leading camera prefix removed, or `null`
 * if no prefix matched. Port of `stripLeadingPrefix` in
 * `packages/rename_core/lib/src/prefix.dart`, which ports
 * `strip_leading_prefix` in `rename_camera_prefixes.py` (canonical).
 *
 * - Leading position 0 only: `MY_IMG_foo.jpg` does not match.
 * - Case-sensitive by default: `img_foo.jpg` does not match.
 *   Case-insensitive mode uppercases both sides before comparing
 *   (with [Locale.ROOT] — Turkish-proof, same house rule as
 *   `MimeTypes.kt`) and strips by prefix length from the original.
 * - First matching prefix in [prefixes] wins.
 * - The remainder is preserved verbatim (`_HDR`, `_BURST`, extension).
 * - Returns `""` (not `null`) when the strip would leave an empty
 *   name — the caller must skip it as `skippedEmptyResult`.
 * - Returns `null` when no prefix matched.
 */
fun stripLeadingPrefix(
    filename: String,
    prefixes: List<String> = defaultPrefixes,
    caseSensitive: Boolean = true,
): String? {
    val nameCmp = if (caseSensitive) filename else filename.uppercase(Locale.ROOT)
    for (prefix in prefixes) {
        val prefixCmp = if (caseSensitive) prefix else prefix.uppercase(Locale.ROOT)
        if (nameCmp.startsWith(prefixCmp)) {
            val stripped = filename.substring(prefix.length)
            if (stripped.isEmpty()) {
                return ""
            }
            return stripped
        }
    }
    return null
}
