@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import moe.karla.maven.publishing.MavenPublishingExtension

plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"

    id("org.jetbrains.dokka") version "1.8.10"
    id("com.github.gmazzo.buildconfig") version "3.1.0"


    signing
    `java-library`
    `maven-publish`
    id("moe.karla.maven-publishing") version "1.3.1"

}

group = "cn.chahuyun"
version = "1.8.0"

repositories {
    mavenCentral()
//    maven { url "https://maven.aliyun.com/repository/public" }
//    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {

    //依赖
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")
    compileOnly("cn.chahuyun:HuYanSession:2.3.1")
    compileOnly("cn.chahuyun:HuYanAuthorize:1.2.6")

    //使用库
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("cn.hutool:hutool-all:5.8.30")
    implementation("org.apache.poi:poi-ooxml:4.1.2")

    implementation("cn.chahuyun:hibernate-plus:1.0.17")
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_17
}

buildConfig {
    className("BuildConstants")
    packageName("cn.chahuyun.economy")
    useKotlinOutput()
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField(
        "java.time.Instant",
        "BUILD_TIME",
        "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)"
    )
}


mavenPublishing {
    // 设置成手动发布（运行结束后要到 Central 确认发布），如果要自动发布，就用 AUTOMATIC
    publishingType = MavenPublishingExtension.PublishingType.USER_MANAGED
    // 改成你自己的信息
    url = "https://github.com/moyuyanli/HuYanEconomy"
    developer("moyuyanli", "572490972@qq.com")
}