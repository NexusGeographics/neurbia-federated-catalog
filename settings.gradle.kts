import org.gradle.kotlin.dsl.project

rootProject.name = "neurbia-federated-catalog"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

include(":src:target-node-directory-api")
include(":src:federated-catalog-asset-filter")