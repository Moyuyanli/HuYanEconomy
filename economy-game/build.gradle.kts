plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

dependencies {
    api(project(":economy-common"))
    api(project(":economy-data"))
    api(project(":economy-image"))
    api(project(":economy-core"))
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("../src/main/kotlin")
            kotlin.include(
                "cn/chahuyun/economy/constant/FarmConstants.kt",
                "cn/chahuyun/economy/constant/FishPondLevelConstant.kt",
                "cn/chahuyun/economy/constant/PrizeType.kt",
                "cn/chahuyun/economy/constant/RaffleType.kt"
            )
        }
    }
}
