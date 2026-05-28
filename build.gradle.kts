import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sentryAndroid) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    configurations.configureEach {
        resolutionStrategy.force(
            "com.google.protobuf:protobuf-java:3.25.5",
            "commons-io:commons-io:2.14.0",
            "io.netty:netty-buffer:4.1.133.Final",
            "io.netty:netty-codec:4.1.133.Final",
            "io.netty:netty-codec-http:4.1.133.Final",
            "io.netty:netty-codec-http2:4.1.133.Final",
            "io.netty:netty-codec-socks:4.1.133.Final",
            "io.netty:netty-common:4.1.133.Final",
            "io.netty:netty-handler:4.1.133.Final",
            "io.netty:netty-handler-proxy:4.1.133.Final",
            "io.netty:netty-resolver:4.1.133.Final",
            "io.netty:netty-transport:4.1.133.Final",
            "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
        )
    }

    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint("1.5.0").editorConfigOverride(
                mapOf(
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_standard_backing-property-naming" to "disabled",
                    "ktlint_standard_value-parameter-comment" to "disabled",
                    "ktlint_standard_no-wildcard-imports" to "disabled"
                )
            )
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.5.0")
        }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = file("detekt-baseline.xml")
        source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(false)
        }
    }
}
