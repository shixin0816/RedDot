package com.reddot.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodoStore {

    public static final String PREFS = "reddot";
    private static final String K_TODOS = "todos";
    private static final String K_PENDING = "pending_todo";
    private static final String K_INIT = "init_done";

    public static final String K_BUBBLE = "bubble_visible";
    public static final String K_ENABLED = "overlay_enabled";

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 首次启动放一条示例，方便一眼看懂怎么用 */
    public static synchronized void initSample(Context c) {
        SharedPreferences p = sp(c);
        if (p.getBoolean(K_INIT, false)) return;
        if (getTodos(c).isEmpty()) {
            Todo t = new Todo();
            t.id = System.currentTimeMillis();
            t.date = todayKey();
            t.l1 = "英语学习";
            t.l2 = "背 20 个单词";
            t.targetName = "B站 · 英语单词";
            t.targetUrl = "https://search.bilibili.com/all?keyword=%E8%8B%B1%E8%AF%AD%E5%8D%95%E8%AF%8D";
            save(c, t);
        }
        p.edit().putBoolean(K_INIT, true).apply();
    }

    public static synchronized List<Todo> getTodos(Context c) {
        List<Todo> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(K_TODOS, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Todo.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    public static Todo get(Context c, long id) {
        for (Todo t : getTodos(c)) if (t.id == id) return t;
        return null;
    }

    // ---------------- 日期 ----------------

    private static SimpleDateFormat keyFormat() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    public static String keyOf(Calendar cal) {
        return keyFormat().format(cal.getTime());
    }

    public static String todayKey() {
        return keyFormat().format(new Date());
    }

    public static String shiftKey(String baseKey, int days) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(keyFormat().parse(baseKey));
            cal.add(Calendar.DAY_OF_MONTH, days);
            return keyOf(cal);
        } catch (Exception e) {
            return todayKey();
        }
    }

    /** 11月19日 周四 */
    public static String prettyDate(String key) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(keyFormat().parse(key));
            String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            return (cal.get(Calendar.MONTH) + 1) + "月" + cal.get(Calendar.DAY_OF_MONTH)
                    + "日 " + weeks[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) {
            return key == null ? "" : key;
        }
    }

    public static String dayTag(String key) {
        if (todayKey().equals(key)) return "今天";
        if (shiftKey(todayKey(), 1).equals(key)) return "明天";
        if (shiftKey(todayKey(), 2).equals(key)) return "后天";
        return "";
    }

    public static String findTodoIdDate(Context c, long id) {
        Todo t = get(c, id);
        return t == null ? todayKey() : t.date;
    }

    // ---------------- 按日期取 ----------------

    public static synchronized List<Todo> getTodos(Context c, String dateKey) {
        List<Todo> out = new ArrayList<>();
        String key = (dateKey == null || dateKey.length() == 0) ? todayKey() : dateKey;
        for (Todo t : getTodos(c)) {
            if (key.equals(t.date)) out.add(t);
        }
        return out;
    }

    public static synchronized void save(Context c, Todo todo) {
        List<Todo> all = getTodos(c);
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id == todo.id) {
                all.set(i, todo);
                found = true;
                break;
            }
        }
        if (!found) all.add(todo);
        write(c, all);
    }

    public static synchronized void delete(Context c, long id) {
        List<Todo> all = getTodos(c);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id == id) {
                all.remove(i);
                break;
            }
        }
        write(c, all);
    }

    public static synchronized void toggleDone(Context c, long id) {
        List<Todo> all = getTodos(c);
        for (Todo t : all) {
            if (t.id == id) {
                t.done = !t.done;
                write(c, all);
                return;
            }
        }
    }

    public static synchronized void setTargetUrl(Context c, long id, String url, String name) {
        Todo t = get(c, id);
        if (t == null) return;
        t.targetUrl = url;
        t.targetPackage = "";
        t.targetName = name;
        save(c, t);
    }

    private static void write(Context c, List<Todo> all) {
        JSONArray arr = new JSONArray();
        for (Todo t : all) {
            try {
                arr.put(t.toJson());
            } catch (Exception ignored) {
            }
        }
        sp(c).edit().putString(K_TODOS, arr.toString()).apply();
    }

    public static void setPending(Context c, long id) {
        sp(c).edit().putLong(K_PENDING, id).apply();
    }

    public static long getPending(Context c) {
        return sp(c).getLong(K_PENDING, -1);
    }

    public static void clearPending(Context c) {
        sp(c).edit().remove(K_PENDING).apply();
    }
}
