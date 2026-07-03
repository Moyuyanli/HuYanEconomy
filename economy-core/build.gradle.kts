plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

dependencies {
    api(project(":economy-common"))
    api(project(":economy-data"))
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("../src/main/kotlin")
            kotlin.include("cn/chahuyun/economy/privatebank/PrivateBankLedger.kt")
        }
    }
}
