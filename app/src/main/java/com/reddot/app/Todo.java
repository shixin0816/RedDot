package com.reddot.app;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 一条待办：最多三级
 * 第一级 l1（英语学习） -> 第二级 l2（背单词） -> 第三级 跳转目标（某个博主主页 / 某个视频）
 *
 * 重复方式：
 *  - 单次：只在 date 这一天出现
 *  - 每天：每天都出现，完成状态按天单独记录（今天勾了不影响明天）
 *  - 长期：一直出现，完成状态是全局的，适合「整理错题本」这类持续项目
 */
public class Todo {

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_LONG = 2;

    public long id;
    public String date = "";
    public String l1 = "";
    public String l2 = "";
    public String targetName = "";
    public String targetUrl = "";
    public String targetPackage = "";
    public boolean done;

    /** 0 单次 / 1 每天 / 2 长期 */
    public int repeat = REPEAT_NONE;
    /** 累计专注秒数（已完成的部分） */
    public int spentSec;
    /** 正在计时时的开始时间戳，0 表示没在计时 */
    public long runningSince;

    public boolean hasTarget() {
        return (targetUrl != null && targetUrl.length() > 0)
                || (targetPackage != null && targetPackage.length() > 0);
    }

    public String targetLabel() {
        if (targetUrl != null && targetUrl.length() > 0) {
            return (targetName != null && targetName.length() > 0) ? targetName : targetUrl;
        }
        if (targetPackage != null && targetPackage.length() > 0) {
            return (targetName != null && targetName.length() > 0) ? targetName : targetPackage;
        }
        return "未设置";
    }

    public String path() {
        if (l2 != null && l2.length() > 0) return l1 + " > " + l2;
        return l1;
    }

    /** 长按复制用的纯文本 */
    public String copyText() {
        StringBuilder sb = new StringBuilder();
        if (l1 != null && l1.length() > 0) sb.append(l1);
        if (l2 != null && l2.length() > 0) {
            if (sb.length() > 0) sb.append(" > ");
            sb.append(l2);
        }
        if (targetUrl != null && targetUrl.length() > 0) {
            sb.append("\n").append(targetUrl);
        } else if (targetPackage != null && targetPackage.length() > 0) {
            sb.append("\n打开应用：").append(targetLabel());
        }
        return sb.toString();
    }

    public boolean isRepeating() {
        return repeat == REPEAT_DAILY || repeat == REPEAT_LONG;
    }

    public String repeatLabel() {
        if (repeat == REPEAT_DAILY) return "每天";
        if (repeat == REPEAT_LONG) return "长期";
        return "";
    }

    /** 含正在进行的这一段的秒数 */
    public int liveSec() {
        if (runningSince > 0) {
            long add = (System.currentTimeMillis() - runningSince) / 1000;
            if (add < 0) add = 0;
            return spentSec + (int) add;
        }
        return spentSec;
    }

    public static String hms(int sec) {
        if (sec < 0) sec = 0;
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        if (h > 0) return h + ":" + two(m) + ":" + two(s);
        return m + ":" + two(s);
    }

    /** 统计里更短的写法：42m / 1.5h */
    public static String human(int sec) {
        if (sec < 60) return sec + "秒";
        int m = sec / 60;
        if (m < 60) return m + "分钟";
        int h = m / 60;
        int mm = m % 60;
        return mm == 0 ? h + "小时" : h + "小时" + mm + "分";
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("date", date);
        o.put("l1", l1);
        o.put("l2", l2);
        o.put("tn", targetName);
        o.put("tu", targetUrl);
        o.put("tp", targetPackage);
        o.put("done", done);
        o.put("rp", repeat);
        o.put("ss", spentSec);
        o.put("rs", runningSince);
        return o;
    }

    public static Todo fromJson(JSONObject o) {
        Todo t = new Todo();
        t.id = o.optLong("id");
        t.date = o.optString("date", "");
        t.l1 = o.optString("l1", "");
        t.l2 = o.optString("l2", "");
        t.targetName = o.optString("tn", "");
        t.targetUrl = o.optString("tu", "");
        t.targetPackage = o.optString("tp", "");
        t.done = o.optBoolean("done", false);
        t.repeat = o.optInt("rp", REPEAT_NONE);
        t.spentSec = o.optInt("ss", 0);
        t.runningSince = o.optLong("rs", 0);
        return t;
    }
}
