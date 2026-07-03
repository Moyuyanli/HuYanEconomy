plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

dependencies {
    api(project(":economy-common"))
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("../src/main/kotlin")
            kotlin.include(
                "cn/chahuyun/economy/utils/EconomyImageRenderer.kt",
                "cn/chahuyun/economy/utils/ImageUtil.kt"
            )
        }
    }
}
