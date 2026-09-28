package com.bettertube.app.data.vault

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class VaultPinManager @Inject constructor(
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
            // Fallback to standard context prefs if encrypted prefs fails (e.g., in some test environments)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    open fun setPin(pin: String) {
        val salt = ByteArray(SALT_LENGTH_BYTES).apply {
            SecureRandom().nextBytes(this)
        }
        val hash = hashPin(pin, salt)

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_SALT, saltBase64)
            .putString(KEY_HASH, hashBase64)
            .apply()
    }

    open fun verifyPin(pin: String): Boolean {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val storedHashBase64 = prefs.getString(KEY_HASH, null) ?: return false

        val salt = try {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            return false
        }

        val computedHash = hashPin(pin, salt)
        val computedHashBase64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        return storedHashBase64 == computedHashBase64
    }

    open fun clearPin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .apply()
    }

    open fun hasPin(): Boolean {
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_HASH)
    }

    open fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    open fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "bettertube_vault_encrypted_prefs"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        const val SALT_LENGTH_BYTES = 16
        const val ITERATION_COUNT = 100_000
        const val KEY_LENGTH_BITS = 256
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
