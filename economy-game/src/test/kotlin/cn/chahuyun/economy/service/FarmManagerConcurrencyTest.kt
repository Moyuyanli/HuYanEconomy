package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.FarmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FarmManagerConcurrencyTest {

    @Test
    fun `two user lock allows only one concurrent steal marker`() {
        runBlocking {
            var stolen = false
            var successfulSteals = 0

            listOf(
                async {
                    FarmManager.withTwoUserLocks(10001L, 20001L, Dispatchers.Default) {
                        if (!stolen) {
                            Thread.sleep(10)
                            stolen = true
                            successfulSteals += 1
                        }
                    }
                },
                async {
                    FarmManager.withTwoUserLocks(30001L, 20001L, Dispatchers.Default) {
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
