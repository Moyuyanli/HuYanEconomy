package cn.chahuyun.economy.core

import cn.chahuyun.economy.model.user.UserFactorDto
import cn.chahuyun.economy.service.UserFactorBuffCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserFactorBuffCodecTest {

    @Test
    fun `malformed legacy buff data does not break feature commands`() {
        val factor = UserFactorDto(id = 1L, buff = "not-json")

        assertNull(UserFactorBuffCodec.getBuffValue(factor, "red-eyes"))
        assertEquals(
            "123",
            UserFactorBuffCodec.getBuffValue(
                UserFactorBuffCodec.withBuffValue(factor, "red-eyes", "123"),
                "red-eyes",
            ),
        )
    }
}
