// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvenanceTest {
    @Test
    fun mapsKnownNames() {
        assertEquals(Provenance.exif, parseProvenance("exif"))
        assertEquals(Provenance.container, parseProvenance("container"))
        assertEquals(Provenance.filesystem, parseProvenance("filesystem"))
        assertEquals(Provenance.unknown, parseProvenance("unknown"))
    }

    @Test
    fun fallsBackToUnknown() {
        assertEquals(Provenance.unknown, parseProvenance("EXIF"))
        assertEquals(Provenance.unknown, parseProvenance(""))
        assertEquals(Provenance.unknown, parseProvenance(null))
        assertEquals(Provenance.unknown, parseProvenance(42))
        assertEquals(Provenance.unknown, parseProvenance("made-up-source"))
    }
}

class MetaFieldTest {
    @Test
    fun carriesValueWithProvenance() {
        val field = MetaField("2026:09:14 11:05:23", Provenance.exif)
        assertEquals("2026:09:14 11:05:23", field.value)
        assertEquals(Provenance.exif, field.provenance)
        assertTrue(field.isKnown)
    }

    @Test
    fun unknownProvenanceIsNeverKnown() {
        assertTrue(!MetaField("x", Provenance.unknown).isKnown)
        assertTrue(!MetaField<String>(null, Provenance.unknown).isKnown)
        assertTrue(!MetaField<String>(null, Provenance.exif).isKnown)
    }
}
