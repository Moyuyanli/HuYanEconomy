package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.data.repository.UserFishingInfoRepository
import cn.chahuyun.economy.data.repository.UserPondRepository
import cn.chahuyun.economy.entity.fishing.UserFishingInfoEntity
import cn.chahuyun.economy.entity.fishing.UserPondEntity

/**
 * 用户鱼塘（`hye_user_pond`）服务。
 *
 * 新版模型中鱼塘归属用户，[UserPondEntity.level] 决定可钓到的鱼的等级上限，
 * [UserPondEntity.feeRatePermille] 为该鱼塘的收费率（千分比）。
 */
object FishingPondService {

    /** 用户默认鱼塘名称后缀。 */
    private const val POND_SUFFIX = "的鱼塘"

    /** 默认收费率：5%（千分比）。 */
    const val DEFAULT_FEE_PERMILLE: Int = 50

    /**
     * 取得用户当前使用的鱼塘；未设置默认鱼塘时自动创建一个 1 级鱼塘并写回默认。
     */
    fun resolveDefaultPond(fishInfo: UserFishingInfoEntity, userId: Long, nickname: String): UserPondEntity {
        val defaultId = fishInfo.defaultPondId
        if (defaultId != null) {
            UserPondRepository.findById(defaultId)?.let { return it }
        }
        val pond = createPond(userId, "${nickname.ifBlank { "无名" }}$POND_SUFFIX")
        fishInfo.defaultPondId = pond.id
        fishInfo.touch()
        UserFishingInfoRepository.save(fishInfo)
        return pond
    }

    /** 创建鱼塘；同名鱼塘已存在时直接返回已有记录。 */
    fun createPond(userId: Long, name: String): UserPondEntity {
        UserPondRepository.findByUserAndName(userId, name)?.let { return it }
        return UserPondRepository.save(
            UserPondEntity(
                userId = userId,
                name = name,
                level = FishPondLevelConstant.MIN_LEVEL,
                feeRatePermille = DEFAULT_FEE_PERMILLE,
            )
        )
    }

    /** 创建自定义名称鱼塘；同名时返回 null 由调用方提示。 */
    fun createNamedPond(userId: Long, name: String): UserPondEntity? {
        if (UserPondRepository.findByUserAndName(userId, name) != null) return null
        return UserPondRepository.save(
            UserPondEntity(
                userId = userId,
                name = name,
                level = FishPondLevelConstant.MIN_LEVEL,
                feeRatePermille = DEFAULT_FEE_PERMILLE,
            )
        )
    }

    fun listByUser(userId: Long): List<UserPondEntity> = UserPondRepository.listByUser(userId)

    /** 鱼塘可钓的鱼等级上限。 */
    fun maxFishLevel(pond: UserPondEntity): Int = pond.level.coerceAtLeast(1)

    /** 鱼塘收费率（0.0~1.0）。 */
    fun feeRate(pond: UserPondEntity): Double = pond.feeRatePermille.coerceIn(0, 1000) / 1000.0

    /** 升到下一级所需的等级常量；已满级返回 null。 */
    fun nextUpgrade(pond: UserPondEntity): FishPondLevelConstant? =
        if (pond.level >= FishPondLevelConstant.MAX_LEVEL) null else FishPondLevelConstant.of(pond.level + 1)

    fun incrementFishingCount(pond: UserPondEntity) {
        pond.totalFishingCount += 1
        pond.touch()
        UserPondRepository.save(pond)
    }
}
