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

val overflowGatewayVersion = "0.1.0-gateway-SNAPSHOT"

dependencies {
    implementation(project(":economy-common"))
    implementation(project(":economy-data"))
    implementation(project(":economy-image"))
    implementation(project(":economy-core"))
    implementation(project(":economy-game"))

    val auth = "1.3.7"
    compileOnly("cn.chahuyun:HuYanAuthorize:$auth@mirai2.jar")
    ksp("cn.chahuyun:HuYanAuthorize-ksp:$auth")

    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("cn.hutool:hutool-all:5.8.40") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }

    // Hibernate runtime is provided by mirai-hibernate-plugin.
    implementation("cn.chahuyun:hibernate-plus:2.1.1")

    testConsoleRuntime("cn.chahuyun:overflow-gateway:$overflowGatewayVersion")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

configurations.named("testConsoleRuntime") {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

val sourceSets = the<SourceSetContainer>()

tasks.register<JavaExec>("debugOverflowProviderLoad") {
    group = "verification"
    description = "Loads Overflow adapter jars on the same runtime classpath used by runConsole."
    classpath = sourceSets["test"].runtimeClasspath + configurations.named("testConsoleRuntime").get()
    mainClass.set("top.mrxiaom.overflow.gateway.loader.ProviderLoadDebugMainKt")
    args(layout.projectDirectory.dir("debug-sandbox/adapters").asFile.absolutePath)
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
        ":economy-base:jar",
        ":economy-data:jar",
        ":economy-image:jar",
        ":economy-action:jar",
        ":economy-core:jar",
        ":economy-game:jar"
    )
}

afterEvaluate {
    tasks.named<JavaExec>("runConsole").configure {
        val gatewayConfig = workingDir.resolve("overflow.json")
        val exampleGatewayConfig = project.file("src/test/resources/overflow.example.json")
        systemProperty("overflow.config", gatewayConfig.absolutePath)
        systemProperty("overflow.data", workingDir.resolve("overflow-gateway").absolutePath)
        systemProperty("overflow.adapters", workingDir.resolve("adapters").absolutePath)

        doFirst {
            if (!gatewayConfig.exists()) {
                gatewayConfig.parentFile.mkdirs()
                exampleGatewayConfig.copyTo(gatewayConfig)
                logger.lifecycle("Created runConsole Gateway configuration from: ${exampleGatewayConfig.absolutePath}")
            }
            require(gatewayConfig.isFile) {
                "Missing runConsole Overflow configuration: ${gatewayConfig.absolutePath}"
            }
            workingDir.resolve("adapters").mkdirs()
            logger.lifecycle(
                "runConsole uses Overflow Gateway snapshot: cn.chahuyun:overflow-gateway:$overflowGatewayVersion"
            )
            logger.lifecycle("Gateway configuration: ${gatewayConfig.absolutePath}")
        }

        fun pluginJarCandidates(pluginsDir: File) = pluginsDir.listFiles { file ->
            file.isFile &&
                    file.name.endsWith(".mirai2.jar") &&
                    (file.name.startsWith("${project.name}-") || file.name.startsWith("${rootProject.name}-"))
        }.orEmpty()

        val cleanDevPluginJars = Action<Task> {
            val pluginsDir = workingDir.resolve("plugins")
            pluginJarCandidates(pluginsDir).forEach { it.delete() }
        }

        val syncDevPluginJar = Action<Task> {
            val pluginsDir = workingDir.resolve("plugins")
            val generatedJar = pluginsDir.resolve("${project.name}-dev.mirai2.jar")
            val expectedJar = pluginsDir.resolve("${rootProject.name}-dev.mirai2.jar")

            if (generatedJar.exists()) {
                generatedJar.copyTo(expectedJar, overwrite = true)
            }

            pluginJarCandidates(pluginsDir)
                .filterNot { it.name == expectedJar.name }
                .forEach { it.delete() }
        }

        actions.add(0, cleanDevPluginJars)
        actions.add(actions.lastIndex, syncDevPluginJar)
    }
}
