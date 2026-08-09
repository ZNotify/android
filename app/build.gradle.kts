@file:Suppress("UnstableApiUsage")

import dev.zxilly.gradle.exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
    id("dev.zxilly.gradle.keeper")
}

val isCI = System.getenv("CI") == "true"

keeper {
    expectValue = false

    if (isCI) {
        environment(nameMapping = true)
    } else {
        properties()
    }
}


val gitCommitId = "git rev-parse --short HEAD".exec()
val gitLastCommitMessage = "git log -1 --pretty=%B".exec()

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
            val versionAll = gitLastCommitMessage.split("[release:")[1]
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
    namespace = "top.learningman.push"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "top.learningman.push"
        minSdk {
            version = release(28)
        }
        targetSdk {
            version = release(36)
        }
        versionCode = currentVersionCode.toInt()
        versionName = versionBase
    }

    signingConfigs {
        create("auto") {
            val password = secret.get("password")

            storeFile = file("../release.jks")
            keyAlias = "key"
            storePassword = password
            keyPassword = password
        }
    }
    buildTypes {
        create("unsigned") {
            signingConfig = null
        }

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
            excludes += "META-INF/atomicfu.kotlin_module"
        }
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    flavorDimensions += listOf("pub")
    productFlavors {
        create("free") {
            dimension = "pub"
            versionNameSuffix = "(free)"
        }
        create("github") {
            dimension = "pub"
            versionNameSuffix = "(github)"
        }
        create("play") {
            dimension = "pub"
            versionNameSuffix = "(play)"
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("com.google.android.material:material:1.14.0")

    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-installations")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-base:18.10.0")

    add("playImplementation", "com.google.android.play:app-update:2.1.0")
    add("playImplementation", "com.google.android.play:app-update-ktx:2.1.0")

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.8")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.8")

    val lifecycleVersion = "2.11.0"
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.browser:browser:1.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("com.github.code-mc:material-icon-lib:1.1.5")

    val ktorVersion = "3.5.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    implementation("dev.zxilly:notify-sdk:2.3.3")

    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:${markwonVersion}")
    implementation("io.noties.markwon:ext-tables:${markwonVersion}")
    implementation("io.noties.markwon:html:${markwonVersion}")
    implementation("io.noties.markwon:image:${markwonVersion}")

    implementation("com.github.Zxilly:SetupWizardLib:master-SNAPSHOT")
    implementation("com.github.XomaDev:MIUI-autostart:1.3")

    val upgraderVersion = "0.4.0"
    add("githubImplementation", "dev.zxilly.lib:upgrader:$upgraderVersion")
}
