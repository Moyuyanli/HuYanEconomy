plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
    `java-library`
}

dependencies {
    api(project(":economy-common"))

    compileOnly("net.mamoe:mirai-core-api:2.16.0")
    compileOnly("cn.chahuyun:hibernate-plus:2.1.1")

    testImplementation(kotlin("test"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}
