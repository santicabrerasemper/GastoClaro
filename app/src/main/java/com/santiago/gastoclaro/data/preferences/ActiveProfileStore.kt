package com.santiago.gastoclaro.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "gasto_claro_preferences")

@Singleton
class ActiveProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val activeProfileId = longPreferencesKey("active_profile_id")
        val selectedMonth = stringPreferencesKey("selected_month")
    }

    val activeProfileId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[Keys.activeProfileId]
    }

    val selectedMonth: Flow<YearMonth> = context.dataStore.data.map { preferences ->
        preferences[Keys.selectedMonth]
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: YearMonth.now()
    }

    suspend fun setActiveProfile(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id == null) preferences.remove(Keys.activeProfileId)
            else preferences[Keys.activeProfileId] = id
        }
    }

    suspend fun setSelectedMonth(period: YearMonth) {
        context.dataStore.edit { preferences ->
            preferences[Keys.selectedMonth] = period.toString()
        }
    }
}
