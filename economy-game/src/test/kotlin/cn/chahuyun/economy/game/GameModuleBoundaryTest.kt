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

    /**
     * 第三批迁移后，四个玩法（农场/猜签/抽奖/抢劫）必须完整落在 `games/<玩法>/` 下：
     * 每个目录都有 `Usecase`（带 `@Action`）与 `Rules`（带 `@Before`），
     * 且 `service/`、`manager/`、`usecase/`、`model/` 这些横向包里只剩钓鱼相关的过渡文件。
     *
     * 钓鱼按要求后期重新实现，因此这里显式放行 `Fishing*` 与 `Game*`（协程/概览等游戏级基础设施）。
     */
    @Test
    fun `gameplay code lives under games package`() {
        val gamesRoot = Path.of("src/main/kotlin/cn/chahuyun/economy/games")
        val features = mapOf(
            "farm" to "FarmUsecase.kt",
            "lottery" to "LotteryUsecase.kt",
            "raffle" to "RaffleUsecase.kt",
            "rob" to "RobUsecase.kt",
        )
        val missing = features.flatMap { (feature, usecase) ->
            val dir = gamesRoot.resolve(feature)
            listOf(
                dir.resolve(usecase),
                dir.resolve(usecase.replace("Usecase", "Rules")),
            ).filterNot { it.isRegularFile() }
                .map { "${feature}: ${it.fileName}" }
        }
        assertTrue(missing.isEmpty(), "games/<玩法>/ 必须同时有 Usecase 与 Rules：\n${missing.joinToString("\n")}")

        val legacyRoots = listOf("service", "manager", "usecase", "model").map {
            Path.of("src/main/kotlin/cn/chahuyun/economy/$it")
        }
        val leftovers = legacyRoots.filter { Files.exists(it) }
            .flatMap(::sourceFiles)
            .map { it.name }
            .filterNot { it.startsWith("Fishing") || it.startsWith("Game") }

        assertTrue(
            leftovers.isEmpty(),
            "service/manager/usecase/model 下只允许残留钓鱼与游戏级基础设施，实际还有：\n${leftovers.joinToString("\n")}",
        )
    }

    private fun sourceFiles(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() && it.name.endsWith(".kt") }
                .toList()
        }
}
