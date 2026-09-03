pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        maven("https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-releases/")
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
    }
}

rootProject.name = "Glass"
include(":backdrop")
include(":shapes")
include(":app")
include(":androidApp")
