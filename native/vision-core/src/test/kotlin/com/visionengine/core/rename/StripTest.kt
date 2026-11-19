// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StripTest {
    @Test
    fun stripsLeadingOnly() {
        assertEquals("foo.jpg", stripLeadingPrefix("IMG_foo.jpg"))
        assertEquals("foo.mp4", stripLeadingPrefix("VID_foo.mp4"))
        assertNull(stripLeadingPrefix("MY_IMG_foo.jpg"))
        assertNull(stripLeadingPrefix("img_foo.jpg"))
        assertEquals("foo.jpg", stripLeadingPrefix("img_foo.jpg", caseSensitive = false))
        assertEquals("", stripLeadingPrefix("IMG_"))
        assertEquals("IMG_x.jpg", stripLeadingPrefix("IMG_IMG_x.jpg"))
    }

    @Test
    fun preservesSuffixVerbatim() {
        assertEquals(
            "20260905_110523079_HDR.jpg",
            stripLeadingPrefix("IMG_20260905_110523079_HDR.jpg"),
        )
        assertEquals("x_BURST.jpg", stripLeadingPrefix("IMG_x_BURST.jpg"))
    }

    @Test
    fun caseInsensitiveUppercasesBothSides() {
        assertEquals("a.jpg", stripLeadingPrefix("ImG_a.jpg", caseSensitive = false))
        assertEquals("a.jpg", stripLeadingPrefix("IMG_a.jpg", caseSensitive = false))
    }

    @Test
    fun firstMatchingPrefixWins() {
        assertEquals("x.jpg", stripLeadingPrefix("IMG_x.jpg", prefixes = listOf("IMG_", "IM")))
        assertEquals("G_x.jpg", stripLeadingPrefix("IMG_x.jpg", prefixes = listOf("IM", "IMG_")))
    }

    @Test
    fun customPrefixes() {
        assertEquals("x.jpg", stripLeadingPrefix("DSC_x.jpg", prefixes = listOf("DSC_")))
        assertNull(stripLeadingPrefix("IMG_x.jpg", prefixes = listOf("DSC_")))
    }

    @Test
    fun emptyPrefixListMatchesNothing() {
        assertNull(stripLeadingPrefix("IMG_x.jpg", prefixes = emptyList()))
    }

    @Test
    fun ignoreCaseIsTurkishProof() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            // In tr_TR, "i".uppercase() is "İ", not "I" — Locale.ROOT
            // must be used, or "img_" would never match "IMG_".
            assertEquals("foo.jpg", stripLeadingPrefix("img_foo.jpg", caseSensitive = false))
            assertEquals("foo.jpg", stripLeadingPrefix("IMG_foo.jpg", caseSensitive = false))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
