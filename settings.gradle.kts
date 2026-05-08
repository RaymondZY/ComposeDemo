pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

rootProject.name = "ComposeDemo"
include(":app")
include(":service:user-center:api")
include(":service:user-center:impl")
include(":service:user-center:mock")
include(":service:storage:api")
include(":service:storage:impl")
include(":service:storage:mock")
include(":scaffold:core")
include(":scaffold:android")
include(":biz:home:domain")
include(":biz:home:presentation")
include(":biz:feed:domain")
include(":biz:feed:presentation")
include(":biz:story:domain")
include(":biz:story:presentation")
include(":biz:story:message:domain")
include(":biz:story:message:presentation")
include(":biz:story:infobar:domain")
include(":biz:story:infobar:presentation")
include(":biz:story:input:domain")
include(":biz:story:input:presentation")
include(":biz:story:background:domain")
include(":biz:story:background:presentation")
include(":biz:story:story-panel:domain")
include(":biz:story:story-panel:presentation")
include(":service:feed:api")
include(":service:feed:mock")
