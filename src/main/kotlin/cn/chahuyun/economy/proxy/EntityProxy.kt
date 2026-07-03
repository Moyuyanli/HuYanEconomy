package cn.chahuyun.economy.data.proxy

/**
 * 瀹炰綋浠ｇ悊鍣ㄦ帴鍙?
 *
 * 涓氬姟灞傞€氳繃姝ゆ帴鍙ｈ闂暟鎹紝鏃犻渶鍏冲績搴曞眰鏁版嵁婧愮増鏈€?
 * 浠ｇ悊鍣ㄥ唴閮ㄦ牴鎹?[DataSourceStrategy] 鑷姩閫夋嫨鏁版嵁婧愶紝骞堕€氳繃 Converter 瀹屾垚 DTO 杞崲銆?
 *
 * @param D DTO绫诲瀷
 */
interface EntityProxy<D> {

    // ============ 鏌ヨ鎿嶄綔 ============

    /**
     * 鏍规嵁ID鏌ヨ
     */
    fun findById(id: Long): D?

    /**
     * 鏍规嵁涓氬姟閿煡璇紙濡俀Q鍙枫€侀摱琛岀紪鐮佺瓑锛?
     */
    fun findByKey(key: String): D? = null

    /**
     * 鏌ヨ鎵€鏈?
     */
    fun findAll(): List<D>

    /**
     * 鏉′欢鏌ヨ
     */
    fun findWhere(predicate: (D) -> Boolean): List<D>

    /**
     * 鍒嗛〉鏌ヨ
     */
    fun findPage(offset: Int, limit: Int): List<D> = emptyList()

    // ============ 鍐欏叆鎿嶄綔 ============

    /**
     * 淇濆瓨锛堟柊澧炴垨鏇存柊锛?
     */
    fun save(dto: D): D

    /**
     * 鎵归噺淇濆瓨
     */
    fun saveAll(dtos: List<D>): List<D>

    /**
     * 鍒犻櫎
     */
    fun delete(id: Long): Boolean

    // ============ 鐗堟湰杩佺Щ鏀寔 ============

    /**
     * 鑾峰彇褰撳墠浣跨敤鐨勬暟鎹簮鐗堟湰
     */
    fun getCurrentVersion(): DataVersion

    /**
     * 鑾峰彇浠ｇ悊鍣ㄧ鐞嗙殑妯″潡鍚嶇О
     */
    fun getModuleName(): String

    /**
     * 灏嗘暟鎹粠褰撳墠鐗堟湰杩佺Щ鍒扮洰鏍囩増鏈?
     */
    fun migrateTo(targetVersion: DataVersion): MigrationResult
}
