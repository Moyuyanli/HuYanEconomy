import moe.karla.maven.publishing.MavenPublishingExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false

    id("net.mamoe.mirai-console") version "2.16.0" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("com.github.gmazzo.buildconfig") version "3.1.0" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    signing
    `java-library`
    `maven-publish`
    id("moe.karla.maven-publishing") version "1.3.1"
}

allprojects {
    group = "cn.chahuyun"
    version = "2.0.0"

    // mirai-console already carries log4j-api in the app classloader.
    // Keep every module from packaging log4j artifacts into the plugin classloader.
    configurations.configureEach {
        exclude(group = "org.apache.logging.log4j")
    }
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
    }
}

val syncPluginToRoot by tasks.registering(Sync::class) {
    group = "mirai"
    description = "Sync the plugin artifact from :economy-main to the root build/mirai directory."
    dependsOn(":economy-main:buildPlugin")
    from(project(":economy-main").layout.buildDirectory.dir("mirai")) {
        include("${rootProject.name}-*.mirai2.jar")
    }
    into(layout.buildDirectory.dir("mirai"))
}

tasks.register("buildPlugin") {
    group = "mirai"
    description = "Build the Mirai plugin from :economy-main."
    dependsOn(syncPluginToRoot)
}

mavenPublishing {
    publishingType = MavenPublishingExtension.PublishingType.USER_MANAGED
    url = "https://github.com/moyuyanli/HuYanEconomy"
    developer("moyuyanli", "572490972@qq.com")
}
