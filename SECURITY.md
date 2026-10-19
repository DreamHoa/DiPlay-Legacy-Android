# Credentials and release packaging

Source builds contain no accessory identity. Repository Secrets provision the authorized MFi key/certificate only for standalone APK packaging. Those two files are extractable from an authenticated APK; a draft Release limits access to repository collaborators. The workflow never publishes it automatically.

The separate Android APK-signing key is never included in an APK or Git. The public `signing-cert.sha256` contains only its certificate fingerprint. Keep the signing backup and the matching Secret; changing this key breaks upgrades.

Public-tree checks reject credential files and private-key blocks. Authenticated packaging disables Gradle configuration and output caching. Review diagnostic reports before posting them; pairing records and credentials do not belong in issues.
