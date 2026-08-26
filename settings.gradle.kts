@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        maven {
            name = "LocalOverflowGatewayStage"
            url = uri(file("../bot/Overflow-Gateway/build/maven-publishing-stage"))
        }
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        google()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        kotlin("plugin.jpa") version "1.9.21"
        kotlin("plugin.allopen") version "1.9.21"
    }
}

rootProject.name = "HuYanEconomy"

include(
    "economy-common",
    "economy-data",
    "economy-image",
    "economy-core",
    "economy-game",
    "economy-main",
)
