# Manager API 列表（v2.0.0）

更新时间：2026-01-17

本文用于汇总 `src/main/kotlin/cn/chahuyun/economy/manager` 下各 Manager 对外提供的主要 API。

约定：
- Action 层只做“指令入口/参数解析/权限校验/分发”。
- Usecase 层承载业务流程编排。
- Manager 层提供可复用的模块能力（初始化/定时任务/缓存/通用能力）。

---

## BackpackManager

- `suspend fun showBackpack(bot, backpacks, group, currentPage, maxPage)`：渲染并发送背包转发消息。
- `fun addPropToBackpack(userInfo, code, kind, id)`：向用户背包添加道具记录。
- `fun delPropToBackpack(userInfo, id)`：按道具 ID 删除背包道具，并销毁道具实例。
- `fun delPropToBackpack(userInfo, userBackpack)`：按背包对象删除。
- `fun checkPropInUser(userInfo, id)`：检查是否拥有某道具 ID。
- `fun checkPropInUser(userInfo, code)`：检查是否拥有某道具 code。

## BankManager

- `fun init()`：初始化主银行（确保全局主银行存在），并启动利息定时任务。

## BankInterestTask（内部定时任务）

- `override fun execute()`：按利率给主银行储户结息（由 `BankManager.init()` 调度）。

## EventPropsManager

- `suspend fun viewShop(event, page)`：展示道具商店分页，并处理“下一页/上一页”交互。

## GamesManager

- `fun init()`：注册游戏模块定时任务（如鱼塘自动升级）。
- `fun shutdown()`：停止协程域。
- `fun clearCooling()`：清空钓鱼冷却缓存。
- `fun removeCooling(qq)`：移除某用户冷却。
- `suspend fun checkAndProcessFishing(userInfo, isFishingTitle, fishInfo, subject, chain): Boolean`：钓鱼并发/冷却检查；返回 `true` 表示已拦截并提示。
- `suspend fun failedFishing(userInfo, user, subject, fishInfo): Boolean`：钓鱼失败/惩罚判定；返回 `true` 表示本次已失败并处理。
- `suspend fun buyFishRod(event)`：购买鱼竿（目前未直接暴露指令，供 action/usecase 复用）。
- `suspend fun upFishRod(event)`：升级鱼竿。
- `suspend fun fishTop(event)`：钓鱼榜。
- `suspend fun viewFishLevel(event)`：查看鱼竿等级。

## LotteryManager

- `fun init()`：初始化彩票模块，按存量记录启动分钟/小时/每日定时。
- `fun ensureHoursSchedule()`：确保小时定时存在。
- `fun ensureMinutesSchedule()`：确保分钟定时存在。
- `fun result(type, location, lotteryInfo)`：发放奖金并通知开奖结果。
- `fun close()`：停止 Cron（沿用旧行为）。

## LotteryMinutesTask / LotteryHoursTask / LotteryDayTask（内部定时任务）

- `override fun execute()`：各粒度的开奖逻辑（由 `LotteryManager` 调度）。

## LuckyDrawManager

- `fun take(userId): UserRaffle`：获取/创建抽奖统计实体。
- `fun checkSingleCooldown(userId): Boolean`：检查单抽冷却。
- `fun checkTenCooldown(userId): Boolean`：检查十连冷却。
- `fun singleRemainingSeconds(userId): Int`：单抽剩余冷却（秒）。
- `fun tenRemainingSeconds(userId): Int`：十连剩余冷却（秒）。
- `fun updateCooldown(userId, isTen=false)`：更新冷却时间戳。

## MissionManager

- 当前为空（占位）。

## PrivateBankManager

- `fun init()`：注册银行（PrivateBank 模块）相关定时任务（利息结算/到期贷款追缴/国卷发行/狐卷发行与结算/到期回流）。

> 具体业务能力位于 `privatebank.*Service` 与 `privatebank.*Repository`。

## RedPackManager

- `fun generateRandomPack(totalAmount, count): List<Double>`：二倍均值法生成随机红包金额列表。
- `suspend fun viewRedPack(subject, bot, redPacks, forwardMessage)`：渲染红包列表转发消息。
- `suspend fun getRedPack(sender, subject, redPack, message, skipMessage=false, passwordOverride=null): GrabResult`：领取红包（含口令校验/随机分配/完结清理）。
- `suspend fun expireRedPack(group, redPack)`：红包过期退还剩余金币。

## RobManager

- `fun getCoolingRemainingMinutes(qq, cooldownMinutes): Long`：获取抢劫冷却剩余分钟。
- `fun markCooling(qq)`：记录冷却时间。
- `fun getRobInfo(userInfo): RobInfo`：获取/创建抢劫统计信息。

## SignManager

- `fun randomSignGold(event)`：签到基础金币随机（供 `SignEvent` 监听使用）。
- `fun signProp(event)`：签到道具/称号加成逻辑（供 `SignEvent` 监听使用）。

## TitleManager

- `fun init()`：注册称号模板并做历史数据修正。
- `fun getDefaultTitle(userInfo): TitleInfo`：获取当前启用称号（或默认称号）。
- `fun addTitleInfo(userInfo, titleTemplateCode): Boolean`：给用户添加称号。
- `fun checkTitleIsExist(userInfo, titleCode): Boolean`：是否拥有称号。
- `fun checkTitleIsOnEnable(userInfo, titleCode): Boolean`：称号是否处于启用状态。
- `fun checkTitleTime(titleInfo): Boolean`：检查过期并清理；返回 `true` 表示已过期并删除。
- `fun checkMonopolyJava(userInfo, subject)` / `suspend fun checkMonopoly(userInfo, subject)`：大富翁称号授予。
- `fun checkSignTitleJava(userInfo, subject)` / `suspend fun checkSignTitle(userInfo, subject)`：签到称号授予。
- `suspend fun checkFishTitle(userInfo, subject)`：钓鱼榜榜首称号授予。

## TransferManager

- `fun transfer(originUser, toUser, money): String`：转账对外 API（返回文案）。

## UserCoreManager

- `fun getUserInfo(user): UserInfo`：按 `User` 获取/创建用户信息（并回填运行时上下文）。
- `fun getUserInfo(account): UserInfo`：按经济账号获取用户信息。
- `fun getUserInfo(userId): UserInfo?`：按 QQ 获取用户信息（不创建）。
- `fun getUserInfo(uuid): UserInfo?`：按 funding uuid 获取用户信息。
- `fun getUserInfoImageBase(userInfo): BufferedImage?`：生成个人信息底图。

## UserStatusManager

- `fun checkUserInHome(userInfo): Boolean` / `fun checkUserNotInHome(userInfo): Boolean`：是否在家。
- `fun moveHome(userInfo)`：移动到家。
- `fun checkUserInHospital(userInfo): Boolean` / `fun moveHospital(userInfo, recovery)`：医院状态与迁移。
- `fun checkUserInPrison(userInfo): Boolean` / `fun movePrison(userInfo, recovery)`：监狱状态与迁移。
- `fun checkUserInFishpond(userInfo): Boolean` / `fun moveFishpond(userInfo, recovery)`：鱼塘状态与迁移。
- `fun checkUserInFactory(userInfo): Boolean` / `fun moveFactory(userInfo, recovery)`：工厂状态与迁移。
- `fun getUserStatus(userInfo): UserStatus` / `fun getUserStatus(qq): UserStatus`：获取/创建用户状态，并在需要时自动恢复到 HOME。
