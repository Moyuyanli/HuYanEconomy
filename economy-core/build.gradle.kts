plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

dependencies {
    api(project(":economy-common"))
    api(project(":economy-data"))
    api(project(":economy-image"))

    val ofVersion = "1.0.8"
    compileOnly("net.mamoe:mirai-console:2.16.0")
    compileOnly("net.mamoe:mirai-core-api:2.16.0")
    compileOnly("top.mrxiaom.mirai:overflow-core-api:$ofVersion")
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")

    val auth = "1.3.7"
    compileOnly("cn.chahuyun:HuYanAuthorize:$auth")

    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation("com.github.gotson:webp-imageio:0.2.2")

    compileOnly("cn.chahuyun:hibernate-plus:2.1.1")

    testImplementation(kotlin("test"))
}
