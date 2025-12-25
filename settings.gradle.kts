@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        // 壶言私服 - 国内加速镜像代理 (推荐)
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        // KSP 插件与常规依赖来源 (备用)
        google()
        mavenCentral()
    }
}

pluginManagement{
    repositories{
        maven("https://nexus.chahuyun.cn/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        // 壶言私服 - 国内加速镜像代理 (推荐)
        // KSP 插件与常规依赖来源 (备用)
        google()
    }
}

rootProject.name = "HuYanEconomy"