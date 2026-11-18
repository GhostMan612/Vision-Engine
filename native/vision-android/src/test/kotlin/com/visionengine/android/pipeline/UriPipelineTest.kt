// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.thumbs.PhotoThumbs
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaSource
import com.visionengine.core.Provenance
import com.visionengine.core.ThumbStatus
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class UriPipelineTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var context: Context
    private lateinit var treeUri: Uri
    private lateinit var provider: FakeDocsProvider
    private lateinit var trackingOpener: (Uri) -> ParcelFileDescriptor?
    private val openedDescriptors: MutableList<ParcelFileDescriptor> =
        mutableListOf()
    private lateinit var pipeline: MediaPipeline

    private class FakeDocsProvider : ContentProvider() {
        var files: MutableMap<String, File> = mutableMapOf()
        var throwOnOpen: Exception? = null
        val opened: MutableList<ParcelFileDescriptor> = mutableListOf()

        override fun onCreate(): Boolean = true

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            val toThrow = throwOnOpen
            if (toThrow != null) {
                throw toThrow
            }
            val documentId = DocumentsContract.getDocumentId(uri)
            val file = files[documentId]
                ?: throw FileNotFoundException("no such doc: $uri")
            val descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY,
            )
            opened.add(descriptor)
            return descriptor
        }

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0
    }

    @Before
    fun stage() {
        dir = temporaryFolder.newFolder("VE_URI")
        stageAsset("ve_thumb.jpg", "IMG_b.jpg")
        stageAsset("ve_minimal.mp4", "VID_a.mp4")
        File(dir, "bad.jpg").writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        context = ApplicationProvider.getApplicationContext()
        treeUri = Uri.parse(
            "content://com.example.docs/tree/primary%3APictures",
        )
        provider = FakeDocsProvider()
        provider.files["primary:Pictures/IMG_b.jpg"] = File(dir, "IMG_b.jpg")
        provider.files["primary:Pictures/VID_a.mp4"] = File(dir, "VID_a.mp4")
        provider.files["primary:Pictures/bad.jpg"] = File(dir, "bad.jpg")
        val info = ProviderInfo().apply {
            authority = "com.example.docs"
            grantUriPermissions = true
        }
        provider.attachInfo(context, info)
        ShadowContentResolver.registerProviderInternal(
            "com.example.docs",
            provider,
        )
        trackingOpener = { uri ->
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (descriptor != null) {
                openedDescriptors.add(descriptor)
            }
            descriptor
        }
        pipeline = MediaPipeline(
            context.contentResolver,
            openDescriptor = trackingOpener,
        )
    }

    @Test
    fun openFileDescriptorReachesFakeProvider() {
        val uri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            "primary:Pictures/IMG_b.jpg",
        )
        val openedBefore = provider.opened.size
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw AssertionError("openFileDescriptor returned null")
        assertTrue(descriptor.fileDescriptor.valid())
        val bytes = java.io.FileInputStream(descriptor.fileDescriptor).readBytes()
        assertEquals(
            File(dir, "IMG_b.jpg").readBytes().toList(),
            bytes.toList(),
        )
        assertEquals(openedBefore + 1, provider.opened.size)
        descriptor.close()
        assertFalse(descriptor.fileDescriptor.valid())
    }

    @Test
    fun exifInterfaceFdConstructorFailsUnderRobolectric() {
        val file = File(dir, "IMG_b.jpg")
        val outcomes = mutableListOf<String>()
        fun record(label: String, block: () -> ExifInterface) {
            try {
                val exif = block()
                outcomes.add(
                    "$label: orientation=" +
                        exif.getAttribute(ExifInterface.TAG_ORIENTATION) +
                        " width=" +
                        exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) +
                        " height=" +
                        exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH),
                )
            } catch (e: Exception) {
                outcomes.add("$label: ${e.javaClass.simpleName}")
            }
        }
        record("path") { ExifInterface(file.absolutePath) }
        record("stream-fd") { FileInputStream(file).use { ExifInterface(it.fd) } }
        val descriptor = context.contentResolver.openFileDescriptor(
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                "primary:Pictures/IMG_b.jpg",
            ),
            "r",
        ) ?: throw AssertionError("no descriptor")
        try {
            record("provider-fd") { ExifInterface(descriptor.fileDescriptor) }
        } finally {
            descriptor.close()
        }
        record("input-stream") { FileInputStream(file).use { ExifInterface(it) } }
        assertEquals(
            listOf(
                "path: orientation=6 width=24 height=16",
                "stream-fd: NullPointerException",
                "provider-fd: NullPointerException",
                "input-stream: orientation=6 width=24 height=16",
            ),
            outcomes,
        )
    }

    @Test
    fun exifInterfaceFromProviderDescriptorStreamReadsFixture() {
        val uri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            "primary:Pictures/IMG_b.jpg",
        )
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw AssertionError("no descriptor")
        try {
            val exif = FileInputStream(descriptor.fileDescriptor).use { stream ->
                ExifInterface(stream)
            }
            assertEquals("6", exif.getAttribute(ExifInterface.TAG_ORIENTATION))
            assertEquals("24", exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH))
            assertEquals("16", exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
        } finally {
            descriptor.close()
        }
        assertFalse(descriptor.fileDescriptor.valid())
    }

    @Test
    fun providerReturnsExactBadJpgBytes() {
        val uri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            "primary:Pictures/bad.jpg",
        )
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw AssertionError("no descriptor")
        try {
            val bytes = FileInputStream(descriptor.fileDescriptor).use {
                it.readBytes()
            }
            assertEquals(listOf<Byte>(1, 2, 3, 4, 5, 6), bytes.toList())
        } finally {
            descriptor.close()
        }
    }

    @Test
    fun decodeThumbnailDecodesGoodFixtureDirectly() {
        val bytes = File(dir, "IMG_b.jpg").readBytes()
        val decoded = PhotoThumbs.decodeThumbnail(
            bytes,
            6,
            256,
            64L * 1024L * 1024L,
        ) ?: throw AssertionError("good fixture did not decode")
        assertEquals(256, decoded.width)
        assertEquals(384, decoded.height)
    }

    @Test
    fun loadPageInvokesOnRecordPerRecordInOrder() {
        val sources = listOf(
            uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
            uriSource("VID_a.mp4", MediaKind.video, "video/mp4"),
            uriSource("bad.jpg", MediaKind.photo, "image/jpeg"),
        )
        val published = mutableListOf<String>()
        val records = pipeline.loadPage(sources, 0, 10) { record ->
            published.add(record.source.displayName)
        }
        assertEquals(
            listOf("IMG_b.jpg", "VID_a.mp4", "bad.jpg"),
            published,
        )
        assertEquals(
            records.map { it.source.displayName },
            published,
        )
    }

    @Test
    fun secondLoadPageSharesThumbnailCache() {
        val sources = listOf(
            uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        val first = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.ready, first.info.status)
        assertEquals(0, pipeline.cache.hits)
        val again = pipeline.loadPage(sources, 0, 10)
        val second = pipeline.thumbnailFor(again.single())
        assertEquals(first.bytes, second.bytes)
        assertEquals(1, pipeline.cache.hits)
        val fresh = MediaPipeline(
            context.contentResolver,
            openDescriptor = trackingOpener,
        )
        val third = fresh.thumbnailFor(records.single())
        assertEquals(ThumbStatus.ready, third.info.status)
        assertEquals(0, fresh.cache.hits)
    }

    @Test
    fun uriPhotoProbesOrientationAndCorrectedDimensions() {
        val records = pipeline.loadPage(
            listOf(uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg")),
            0,
            10,
        )
        assertEquals(1, records.size)
        assertEquals(ExtractStatus.ok, records.single().status)
        assertEquals(6L, records.single().photo?.orientation?.value)
        assertEquals(Provenance.exif, records.single().photo?.orientation?.provenance)
        assertEquals(24L, records.single().photo?.widthRaw?.value)
        assertEquals(16L, records.single().photo?.heightRaw?.value)
        assertEquals(16L, records.single().photo?.width)
        assertEquals(24L, records.single().photo?.height)
        assertTrue(allDescriptorsClosed())
    }

    @Test
    fun uriPhotoThumbnailDecodesCachesAndClosesDescriptor() {
        val records = pipeline.loadPage(
            listOf(uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg")),
            0,
            10,
        )
        val first = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.ready, first.info.status)
        assertEquals(256, first.info.width)
        assertEquals(384, first.info.height)
        assertTrue(first.bytes != null)
        val second = pipeline.thumbnailFor(records.single())
        assertEquals(first.bytes, second.bytes)
        assertEquals(1, pipeline.cache.hits)
        assertTrue(allDescriptorsClosed())
    }

    @Test
    fun uriVideoProbesWithoutLocalPath() {
        val records = pipeline.loadPage(
            listOf(uriSource("VID_a.mp4", MediaKind.video, "video/mp4")),
            0,
            10,
        )
        assertEquals(1, records.size)
        assertEquals(ExtractStatus.ok, records.single().status)
        assertEquals(0L, records.single().video?.rotation?.value)
        assertTrue(allDescriptorsClosed())
    }

    @Test
    fun uriVideoThumbUnavailableOnTracklessFile() {
        val records = pipeline.loadPage(
            listOf(uriSource("VID_a.mp4", MediaKind.video, "video/mp4")),
            0,
            10,
        )
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.unavailable, thumb.info.status)
        assertEquals("no frame", thumb.info.reason)
        assertNull(thumb.bytes)
        assertTrue(allDescriptorsClosed())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun uriDescriptorClosesOnDecodeFailure() {
        val records = pipeline.loadPage(
            listOf(uriSource("bad.jpg", MediaKind.photo, "image/jpeg")),
            0,
            10,
        )
        assertEquals(ExtractStatus.partial, records.single().status)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.failed, thumb.info.status)
        assertEquals("decode failed", thumb.info.reason)
        assertTrue(allDescriptorsClosed())
    }

    @Test
    fun uriInaccessibleSourceStaysUnreadable() {
        provider.throwOnOpen = FileNotFoundException("gone")
        val records = pipeline.loadPage(
            listOf(uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg")),
            0,
            10,
        )
        assertEquals(ExtractStatus.unreadable, records.single().status)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.unavailable, thumb.info.status)
        assertEquals("unreadable", thumb.info.reason)
    }

    @Test
    fun uriOversizedPhotoThumbStaysUnavailable() {
        val smallCap = MediaPipeline(
            context.contentResolver,
            maxSourceBytes = 10L,
            openDescriptor = trackingOpener,
        )
        val records = smallCap.loadPage(
            listOf(uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg")),
            0,
            10,
        )
        assertEquals(ExtractStatus.ok, records.single().status)
        val thumb = smallCap.thumbnailFor(records.single())
        assertEquals(ThumbStatus.unavailable, thumb.info.status)
        assertEquals("unreadable", thumb.info.reason)
        assertTrue(allDescriptorsClosed())
    }

    @Test
    fun uriFilePathRegressionStillReadsLocalFiles() {
        val file = File(dir, "IMG_b.jpg")
        val sources = listOf(
            MediaSource(
                id = file.absolutePath,
                displayName = "IMG_b.jpg",
                kind = MediaKind.photo,
                filePath = file.absolutePath,
                mime = "image/jpeg",
                sizeBytes = file.length(),
                modifiedMs = file.lastModified(),
            ),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(ExtractStatus.ok, records.single().status)
        assertEquals(16L, records.single().photo?.width)
        assertEquals(24L, records.single().photo?.height)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.ready, thumb.info.status)
    }

    private fun uriSource(
        name: String,
        kind: MediaKind,
        mime: String?,
    ): MediaSource {
        val file = File(dir, name)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            "primary:Pictures/$name",
        ).toString()
        return MediaSource(
            id = documentUri,
            displayName = name,
            kind = kind,
            documentUri = documentUri,
            mime = mime,
            sizeBytes = if (file.isFile) file.length() else null,
            modifiedMs = if (file.isFile) file.lastModified() else null,
        )
    }

    private fun allDescriptorsClosed(): Boolean {
        return openedDescriptors.isNotEmpty() &&
            openedDescriptors.all { !it.fileDescriptor.valid() }
    }

    private fun stageAsset(asset: String, name: String) {
        val stream = javaClass.classLoader!!.getResourceAsStream(
            "fixtures/$asset",
        ) ?: throw IllegalStateException("missing fixture: $asset")
        File(dir, name).outputStream().use { output ->
            stream.copyTo(output)
        }
    }
}
