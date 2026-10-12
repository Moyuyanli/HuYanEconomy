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

    // H2 集成测试：主线上 hibernate-plus / mirai 都是 compileOnly（运行期由宿主提供），
    // 测试里需要真实实现 + JDBC 驱动才能起一个内存/文件型数据库。
    testImplementation("cn.chahuyun:hibernate-plus:2.1.1")
    testImplementation("com.h2database:h2:2.2.224")
    // hibernate-plus 用 Reflections 扫描实体包，运行期需要它。
    testImplementation("org.reflections:reflections:0.10.2")
    // 只需要 mirai 的类型参与编译；测试不碰 User，运行期不需要整个 mirai 依赖图。
    testCompileOnly("net.mamoe:mirai-core-api:2.16.0")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}
