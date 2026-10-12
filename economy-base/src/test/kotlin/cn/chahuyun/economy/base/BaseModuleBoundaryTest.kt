package cn.chahuyun.economy.base

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class BaseModuleBoundaryTest {

    @Test
    fun `base module does not depend on business modules or the upstream economy plugin`() {
        val forbidden = listOf(
            "cn.chahuyun.economy.data",
            "cn.chahuyun.economy.image",
            "cn.chahuyun.economy.core",
            "cn.chahuyun.economy.game",
            "cn.chahuyun.economy.main",
            "cn.chahuyun.economy.action",
            "cn.chahuyun.economy.usecase",
            "cn.chahuyun.economy.manager",
            "cn.chahuyun.economy.repair",
            "xyz.cssxsh.mirai.economy"
        )

        assertNoForbiddenText(
            roots = listOf(Path.of("src/main/kotlin"), Path.of("build.gradle.kts")),
            forbidden = forbidden
        )
    }

    private fun assertNoForbiddenText(roots: List<Path>, forbidden: List<String>) {
        val hits = roots.flatMap { root ->
            if (root.isRegularFile()) listOf(root) else Files.walk(root).use { stream ->
                stream.filter { it.isRegularFile() && (it.name.endsWith(".kt") || it.name.endsWith(".kts")) }
                    .toList()
            }
        }.flatMap { file ->
            val text = file.readText()
            forbidden.filter { it in text }.map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "economy-base boundary violations:\n${hits.joinToString("\n")}")
    }
}
