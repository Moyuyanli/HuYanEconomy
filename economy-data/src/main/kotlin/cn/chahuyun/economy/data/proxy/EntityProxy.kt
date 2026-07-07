package cn.chahuyun.economy.data.proxy

/**
 * 实体代理接口。
 *
 * 业务层通过该接口访问数据，无需关心底层数据源版本。
 * 代理内部根据 [DataSourceStrategy] 选择数据源，并通过 Converter 完成 DTO 转换。
 *
 * @param D DTO 类型
 */
interface EntityProxy<D> {

    // ============ 查询操作 ============

    /**
     * 根据 ID 查询。
     */
    fun findById(id: Long): D?

    /**
     * 根据业务键查询，例如 QQ 号、银行编码等。
     */
    fun findByKey(key: String): D? = null

    /**
     * 查询全部。
     */
    fun findAll(): List<D>

    /**
     * 条件查询。
     */
    fun findWhere(predicate: (D) -> Boolean): List<D>

    /**
     * 分页查询。
     */
    fun findPage(offset: Int, limit: Int): List<D> = emptyList()

    // ============ 写入操作 ============

    /**
     * 保存，包含新增或更新。
     */
    fun save(dto: D): D

    /**
     * 批量保存。
     */
    fun saveAll(dtos: List<D>): List<D>

    /**
     * 删除。
     */
    fun delete(id: Long): Boolean

    // ============ 版本迁移支持 ============

    /**
     * 获取当前使用的数据源版本。
     */
    fun getCurrentVersion(): DataVersion

    /**
     * 获取代理器管理的模块名称。
     */
    fun getModuleName(): String

    /**
     * 将数据从当前版本迁移到目标版本。
     */
    fun migrateTo(targetVersion: DataVersion): MigrationResult
}
