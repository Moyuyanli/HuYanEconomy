package cn.chahuyun.economy.data

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class DataModuleBoundaryTest {

    @Test
    fun `data module does not depend on upper modules or mirai sending types`() {
        val forbidden = listOf(
            "cn.chahuyun.economy.core",
            "cn.chahuyun.economy.game",
            "cn.chahuyun.economy.main",
            "cn.chahuyun.economy.image",
            "cn.chahuyun.economy.manager",
            "cn.chahuyun.economy.usecase",
            "cn.chahuyun.economy.action",
            "cn.chahuyun.economy.plugin",
            "cn.chahuyun.economy.privatebank",
            "cn.chahuyun.economy.utils.EconomyUtil",
            "cn.chahuyun.economy.utils.MessageUtil",
            "net.mamoe",
            "KotlinPlugin",
            "JvmPlugin",
            "uploadImage",
            "sendMessage"
        )

        assertNoForbiddenText(
            roots = listOf(Path.of("src/main/kotlin"), Path.of("build.gradle.kts")),
            forbidden = forbidden
        )
    }

    @Test
    fun `data proxy modules do not directly access HibernateDataStore`() {
        val hits = Files.walk(Path.of("src/main/kotlin/cn/chahuyun/economy/data/proxy/module")).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".kt") }
                .filter { "HibernateDataStore" in it.readText() }
                .map { it.toString() }
                .toList()
        }

        assertTrue(hits.isEmpty(), "data proxy modules must use repositories:\n${hits.joinToString("\n")}")
    }

    @Test
    fun `data proxy and repository text does not contain common mojibake fragments`() {
        val mojibakeFragments = listOf(
            "閽", "鎶", "鍒", "寮", "鍏", "楸", "甯", "惰",
            "绋", "鏁", "鏃", "鐢", "鍙", "鐨", "缁", "瑙",
            "杩", "澶", "瘜", "缈", "灏", "锟", "�"
        )

        val hits = listOf(
            Path.of("src/main/kotlin/cn/chahuyun/economy/data/proxy"),
            Path.of("src/main/kotlin/cn/chahuyun/economy/data/repository")
        ).flatMap { root ->
            Files.walk(root).use { stream ->
                stream.filter { it.isRegularFile() && it.name.endsWith(".kt") }
                    .toList()
            }
        }.flatMap { file ->
            val text = file.readText()
            mojibakeFragments.filter { it in text }.map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "data proxy/repository text contains mojibake fragments:\n${hits.joinToString("\n")}")
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

        assertTrue(hits.isEmpty(), "data boundary violations:\n${hits.joinToString("\n")}")
    }
}
