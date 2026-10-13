@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        maven {
            name = "LocalOverflowGatewayStage"
            url = uri(file("../bot/Overflow-Gateway/build/maven-publishing-stage"))
        }
        // 公共仓库排在前面，私有 Nexus 放在最后并限定分组。
        // 原因：Gradle 只会在 404 时继续找下一个仓库，遇到「连接超时」会直接判定整个解析失败。
        // nexus.chahuyun.cn 一旦不可达，只要它排在链首、又不做分组限制，
        // 任何第三方依赖（例如 org.jetbrains.* / com.github.gmazzo.*）都会先撞上它而卡住 IDE 同步。
        mavenCentral()
        google()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        maven("https://nexus.chahuyun.cn/repository/maven-public/") {
            name = "ChahuyunNexus"
            content {
                // 只有 cn.chahuyun 自己的构件（hibernate-plus / HuYanAuthorize*）可能依赖该仓库，
                // 限制分组后其余依赖不会再来问它，坏掉也不影响构建。
                includeGroup("cn.chahuyun")
            }
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://nexus.chahuyun.cn/repository/maven-public/") {
            name = "ChahuyunNexus"
            content {
                includeGroup("cn.chahuyun")
            }
        }
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
    "economy-action",
    "economy-action-ksp",
    "economy-core",
    "economy-game",
    "economy-main",
)
