package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.user.UserInfoEntity

/**
 * 新一代用户主表（`hye_user_info`）数据访问层。
 *
 * 2.0 新实体体系要求所有业务表的 `user_id` 引用 `hye_user_info.id`；
 * 该仓储只负责按 QQ 解析/创建稳定的内部主键，业务模块不得自行把 QQ 写入 `user_id`。
 */
object NewUserInfoRepository {

    @JvmStatic
    fun findById(id: Long): UserInfoEntity? =
        HibernateDataStore.selectOneById(UserInfoEntity::class.java, id)

    @JvmStatic
    fun findByQq(qq: Long): UserInfoEntity? =
        HibernateDataStore.selectOne(UserInfoEntity::class.java, "qq", qq)

    @JvmStatic
    fun listAll(): List<UserInfoEntity> =
        HibernateDataStore.selectList(UserInfoEntity::class.java)

    @JvmStatic
    fun save(entity: UserInfoEntity): UserInfoEntity =
        HibernateDataStore.merge(entity)

    /**
     * 解析用户内部主键；不存在时创建一条 `hye_user_info` 记录。
     *
     * @param qq 外部 QQ 身份
     * @param name 用户昵称（为空时回落为 QQ）
     * @param registerGroup 首次注册群标识（可为空字符串）
     * @return `hye_user_info.id`
     */
    @JvmStatic
    fun getOrCreateId(qq: Long, name: String, registerGroup: String): Long {
        val existing = findByQq(qq)
        if (existing != null) {
            if (name.isNotBlank() && existing.name != name) {
                existing.name = name
                existing.touch()
                save(existing)
            }
            return existing.id
        }
        val created = save(
            UserInfoEntity(
                qq = qq,
                userKey = "u$qq",
                name = name.ifBlank { qq.toString() },
                registerGroup = registerGroup,
            )
        )
        return created.id
    }
}
