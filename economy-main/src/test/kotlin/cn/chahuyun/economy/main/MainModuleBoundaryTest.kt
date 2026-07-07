package cn.chahuyun.economy.main

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class MainModuleBoundaryTest {

    @Test
    fun `main module does not directly access HibernateFactory`() {
        val hits = sourceFiles(Path.of("src/main/kotlin")).flatMap { file ->
            val text = file.readText()
            listOf("HibernateFactory", "cn.chahuyun.hibernate.HibernateFactory")
                .filter { it in text }
                .map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "main must initialize the plugin without direct HibernateFactory access:\n${hits.joinToString("\n")}")
    }

    @Test
    fun `GamesAction stays as command entry and fish events stay in GameEventService`() {
        val gamesAction = Path.of("src/main/kotlin/cn/chahuyun/economy/action/GamesAction.kt").readText()
        val eventService = Path.of("src/main/kotlin/cn/chahuyun/economy/service/GameEventService.kt").readText()

        val actionForbidden = listOf("CoroutineScope", "FishStartEvent", "FishRollEvent", "companion object")
            .filter { it in gamesAction }

        assertTrue(
            actionForbidden.isEmpty(),
            "GamesAction must stay a thin command entry, but found:\n${actionForbidden.joinToString("\n")}"
        )

        val serviceRequired = listOf("FishStartEvent", "FishRollEvent", "GamesUsecase.fishStart", "GamesUsecase.fishRoll")
            .filterNot { it in eventService }

        assertTrue(
            serviceRequired.isEmpty(),
            "GameEventService must keep custom fish event delegation:\n${serviceRequired.joinToString("\n")}"
        )
    }

    @Test
    fun `main user visible entry text does not contain common mojibake fragments`() {
        val roots = listOf(
            Path.of("src/main/kotlin/cn/chahuyun/economy/action"),
            Path.of("src/main/kotlin/cn/chahuyun/economy/command"),
            Path.of("src/main/kotlin/cn/chahuyun/economy/service"),
            Path.of("src/main/kotlin/cn/chahuyun/economy/plugin")
        )

        val mojibakeFragments = listOf(
            "閽", "鎶", "鍒", "寮", "鍏", "楸", "甯", "惰",
            "绋", "鏁", "鏃", "鐢", "鍙", "鐨", "缁", "瑙",
            "杩", "锟", "�"
        )

        val hits = roots.flatMap(::sourceFiles).flatMap { file ->
            val text = file.readText()
            mojibakeFragments.filter { it in text }.map { "${file}: $it" }
        }

        assertTrue(hits.isEmpty(), "main entry user-visible text contains mojibake fragments:\n${hits.joinToString("\n")}")
    }

    private fun sourceFiles(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".kt") }
                .toList()
        }
}
