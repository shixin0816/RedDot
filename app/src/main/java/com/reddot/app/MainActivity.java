package com.reddot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    private static final Set<String> COLLAPSED = new HashSet<>();

    private Palette p;
    private String dayKey;
    private boolean overlayOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dayKey = TodoStore.todayKey();
        TodoStore.initSample(this);
        maybeRequestNotificationPermission();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();

        Intent it = getIntent();
        if (it != null && it.getBooleanExtra("show_bubble", false)) {
            it.removeExtra("show_bubble");
            if (Settings.canDrawOverlays(this)) {
                TodoStore.sp(this).edit()
                        .putBoolean(TodoStore.K_ENABLED, true)
                        .putBoolean(TodoStore.K_BUBBLE, true).apply();
                startOverlay(OverlayService.CMD_SHOW);
                refresh();
            }
        } else if (overlayOn) {
            startOverlay(OverlayService.CMD_NORMAL);
        }
    }

    // ---------------- 界面 ----------------

    private void refresh() {
        p = Palette.get(this);
        overlayOn = TodoStore.sp(this).getBoolean(TodoStore.K_ENABLED, true)
                && Settings.canDrawOverlays(this);
        applyStatusBar();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(p.bg);
        sv.setFillViewport(true);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(p.bg);

        root.addView(hero());
        root.addView(themeEntry());
        root.addView(dateBar());
        root.addView(addButton());
        root.addView(groupsSection());
        root.addView(tip());

        sv.addView(root, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
    }

    private void applyStatusBar() {
        getWindow().setStatusBarColor(p.bg);
        getWindow().setNavigationBarColor(p.bg);
        if (Build.VERSION.SDK_INT >= 23) {
            View dv = getWindow().getDecorView();
            int flags = dv.getSystemUiVisibility();
            if (p.light) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            dv.setSystemUiVisibility(flags);
        }
    }

    private View hero() {
        LinearLayout box = Ui.col(this);
        GradientDrawable bgd = Ui.gradCornerTop(this, p.heroTop, p.heroBottom, 28);
        box.setBackground(bgd);
        Ui.pad(box, this, 20, 54, 20, 18);

        LinearLayout brand = Ui.row(this);

        FrameLayout logo = new FrameLayout(this);
        logo.setBackground(Ui.grad(this, p.accent, p.accent2, 15));
        View ring = new View(this);
        ring.setBackground(Ui.oval(this, Color.TRANSPARENT, 0xFFFFFFFF, 3));
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                Ui.dp(this, 21), Ui.dp(this, 21));
        rlp.gravity = Gravity.CENTER;
        logo.addView(ring, rlp);
        brand.addView(logo, Ui.lp(Ui.dp(this, 46), Ui.dp(this, 46)));

        LinearLayout titles = Ui.col(this);
        brand.addView(titles, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Ui.margin(titles, this, 12, 0, 0, 0);
        titles.addView(Ui.tv(this, "RedDot", 25, p.ink, true));
        titles.addView(Ui.tv(this, TodoStore.prettyDate(TodoStore.todayKey()) + " · 今日待办",
                12, p.sub, false));

        LinearLayout pal = Ui.row(this);
        pal.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 14));
        pal.setGravity(Gravity.CENTER);
        int[] dots = {p.accent, p.accent2, p.hint};
        for (int c : dots) {
            View d = new View(this);
            d.setBackground(Ui.oval(this, c, 0, 0));
            LinearLayout.LayoutParams dlp = Ui.lp(Ui.dp(this, 9), Ui.dp(this, 9));
            dlp.rightMargin = Ui.dp(this, 3);
            pal.addView(d, dlp);
        }
        brand.addView(pal, Ui.lp(Ui.dp(this, 42), Ui.dp(this, 42)));
        pal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openThemes();
            }
        });

        box.addView(brand);

        // 桌面圆点开关卡片
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.stroke(this, p.card, p.cardStroke, 1, 18));
        Ui.pad(card, this, 16, 16, 16, 16);
        card.addView(Ui.tv(this, "桌面圆点", 16, p.ink, true));
        TextView desc = Ui.tv(this,
                "开启后桌面上浮着半透明圆点：单击看今日待办，双击隐藏，可随手拖动。",
                12, p.sub, false);
        desc.setLineSpacing(Ui.dp(this, 3), 1f);
        LinearLayout.LayoutParams dlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dp(this, 6);
        card.addView(desc, dlp);

        LinearLayout row = Ui.row(this);
        row.addView(Ui.tv(this, "在桌面显示圆点", 14, p.ink, false),
                Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(toggle());
        LinearLayout.LayoutParams rlp2 = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp2.topMargin = Ui.dp(this, 13);
        card.addView(row, rlp2);

        LinearLayout.LayoutParams clp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(this, 16);
        box.addView(card, clp);
        return box;
    }

    private View toggle() {
        final FrameLayout holder = new FrameLayout(this);
        holder.setBackground(overlayOn
                ? Ui.grad(this, p.accent, p.accent2, 14)
                : Ui.solid(this, p.softLine, 14));
        View knob = new View(this);
        knob.setBackground(Ui.oval(this, 0xFFFFFFFF, 0, 0));
        FrameLayout.LayoutParams klp = new FrameLayout.LayoutParams(
                Ui.dp(this, 22), Ui.dp(this, 22));
        klp.gravity = Gravity.CENTER_VERTICAL;
        klp.leftMargin = Ui.dp(this, overlayOn ? 23 : 3);
        holder.addView(knob, klp);
        holder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleOverlay(holder);
            }
        });
        return holder;
    }

    private View themeEntry() {
        LinearLayout row = Ui.row(this);
        row.setBackground(Ui.stroke(this, p.card, p.cardStroke, 1, 18));
        Ui.pad(row, this, 16, 14, 16, 14);

        View sw = new View(this);
        sw.setBackground(Ui.grad(this, p.accent, p.accent2, 12));
        row.addView(sw, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));

        LinearLayout col = Ui.col(this);
        row.addView(col, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Ui.margin(col, this, 12, 0, 0, 0);
        col.addView(Ui.tv(this, "主题外观", 14, p.ink, true));
        col.addView(Ui.tv(this, p.label(), 11.5f, p.sub, false));

        TextView chev = Ui.tv(this, "›", 18, p.hint, false);
        row.addView(chev);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openThemes();
            }
        });

        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 20);
        lp.rightMargin = Ui.dp(this, 20);
        lp.topMargin = Ui.dp(this, 14);
        row.setLayoutParams(lp);
        return row;
    }

    private View dateBar() {
        LinearLayout bar = Ui.row(this);
        bar.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 14));
        Ui.pad(bar, this, 10, 8, 10, 8);

        TextView prev = Ui.tv(this, "‹", 16, p.sub, false);
        prev.setGravity(Gravity.CENTER);
        prev.setBackground(Ui.solid(this, p.soft, 10));
        bar.addView(prev, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));

        LinearLayout center = Ui.col(this);
        bar.addView(center, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        center.setGravity(Gravity.CENTER);
        List<Todo> todos = TodoStore.getTodos(this, dayKey);
        String tag = TodoStore.dayTag(dayKey);
        String head = TodoStore.prettyDate(dayKey) + (tag.isEmpty() ? "" : "  " + tag);
        center.addView(Ui.tv(this, head, 13.5f, p.ink, false));
        String hint = (dayKey.equals(TodoStore.todayKey()) ? "点 ‹ › 换一天" : "点日期回到今天")
                + " · " + todos.size() + " 项";
        center.addView(Ui.tv(this, hint, 10.5f, p.hint, false));

        TextView next = Ui.tv(this, "›", 16, p.sub, false);
        next.setGravity(Gravity.CENTER);
        next.setBackground(Ui.solid(this, p.soft, 10));
        bar.addView(next, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dayKey = TodoStore.shiftKey(dayKey, -1);
                refresh();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dayKey = TodoStore.shiftKey(dayKey, 1);
                refresh();
            }
        });
        center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dayKey.equals(TodoStore.todayKey())) {
                    dayKey = TodoStore.todayKey();
                    refresh();
                    toast("已回到今天");
                }
            }
        });

        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 20);
        lp.rightMargin = Ui.dp(this, 20);
        lp.topMargin = Ui.dp(this, 14);
        bar.setLayoutParams(lp);
        return bar;
    }

    private View addButton() {
        TextView add = Ui.tv(this, "＋  新建待办", 15, p.btnInk, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.grad(this, p.accent, p.accent2, 15));
        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 50));
        lp.leftMargin = Ui.dp(this, 20);
        lp.rightMargin = Ui.dp(this, 20);
        lp.topMargin = Ui.dp(this, 16);
        add.setLayoutParams(lp);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EditActivity.class);
                i.putExtra("date", dayKey);
                startActivity(i);
            }
        });
        return add;
    }

    private View groupsSection() {
        LinearLayout box = Ui.col(this);
        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 20);
        lp.rightMargin = Ui.dp(this, 20);
        lp.topMargin = Ui.dp(this, 16);
        box.setLayoutParams(lp);

        List<Todo> todos = TodoStore.getTodos(this, dayKey);
        if (todos.isEmpty()) {
            LinearLayout empty = Ui.col(this);
            empty.setBackground(Ui.stroke(this, p.card, p.cardStroke, 1, 18));
            Ui.pad(empty, this, 16, 26, 16, 26);
            TextView t = Ui.tv(this, "这天还没有待办\n点「新建待办」加一条", 13, p.hint, false);
            t.setGravity(Gravity.CENTER);
            empty.addView(t);
            box.addView(empty);
            return box;
        }

        Map<String, List<Todo>> groups = new LinkedHashMap<>();
        for (Todo t : todos) {
            String g = TextUtils.isEmpty(t.l1) ? "未分类" : t.l1;
            List<Todo> arr = groups.get(g);
            if (arr == null) {
                arr = new ArrayList<>();
                groups.put(g, arr);
            }
            arr.add(t);
        }

        for (Map.Entry<String, List<Todo>> e : groups.entrySet()) {
            box.addView(groupCard(e.getKey(), e.getValue()));
        }
        return box;
    }

    private View groupCard(final String name, final List<Todo> items) {
        boolean open = !COLLAPSED.contains(name);
        int done = 0;
        for (Todo t : items) if (t.done) done++;

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.stroke(this, p.card, p.cardStroke, 1, 18));

        LinearLayout head = Ui.row(this);
        Ui.pad(head, this, 13, 14, 13, 14);

        View bar = new View(this);
        bar.setBackground(Ui.grad(this, p.accent, p.accent2, 3));
        head.addView(bar, Ui.lp(Ui.dp(this, 4), Ui.dp(this, 26)));

        TextView nm = Ui.tv(this, name, 16, p.ink, true);
        head.addView(nm, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Ui.margin(nm, this, 9, 0, 0, 0);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.END);

        boolean full = done == items.size();
        TextView prog = Ui.tv(this, done + "/" + items.size(), 11,
                full ? p.accent : p.sub, false);
        prog.setBackground(Ui.stroke(this,
                full ? p.accentSoft(0x24) : p.soft,
                full ? p.accentSoft(0x3D) : p.softLine, 1, 8));
        Ui.pad(prog, this, 8, 3, 8, 3);
        head.addView(prog);

        TextView add = Ui.tv(this, "＋", 16, p.accent, false);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.stroke(this, p.accentSoft(0x1F), p.accentSoft(0x2E), 1, 9));
        LinearLayout.LayoutParams alp = Ui.lp(Ui.dp(this, 27), Ui.dp(this, 27));
        alp.leftMargin = Ui.dp(this, 9);
        head.addView(add, alp);

        TextView chev = Ui.tv(this, "›", 15, p.hint, false);
        chev.setRotation(open ? 90 : 0);
        LinearLayout.LayoutParams clp = Ui.lp(Ui.dp(this, 14),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.leftMargin = Ui.dp(this, 6);
        head.addView(chev, clp);

        card.addView(head);

        final LinearLayout body = Ui.col(this);
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        for (final Todo t : items) {
            body.addView(Ui.line(this, p.cardStroke));
            body.addView(itemRow(t));
        }
        card.addView(body);

        head.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (COLLAPSED.contains(name)) COLLAPSED.remove(name);
                else COLLAPSED.add(name);
                refresh();
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EditActivity.class);
                i.putExtra("date", dayKey);
                i.putExtra("l1", name);
                startActivity(i);
            }
        });

        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 10);
        card.setLayoutParams(lp);
        return card;
    }

    private View itemRow(final Todo t) {
        LinearLayout row = Ui.row(this);
        Ui.pad(row, this, 20, 11, 13, 11);

        TextView check = Ui.tv(this, t.done ? "✓" : "○", 14,
                t.done ? p.hint : p.accent, false);
        check.setGravity(Gravity.CENTER);
        check.setBackground(Ui.oval(this, Color.TRANSPARENT, p.softLine, 1));
        row.addView(check, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));

        LinearLayout col = Ui.col(this);
        row.addView(col, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Ui.margin(col, this, 11, 0, 0, 0);

        String label = TextUtils.isEmpty(t.l2) ? "（没填子级）" : t.l2;
        TextView title = Ui.tv(this, label, 14.5f, t.done ? p.hint : p.ink, false);
        if (t.done) {
            title.setPaintFlags(title.getPaintFlags()
                    | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        col.addView(title);

        if (t.hasTarget()) {
            TextView chip = Ui.tv(this, "↗ " + t.targetLabel(), 10.5f, p.accent, false);
            chip.setBackground(Ui.stroke(this, p.accentSoft(0x22), p.accentSoft(0x3D), 1, 9));
            Ui.pad(chip, this, 8, 3, 8, 3);
            LinearLayout.LayoutParams clp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.topMargin = Ui.dp(this, 6);
            col.addView(chip, clp);
        } else {
            TextView none = Ui.tv(this, "未设置跳转", 10.5f, p.hint, false);
            none.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 9));
            Ui.pad(none, this, 8, 3, 8, 3);
            LinearLayout.LayoutParams clp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.topMargin = Ui.dp(this, 6);
            col.addView(none, clp);
        }

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TodoStore.toggleDone(MainActivity.this, t.id);
                refresh();
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EditActivity.class);
                i.putExtra("todo_id", t.id);
                startActivity(i);
            }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                confirmDelete(t);
                return true;
            }
        });
        return row;
    }

    private View tip() {
        LinearLayout box = Ui.col(this);
        TextView t = Ui.tv(this,
                "点母级标题展开 / 收起，右侧 ＋ 直接给这一组加子级。\n点 ○ 标记完成 · 右上角调色盘换主题 · 点日期回到今天。",
                11, p.hint, false);
        t.setLineSpacing(Ui.dp(this, 4), 1f);
        box.addView(t);

        if (TuningTips.isStrict()) {
            TextView link = Ui.tv(this, "圆点被清掉了？点这里看设置步骤 ›", 11.5f, p.accent, false);
            LinearLayout.LayoutParams llp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.topMargin = Ui.dp(this, 10);
            box.addView(link, llp);
            link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TuningTips.show(MainActivity.this);
                }
            });
        }

        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 20);
        lp.rightMargin = Ui.dp(this, 20);
        lp.topMargin = Ui.dp(this, 18);
        box.setLayoutParams(lp);
        return box;
    }

    // ---------------- 交互 ----------------

    private void openThemes() {
        ThemeDialog.show(this, new ThemeDialog.OnPick() {
            @Override
            public void onPick(Palette picked) {
                refresh();
                if (TodoStore.sp(MainActivity.this).getBoolean(TodoStore.K_ENABLED, true)) {
                    startOverlay(OverlayService.CMD_NORMAL);
                }
                toast("已切换 · " + picked.name);
            }
        });
    }

    private void onToggleOverlay(View holder) {
        if (!overlayOn) {
            if (!Settings.canDrawOverlays(this)) {
                String msg = "要在桌面上浮一个圆点，需要授予「显示在其他应用上层」权限。\n\n授权后回到这里，再把开关打开。";
                if (TuningTips.isHonor()) {
                    msg = "部分国产机型需要在两个地方设置：\n\n"
                            + "① 悬浮窗权限：设置 → 应用和服务 → 应用管理 → RedDot → 权限 → 悬浮窗 → 允许\n\n"
                            + "② 应用启动管理：设置 → 应用和服务 → 应用启动管理 → RedDot → 关掉自动管理，三项都打开\n\n"
                            + "只开第一步的话，圆点过一会儿会被系统清掉。";
                }
                AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                        .setTitle("需要悬浮窗权限")
                        .setMessage(msg)
                        .setPositiveButton("去授权", (d, w) -> {
                            try {
                                startActivity(new Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception e) {
                                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                            }
                        });
                if (TuningTips.isStrict()) {
                    dlg.setNeutralButton("完整设置步骤", (d, w) -> TuningTips.show(this));
                }
                dlg.setNegativeButton("取消", null).show();
                return;
            }
            TodoStore.sp(this).edit()
                    .putBoolean(TodoStore.K_ENABLED, true)
                    .putBoolean(TodoStore.K_BUBBLE, true).apply();
            overlayOn = true;
            startOverlay(OverlayService.CMD_SHOW);
            toast("圆点已显示：单击看待办，双击隐藏，可拖动");
        } else {
            TodoStore.sp(this).edit().putBoolean(TodoStore.K_ENABLED, false).apply();
            stopService(new Intent(this, OverlayService.class));
            overlayOn = false;
            toast("圆点已关闭");
        }
        refresh();
    }

    private void confirmDelete(final Todo t) {
        new AlertDialog.Builder(this)
                .setTitle("删除这条子级")
                .setMessage("确定删除「" + (TextUtils.isEmpty(t.l2) ? t.l1 : t.l2) + "」吗？")
                .setPositiveButton("删除", (d, w) -> {
                    TodoStore.delete(MainActivity.this, t.id);
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void startOverlay(String cmd) {
        Intent i = new Intent(this, OverlayService.class).putExtra(OverlayService.CMD, cmd);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 10);
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
