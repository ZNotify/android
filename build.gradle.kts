buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0-rc02")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
        classpath("com.google.gms:google-services:4.3.14")
    }
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
