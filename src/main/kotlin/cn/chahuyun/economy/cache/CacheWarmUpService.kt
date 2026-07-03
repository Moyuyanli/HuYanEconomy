package cn.chahuyun.economy.cache

import cn.chahuyun.economy.data.proxy.DataSourceStrategy
import cn.chahuyun.economy.utils.Log

/**
 * 缂撳瓨棰勭儹鏈嶅姟
 *
 * 鏈嶅姟鍚姩鏃惰嚜鍔ㄥ姞杞界儹鐐规暟鎹埌Redis銆?
 * 棰勭儹澶辫触涓嶅奖鍝嶆湇鍔℃甯稿惎鍔ㄣ€?
 */
class CacheWarmUpService(
    private val cacheManager: CacheManager,
    private val strategy: DataSourceStrategy
) {
    /**
     * 鎵ц缂撳瓨棰勭儹锛屽湪鎻掍欢onEnable闃舵寮傛璋冪敤
     */
    suspend fun warmUp() {
        if (!strategy.isRedisEnabled()) {
            Log.info("Redis鏈惎鐢紝璺宠繃缂撳瓨棰勭儹")
            return
        }

        Log.info("========== 寮€濮嬬紦瀛橀鐑?==========")
        try {
            // TODO: Phase 3 - 瀹炵幇鍚勬ā鍧楃殑缂撳瓨棰勭儹閫昏緫
            Log.info("========== 缂撳瓨棰勭儹鍏ㄩ儴瀹屾垚 ==========")
        } catch (e: Exception) {
            Log.error("缂撳瓨棰勭儹杩囩▼鍙戠敓寮傚父锛屾湇鍔￠檷绾т负绾疍B妯″紡", e)
        }
    }
}
