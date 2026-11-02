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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ArBattery"

include(":app")
include(":companion")

include(":core:common")
include(":core:database")
include(":core:bluetooth")
include(":core:network")
include(":core:designsystem")
include(":core:device")

include(":feature:dashboard")
include(":feature:discovery")
include(":feature:pairing")
include(":feature:device_details")
include(":feature:history")
include(":feature:settings")

include(":widget")
