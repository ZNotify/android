@file:Suppress("UnstableApiUsage")

fun getProp(key: String): String? {
    val isCI = System.getenv("CI") == "true"
    return if (isCI) {
        System.getenv(key)
    } else {
        val localProperties = File(rootDir, "local.properties")
        if (localProperties.exists()) {
            java.util.Properties().apply {
                localProperties.inputStream().use { load(it) }
            }.getProperty(key)
        } else null
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://androidx.dev/storage/compose-compiler/repository/")
        }
        maven {
            url = uri("https://maven.pkg.github.com/Zxilly/upgrader")
            credentials {
                username = getProp("GITHUB_USER") ?: "Zxilly"
                password = getProp("GITHUB_TOKEN")
            }
        }
    }
}
rootProject.name = "Notify"

include(":app")
