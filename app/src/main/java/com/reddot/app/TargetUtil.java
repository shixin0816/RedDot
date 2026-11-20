package com.reddot.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetUtil {

    private static final Pattern URL_START =
            Pattern.compile("https?://", Pattern.CASE_INSENSITIVE);

    /** URL 里允许出现的字符（ASCII），遇到别的就截断，避免把后面的中文一起吞进来 */
    private static final String URL_CHARS =
            "-._~:/?#[]@!$&'()*+,;=%";

    /**
     * 从分享文本 / 剪贴板里抠出第一条链接。
     * 小红书的分享文本长这样：
     * 「34 发布了一篇小红书笔记，快来看吧！ 😆 xxx 😆 http://xhslink.com/a/bCdEf，复制本条信息…」
     * 所以不能简单用 \S+，要按字符白名单扫。
     */
    public static String extractUrl(String text) {
        if (text == null) return null;
        Matcher m = URL_START.matcher(text);
        while (m.find()) {
            StringBuilder sb = new StringBuilder();
            for (int i = m.start(); i < text.length(); i++) {
                char ch = text.charAt(i);
                if ((ch >= 'a' && ch <= 'z')
                        || (ch >= 'A' && ch <= 'Z')
                        || (ch >= '0' && ch <= '9')
                        || URL_CHARS.indexOf(ch) >= 0) {
                    sb.append(ch);
                } else {
                    break;
                }
            }
            String u = trimTail(sb.toString());
            // 至少要有域名，避免把 "https://" 这种半截当链接
            if (u.length() > 12 && u.indexOf('.', 8) > 8) return u;
        }
        return null;
    }

    /** 去掉链接结尾可能被带上的标点 */
    private static String trimTail(String u) {
        while (u.length() > 0) {
            char last = u.charAt(u.length() - 1);
            if (".,;:!?)]}>'\"".indexOf(last) >= 0) {
                u = u.substring(0, u.length() - 1);
            } else {
                break;
            }
        }
        return u;
    }

    public static String appNameFromUrl(String url) {
        if (url == null) return "链接";
        String u = url.toLowerCase(Locale.US);
        if (u.contains("xiaohongshu.com") || u.contains("xhslink.com")) return "小红书";
        if (u.contains("bilibili.com") || u.contains("b23.tv")) return "哔哩哔哩";
        if (u.contains("weread.qq.com")) return "微信读书";
        if (u.contains("zhihu.com")) return "知乎";
        if (u.contains("douyin.com")) return "抖音";
        if (u.contains("weibo.com")) return "微博";
        if (u.contains("youtube.com") || u.contains("youtu.be")) return "YouTube";
        try {
            String host = new java.net.URI(url).getHost();
            return host == null ? "链接" : host;
        } catch (Exception e) {
            return "链接";
        }
    }

    /**
     * 尽量把 https 链接翻译成 App 专属 scheme。
     * B站规则稳定：bilibili://video/BVxxxx
     * 小红书 xhsdiscover:// 需要真机校准，暂时返回 null，走系统链接分发。
     */
    public static String toAppScheme(String url) {
        if (url == null) return null;
        try {
            String u = url.toLowerCase(Locale.US);
            if (u.contains("bilibili.com/video/")) {
                String rest = url.substring(url.indexOf("/video/") + 7);
                int cut = rest.length();
                for (int i = 0; i < rest.length(); i++) {
                    char ch = rest.charAt(i);
                    if (ch == '?' || ch == '&' || ch == '/' || ch == '#') {
                        cut = i;
                        break;
                    }
                }
                String bv = rest.substring(0, cut);
                if (bv.startsWith("BV")) return "bilibili://video/" + bv;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean tryOpen(Context c, Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 这个链接默认会被哪个应用打开。用来把「最近跳转过的应用」排到前面。
     * 系统没有默认处理者时返回 null。
     */
    public static String resolvePackage(Context c, String url) {
        if (c == null || url == null || url.length() == 0) return null;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            android.content.pm.ResolveInfo ri = c.getPackageManager().resolveActivity(i, 0);
            if (ri != null && ri.activityInfo != null) return ri.activityInfo.packageName;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 三级回退：App scheme -> https（让系统交给 App 处理）-> 浏览器
     */
    public static void openTarget(Context c, Todo t) {
        String url = t.targetUrl;
        if (url != null && url.length() > 0) {
            String scheme = toAppScheme(url);
            if (scheme != null) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    if (c.getPackageManager().resolveActivity(i, 0) != null && tryOpen(c, i)) {
                        Recents.touch(c, resolvePackage(c, scheme));
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (tryOpen(c, web)) {
                Recents.touch(c, resolvePackage(c, url));
                return;
            }
            Toast.makeText(c, "打不开这个链接", Toast.LENGTH_SHORT).show();
            return;
        }

        String pkg = t.targetPackage;
        if (pkg != null && pkg.length() > 0) {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null && tryOpen(c, i)) {
                Recents.touch(c, pkg);
                return;
            }
            Toast.makeText(c, "没找到这个应用", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(c, "这条待办还没设置跳转目标", Toast.LENGTH_SHORT).show();
    }
}
