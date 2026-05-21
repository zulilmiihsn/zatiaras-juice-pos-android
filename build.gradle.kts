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
}

subprojects {
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
}
