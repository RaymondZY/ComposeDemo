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

rootProject.name = "ComposeDemo"
include(":app")
include(":biz:todo-list:domain")
include(":biz:todo-list:presentation")
include(":biz:login:domain")
include(":biz:login:presentation")
include(":service:user-center:api")
include(":service:user-center:impl")
include(":service:user-center:mock")
