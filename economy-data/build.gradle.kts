plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

dependencies {
    api(project(":economy-common"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    compileOnly("cn.chahuyun:hibernate-plus:2.1.1")
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("../src/main/kotlin")
            kotlin.include(
                "cn/chahuyun/economy/model/GlobalFactorDto.kt",
                "cn/chahuyun/economy/model/LotteryInfoDto.kt",
                "cn/chahuyun/economy/model/bank/BankInfoDto.kt",
                "cn/chahuyun/economy/model/fish/FishDto.kt",
                "cn/chahuyun/economy/model/privatebank/**",
                "cn/chahuyun/economy/model/props/PropsDataDto.kt",
                "cn/chahuyun/economy/model/raffle/RaffleRecordDto.kt",
                "cn/chahuyun/economy/model/redpack/**",
                "cn/chahuyun/economy/model/rob/**",
                "cn/chahuyun/economy/model/user/TitleInfoDto.kt",
                "cn/chahuyun/economy/model/user/UserBackpackDto.kt",
                "cn/chahuyun/economy/model/user/UserFactorDto.kt",
                "cn/chahuyun/economy/model/user/UserPropertyDto.kt",
                "cn/chahuyun/economy/model/user/UserRaffleDto.kt",
                "cn/chahuyun/economy/model/user/UserStatusDto.kt",
                "cn/chahuyun/economy/model/yiyan/YiYan.kt",
                "cn/chahuyun/economy/proxy/DataSourceStrategy.kt",
                "cn/chahuyun/economy/proxy/DataSourceStrategyImpl.kt",
                "cn/chahuyun/economy/proxy/DataVersion.kt",
                "cn/chahuyun/economy/proxy/EntityProxy.kt",
                "cn/chahuyun/economy/proxy/MigrationResult.kt",
                "cn/chahuyun/economy/entity/v2/**"
            )
        }
    }
}
