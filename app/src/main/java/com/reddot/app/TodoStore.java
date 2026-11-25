package com.reddot.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodoStore {

    public static final String PREFS = "reddot";
    private static final String K_TODOS = "todos";
    private static final String K_CHECKS = "checks";
    private static final String K_PENDING = "pending_todo";
    private static final String K_INIT = "init_done";

    public static final String K_BUBBLE = "bubble_visible";
    public static final String K_ENABLED = "overlay_enabled";

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 首次启动放一条示例，方便一眼看懂怎么用（设成每天，顺便演示重复任务） */
    public static synchronized void initSample(Context c) {
        SharedPreferences p = sp(c);
        if (p.getBoolean(K_INIT, false)) return;
        if (getTodos(c).isEmpty()) {
            Todo t = new Todo();
            t.id = System.currentTimeMillis();
            t.date = todayKey();
            t.l1 = "英语学习";
            t.l2 = "背 20 个单词";
            t.repeat = Todo.REPEAT_DAILY;
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

    /** 周一~周日 的短标签 */
    public static String weekLabel(String key) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(keyFormat().parse(key));
            String[] weeks = {"日", "一", "二", "三", "四", "五", "六"};
            return weeks[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) {
            return "";
        }
    }

    /** 某天是几号 */
    public static String dayOfMonth(String key) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(keyFormat().parse(key));
            return String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        } catch (Exception e) {
            return "";
        }
    }

    public static String findTodoIdDate(Context c, long id) {
        Todo t = get(c, id);
        return t == null ? todayKey() : t.date;
    }

    // ---------------- 按天取（含重复任务） ----------------

    public static synchronized List<Todo> getTodos(Context c, String dateKey) {
        List<Todo> out = new ArrayList<>();
        String key = (dateKey == null || dateKey.length() == 0) ? todayKey() : dateKey;
        for (Todo t : getTodos(c)) {
            if (t.repeat == Todo.REPEAT_LONG) {
                out.add(t);
            } else if (t.repeat == Todo.REPEAT_DAILY) {
                // 每天任务：完成状态按天取，不污染任务本身的 done
                t.done = isChecked(c, t.id, key);
                out.add(t);
            } else if (key.equals(t.date)) {
                out.add(t);
            }
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
        // 顺手清掉完成记录，否则统计里会留下已删除任务的痕迹
        try {
            JSONObject o = checks(c);
            o.remove(String.valueOf(id));
            sp(c).edit().putString(K_CHECKS, o.toString()).apply();
        } catch (Exception ignored) {
        }
        write(c, all);
    }

    // ---------------- 完成记录（按天） ----------------

    private static JSONObject checks(Context c) {
        try {
            return new JSONObject(sp(c).getString(K_CHECKS, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static synchronized boolean isChecked(Context c, long id, String date) {
        JSONArray a = checks(c).optJSONArray(String.valueOf(id));
        if (a == null) return false;
        for (int i = 0; i < a.length(); i++) {
            if (date.equals(a.optString(i))) return true;
        }
        return false;
    }

    public static synchronized void setChecked(Context c, long id, String date, boolean on) {
        try {
            JSONObject o = checks(c);
            String k = String.valueOf(id);
            JSONArray old = o.optJSONArray(k);
            List<String> list = new ArrayList<>();
            if (old != null) {
                for (int i = 0; i < old.length(); i++) {
                    String s = old.optString(i);
                    if (!list.contains(s)) list.add(s);
                }
            }
            if (on) {
                if (!list.contains(date)) list.add(date);
            } else {
                list.remove(date);
            }
            JSONArray n = new JSONArray();
            for (String s : list) n.put(s);
            if (n.length() == 0) o.remove(k);
            else o.put(k, n);
            sp(c).edit().putString(K_CHECKS, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** 勾选 / 取消。每天任务写到当天的记录里，单次和长期任务写任务本身 */
    public static synchronized void toggleDone(Context c, long id, String dateKey) {
        String key = (dateKey == null || dateKey.length() == 0) ? todayKey() : dateKey;
        Todo t = get(c, id);
        if (t == null) return;
        if (t.repeat == Todo.REPEAT_DAILY) {
            boolean now = !isChecked(c, id, key);
            setChecked(c, id, key, now);
            t.done = now;
        } else {
            t.done = !t.done;
            // 单次 / 长期也留一条完成记录，统计视图才有东西可画
            setChecked(c, id, todayKey(), t.done);
            save(c, t);
        }
    }

    public static synchronized void toggleDone(Context c, long id) {
        toggleDone(c, id, todayKey());
    }

    // ---------------- 计时 ----------------

    public static synchronized void startTimer(Context c, long id) {
        Todo t = get(c, id);
        if (t == null || t.runningSince > 0) return;
        t.runningSince = System.currentTimeMillis();
        save(c, t);
    }

    public static synchronized void stopTimer(Context c, long id) {
        Todo t = get(c, id);
        if (t == null || t.runningSince <= 0) return;
        long add = (System.currentTimeMillis() - t.runningSince) / 1000;
        t.spentSec += (int) Math.max(0, add);
        t.runningSince = 0;
        save(c, t);
    }

    public static synchronized void toggleTimer(Context c, long id) {
        Todo t = get(c, id);
        if (t == null) return;
        if (t.runningSince > 0) stopTimer(c, id);
        else startTimer(c, id);
    }

    public static synchronized boolean isRunning(Context c, long id) {
        Todo t = get(c, id);
        return t != null && t.runningSince > 0;
    }

    public static synchronized int totalSpent(Context c) {
        int sum = 0;
        for (Todo t : getTodos(c)) sum += t.liveSec();
        return sum;
    }

    // ---------------- 统计 ----------------

    /** 某一天完成了几条（按任务去重） */
    public static synchronized int checkedCountOn(Context c, String date) {
        int n = 0;
        JSONObject o = checks(c);
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            JSONArray a = o.optJSONArray(k);
            if (a == null) continue;
            for (int i = 0; i < a.length(); i++) {
                if (date.equals(a.optString(i))) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    /** 某条任务累计完成了几次 */
    public static synchronized int checkCount(Context c, long id) {
        JSONArray a = checks(c).optJSONArray(String.valueOf(id));
        return a == null ? 0 : a.length();
    }

    /** 连续完成天数；今天还没开始时，从昨天往前算 */
    public static synchronized int streakDays(Context c) {
        String key = todayKey();
        if (checkedCountOn(c, key) == 0) key = shiftKey(key, -1);
        int n = 0;
        while (n < 3650 && checkedCountOn(c, key) > 0) {
            n++;
            key = shiftKey(key, -1);
        }
        return n;
    }

    /** 今天全部任务（含重复）的完成进度 */
    public static int[] todayProgress(Context c) {
        List<Todo> list = getTodos(c, todayKey());
        int done = 0;
        for (Todo t : list) if (t.done) done++;
        return new int[]{done, list.size()};
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
