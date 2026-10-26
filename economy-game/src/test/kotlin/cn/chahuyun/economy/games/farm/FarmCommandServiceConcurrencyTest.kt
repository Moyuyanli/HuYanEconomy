package cn.chahuyun.economy.games.farm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FarmCommandServiceConcurrencyTest {

    @Test
    fun `two user lock allows only one concurrent steal marker`() {
        runBlocking {
            var stolen = false
            var successfulSteals = 0

            listOf(
                async {
                    FarmCommandService.withTwoUserLocks(10001L, 20001L, Dispatchers.Default) {
                        if (!stolen) {
                            Thread.sleep(10)
                            stolen = true
                            successfulSteals += 1
                        }
                    }
                },
                async {
                    FarmCommandService.withTwoUserLocks(30001L, 20001L, Dispatchers.Default) {
                        if (!stolen) {
                            Thread.sleep(10)
                            stolen = true
                            successfulSteals += 1
                        }
                    }
                },
            ).awaitAll()

            assertEquals(1, successfulSteals)
        }
    }
}
