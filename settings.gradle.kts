@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        maven {
            name = "LocalOverflowGatewayStage"
            url = uri(file("../bot/Overflow-Gateway/build/maven-publishing-stage"))
        }
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
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
        kotlin("plugin.jpa") version "1.9.20"
        kotlin("plugin.allopen") version "1.9.20"
    }
}

rootProject.name = "HuYanEconomy"

include(
    "economy-common",
    "economy-base",
    "economy-data",
    "economy-image",
    "economy-core",
    "economy-game",
    "economy-main",
)
