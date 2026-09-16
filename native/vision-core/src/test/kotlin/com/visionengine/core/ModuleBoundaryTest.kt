// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

val forbiddenImportPrefixes = listOf(
    "android.",
    "androidx.",
    "com.android.",
    "io.flutter.",
)

fun findForbiddenImports(sources: Map<String, String>): List<String> {
    val hits = mutableListOf<String>()
    for ((path, content) in sources) {
        var inBlock = false
        for ((index, raw) in content.lines().withIndex()) {
            var line = raw.trim()
            if (!inBlock) {
                val open = line.indexOf("/*")
                if (open >= 0) {
                    val close = line.indexOf("*/", open + 2)
                    line = if (close >= 0) {
                        line.removeRange(open, close + 2)
                    } else {
                        inBlock = true
                        line.substring(0, open)
                    }
                }
            } else {
                val close = line.indexOf("*/")
                if (close < 0) {
                    continue
                }
                inBlock = false
                line = line.substring(close + 2)
            }
            val code = line.trim()
            if (code.startsWith("//") || code.startsWith("*") || code.startsWith("#")) {
                continue
            }
            if (!code.startsWith("import ")) {
                continue
            }
            val target = code.removePrefix("import ").trim().trimEnd(';')
            if (forbiddenImportPrefixes.any { target.startsWith(it) }) {
                hits.add("$path:${index + 1}:$target")
            }
        }
    }
    return hits
}

class ModuleBoundaryTest {
    @Test
    fun flagsForbiddenImports() {
        val files = mapOf(
            "Clean.kt" to "package a\n\nimport java.io.File\n",
            "Bad.kt" to "package a\n\nimport android.os.Build\n",
            "Commented.kt" to "package a\n\n// import android.os.Build\n",
        )
        assertEquals(
            listOf("Bad.kt:3:android.os.Build"),
            findForbiddenImports(files),
        )
    }

    @Test
    fun mainSourcesContainNoForbiddenImports() {
        val root = File("src/main")
        val sources = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { it.path to it.readText() }
        val hits = findForbiddenImports(sources)
        assertTrue("forbidden imports in vision-core: $hits", hits.isEmpty())
    }
}
