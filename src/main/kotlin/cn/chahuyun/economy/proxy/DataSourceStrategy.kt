package cn.chahuyun.economy.data.proxy

/**
 * 鏁版嵁婧愮瓥鐣ユ帴鍙?
 *
 * 鍐冲畾鏌愪釜妯″潡浣跨敤鍝釜鐗堟湰鐨勬暟鎹簮銆?
 * 閫氳繃閰嶇疆鍙互鎺у埗涓嶅悓妯″潡浣跨敤涓嶅悓鐨勬暟鎹簮鐗堟湰锛屽疄鐜板钩婊戣縼绉汇€?
 */
interface DataSourceStrategy {

    /**
     * 鑾峰彇鎸囧畾妯″潡鐨勬暟鎹簮鐗堟湰
     *
     * @param module 妯″潡鍚嶇О锛屽 "user", "bank", "fish" 绛?
     * @return 鏁版嵁婧愮増鏈?
     */
    fun getVersion(module: String): DataVersion

    /**
     * 鏄惁鍚敤Redis缂撳瓨
     */
    fun isRedisEnabled(): Boolean

    /**
     * 鑾峰彇闄嶇骇閾撅紙褰撻閫夋暟鎹簮涓嶅彲鐢ㄦ椂鐨勯檷绾ч『搴忥級
     */
    fun getFallbackChain(module: String): List<DataVersion>
}
