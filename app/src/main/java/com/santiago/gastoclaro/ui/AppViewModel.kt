package com.santiago.gastoclaro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.ProfileEntity
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import com.santiago.gastoclaro.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class AppUiState(
    val isLoading: Boolean = true,
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfileId: Long? = null,
    val activeProfile: ProfileEntity? = null,
    val selectedMonth: YearMonth = YearMonth.now()
)

@HiltViewModel
class AppViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val financeRepository: FinanceRepository,
    private val activeProfileStore: ActiveProfileStore
) : ViewModel() {
    val uiState: StateFlow<AppUiState> = combine(
        profileRepository.observeProfiles(),
        activeProfileStore.activeProfileId,
        activeProfileStore.selectedMonth
    ) { profiles, activeId, selectedMonth ->
        AppUiState(
            isLoading = false,
            profiles = profiles,
            activeProfileId = activeId,
            activeProfile = profiles.firstOrNull { it.id == activeId },
            selectedMonth = selectedMonth
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch {
            financeRepository.ensureClosedMonths(LocalDate.now())
        }
        viewModelScope.launch {
            combine(profileRepository.observeProfiles(), activeProfileStore.activeProfileId) { profiles, activeId ->
                profiles to activeId
            }.collect { (profiles, activeId) ->
                val valid = activeId != null && profiles.any { it.id == activeId }
                when {
                    profiles.isEmpty() && activeId != null -> activeProfileStore.setActiveProfile(null)
                    profiles.isNotEmpty() && !valid -> activeProfileStore.setActiveProfile(profiles.first().id)
                }
            }
        }
    }

    fun selectProfile(profileId: Long) {
        viewModelScope.launch { activeProfileStore.setActiveProfile(profileId) }
    }

    fun selectMonth(period: YearMonth) {
        viewModelScope.launch { activeProfileStore.setSelectedMonth(period) }
    }

    fun refreshClosures() {
        viewModelScope.launch { financeRepository.ensureClosedMonths(LocalDate.now()) }
    }
}
