plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    id("net.mamoe.mirai-console")
    id("org.jetbrains.dokka")
    id("com.google.devtools.ksp")

    `java-library`
}

base {
    archivesName.set(rootProject.name)
}

dependencies {
    implementation(project(":economy-common"))
    implementation(project(":economy-data"))
    implementation(project(":economy-image"))
    implementation(project(":economy-core"))
    implementation(project(":economy-game"))

    val ofVersion = "1.0.8"
    compileOnly("top.mrxiaom.mirai:overflow-core-api:$ofVersion")
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")

    val auth = "1.3.7"
    compileOnly("cn.chahuyun:HuYanAuthorize:$auth")
    ksp("cn.chahuyun:HuYanAuthorize-ksp:$auth")

    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation("org.apache.poi:poi-ooxml:5.4.0") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }

    // Hibernate runtime is provided by mirai-hibernate-plugin.
    implementation("cn.chahuyun:hibernate-plus:2.1.1")

    testConsoleRuntime("top.mrxiaom.mirai:overflow-core:$ofVersion")
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("../src/main/kotlin")
            kotlin.exclude(
                "cn/chahuyun/economy/common/**",
                "cn/chahuyun/economy/constant/EconPerm.kt",
                "cn/chahuyun/economy/constant/PropConstant.kt",
                "cn/chahuyun/economy/constant/PropsKind.kt",
                "cn/chahuyun/economy/constant/TitleCode.kt",
                "cn/chahuyun/economy/constant/UserLocation.kt",
                "cn/chahuyun/economy/utils/DateUtil.kt",
                "cn/chahuyun/economy/utils/EconomyImageRenderer.kt",
                "cn/chahuyun/economy/utils/FormatUtil.kt",
                "cn/chahuyun/economy/utils/ImageUtil.kt",
                "cn/chahuyun/economy/utils/Log.kt",
                "cn/chahuyun/economy/utils/MoneyFormatUtil.kt",
                "cn/chahuyun/economy/utils/TimeConvertUtil.kt",
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
                "cn/chahuyun/economy/entity/v2/**",
                "cn/chahuyun/economy/privatebank/PrivateBankLedger.kt",
                "cn/chahuyun/economy/constant/FarmConstants.kt",
                "cn/chahuyun/economy/constant/FishPondLevelConstant.kt",
                "cn/chahuyun/economy/constant/PrizeType.kt",
                "cn/chahuyun/economy/constant/RaffleType.kt"
            )
        }
        named("test") {
            kotlin.srcDir("../src/test/kotlin")
        }
    }
}

sourceSets {
    named("main") {
        java.srcDirs("../src/main/java")
        resources.srcDirs("../src/main/resources")
    }
    named("test") {
        java.srcDirs("../src/test/java")
        resources.srcDirs("../src/test/resources")
    }
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
    noTestCore = true
    setupConsoleTestRuntime {
        classpath = classpath.filter {
            !it.nameWithoutExtension.startsWith("mirai-core-jvm")
        }
    }
}

tasks.matching { it.name.startsWith("kspKotlin") }.configureEach {
    outputs.upToDateWhen { false }
}

tasks.matching { it.name == "buildPlugin" }.configureEach {
    dependsOn(
        ":economy-common:jar",
        ":economy-data:jar",
        ":economy-image:jar",
        ":economy-core:jar",
        ":economy-game:jar"
    )
}
