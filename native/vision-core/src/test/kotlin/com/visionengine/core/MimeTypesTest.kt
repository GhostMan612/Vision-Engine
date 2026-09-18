// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MimeTypesTest {
    @Test
    fun providerMimeWinsOverExtension() {
        assertEquals(
            MediaKind.photo,
            kindForMime("image/jpeg", "weird.bin"),
        )
        assertEquals(
            MediaKind.video,
            kindForMime("video/mp4", "clip.jpg"),
        )
        assertEquals("image/jpeg", mimeFor("image/jpeg", "weird.bin"))
    }

    @Test
    fun mimeIsNormalizedBeforeMatching() {
        assertEquals(
            MediaKind.photo,
            kindForMime("  IMAGE/JPEG; charset=x ", "a.bin"),
        )
        assertEquals("image/png", mimeFor("IMAGE/PNG", "a.bin"))
    }

    @Test
    fun missingMimeFallsBackToExtension() {
        assertEquals(MediaKind.photo, kindForMime(null, "a.HEIC"))
        assertEquals(MediaKind.video, kindForMime("", "a.MKV"))
        assertEquals("image/x-adobe-dng", mimeFor(null, "a.dng"))
        assertEquals(MediaKind.unsupported, kindForMime(null, "a.txt"))
        assertEquals(null, mimeFor(null, "a.txt"))
        assertEquals(MediaKind.unsupported, kindForMime(null, "noext"))
        assertEquals(MediaKind.unsupported, kindForMime(null, "trailing."))
    }

    @Test
    fun avifStaysUnsupported() {
        assertEquals(MediaKind.unsupported, kindForMime("image/avif", "a.avif"))
        assertEquals(MediaKind.unsupported, kindForExtension("a.avif"))
        assertEquals(null, mimeFor("image/avif", "a.avif"))
    }

    @Test
    fun directoryMimeIsRecognized() {
        assertEquals(true, isDirectoryMime("vnd.android.document/directory"))
        assertEquals(false, isDirectoryMime("image/jpeg"))
        assertEquals(false, isDirectoryMime(null))
    }

    @Test
    fun classificationIgnoresDeviceLocale() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale("tr", "TR"))
        try {
            assertEquals(
                MediaKind.photo,
                kindForMime(null, "IMG_X.JPG"),
            )
            assertEquals(
                listOf("I.jpg", "j.jpg"),
                planDiscovery(
                    listOf(
                        MediaSource(
                            id = "2",
                            displayName = "j.jpg",
                            kind = MediaKind.photo,
                            filePath = "/t/j.jpg",
                        ),
                        MediaSource(
                            id = "1",
                            displayName = "I.jpg",
                            kind = MediaKind.photo,
                            filePath = "/t/I.jpg",
                        ),
                    ),
                ).map { it.displayName },
            )
        } finally {
            Locale.setDefault(previous)
        }
    }
}

class PagingTest {
    @Test
    fun slicesDeterministicallyWithClampedEdges() {
        val all = listOf(0, 1, 2, 3, 4)
        assertEquals(listOf(1, 2), pageOf(all, offset = 1, limit = 2))
        assertEquals(listOf(4), pageOf(all, offset = 4, limit = 9))
        assertEquals(emptyList<Int>(), pageOf(all, offset = 5, limit = 2))
        assertEquals(emptyList<Int>(), pageOf(all, offset = -1, limit = 2))
        assertEquals(emptyList<Int>(), pageOf(all, offset = 0, limit = 0))
    }
}
