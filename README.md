# DualLayerSecurity - Android Application

A complete Android application implementing dual-layer secure communication using AES-256 encryption and LSB steganography.

## Features

### Cryptography Layer
- **AES-256 CBC Encryption**: Industry-standard symmetric encryption
- **Secure Key Management**: Uses Android EncryptedSharedPreferences
- **SHA-256 Integrity Checking**: Ensures data hasn't been tampered with
- **RSA Support**: 2048-bit RSA encryption capability

### Steganography Layer
- **LSB Image Embedding**: Hides encrypted data in image pixels
- **Capacity Validation**: Ensures image can hold the encrypted payload
- **Bitwise Operations**: Modifies only the least significant bit of RGB channels
- **Visual Preservation**: Stego-images remain visually unchanged

### Dual-Layer Processing
1. **Encrypt & Hide**: Message → AES Encryption → LSB Embedding → Stego Image
2. **Extract & Decrypt**: Stego Image → LSB Extraction → AES Decryption → Original Message

## Architecture

```
MVVM + Repository + UseCases
├── data/
│   ├── crypto/          # CryptoManager (AES/RSA)
│   ├── steganography/   # SteganographyManager (LSB)
│   └── models/          # Data models
├── domain/
│   ├── usecases/        # CryptoStegoUseCase
│   └── repository/      # KeyRepository
├── ui/
│   ├── screens/         # Compose UI screens
│   ├── viewmodels/      # ViewModels
│   ├── navigation/      # Navigation
│   └── theme/           # Material 3 theme
└── utils/               # ImageUtils
```

## Requirements

- Android Studio Hedgehog or later
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9.20
- Jetpack Compose

## Setup Instructions

1. **Clone or extract the project**

2. **Open in Android Studio**
   - File → Open → Select the project directory

3. **Sync Gradle**
   - Android Studio will automatically sync dependencies
   - If not, click "Sync Now" in the notification bar

4. **Build the project**
   ```bash
   ./gradlew build
   ```

5. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click Run (Shift+F10)

## Usage

### Encrypting and Hiding a Message

1. Launch the app
2. Tap "Encrypt & Hide Message"
3. Enter your secret message
4. Select a cover image from your gallery
5. Tap "Generate Stego Image"
6. Save the stego image to your gallery

### Extracting and Decrypting a Message

1. Launch the app
2. Tap "Extract & Decrypt Message"
3. Select the stego image
4. Tap "Extract Message"
5. View the decrypted message

## Testing

Run unit tests:
```bash
./gradlew test
```

Tests include:
- AES encryption/decryption correctness
- Image capacity validation
- Embedded bytes == extracted bytes verification
- Corrupted image detection
- Integrity check validation
- Wrong key detection

## Security Features

1. **AES-256 CBC Mode**: Strong encryption with random IV
2. **EncryptedSharedPreferences**: Secure key storage using Android Keystore
3. **SHA-256 Checksums**: Integrity validation before decryption
4. **Exception Handling**: Comprehensive error handling with sealed classes
5. **No Plaintext Storage**: Keys never stored in plaintext

## Technical Implementation

### Cryptography
```kotlin
// AES-256 encryption with CBC mode
cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
val encryptedBytes = cipher.doFinal(message.toByteArray())
```

### Steganography
```kotlin
// LSB modification per pixel
val red = (pixel.red and 0xFE) or bit  // Clear LSB and set new bit
val green = (pixel.green and 0xFE) or bit
val blue = (pixel.blue and 0xFE) or bit
```

### Capacity Calculation
```kotlin
// Available capacity = (width × height × 3 channels) / 8 - header
val capacity = (bitmap.width * bitmap.height * 3) / 8 - 4
```

## Dependencies

- Jetpack Compose (UI)
- Material 3 (Design)
- AndroidX Security Crypto (Key storage)
- Kotlin Coroutines (Async operations)
- Navigation Compose (Navigation)
- JUnit & Robolectric (Testing)

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/duallayersecurity/app/
│   │   │   ├── data/
│   │   │   │   ├── crypto/CryptoManager.kt
│   │   │   │   ├── steganography/SteganographyManager.kt
│   │   │   │   └── models/
│   │   │   ├── domain/
│   │   │   │   ├── usecases/CryptoStegoUseCase.kt
│   │   │   │   └── repository/KeyRepository.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   ├── viewmodels/CryptoStegoViewModel.kt
│   │   │   │   ├── navigation/AppNavigation.kt
│   │   │   │   └── theme/
│   │   │   ├── utils/ImageUtils.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── test/
│       └── java/com/duallayersecurity/app/
│           ├── CryptoManagerTest.kt
│           ├── SteganographyManagerTest.kt
│           └── CryptoStegoUseCaseTest.kt
├── build.gradle.kts
└── proguard-rules.pro
```

## License

This is a demonstration project for educational purposes.

## Notes

- The app uses a single AES key stored securely in EncryptedSharedPreferences
- Images should be at least 100x100 pixels for small messages
- Larger messages require larger cover images
- The stego image is saved as PNG to preserve pixel data
- All cryptographic operations run on background threads
