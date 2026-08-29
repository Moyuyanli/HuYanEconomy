plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

dependencies {
    api(project(":economy-common"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    compileOnly("cn.chahuyun:hibernate-plus:2.1.1")

    testImplementation(kotlin("test"))
}
allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}
