import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension

fun Project.setupMavenCentralPublication(artifactsBlock: MavenPublication.() -> Unit) {
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    extensions.configure(PublishingExtension::class) {
        publications {
            create<MavenPublication>("maven") {
                from(components.getByName("kotlin"))
                groupId = rootProject.group.toString()
                artifactId = project.name
                version = rootProject.version.toString()

                artifactsBlock()
                pom(mavenPom(artifactId))
            }
        }
    }
    extensions.configure(SigningExtension::class) {
        val signingKey = findProperty("signingKey")?.toString()
        val signingPassword = findProperty("signingPassword")?.toString()
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(extensions.getByType(PublishingExtension::class).publications.getByName("maven"))
        } else {
            logger.warn("子模块 ${project.name} 未找到签名配置")
        }
    }
}
fun mavenPom(artifactId: String): Action<MavenPom> = action {
    name.set(artifactId) // 使用项目名称，或设置为自定义名称
    description.set("这是一个名为 HuYanEconomy 的 Mirai 插件，提供了经济系统功能。")
    url.set("https://github.com/Moyuyanli/HuYanEconomy")

    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("https://github.com/Moyuyanli/HuYanEconomy/blob/master/LICENSE")
        }
    }

    developers {
        developer {
            id.set("Moyuyanli") // 你的GitHub用户名或其他ID
            name.set("Moyuyanli") // 你的真实姓名
            email.set("572490972@qq.com") // 你的电子邮件地址
        }
    }

    scm {
        connection.set("scm:git:git://github.com/Moyuyanli/HuYanEconomy.git")
        developerConnection.set("scm:git:ssh://github.com:Moyuyanli/HuYanEconomy.git")
        url.set("https://github.com/Moyuyanli/HuYanEconomy")
    }
}

inline fun <reified T : Any> action(
    crossinline block: T.() -> Unit
): Action<T> = Action<T> { block() }


