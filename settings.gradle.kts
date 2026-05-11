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
include(":scaffold:platform")
include(":biz:home:core")
include(":biz:home:platform")
include(":biz:feed:core")
include(":biz:feed:platform")
include(":biz:story:core")
include(":biz:story:platform")
include(":biz:story:message:core")
include(":biz:story:message:platform")
include(":biz:story:infobar:core")
include(":biz:story:infobar:platform")
include(":biz:story:comment-panel:core")
include(":biz:story:comment-panel:platform")
include(":biz:story:share-panel:core")
include(":biz:story:share-panel:platform")
include(":biz:story:input:core")
include(":biz:story:input:platform")
include(":biz:story:background:core")
include(":biz:story:background:platform")
include(":biz:story:story-panel:core")
include(":biz:story:story-panel:platform")
include(":service:feed:api")
include(":service:feed:mock")
