plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    `java-library`
}

// 规则链代码生成：facade 生成在声明包内，模块描述生成在 actionchain.generated 包。
ksp {
    arg("actionchain.moduleId", "economy-core")
}

dependencies {
    api(project(":economy-action"))
    ksp(project(":economy-action-ksp"))
    api(project(":economy-common"))
    api(project(":economy-base"))
    api(project(":economy-data"))
    api(project(":economy-image"))

    compileOnly("net.mamoe:mirai-console:2.16.0")
    compileOnly("net.mamoe:mirai-core-api:2.16.0")

    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")

    val auth = "1.3.7"
    compileOnly("cn.chahuyun:HuYanAuthorize:$auth@mirai2.jar")

    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation("com.github.gotson:webp-imageio:0.2.2")

    compileOnly("cn.chahuyun:hibernate-plus:2.1.1")

    testImplementation(kotlin("test"))

    // 经济账本集成测试：主线 hibernate-plus 是 compileOnly（运行期由宿主提供），
    // 测试里需要真实实现 + JDBC 驱动才能起一个文件型 H2 库，验证签到入账真的落库。
    testImplementation("cn.chahuyun:hibernate-plus:2.1.1")
    testImplementation("com.h2database:h2:2.2.224")
    // hibernate-plus 用 Reflections 扫描实体包。
    testImplementation("org.reflections:reflections:0.10.2")
}
