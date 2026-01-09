# ⚙️ Spec: Phase 1 - Core Modules Implementation

> **Plan**: [01-core-architecture.md](../plans/01-core-architecture.md)
> **Status**: ✅ Completed
> **Version**: 1.0

## 1. Module Structure & Dependencies

We will implement a standard Android multi-module structure.

| Module | Type | Dependencies | Description |
| :--- | :--- | :--- | :--- |
| `:app` | `com.android.application` | implementation(project(":core:ui"))<br>implementation(project(":core:data"))<br>implementation(project(":feature:...")) | Only handles DI entry point, Navigation Graph, and Feature wiring. |
| `:core:ui` | `com.android.library` | None (mostly). | Design System, Theme, Color, Type, Shared Composables. NO Business Logic. |
| `:core:data` | `com.android.library` | implementation(project(":core:domain"))<br>Room, Ktor, Supabase | Implementations of Repositories, Database DAOs, Network Clients. |
| `:core:domain` | `java-library` (Kotlin) | None. | Pure Kotlin. Entities, Repository Interfaces, UseCases, Result Wrapper. |

## 2. Dependency Management (Version Catalog)

We will **migrate to `libs.versions.toml`** immediately to manage dependencies cleanly across modules.

**File:** `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.2.0" # or match current
kotlin = "1.9.22" # or match current
coreKtx = "1.12.0"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.7.0"
activityCompose = "1.8.2"
composeBom = "2023.08.00"
hilt = "2.50"
ksp = "1.9.22-1.0.17"
room = "2.6.1"
ktor = "2.3.7"
coil = "2.5.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
jetbrainsKotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
hiltAndroid = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

## 3. Implementation Steps

### Step 1: Initialize Version Catalog
*   Create `gradle/libs.versions.toml`.
*   Update `settings.gradle.kts` to enable it (if not already by default in newer Gradle).
*   Refactor `app/build.gradle.kts` to use the catalog.

### Step 2: Create `:core:domain` (Kotlin Library)
*   Create `core/domain/build.gradle.kts`.
*   Add basic `Result.kt` utility.
*   Update `settings.gradle.kts`.

### Step 3: Create `:core:ui` (Android Library)
*   Create `core/ui/build.gradle.kts`.
*   Establish `com.zatiaras.pos.core.ui` namespace.
*   **Move**: `app/src/main/java/com/zatiaras/pos/ui/theme` -> `core/ui/src/main/java/com/zatiaras/pos/core/ui/theme`.
*   **Fix Imports**: Update `:app` to import theme from `:core:ui`.

### Step 4: Create `:core:data` (Android Library)
*   Create `core/data/build.gradle.kts`.
*   Dependencies: `:core:domain`, Hilt, Room.
*   Setup basic Hilt Module placeholder.

### Step 5: Wiring
*   Clean up `:app` dependencies.
*   Verify build.

## 4. Acceptance Criteria
1.  Project compiles with `./gradlew assembleDebug`.
2.  `libs.versions.toml` is the source of truth for versions.
3.  `:app` can successfully launch the MainActivity using the Theme from `:core:ui`.
