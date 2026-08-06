# CardSimulator — Android EMV card simulator via NFC HCE

Android app (Kotlin + Jetpack Compose) that emulates EMV bank cards over NFC Host Card Emulation (HCE): an HCE service receives APDUs, an EMV kernel drives the transaction state machine, and card profiles/key material are persisted locally.

## Project

- Stack: Kotlin 1.7.20, AGP 7.2.0, Compose (BOM 2022.12.00, Material3), Navigation Compose, Room 2.4.3 (KSP), Koin 3.4.3, kotlinx.serialization, kotlinx.coroutines, Timber.
- SDK: compile/target 33, min 28, Java 11 (`jvmTarget = "11"`).
- App id / namespace: `com.szzt.cardsimulator`.
- Entry points: `CardSimulatorApp` (Application — plants Timber, starts Koin), `ui/MainActivity` (Compose host), `hce/impl/CardEmulationService` (NFC HCE APDU service, registered in `AndroidManifest.xml`).
- Dependency versions live in `gradle/libs.versions.toml` (version catalog — add deps there, not inline).

## Commands (Windows — use `gradlew.bat`)

- Build debug APK: `.\gradlew.bat assembleDebug`
- Unit tests (JUnit5): `.\gradlew.bat testDebugUnitTest`
- Install on device/emulator: `.\gradlew.bat installDebug`

## Architecture

Feature packages under `app/src/main/java/com/szzt/cardsimulator/`, each split into `api/` (interfaces), `impl/` (`Default*` implementations), and `model/`:

- `emv/` — EMV transaction kernel: `EmvKernel` state machine (IDLE → SELECTED → GPO_DONE → DATA_READ → AC_GENERATED → COMPLETE), `CryptoEngine`, `CertificateProvider`; TLV/EMV models (`TlvObject`, `EmvTag`, `AflEntry`, `CardNetwork`).
- `hce/` — NFC layer: `CardEmulationService` (system entry), `HceRouter` (routes APDUs to the kernel, maps exceptions to SW 0x6F00); `ApduCommand`/`ApduResponse` models.
- `profile/` — card profiles: `ProfileRepository` backed by Room (`RoomProfileRepository`, `db/` DAO/entity/database); models `CardProfile`, `CvmEntry`, `KeyMaterial`.
- `keymgmt/` — key import/storage: `KeyImporter` (JSON via kotlinx.serialization), `KeyStore` (`FileBasedKeyStore`).
- `log/` — APDU logging: `ApduLogger` (`InMemoryApduLogger`, Flow-based).
- `ui/` — Compose: `navigation/AppNavigation`, screens `profile/`, `log/`, `settings/`, each with a ViewModel exposing `StateFlow`; `theme/` (Material3).
- `di/` — Koin modules: `AppModule.appModule` aggregates `hceModule`, `emvModule`, `profileModule`, `keyMgmtModule`, `logModule`, `viewModelModule`.

## Conventions

- Feature layering: interface in `api/`, concrete `Default*` class in `impl/`; impls get dependencies via constructor injection (Koin).
- DI: register every module in `di/AppModule.kt`; `CardSimulatorApp.onCreate` starts Koin with `appModule`.
- Logging: Timber only (`Timber.d/e`), DebugTree planted in debug builds; never `println`/`Log` directly.
- Async: `suspend` functions for APDU/IO paths; ViewModels expose `StateFlow` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`.
- Errors: kernel/router map failures to APDU status words (e.g. 0x6F00 internal error) rather than throwing to the NFC layer.
- KDoc on all public interfaces; state machine states documented in the enum.
- No secrets in code — key material is user-imported and stored via `keymgmt`/`profile` persistence.

## Notes

- No `app/src/test` sources yet, though JUnit5 + Turbine + coroutines-test are declared — add tests under `app/src/test/java/com/szzt/cardsimulator/`.
- Root `build.txt` is a stale build log artifact; `build/`, `.gradle/`, `.idea/` are local outputs.
- Not a git repository yet (`.gitignore` present, no `.git`).
