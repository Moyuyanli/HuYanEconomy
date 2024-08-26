plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"

    id("net.mamoe.mirai-console") version "2.16.0"

    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "cn.chahuyun"
version = "1.4.5"

repositories {
    mavenCentral()
//    maven { url "https://maven.aliyun.com/repository/public" }
//    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {

    //依赖
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")
    compileOnly("cn.chahuyun:HuYanSession:2.3.1")
    compileOnly("cn.chahuyun:HuYanAuthorize:1.0.0")
    //使用库
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("cn.hutool:hutool-all:5.8.30")
    implementation("org.apache.poi:poi-ooxml:4.1.2")

    implementation("cn.chahuyun:hibernate-plus:1.0.15")

}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
}


mavenCentralPublish {
    useCentralS01()

    licenseApacheV2()

    singleDevGithubProject("moyuyanli", "HuYanEconomy")

    // 设置 Publish 临时目录
    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
        ?: buildDir.resolve("publishing-tmp")

    // 设置额外上传内容
    publication {
        artifact(tasks["buildPlugin"])
    }
}