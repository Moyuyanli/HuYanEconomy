plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.gmazzo.buildconfig")
    `java-library`
}

dependencies {
    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("../src/main/kotlin")
            kotlin.include(
                "cn/chahuyun/economy/common/**",
                "cn/chahuyun/economy/constant/EconPerm.kt",
                "cn/chahuyun/economy/constant/PropConstant.kt",
                "cn/chahuyun/economy/constant/PropsKind.kt",
                "cn/chahuyun/economy/constant/TitleCode.kt",
                "cn/chahuyun/economy/constant/UserLocation.kt",
                "cn/chahuyun/economy/utils/DateUtil.kt",
                "cn/chahuyun/economy/utils/FormatUtil.kt",
                "cn/chahuyun/economy/utils/Log.kt",
                "cn/chahuyun/economy/utils/MoneyFormatUtil.kt",
                "cn/chahuyun/economy/utils/TimeConvertUtil.kt"
            )
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
