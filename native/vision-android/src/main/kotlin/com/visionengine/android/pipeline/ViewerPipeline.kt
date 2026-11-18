// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import com.visionengine.core.MediaRecord
import com.visionengine.core.MediaSource

interface ViewerPipeline {
    fun loadPage(
        sources: List<MediaSource>,
        offset: Int,
        limit: Int,
        onRecord: ((MediaRecord) -> Unit)? = null,
    ): List<MediaRecord>

    fun thumbnailFor(record: MediaRecord): ThumbFetch
}
