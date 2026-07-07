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
            "閽", "鎶", "鍒", "寮", "鍏", "楸", "甯", "惰",
            "绋", "鏁", "鏃", "鐢", "鍙", "鐨", "缁", "瑙",
            "杩", "澶", "瘜", "缈", "灏", "锟", "�"
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
