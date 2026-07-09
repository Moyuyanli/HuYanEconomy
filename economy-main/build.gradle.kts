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
    testImplementation(kotlin("test"))
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

afterEvaluate {
    tasks.named<JavaExec>("runConsole").configure {
        fun pluginJarCandidates(pluginsDir: File) = pluginsDir.listFiles { file ->
            file.isFile &&
                file.name.endsWith(".mirai2.jar") &&
                (file.name.startsWith("${project.name}-") || file.name.startsWith("${rootProject.name}-"))
        }.orEmpty()

        val cleanDevPluginJars = org.gradle.api.Action<org.gradle.api.Task> {
            val pluginsDir = workingDir.resolve("plugins")
            pluginJarCandidates(pluginsDir).forEach { it.delete() }
        }

        val syncDevPluginJar = org.gradle.api.Action<org.gradle.api.Task> {
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
