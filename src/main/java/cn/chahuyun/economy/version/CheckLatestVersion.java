package cn.chahuyun.economy.version;


import cn.chahuyun.economy.EconomyBuildConstants;
import cn.chahuyun.economy.utils.Log;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;


public class CheckLatestVersion {

    /**
     * 检查版本
     */
    public static void init() {
        String response;
        try {
            response = HttpUtil.get("https://api.github.com/repos/Moyuyanli/HuYanEconomy/releases/latest");
        } catch (Exception e) {
            Log.warning("检查最新版本失败!");
            return;
        }
        JSONObject json = JSONUtil.parseObj(response);
        String newVersion = json.getStr("tag_name");
        String updateMsg = json.getStr("body");
        if (newVersion == null) {
            Log.error("无法获取最新版本号！");
            return;
        }
        if (!newVersion.substring(1).equals(EconomyBuildConstants.VERSION)) {
            Log.warning("发现最新版本！版本：" + newVersion);
            Log.warning("发现最新版本！版本：" + newVersion);
            Log.warning("发现最新版本！版本：" + newVersion);
            if (updateMsg != null) {
                updateMsg = updateMsg.replace("#", "")
                        .replace("`", "")
                        .replace("*", "")
                        .replace("_", "")
                        .replace("\r\n", " ")
                        .replaceAll("/(https?://)?(([0-9a-z.]+\\.[a-z]+)|(([0-9]{1,3}\\.){3}[0-9]{1,3}))(:[0-9]+)?(/[0-9a-z%/.\\-_]*)?(\\?[0-9a-z=&%_\\-]*)?(#[0-9a-z=&%_\\-]*)?/gi", "");

                Log.warning(updateMsg);
            } else {
                Log.error("无法获取更新日志！");
            }
            return;
        }
        Log.info("已是最新版本！版本: " + EconomyBuildConstants.VERSION);

    }
}
