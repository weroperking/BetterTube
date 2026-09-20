package com.bettertube.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.bettertube.app.data.preferences.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: OnboardingPreferences
) : ViewModel() {

    fun hasCompletedOnboarding(): Boolean = prefs.hasCompleted()

    fun markCompleted() {
        prefs.markCompleted()
    }
}
