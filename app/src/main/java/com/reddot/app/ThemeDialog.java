package com.reddot.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 底部弹出的主题选择面板 */
public class ThemeDialog {

    public interface OnPick {
        void onPick(Palette p);
    }

    public static void show(final Context ctx, final OnPick cb) {
        final Palette cur = Palette.get(ctx);
        final Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = Ui.col(ctx);
        sheet.setBackground(Ui.gradCornerTop(ctx, cur.sheet, cur.sheet, 24));
        Ui.pad(sheet, ctx, 16, 10, 16, 20);

        LinearLayout head = Ui.row(ctx);
        View dot = new View(ctx);
        dot.setBackground(Ui.oval(ctx, cur.accent, 0, 0));
        head.addView(dot, Ui.lp(Ui.dp(ctx, 7), Ui.dp(ctx, 7)));
        TextView h = Ui.tv(ctx, "主题外观", 19, cur.ink, true);
        head.addView(h, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        Ui.margin(h, ctx, 8, 0, 0, 0);
        TextView count = Ui.tv(ctx, Palette.all().size() + " 套", 11, cur.hint, false);
        head.addView(count, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        head.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(head, ctx, 4, 4, 4, 12);
        sheet.addView(head);

        ScrollView sv = new ScrollView(ctx);
        LinearLayout list = Ui.col(ctx);
        sv.addView(list, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        for (final Palette p : Palette.all()) {
            boolean on = p.id.equals(cur.id);
            LinearLayout row = Ui.row(ctx);
            row.setBackground(Ui.stroke(ctx,
                    on ? p.accentSoft(0x24) : cur.soft,
                    on ? p.accentSoft(0x66) : cur.softLine, 1, 16));
            Ui.pad(row, ctx, 12, 10, 12, 10);

            View sw = new View(ctx);
            sw.setBackground(Ui.grad(ctx, p.accent, p.accent2, 14));
            row.addView(sw, Ui.lp(Ui.dp(ctx, 42), Ui.dp(ctx, 42)));

            LinearLayout texts = Ui.col(ctx);
            row.addView(texts, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            Ui.margin(texts, ctx, 12, 0, 0, 0);
            TextView n = Ui.tv(ctx, p.label(), 14.5f, cur.ink, true);
            texts.addView(n);
            TextView ds = Ui.tv(ctx, p.desc, 11.5f, cur.sub, false);
            texts.addView(ds);

            TextView tick = Ui.tv(ctx, "✓", 12, 0xFFFFFFFF, true);
            tick.setGravity(Gravity.CENTER);
            tick.setBackground(Ui.oval(ctx, p.accent, 0, 0));
            row.addView(tick, Ui.lp(Ui.dp(ctx, 22), Ui.dp(ctx, 22)));
            tick.setVisibility(on ? View.VISIBLE : View.INVISIBLE);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Palette.set(ctx, p.id);
                    d.dismiss();
                    if (cb != null) cb.onPick(p);
                }
            });

            LinearLayout wrap = Ui.col(ctx);
            wrap.addView(row, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            Ui.margin(row, ctx, 0, 0, 0, 8);
            list.addView(wrap);
        }

        sheet.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(ctx, 424)));

        TextView hint = Ui.tv(ctx, "共 " + Palette.all().size() + " 套 · 上下滑动查看", 11, cur.hint, false);
        hint.setGravity(Gravity.CENTER);
        Ui.pad(hint, ctx, 0, 9, 0, 0);
        sheet.addView(hint);

        TextView done = Ui.tv(ctx, "完成", 14, cur.accent, true);
        done.setGravity(Gravity.CENTER);
        done.setBackground(Ui.stroke(ctx, cur.accentSoft(0x1F), cur.accentSoft(0x4D), 1, 13));
        LinearLayout.LayoutParams dlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 46));
        dlp.topMargin = Ui.dp(ctx, 12);
        sheet.addView(done, dlp);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.setContentView(sheet);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.55f);
            w.setGravity(Gravity.BOTTOM);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            w.setLayout(dm.widthPixels, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
        if (w != null) {
            DisplayMetrics dm2 = ctx.getResources().getDisplayMetrics();
            w.setLayout(dm2.widthPixels, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}
