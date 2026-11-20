package com.reddot.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

/**
 * 读剪贴板。
 *
 * Android 10（API 29）起，只有「当前获得焦点的应用」才能读剪贴板，
 * 否则 getPrimaryClip() 直接返回 null。所以千万不要在 onCreate 里读，
 * 要等窗口拿到焦点之后再读，读不到就重试几次。
 */
public class Clip {

    /** 原始文本，读不到返回 null */
    public static String raw(Context c) {
        if (c == null) return null;
        try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return null;

            ClipData cd = null;
            try {
                cd = cm.getPrimaryClip();
            } catch (Throwable ignored) {
                // 部分 ROM 在无焦点时会抛 SecurityException，退回 hasPrimaryClip 探测
            }
            if (cd == null) {
                try {
                    if (!cm.hasPrimaryClip()) return null;
                    cd = cm.getPrimaryClip();
                } catch (Throwable ignored) {
                    return null;
                }
            }
            if (cd == null || cd.getItemCount() == 0) return null;

            ClipData.Item item = cd.getItemAt(0);
            CharSequence cs = item.coerceToText(c);
            if (cs == null) {
                cs = item.getText();
            }
            if (cs == null) return null;

            String s = clean(cs.toString());
            return s.length() == 0 ? null : s;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 剪贴板里的第一条链接，没有返回 null */
    public static String url(Context c) {
        return TargetUtil.extractUrl(raw(c));
    }

    /** 剪贴板里有没有内容（用来分辨「没权限」和「没复制」） */
    public static boolean hasSomething(Context c) {
        return raw(c) != null;
    }

    /** 去掉零宽字符和首尾空白，复制来的文本常带这些 */
    private static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\u200B' || ch == '\u200C' || ch == '\u200D' || ch == '\uFEFF') continue;
            sb.append(ch);
        }
        return sb.toString().trim();
    }

    /** Android 12+ 读剪贴板时系统会弹「已读取剪贴板」提示，这里只是给界面用的一句说明 */
    public static boolean showsSystemToast() {
        return Build.VERSION.SDK_INT >= 31;
    }
}
