package com.reddot.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 点红点后从底部弹出的「今日待办」，按母级分组显示 */
public class TodoSheetDialog {

    public static void show(final Context ctx, int windowType) {
        final Palette p = Palette.get(ctx);
        final List<Todo> todos = TodoStore.getTodos(ctx, TodoStore.todayKey());
        final Dialog[] holder = new Dialog[1];

        LinearLayout sheet = Ui.col(ctx);
        sheet.setBackground(Ui.gradCornerTop(ctx, p.sheet, p.sheet, 24));
        Ui.pad(sheet, ctx, 16, 10, 16, 22);

        View handle = new View(ctx);
        handle.setBackground(Ui.solid(ctx, p.softLine, 2));
        LinearLayout.LayoutParams hlp = Ui.lp(Ui.dp(ctx, 38), Ui.dp(ctx, 4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = Ui.dp(ctx, 12);
        sheet.addView(handle, hlp);

        LinearLayout head = Ui.row(ctx);
        View dot = new View(ctx);
        dot.setBackground(Ui.oval(ctx, p.accent, 0, 0));
        head.addView(dot, Ui.lp(Ui.dp(ctx, 7), Ui.dp(ctx, 7)));
        TextView title = Ui.tv(ctx, "今日待办", 19, p.ink, true);
        head.addView(title);
        Ui.margin(title, ctx, 9, 0, 0, 0);
        int left = 0;
        for (Todo t : todos) if (!t.done) left++;
        TextView meta = Ui.tv(ctx, "今天 · " + left + " 项", 11, p.hint, false);
        head.addView(meta, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        meta.setGravity(Gravity.END);
        Ui.pad(head, ctx, 4, 0, 4, 12);
        sheet.addView(head);

        ScrollView sv = new ScrollView(ctx);
        LinearLayout body = Ui.col(ctx);

        if (todos.isEmpty()) {
            TextView empty = Ui.tv(ctx, "今天还没有待办\n去 RedDot 里加一条吧", 13, p.hint, false);
            empty.setGravity(Gravity.CENTER);
            empty.setLineSpacing(Ui.dp(ctx, 5), 1f);
            Ui.pad(empty, ctx, 0, 26, 0, 26);
            body.addView(empty);
        } else {
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

            boolean first = true;
            for (Map.Entry<String, List<Todo>> e : groups.entrySet()) {
                if (!first) body.addView(Ui.spacer(ctx, 10));
                first = false;

                int done = 0;
                for (Todo t : e.getValue()) if (t.done) done++;

                LinearLayout gh = Ui.row(ctx);
                View gd = new View(ctx);
                gd.setBackground(Ui.oval(ctx, p.accent, 0, 0));
                gh.addView(gd, Ui.lp(Ui.dp(ctx, 6), Ui.dp(ctx, 6)));
                TextView gn = Ui.tv(ctx, e.getKey(), 13, p.ink, true);
                gh.addView(gn);
                Ui.margin(gn, ctx, 7, 0, 0, 0);
                TextView gc = Ui.tv(ctx, done + "/" + e.getValue().size(), 10.5f, p.hint, false);
                gh.addView(gc, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                gc.setGravity(Gravity.END);
                Ui.pad(gh, ctx, 2, 1, 2, 7);
                body.addView(gh);

                for (final Todo t : e.getValue()) {
                    LinearLayout item = Ui.row(ctx);
                    item.setBackground(Ui.stroke(ctx, p.card, p.cardStroke, 1, 14));
                    Ui.pad(item, ctx, 12, 11, 12, 11);

                    TextView check = Ui.tv(ctx, t.done ? "✓" : "○", 14,
                            t.done ? p.hint : p.accent, false);
                    check.setGravity(Gravity.CENTER);
                    check.setBackground(Ui.oval(ctx, Color.TRANSPARENT, p.softLine, 1));
                    item.addView(check, Ui.lp(Ui.dp(ctx, 32), Ui.dp(ctx, 32)));

                    LinearLayout col = Ui.col(ctx);
                    item.addView(col, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    Ui.margin(col, ctx, 11, 0, 0, 0);

                    String label = TextUtils.isEmpty(t.l2) ? e.getKey() : t.l2;
                    TextView nm = Ui.tv(ctx, label, 14.5f, p.ink, false);
                    if (t.done) {
                        nm.setPaintFlags(nm.getPaintFlags()
                                | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        nm.setTextColor(p.hint);
                    }
                    col.addView(nm);

                    if (t.hasTarget()) {
                        TextView chip = Ui.tv(ctx, "↗ " + t.targetLabel(), 10.5f, p.accent, false);
                        chip.setBackground(Ui.stroke(ctx, p.accentSoft(0x22),
                                p.accentSoft(0x3D), 1, 9));
                        Ui.pad(chip, ctx, 8, 3, 8, 3);
                        LinearLayout.LayoutParams clp = Ui.lp(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        clp.topMargin = Ui.dp(ctx, 6);
                        col.addView(chip, clp);
                    }

                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder[0] != null) holder[0].dismiss();
                            if (t.hasTarget()) {
                                TargetUtil.openTarget(ctx, t);
                            } else {
                                Toast.makeText(ctx, "这条待办还没设置跳转目标",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    LinearLayout wrap = Ui.col(ctx);
                    wrap.addView(item, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    Ui.margin(item, ctx, 0, 0, 0, 7);
                    body.addView(wrap);
                }
            }
        }

        sv.addView(body, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        sheet.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                todos.size() > 4 ? Ui.dp(ctx, 420) : LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout foot = Ui.row(ctx);
        TextView close = Ui.tv(ctx, "关闭", 14, p.ink, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.stroke(ctx, p.soft, p.softLine, 1, 13));
        LinearLayout.LayoutParams flp = Ui.lp(0, Ui.dp(ctx, 46), 1);
        flp.topMargin = Ui.dp(ctx, 14);
        foot.addView(close, flp);

        TextView manage = Ui.tv(ctx, "管理待办", 14, p.accent, true);
        manage.setGravity(Gravity.CENTER);
        manage.setBackground(Ui.stroke(ctx, p.accentSoft(0x24), p.accentSoft(0x4D), 1, 13));
        LinearLayout.LayoutParams mlp = Ui.lp(0, Ui.dp(ctx, 46), 1);
        mlp.topMargin = Ui.dp(ctx, 14);
        mlp.leftMargin = Ui.dp(ctx, 10);
        foot.addView(manage, mlp);
        sheet.addView(foot);

        Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(sheet);
        dlg.setCanceledOnTouchOutside(true);
        holder[0] = dlg;

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder[0] != null) holder[0].dismiss();
            }
        });
        manage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder[0] != null) holder[0].dismiss();
                Intent i = new Intent(ctx, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ctx.startActivity(i);
            }
        });

        Window w = dlg.getWindow();
        if (w != null) {
            if (windowType != 0) w.setType(windowType);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.55f);
            w.setGravity(Gravity.BOTTOM);
        }
        dlg.show();
        if (w != null) {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            w.setLayout(dm.widthPixels, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}
