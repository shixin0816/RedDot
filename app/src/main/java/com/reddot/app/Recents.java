package com.reddot.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 「最近跳转过的应用」记录，用于把常用的排到选择列表最前面。
 * 只存包名，按最近使用排序，最多留 10 个。
 */
public class Recents {

    private static final String KEY = "recent_pkgs";
    private static final int MAX = 10;

    public static List<String> list(Context c) {
        List<String> out = new ArrayList<>();
        try {
            String raw = TodoStore.sp(c).getString(KEY, "");
            if (raw == null || raw.length() == 0) return out;
            for (String s : raw.split("\\|")) {
                if (s != null && s.trim().length() > 0 && !out.contains(s.trim())) {
                    out.add(s.trim());
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** 记一次跳转：挪到最前面 */
    public static void touch(Context c, String pkg) {
        if (c == null || pkg == null) return;
        pkg = pkg.trim();
        if (pkg.length() == 0 || pkg.equals(c.getPackageName())) return;
        try {
            List<String> now = list(c);
            now.remove(pkg);
            now.add(0, pkg);
            while (now.size() > MAX) now.remove(now.size() - 1);
            StringBuilder sb = new StringBuilder();
            for (String s : now) {
                if (sb.length() > 0) sb.append('|');
                sb.append(s);
            }
            TodoStore.sp(c).edit().putString(KEY, sb.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static int indexOf(Context c, String pkg) {
        if (pkg == null) return -1;
        return list(c).indexOf(pkg);
    }
}
