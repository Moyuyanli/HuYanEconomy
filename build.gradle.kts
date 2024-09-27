@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
import net.mamoe.mirai.console.gradle.wrapNameWithPlatform

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"

    id("net.mamoe.mirai-console") version "2.16.0"

//    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("signing")

}

group = "cn.chahuyun"
version = "1.6.0"

repositories {
    mavenCentral()
//    maven { url "https://maven.aliyun.com/repository/public" }
//    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {

    //依赖
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")
    compileOnly("cn.chahuyun:HuYanSession:2.3.1")
    compileOnly("cn.chahuyun:HuYanAuthorize:1.2.0")

    //使用库
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("cn.hutool:hutool-all:5.8.30")
    implementation("org.apache.poi:poi-ooxml:4.1.2")

    implementation("cn.chahuyun:hibernate-plus:1.0.16")

}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
}


//mavenCentralPublish {
//    useCentralS01()
//
//    licenseApacheV2()
//
//    singleDevGithubProject("moyuyanli", "HuYanEconomy")
//
//    // 设置 Publish 临时目录
//    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
//        ?: buildDir.resolve("publishing-tmp")
//
//    // 设置额外上传内容
//    publication {
//        artifact(tasks["buildPlugin"])
//    }
//}

nexusPublishing {
    repositories {
        create("sonatype") { // 对于2021年2月24日之后注册的用户
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(project.findProperty("sonatypeUsername") as? String ?: System.getenv("SONATYPE_USERNAME"))
            password.set(project.findProperty("sonatypePassword") as? String ?: System.getenv("SONATYPE_PASSWORD"))
        }
    }
}


tasks {
    create<net.mamoe.mirai.console.gradle.BuildMiraiPluginV2>("pluginJar") {
        group = "mirai"
        registerMetadataTask(
            this@tasks,
            "miraiPublicationPrepareMetadata".wrapNameWithPlatform(kotlin.target, true)
        )
        init(kotlin.target)
        destinationDirectory.value(
            project.layout.projectDirectory.dir(project.buildDir.name).dir("mirai")
        )
        archiveExtension.set("mirai2.jar")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(tasks["pluginJar"])

            pom {
                name.set(project.name) // 使用项目名称，或设置为自定义名称
                description.set("这是一个名为 HuYanEconomy 的 Mirai 插件，提供了经济系统功能。")
                url.set("https://github.com/Moyuyanli/HuYanEconomy")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://github.com/Moyuyanli/HuYanEconomy/blob/master/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("Moyuyanli") // 你的GitHub用户名或其他ID
                        name.set("Moyuyanli") // 你的真实姓名
                        email.set("572490972@qq.com") // 你的电子邮件地址
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Moyuyanli/HuYanEconomy.git")
                    developerConnection.set("scm:git:ssh://github.com:Moyuyanli/HuYanEconomy.git")
                    url.set("https://github.com/Moyuyanli/HuYanEconomy")
                }
            }
        }
    }
}


signing {
    val signingKey = findProperty("signingKey")?.toString()
    val signingPassword = findProperty("signingPassword")?.toString()
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(extensions.getByType(PublishingExtension::class).publications.getByName("mavenJava"))
    } else {
        logger.warn("子模块 ${project.name} 未找到签名配置")
    }
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

tasks.create("listTask") {
    doLast {
        println("Lsat available tasks:")
        tasks.forEach { task ->
            println("- ${task.name}")
        }
    }
}

