package cn.chahuyun.economy.model.user

import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.runtime.EconomyRuntime.config
import cn.chahuyun.economy.service.*
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User

object UserInfoBehaviorService {

    fun getUser(dto: UserInfoDto): User =
        UserRuntimeContextService.getUser(dto)

    fun setUser(dto: UserInfoDto, user: User) =
        UserRuntimeContextService.setUser(dto, user)

    fun getGroup(dto: UserInfoDto): Group? =
        UserRuntimeContextService.getGroup(dto)

    fun setGroup(dto: UserInfoDto, group: Group?) =
        UserRuntimeContextService.setGroup(dto, group)

    fun attachRuntime(dto: UserInfoDto, user: User, group: Group?): UserInfoDto =
        UserRuntimeContextService.attach(dto, user, group)

    fun sign(dto: UserInfoDto): Boolean =
        UserSignRuleService.applySign(dto, config.reSignTime)

    fun getFishInfo(dto: UserInfoDto): FishInfoDto =
        UserFishInfoService.findOrCreate(dto)

    fun getString(dto: UserInfoDto): String {
        return EconomyUserProfileFormatter.basicInfo(dto)
    }

    fun getProp(dto: UserInfoDto, code: String): UserBackpackDto {
        return EconomyInventoryService.findUserProp(dto, code)
    }

    fun getPropOrNull(dto: UserInfoDto, code: String): UserBackpackDto? {
        return EconomyInventoryService.findUserPropOrNull(dto, code)
    }
}

var UserInfoDto.user: User
    get() = UserInfoBehaviorService.getUser(this)
    set(value) = UserInfoBehaviorService.setUser(this, value)

var UserInfoDto.group: Group?
    get() = UserInfoBehaviorService.getGroup(this)
    set(value) = UserInfoBehaviorService.setGroup(this, value)

fun UserInfoDto.attachRuntime(user: User, group: Group?): UserInfoDto =
    UserInfoBehaviorService.attachRuntime(this, user, group)

fun UserInfoDto.sign(): Boolean =
    UserInfoBehaviorService.sign(this)

fun UserInfoDto.getFishInfo(): FishInfoDto =
    UserInfoBehaviorService.getFishInfo(this)

fun UserInfoDto.getString(): String =
    UserInfoBehaviorService.getString(this)

fun UserInfoDto.getProp(code: String): UserBackpackDto =
    UserInfoBehaviorService.getProp(this, code)

fun UserInfoDto.getPropOrNull(code: String): UserBackpackDto? =
    UserInfoBehaviorService.getPropOrNull(this, code)
