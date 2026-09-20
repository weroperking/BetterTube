package com.bettertube.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bettertube.app.data.vault.VaultCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, VaultCipher.MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    open fun hasCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    open fun markCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    open fun isReduceMotionEnabled(): Boolean {
        return prefs.getBoolean(KEY_REDUCE_MOTION, false)
    }

    open fun setReduceMotionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "bettertube_onboarding_prefs"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
    }
}
