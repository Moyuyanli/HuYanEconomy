package cn.chahuyun.economy.manager

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText

/**
 * 鑳屽寘绠＄悊鍣?
 * 鑳屽寘鐩稿叧鐨?闈炰簨浠剁洃鍚?閫昏緫銆?
 *
 * 璇存槑锛?
 * - `action.BackpackAction` 浠呬繚鐣欐寚浠ゅ叆鍙ｄ笌鍙傛暟瑙ｆ瀽銆?
 * - 閬撳叿澧炲垹鏌ャ€佽儗鍖呭唴瀹规覆鏌撶瓑鍙鐢ㄩ€昏緫涓嬫矇鍒拌繖閲屻€?
 */
object BackpackManager {

    /**
     * 鏄剧ず鐢ㄦ埛鑳屽寘鍐呭
     *
     * @param bot 鏈哄櫒浜哄疄渚?
     * @param backpacks 鐢ㄦ埛鑳屽寘閬撳叿鍒楄〃
     * @param group 缇ょ粍瀹炰緥
     * @param currentPage 褰撳墠椤电爜
     * @param maxPage 鏈€澶ч〉鐮?
     */
    suspend fun showBackpack(
        bot: Bot,
        backpacks: List<UserBackpackDto>,
        group: Group,
        currentPage: Int,
        maxPage: Int,
    ) {
        val nodes = ForwardMessageBuilder(group)
        nodes.add(bot, PlainText("浠ヤ笅鏄綘鐨勮儗鍖呪啌:"))

        // 閬嶅巻鑳屽寘涓殑閬撳叿骞舵坊鍔犲埌娑堟伅鑺傜偣涓?
        for (backpack in backpacks) {
            val prop = PropsManager.getProp(backpack) ?: continue
            nodes.add(bot, PlainText("鐗╁搧id:${backpack.propId}\n$prop"))
        }
        nodes.add(bot, MessageUtil.formatMessage("--- 褰撳墠椤垫暟: ${currentPage} / 鏈€澶ч〉鏁? ${maxPage} ---"))
        group.sendMessage(nodes.build())
    }

    /**
     * 娣诲姞涓€涓亾鍏峰埌鑳屽寘
     *
     * @param userInfo 鐢ㄦ埛淇℃伅
     * @param code 閬撳叿缂栫爜
     * @param kind 閬撳叿绫诲瀷
     * @param id 閬撳叿ID
     */
    @JvmStatic
    fun addPropToBackpack(userInfo: UserInfoDto, code: String, kind: String, id: Long): UserBackpackDto {
        require(id != 0L) { "涓嶈兘娣诲姞鏃犳晥閬撳叿鍒拌儗鍖? propId=$id, code=$code" }
        val userBackpack = UserBackpackDto(
            userId = userInfo.id,
            propCode = code,
            propKind = kind,
            propId = id
        )
        val saved = backpackProxy.save(userBackpack)
        if (saved.id == 0L || backpackProxy.findById(saved.id) == null) {
            error("淇濆瓨鑳屽寘璁板綍澶辫触: userId=${userInfo.id}, code=$code, propId=$id")
        }
        userInfo.backpacks = userInfo.backpacks + saved
        userInfo.backpackCount = userInfo.backpacks.size
        return saved
    }

    /**
     * 鍙戞斁鍙爢鍙犻亾鍏枫€傚凡鏈夊悓 code 閬撳叿鏃跺悎骞舵暟閲忥紝鍚﹀垯鍒涘缓鏂板疄渚嬪苟鍔犲叆鑳屽寘銆?     */
    @JvmStatic
    fun addStackablePropToBackpack(userInfo: UserInfoDto, code: String, kind: String, amount: Int): UserBackpackDto {
        require(amount > 0) { "鍙戞斁鏁伴噺蹇呴』澶т簬0: code=$code, amount=$amount" }

        userInfo.backpacks.find { it.propCode == code }?.let { backpack ->
            val prop = PropsManager.getProp(backpack)
                ?: error("鑳屽寘閬撳叿鏁版嵁涓嶅瓨鍦? code=$code, propId=${backpack.propId}")
            require(prop is Stackable && prop.isStack) { "鑳屽寘閬撳叿涓嶆槸鍙爢鍙犻亾鍏? code=$code" }
            prop.num += amount
            PropsManager.updateProp(backpack.propId, prop)
            return backpack
        }

        val prop = PropsManager.getTemplate(code, BaseProp::class.java)
        require(prop is Stackable && prop.isStack) { "閬撳叿妯℃澘涓嶆槸鍙爢鍙犻亾鍏? code=$code" }
        prop.num = amount
        val propId = PropsManager.addProp(prop)
        return addPropToBackpack(userInfo, code, kind, propId)
    }

    /**
     * 鏍规嵁閬撳叿ID鍒犻櫎鐢ㄦ埛鑳屽寘涓殑閬撳叿
     *
     * @param userInfo 鐢ㄦ埛淇℃伅
     * @param id 閬撳叿ID
     */
    @JvmStatic
    fun delPropToBackpack(userInfo: UserInfoDto, id: Long) {
        val backpacks = userInfo.backpacks
        val find = backpacks.find { it.propId == id }
        if (find != null) {
            backpackProxy.delete(find.id)
            userInfo.backpacks = userInfo.backpacks.filterNot { it.id == find.id }
            userInfo.backpackCount = userInfo.backpacks.size
            PropsManager.destroyPros(id)
        }
    }

    /**
     * 鏍规嵁UserBackpack瀵硅薄鍒犻櫎鐢ㄦ埛鑳屽寘涓殑閬撳叿
     *
     * @param userInfo 鐢ㄦ埛淇℃伅
     * @param userBackpack 鐢ㄦ埛鑳屽寘瀵硅薄
     */
    @JvmStatic
    fun delPropToBackpack(userInfo: UserInfoDto, userBackpack: UserBackpackDto) {
        backpackProxy.delete(userBackpack.id)
        userInfo.backpacks = userInfo.backpacks.filterNot { it.id == userBackpack.id }
        userInfo.backpackCount = userInfo.backpacks.size
        userBackpack.propId.takeIf { it != 0L }?.let { PropsManager.destroyPros(it) }
    }

    /**
     * 妫€鏌ョ敤鎴疯儗鍖呬腑鏄惁鍖呭惈鎸囧畾ID鐨勯亾鍏?
     *
     * @param userInfo 鐢ㄦ埛淇℃伅
     * @param id 閬撳叿ID
     * @return 濡傛灉鍖呭惈杩斿洖true锛屽惁鍒欒繑鍥瀎alse
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfoDto, id: Long): Boolean {
        return userInfo.backpacks.any { it.propId == id }
    }

    /**
     * 妫€鏌ョ敤鎴疯儗鍖呬腑鏄惁鍖呭惈鎸囧畾缂栫爜鐨勯亾鍏?
     *
     * @param userInfo 鐢ㄦ埛淇℃伅
     * @param code 閬撳叿缂栫爜
     * @return 濡傛灉鍖呭惈杩斿洖true锛屽惁鍒欒繑鍥瀎alse
     */
    @JvmStatic
    fun checkPropInUser(userInfo: UserInfoDto, code: String): Boolean {
        return userInfo.backpacks.any { it.propCode == code }
    }

    private val backpackProxy
        get() = EntityProxyRegistry.get<UserBackpackDto>("user_backpack") ?: error("背包代理器未初始化")
}


