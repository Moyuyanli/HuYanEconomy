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
        val allowedLegacyDatabaseTexts = listOf(
            "[鍙槸涓紶璇碷",
            "[澶у瘜缈乚",
            "[灏忓瘜缈乚"
        )
        val mojibakeFragments = listOf(
            "閽", "鎶", "鍒", "寮", "鍏", "楸", "甯", "惰",
            "绋", "鏁", "鏃", "鐢", "鍙", "鐨", "缁", "瑙",
            "杩", "澶", "瘜", "缈", "灏", "锟", "�"
        )

        val hits = roots.flatMap(::sourceFiles).flatMap { file ->
            val sanitizedText = allowedLegacyDatabaseTexts.fold(file.readText()) { text, allowed ->
                text.replace(allowed, "")
            }
            mojibakeFragments.filter { it in sanitizedText }.map { "${file}: $it" }
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
