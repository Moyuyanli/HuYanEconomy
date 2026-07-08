package cn.chahuyun.economy.game

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class GameModuleBoundaryTest {

    @Test
    fun `game module does not directly access Hibernate internals`() {
        val hits = sourceFiles(Path.of("src/main/kotlin")).flatMap { file ->
            val text = file.readText()
            listOf("HibernateFactory", "cn.chahuyun.hibernate.HibernateFactory", "HibernateDataStore")
                .filter { it in text }
                .map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "game must use data or core services instead of Hibernate internals:\n${hits.joinToString("\n")}")
    }

    @Test
    fun `game user visible text does not contain common mojibake fragments`() {
        val mojibakeFragments = listOf(
            "\u9583", "\u93B6", "\u9352", "\u5BEE", "\u934F", "\u6978", "\u752F", "\u60F0",
            "\u7ECB", "\u93C1", "\u93C3", "\u9422", "\u9359", "\u9428", "\u7F01", "\u7441",
            "\u6769", "\u6FB6", "\u761C", "\u7F08", "\u704F", "\u951F", "\uFFFD"
        )

        val hits = sourceFiles(Path.of("src/main/kotlin")).flatMap { file ->
            val text = file.readText()
            mojibakeFragments.filter { it in text }.map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "game user-visible text contains mojibake fragments:\n${hits.joinToString("\n")}")
    }

    private fun sourceFiles(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".kt") }
                .toList()
        }
}
