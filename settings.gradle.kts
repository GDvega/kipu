pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kipu"
include(":app")
include(":core:designsystem")
include(":core:domain")
include(":core:data")
include(":feature:home")
include(":feature:movements")
include(":feature:envelopes")
include(":feature:commitments")
include(":feature:profile")
include(":feature:onboarding")
include(":feature:plan")
include(":feature:receipts")

