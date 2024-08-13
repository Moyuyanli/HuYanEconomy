package cn.chahuyun.economy.version;


import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.utils.Log;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;


public class CheckLatestVersion {

    /**
     * 检查版本
     *
     * @author Travellerr
     */
    public static void init() {
        String response = HttpUtil.get("https://api.github.com/repos/Moyuyanli/HuYanEconomy/releases/latest");
        JSONObject json = JSONUtil.parseObj(response);
        String newVersion = json.getStr("tag_name");
        String updateMsg = json.getStr("body");
        if (newVersion == null) {
            Log.error(" 无法获取最新版本号！");
            return;
        }
        if (!newVersion.substring(1).equals(HuYanEconomy.VERSION)) {
            Log.warning(" 发现最新版本！版本：v" + newVersion);
            Log.warning(" 发现最新版本！版本：v" + newVersion);
            Log.warning(" 发现最新版本！版本：v" + newVersion);
            if (updateMsg != null) {
                updateMsg = updateMsg.replace("#", "");
                updateMsg = updateMsg.replace("\r\n", " ");
                Log.warning(updateMsg);
            } else {
                Log.error(" 无法获取更新日志！");
            }
            return;
        }
        Log.info(" 已是最新版本！版本: " + HuYanEconomy.VERSION);

    }
}
