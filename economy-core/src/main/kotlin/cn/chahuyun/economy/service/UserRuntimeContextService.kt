package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import java.util.concurrent.ConcurrentHashMap

data class UserRuntimeContext(
    val user: User,
    val group: Group?,
)

object UserRuntimeContextService {

    private val contexts = ConcurrentHashMap<Long, UserRuntimeContext>()

    fun getUser(dto: UserInfoDto): User =
        contexts[dto.qq]?.user ?: error("用户信息未附带 user 对象: ${dto.qq}")

    fun setUser(dto: UserInfoDto, user: User) {
        val old = contexts[dto.qq]
        contexts[dto.qq] = UserRuntimeContext(user, old?.group)
    }

    fun getGroup(dto: UserInfoDto): Group? =
        contexts[dto.qq]?.group

    fun setGroup(dto: UserInfoDto, group: Group?) {
        val old = contexts[dto.qq]
        val runtimeUser = old?.user ?: return
        contexts[dto.qq] = UserRuntimeContext(runtimeUser, group)
    }

    fun attach(dto: UserInfoDto, user: User, group: Group?): UserInfoDto {
        setUser(dto, user)
        setGroup(dto, group)
        return dto
    }
}
