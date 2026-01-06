import moe.karla.maven.publishing.MavenPublishingExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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


    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

group = "cn.chahuyun"
version = "1.9.3"

// 关键：mirai-console 自身（app classloader）已经携带 log4j-api。
// 如果我们的插件（private classloader）也携带任意 log4j 产物，就会出现“同名类被不同 ClassLoader 各加载一份”，
// 最终触发 LinkageError: loader constraint violation（你现在遇到的就是这个）。
// 所以这里全局排除 log4j，让插件侧永远不要解析/打包 log4j。
configurations.configureEach {
    exclude(group = "org.apache.logging.log4j")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    val ofVersion = "1.0.8"
    compileOnly("top.mrxiaom.mirai:overflow-core-api:$ofVersion")

    //依赖
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")

    val auth = "1.3.7"
    compileOnly("cn.chahuyun:HuYanAuthorize:$auth")
    ksp("cn.chahuyun:HuYanAuthorize-ksp:$auth")

    //使用库
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    // mirai-console 运行时（app classloader）已自带 log4j-api。
    // 如果我们把 log4j-api 也打进插件 jar（private classloader），会出现两份 log4j-api 被不同 ClassLoader 加载，
    // 触发 LinkageError: loader constraint violation（你看到的 StatusLogger/AbstractLogger 分属不同 loader）。
    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation("org.apache.poi:poi-ooxml:5.4.0") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }

    // 关键：不要让 hibernate-plus 把 Hibernate/ByteBuddy/HikariCP 等传递依赖再次打进插件包（private classloader），
    // 否则会和 mirai-hibernate-plugin（shared/app）里同名类冲突，产生 ServiceConfigurationError: "xxx not a subtype"。
    // 运行期 Hibernate 由 mirai-hibernate-plugin 提供即可。
    implementation("cn.chahuyun:hibernate-plus:2.1.1")

    testConsoleRuntime("top.mrxiaom.mirai:overflow-core:$ofVersion")
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
    noTestCore = true
    setupConsoleTestRuntime {
        // 移除 mirai-core 依赖
        classpath = classpath.filter {
            !it.nameWithoutExtension.startsWith("mirai-core-jvm")
        }
    }
}

buildConfig {
    className("EconomyBuildConstants")
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

// 强制让 KSP 任务永远“不过期”
tasks.matching { it.name.startsWith("kspKotlin") }.configureEach {
    outputs.upToDateWhen { false }
}