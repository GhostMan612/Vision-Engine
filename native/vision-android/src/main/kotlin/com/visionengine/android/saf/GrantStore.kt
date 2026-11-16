// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.grantDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vision_grants",
)

private val GRANT_URIS = stringSetPreferencesKey("grant_uris")

private fun displayNameKey(uri: String) =
    stringPreferencesKey("grant:$uri:displayName")

private fun displayNameSourceKey(uri: String) =
    stringPreferencesKey("grant:$uri:displayNameSource")

private fun grantedMsKey(uri: String) =
    longPreferencesKey("grant:$uri:grantedMs")

private fun lastSeenHealthyMsKey(uri: String) =
    longPreferencesKey("grant:$uri:lastSeenHealthyMs")

private fun readEntries(prefs: Preferences): List<GrantEntry> {
    val uris = prefs[GRANT_URIS] ?: emptySet()
    return uris.mapNotNull { uri ->
        val displayName = prefs[displayNameKey(uri)] ?: return@mapNotNull null
        val sourceName = prefs[displayNameSourceKey(uri)]
        val source = try {
            if (sourceName == null) {
                DisplayNameSource.TREE_DOCUMENT_ID
            } else {
                DisplayNameSource.valueOf(sourceName)
            }
        } catch (e: IllegalArgumentException) {
            DisplayNameSource.TREE_DOCUMENT_ID
        }
        val grantedMs = prefs[grantedMsKey(uri)] ?: 0L
        val lastSeenHealthyMs = prefs[lastSeenHealthyMsKey(uri)]
        GrantEntry(
            uri = uri,
            displayName = displayName,
            displayNameSource = source,
            grantedMs = grantedMs,
            lastSeenHealthyMs = lastSeenHealthyMs,
        )
    }.sortedBy { it.grantedMs }
}

private fun writeEntry(prefs: MutablePreferences, entry: GrantEntry) {
    prefs[displayNameKey(entry.uri)] = entry.displayName
    prefs[displayNameSourceKey(entry.uri)] = entry.displayNameSource.name
    prefs[grantedMsKey(entry.uri)] = entry.grantedMs
    if (entry.lastSeenHealthyMs == null) {
        prefs.remove(lastSeenHealthyMsKey(entry.uri))
    } else {
        prefs[lastSeenHealthyMsKey(entry.uri)] = entry.lastSeenHealthyMs
    }
    prefs[GRANT_URIS] = (prefs[GRANT_URIS] ?: emptySet()) + entry.uri
}

private fun removeEntry(prefs: MutablePreferences, uri: String) {
    prefs.remove(displayNameKey(uri))
    prefs.remove(displayNameSourceKey(uri))
    prefs.remove(grantedMsKey(uri))
    prefs.remove(lastSeenHealthyMsKey(uri))
    prefs[GRANT_URIS] = (prefs[GRANT_URIS] ?: emptySet()) - uri
}

class GrantStore(context: Context) {
    private val dataStore: DataStore<Preferences> =
        context.applicationContext.grantDataStore

    val entries: Flow<List<GrantEntry>> = dataStore.data.map { prefs ->
        readEntries(prefs)
    }

    suspend fun save(entry: GrantEntry) {
        dataStore.edit { prefs ->
            writeEntry(prefs, entry)
        }
    }

    suspend fun replaceUri(oldUri: String, entry: GrantEntry) {
        dataStore.edit { prefs ->
            if (oldUri != entry.uri) {
                removeEntry(prefs, oldUri)
            }
            writeEntry(prefs, entry)
        }
    }

    suspend fun forget(uri: String) {
        dataStore.edit { prefs ->
            removeEntry(prefs, uri)
        }
    }

    suspend fun markHealthy(uri: String, nowMs: Long) {
        dataStore.edit { prefs ->
            if (uri in (prefs[GRANT_URIS] ?: emptySet())) {
                prefs[lastSeenHealthyMsKey(uri)] = nowMs
            }
        }
    }
}
