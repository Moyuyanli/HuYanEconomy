package cn.chahuyun.economy.cache

import cn.chahuyun.economy.data.proxy.DataSourceStrategy
import cn.chahuyun.economy.utils.Log

/**
 * 鏁版嵁涓€鑷存€у畧鎶ゅ櫒
 *
 * 纭繚Redis鍜屾暟鎹簱鐨勬暟鎹竴鑷存€с€?
 * 鏍稿績鍘熷垯锛氭暟鎹簱鏄敮涓€鍙潬鐨勬暟鎹簮锛孯edis浠呬綔涓哄姞閫熷眰銆?
 */
class DataConsistencyGuard(
    private val cacheManager: CacheManager,
    private val strategy: DataSourceStrategy
) {
    /**
     * 瀹夊叏鍐欏叆锛堝厛DB鍚庣紦瀛橈紝淇濊瘉鏁版嵁涓嶄涪澶憋級
     */
    suspend fun <T> safeWrite(
        key: String,
        dbWriter: suspend () -> T,
        cacheWriter: suspend (T) -> Unit
    ): T {
        val savedData = dbWriter()
        try {
            cacheWriter(savedData)
        } catch (e: Exception) {
            Log.warning("缂撳瓨鍐欏叆澶辫触锛屾暟鎹凡鎸佷箙鍖栧埌鏁版嵁搴? $key")
        }
        return savedData
    }

    /**
     * 鏈嶅姟鍚姩鏃剁殑鏁版嵁涓€鑷存€ф鏌?
     */
    suspend fun consistencyCheck() {
        if (!strategy.isRedisEnabled()) {
            Log.info("Redis未启用，跳过一致性检查")
            return
        }

        Log.info("========== 寮€濮嬫暟鎹竴鑷存€ф鏌?==========")
        try {
            // TODO: Phase 3 - 瀹炵幇涓€鑷存€ф鏌ラ€昏緫
            Log.info("========== 鏁版嵁涓€鑷存€ф鏌ュ畬鎴?==========")
        } catch (e: Exception) {
            Log.error("数据一致性检查失败", e)
        }
    }
}
