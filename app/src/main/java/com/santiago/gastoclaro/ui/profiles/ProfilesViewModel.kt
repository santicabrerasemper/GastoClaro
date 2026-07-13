package com.santiago.gastoclaro.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiago.gastoclaro.data.local.entity.ProfileEntity
import com.santiago.gastoclaro.data.preferences.ActiveProfileStore
import com.santiago.gastoclaro.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val activeProfileStore: ActiveProfileStore
) : ViewModel() {
    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun createProfile(name: String, emoji: String, colorArgb: Int, initialAmountCents: Long) {
        viewModelScope.launch {
            runCatching { profileRepository.createProfile(name, emoji, colorArgb, initialAmountCents) }
                .onSuccess { id ->
                    activeProfileStore.setActiveProfile(id)
                    messages.emit("Perfil creado")
                }
                .onFailure { messages.emit(it.message ?: "No se pudo crear el perfil") }
        }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            runCatching { profileRepository.updateProfile(profile) }
                .onSuccess { messages.emit("Perfil actualizado") }
                .onFailure { messages.emit(it.message ?: "No se pudo actualizar") }
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            runCatching { profileRepository.deleteProfile(profile.id) }
                .onSuccess { messages.emit("Perfil eliminado") }
                .onFailure { messages.emit(it.message ?: "No se pudo eliminar") }
        }
    }
}
