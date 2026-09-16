// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "vision_engine/metadata",
        ).setMethodCallHandler { call, result ->
            try {
                val path = call.argument<String>("path")
                if (path == null) {
                    result.error("BAD_ARGS", "missing path", null)
                    return@setMethodCallHandler
                }
                val payload = when (call.method) {
                    "probePhoto" -> MetadataBridge.probePhoto(path)
                    "probeVideo" -> MetadataBridge.probeVideo(path)
                    "getVideoFrame" -> MetadataBridge.getVideoFrame(
                        path,
                        (call.argument<Number>("positionUs")?.toLong()) ?: 0L,
                    )
                    else -> return@setMethodCallHandler result.notImplemented()
                }
                result.success(payload)
            } catch (e: IllegalArgumentException) {
                result.error("UNREADABLE", e.message, null)
            } catch (e: SecurityException) {
                result.error("UNREADABLE", e.message, null)
            } catch (e: Exception) {
                result.error("PLATFORM_FAIL", e.message, null)
            }
        }
    }
}
