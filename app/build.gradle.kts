import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sentryAndroid)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use(localProperties::load)
}

fun readConfigValue(name: String): String =
    (System.getenv(name) ?: localProperties.getProperty(name) ?: "")
        .replace("\"", "")

val sentryDsn = readConfigValue("SENTRY_DSN")
val sentryEnvironment = readConfigValue("SENTRY_ENVIRONMENT").ifBlank { "production" }
val sentryOrg = readConfigValue("SENTRY_ORG")
val sentryProject = readConfigValue("SENTRY_PROJECT")
val sentryAuthToken = System.getenv("SENTRY_AUTH_TOKEN").orEmpty()
val sentryUploadEnabled =
    sentryOrg.isNotBlank() &&
        sentryProject.isNotBlank() &&
        sentryAuthToken.isNotBlank()

android {
    namespace = "com.zatiaras"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zatiaras.pos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["sentryDsn"] = sentryDsn
        manifestPlaceholders["sentryEnvironment"] = sentryEnvironment
        manifestPlaceholders["sentryRelease"] = "$applicationId@$versionName+$versionCode"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

sentry {
    autoInstallation {
        enabled.set(false)
    }
    autoUploadProguardMapping.set(sentryUploadEnabled)
    includeSourceContext.set(sentryUploadEnabled)
    org.set(sentryOrg)
    projectName.set(sentryProject)
    authToken.set(sentryAuthToken)
}

dependencies {

    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:pos"))
    implementation(project(":feature:reports"))
    implementation(project(":feature:printer"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // WorkManager (Background Sync)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Timber
    implementation(libs.timber)
    implementation(libs.sentry.android)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.kotlin.stdlib)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
