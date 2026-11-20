package com.reddot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/** 国产 ROM 的悬浮窗 / 后台保活引导，荣耀 90（MagicOS）也在内 */
public class TuningTips {

    public static boolean isStrict() {
        String m = String.valueOf(Build.MANUFACTURER).toUpperCase();
        return m.contains("HONOR") || m.contains("HUAWEI") || m.contains("XIAOMI")
                || m.contains("REDMI") || m.contains("OPPO") || m.contains("VIVO")
                || m.contains("ONEPLUS") || m.contains("REALME") || m.contains("MEIZU");
    }

    public static boolean isHonor() {
        String m = String.valueOf(Build.MANUFACTURER).toUpperCase();
        return m.contains("HONOR") || m.contains("HUAWEI");
    }

    public static void show(final Activity a) {
        StringBuilder sb = new StringBuilder();
        if (isHonor()) {
            sb.append("你的机型是荣耀 / 华为（MagicOS），系统对悬浮窗和后台管理比较严，按下面四步设置后圆点才不会被清掉：\n\n")
              .append("1. 悬浮窗权限\n")
              .append("设置 → 应用和服务 → 应用管理 → RedDot → 权限 → 悬浮窗 → 允许\n\n")
              .append("2. 应用启动管理\n")
              .append("设置 → 应用和服务 → 应用启动管理 → RedDot → 关掉「自动管理」，把「自启动」「关联启动」「后台活动」都打开\n\n")
              .append("3. 忽略电池优化\n")
              .append("设置 → 应用和服务 → 特殊访问权限 → 电池优化 → RedDot → 不允许\n\n")
              .append("4. 最近任务加锁\n")
              .append("多任务界面里把 RedDot 卡片下拉加锁（出现小锁图标），清理后台时就不会被一起清掉");
        } else {
            sb.append("国产系统对悬浮窗和后台限制较严，建议做三步：\n\n")
              .append("1. 允许「显示在其他应用上层」\n\n")
              .append("2. 在应用管理里允许自启动 / 后台运行\n\n")
              .append("3. 多任务界面给 RedDot 加锁，避免被一键清理");
        }

        new AlertDialog.Builder(a)
                .setTitle(isHonor() ? "荣耀 90 适配设置" : "机型适配设置")
                .setMessage(sb.toString())
                .setPositiveButton("去应用详情", (d, w) -> {
                    try {
                        a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + a.getPackageName())));
                    } catch (Exception ignored) {
                    }
                })
                .setNeutralButton("去悬浮窗权限", (d, w) -> {
                    try {
                        a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + a.getPackageName())));
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton("知道了", null)
                .show();
    }
}
