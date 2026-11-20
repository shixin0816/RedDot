package com.reddot.app;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 一条待办：最多三级
 * 第一级 l1（英语学习） -> 第二级 l2（背单词） -> 第三级 跳转目标（某个博主主页 / 某个视频）
 */
public class Todo {

    public long id;
    public String date = "";
    public String l1 = "";
    public String l2 = "";
    public String targetName = "";
    public String targetUrl = "";
    public String targetPackage = "";
    public boolean done;

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
        return t;
    }
}
