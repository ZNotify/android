@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.22"
    id("com.google.gms.google-services")
}

fun getCommandResult(command: String): String {
    val process = Runtime.getRuntime().exec(command.split(" ").toTypedArray())
    process.waitFor()
    return process.inputStream.bufferedReader().readText().trim()
}

fun getProp(key: String): String? {
    val isCI = System.getenv("CI") == "true"
    return if (isCI) {
        System.getenv(key)
    } else {
        gradleLocalProperties(rootDir).getProperty(key)
    }
}

val isCI = System.getenv("CI") == "true"

val gitCommitId = getCommandResult("git rev-parse --short HEAD")

val gitLastCommitMessage = getCommandResult("git log -1 --pretty=%B")

val isRelease = gitLastCommitMessage.contains("[release")

val isDebug = gradle.startParameter.taskRequests.any { req ->
    req.args.any { it.endsWith("Debug") }
}

val buildType = if (isDebug) ".debug" else ""

//get current timestamp
val currentVersionCode = System.currentTimeMillis() / 1000

var baseVersionName = "1.0.0"

if (isCI) {
    val currentEvent = System.getenv("GITHUB_EVENT_NAME")
    if (currentEvent == "push") {
        baseVersionName = if (isRelease) {
            val versionAll = gitLastCommitMessage.split("\\[release:")[1]
            val version = versionAll.split("]")[0].trim()
            version
        } else {
            val branch = System.getenv("GITHUB_REF_NAME")
                ?: throw IllegalArgumentException("GITHUB_REF_NAME is not set")
            "$branch.$gitCommitId"
        }
    }
}

val versionBase = "${baseVersionName}${buildType}"

android {
    defaultConfig {
        applicationId = "top.learningman.push"
        minSdk = 28
        targetSdk = 33
        versionCode = currentVersionCode.toInt()
        versionName = versionBase
    }

    signingConfigs {
        create("auto") {
            val password = getProp("PASSWORD")

            if (password == null || password.isEmpty()) {
                throw Exception("Signing password not found.")
            }
            storeFile = file("../release.jks")
            keyAlias = "key"
            storePassword = password
            keyPassword = password
        }
    }
    compileSdk = 33

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("auto")
        }
        release {
            signingConfig = signingConfigs.getByName("auto")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"

//        TODO: use k2
//        freeCompilerArgs += [
//        ]
    }
    packagingOptions {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
            excludes += "META-INF/atomicfu.kotlin_module"
        }
    }
    viewBinding {
        enable = true
    }

    namespace = "top.learningman.push"

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    flavorDimensions.add("pub")
    productFlavors {
        create("free") {
            dimension = "pub"
            versionNameSuffix = "(free)"
        }
        create("github") {
            dimension = "pub"
            versionNameSuffix = "(github)"
        }
        create("appcenter") {
            dimension = "pub"
            versionNameSuffix = "(appcenter)"
        }
        create("play") {
            dimension = "pub"
            versionNameSuffix = "(play)"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.appcompat:appcompat:1.5.1")

    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.material3:material3:1.1.0-alpha02")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.0-alpha02")
    implementation("androidx.compose.animation:animation:1.3.1")
    implementation("androidx.compose.ui:ui-tooling:1.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation("com.google.android.material:compose-theme-adapter:1.2.1")

    implementation("com.google.android.material:material:1.7.0")

    implementation(platform("com.google.firebase:firebase-bom:30.5.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    val playImplementation by configurations
    playImplementation("com.google.android.play:app-update:2.0.1")
    playImplementation("com.google.android.play:app-update-ktx:2.0.1")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    val lifecycleVersion = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    implementation("androidx.fragment:fragment-ktx:1.5.4")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.preference:preference-ktx:1.2.0")

    implementation("androidx.browser:browser:1.5.0-alpha01")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("com.github.code-mc:material-icon-lib:1.1.5")

    val ktorVersion = "2.1.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    implementation("dev.zxilly:notify-sdk:2.2.1")

    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:${markwonVersion}")
    implementation("io.noties.markwon:ext-tables:${markwonVersion}")
    implementation("io.noties.markwon:html:${markwonVersion}")
    implementation("io.noties.markwon:image:${markwonVersion}")

    val appCenterSdkVersion = "5.0.0"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    implementation("com.github.Zxilly:SetupWizardLib:master-SNAPSHOT")
    implementation("com.github.XomaDev:MIUI-autostart:master-SNAPSHOT")

    implementation("dev.zxilly.lib:upgrader:nightly.8f61e9a")
}