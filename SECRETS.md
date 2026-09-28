# Secrets Management

This project uses the Gradle Secrets Plugin to manage sensitive configuration values.

## Required Secrets

The following secrets are expected to be provided via environment variables during CI builds:

| Secret Name | Description |
|---|---|
| `RELEASE_STORE_PASSWORD` | Keystore password for signing release APKs |
| `RELEASE_KEY_ALIAS` | Key alias within the keystore |
| `RELEASE_PASSWORD` | Password for the signing key |
| `FIREBASE_APPCHECK_DEBUG_TOKEN` | Debug token for Firebase App Check (debug builds only) |

## Local Development

For local development, create a `.env` file at the repository root with your secrets:

```
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_PASSWORD=your_key_password
```

The `.env` file is listed in `.gitignore` and will never be committed.

## CI Configuration

In CI, secrets must be set as environment variables. The GitHub Actions workflow
decodes a base64-encoded keystore file into `app/keystore.jks` and reads the
passwords from environment variables.

## .env.example

A template `.env.example` file is provided with placeholder values. Copy it to `.env`
and fill in your actual values:

```bash
cp .env.example .env
```

## Security Notes

- Never commit `.env` or keystore files
- Never log secret values in source code
- Only the release build type is signed with the release keystore
- Debug builds use the default debug keystore
