package cn.chahuyun.economy.core

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class CoreModuleBoundaryTest {

    @Test
    fun `core module does not directly access HibernateFactory`() {
        val hits = sourceFiles(Path.of("src/main/kotlin")).flatMap { file ->
            val text = file.readText()
            listOf("HibernateFactory", "cn.chahuyun.hibernate.HibernateFactory")
                .filter { it in text }
                .map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "core must use data repositories or proxies instead of HibernateFactory:\n${hits.joinToString("\n")}")
    }

    @Test
    fun `core manager and usecase user visible text does not contain unexpected mojibake fragments`() {
        val roots = listOf(
            Path.of("src/main/kotlin/cn/chahuyun/economy/manager"),
            Path.of("src/main/kotlin/cn/chahuyun/economy/usecase")
        )
        val mojibakeFragments = listOf(
            "\u9583", "\u93B6", "\u9352", "\u5BEE", "\u934F", "\u6978", "\u752F", "\u60F0",
            "\u7ECB", "\u93C1", "\u93C3", "\u9422", "\u9359", "\u9428", "\u7F01", "\u7441",
            "\u6769", "\u6FB6", "\u761C", "\u7F08", "\u704F", "\u951F", "\uFFFD"
        )

        val hits = roots.flatMap(::sourceFiles).flatMap { file ->
            val text = file.readText()
            mojibakeFragments.filter { it in text }.map { "${file}: $it" }
        }

        assertTrue(
            hits.isEmpty(),
            "core manager/usecase user-visible text contains unexpected mojibake fragments:\n${hits.joinToString("\n")}"
        )
    }

    private fun sourceFiles(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".kt") }
                .toList()
        }
}
