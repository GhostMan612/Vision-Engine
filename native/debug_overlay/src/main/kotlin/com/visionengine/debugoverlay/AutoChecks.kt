// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.debugoverlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Automated smoke legs. Pure in-app checks — no adb/root needed:
 * the main app is started via [Intent.startActivity] and verified with
 * [ActivityManager.getRunningAppProcesses] (the no-root equivalent of
 * `pidof <package>`).
 */
object AutoChecks {

    private const val WAIT_MS: Long = 10_000
    private const val POLL_MS: Long = 500

    /** "App launches while airplane mode is on": start the main app, verify it holds as a running process. */
    suspend fun launchOffline(
        context: Context,
        mainAppPackage: String,
    ): Pair<ItemStatus, String> {
        val launched = withContext(Dispatchers.Main.immediate) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(mainAppPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
        if (!launched) {
            return ItemStatus.FAIL to "launch failed (no activity for $mainAppPackage)"
        }
        return if (waitForProcess(context, mainAppPackage)) {
            ItemStatus.PASS to "launched offline; $mainAppPackage process is running"
        } else {
            ItemStatus.FAIL to "launched but $mainAppPackage is not in running app processes"
        }
    }

    /** "App has foreground focus": the main app is in the running app processes. */
    suspend fun focusOffline(
        context: Context,
        mainAppPackage: String,
    ): Pair<ItemStatus, String> {
        return withContext(Dispatchers.IO) {
            val running = runningAppProcesses(context)
            if (running.contains(mainAppPackage)) {
                ItemStatus.PASS to "$mainAppPackage is running: $running"
            } else {
                ItemStatus.FAIL to "$mainAppPackage not in running app processes: $running"
            }
        }
    }

    private suspend fun waitForProcess(
        context: Context,
        packageName: String,
        timeoutMs: Long = WAIT_MS,
    ): Boolean = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (runningAppProcesses(context).contains(packageName)) {
                return@withContext true
            }
            delay(POLL_MS)
        }
        runningAppProcesses(context).contains(packageName)
    }

    private fun runningAppProcesses(context: Context): List<String> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return am?.runningAppProcesses.orEmpty().map { it.processName }
    }
}
