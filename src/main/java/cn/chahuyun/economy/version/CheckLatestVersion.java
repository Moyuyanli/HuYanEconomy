package cn.chahuyun.economy.version;


import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.utils.Log;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;


public class CheckLatestVersion {

    /**
     * 检查版本
     *
     * @author Travellerr
     */
    public static void init() {
            try {
                URL url = new URL("https://api.github.com/repos/Moyuyanli/HuYanEconomy/releases/latest");
                InputStream stream = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String msg;
                StringBuilder response = new StringBuilder();
                while ((msg = reader.readLine()) != null) {
                    response.append(msg);
                }
                reader.close();
                JSONObject json = JSONUtil.parseObj(response.toString());
                String newVersion = json.getStr("tag_name");
                String updateMsg = json.getStr("body");
                if (newVersion != null) {
                    if (!newVersion.contains(HuYanEconomy.VERSION)) {
                        Log.warning(" 发现最新版本！版本：" + newVersion);
                        Log.warning(" 发现最新版本！版本：" + newVersion);
                        Log.warning(" 发现最新版本！版本：" + newVersion);
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
                } else {
                    Log.error(" 无法获取最新版本号！");
                    return;
                }
                stream.close();
            } catch (Exception e) {
                Log.error(e.fillInStackTrace().getMessage());
            }
    }
}
