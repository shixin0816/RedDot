package com.reddot.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 完成情况：今日进度、连续天数、近 7 天、母级完成率、每条任务的次数与时长 */
public class StatsActivity extends Activity {

    private Palette p;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Palette.get(this);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        p = Palette.get(this);
        build();
    }

    private void build() {
        applyStatusBar();
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(p.bg);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(p.bg);
        Ui.pad(root, this, 16, 0, 16, 30);

        root.addView(nav());
        root.addView(todayCard());
        root.addView(weekCard());
        root.addView(groupCard());
        root.addView(detailCard());

        TextView tip = Ui.tv(this, "完成记录按天保存，卸载应用会一起清除。", 11, p.hint, false);
        Ui.pad(tip, this, 4, 18, 4, 0);
        root.addView(tip);

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

    private View nav() {
        LinearLayout row = Ui.row(this);
        Ui.pad(row, this, 0, 54, 0, 14);

        TextView back = Ui.tv(this, "‹", 22, p.ink, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 12));
        row.addView(back, Ui.lp(Ui.dp(this, 36), Ui.dp(this, 36)));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView t = Ui.tv(this, "完成情况", 17, p.ink, true);
        row.addView(t);
        Ui.margin(t, this, 12, 0, 0, 0);
        return row;
    }

    private LinearLayout card() {
        LinearLayout c = Ui.col(this);
        c.setBackground(Ui.stroke(this, p.card, p.cardStroke, 1, 18));
        Ui.pad(c, this, 16, 16, 16, 16);
        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 12);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView label(String s) {
        TextView t = Ui.tv(this, s, 12, p.sub, false);
        t.setLetterSpacing(0.06f);
        return t;
    }

    // ---------------- 今日 ----------------

    private View todayCard() {
        LinearLayout c = card();
        c.addView(label("今天"));

        int[] pr = TodoStore.todayProgress(this);
        int done = pr[0];
        int total = pr[1];

        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams rlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(this, 6);
        c.addView(row, rlp);

        LinearLayout left = Ui.row(this);
        row.addView(left, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView big = Ui.tv(this, String.valueOf(done), 40, p.accent, true);
        left.addView(big);
        TextView of = Ui.tv(this, " / " + total + " 项", 15, p.hint, false);
        left.addView(of);
        LinearLayout.LayoutParams olp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        olp.leftMargin = Ui.dp(this, 4);
        olp.bottomMargin = Ui.dp(this, 6);
        of.setLayoutParams(olp);

        LinearLayout right = Ui.col(this);
        right.setGravity(Gravity.END);
        row.addView(right);
        right.addView(Ui.tv(this, "连续 " + TodoStore.streakDays(this) + " 天", 12, p.sub, false));
        TextView spent = Ui.tv(this, "专注 " + Todo.human(TodoStore.totalSpent(this)),
                12, p.sub, false);
        Ui.margin(spent, this, 0, 4, 0, 0);
        right.addView(spent);

        // 进度条
        LinearLayout track = Ui.row(this);
        track.setBackground(Ui.solid(this, p.soft, 4));
        LinearLayout.LayoutParams tlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 7));
        tlp.topMargin = Ui.dp(this, 12);
        c.addView(track, tlp);

        if (total > 0) {
            if (done > 0) {
                View fill = new View(this);
                fill.setBackground(Ui.grad(this, p.accent, p.accent2, 4));
                track.addView(fill, Ui.lp(0, Ui.dp(this, 7), done));
            }
            if (total - done > 0) {
                track.addView(new View(this), Ui.lp(0, Ui.dp(this, 7), total - done));
            }
        }

        return c;
    }

    // ---------------- 近 7 天 ----------------

    private View weekCard() {
        LinearLayout c = card();
        c.addView(label("近 7 天完成数"));

        int[] counts = new int[7];
        String[] keys = new String[7];
        int max = 0;
        for (int i = 0; i < 7; i++) {
            String k = TodoStore.shiftKey(TodoStore.todayKey(), i - 6);
            keys[i] = k;
            counts[i] = TodoStore.checkedCountOn(this, k);
            if (counts[i] > max) max = counts[i];
        }

        LinearLayout chart = Ui.row(this);
        chart.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams clp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 132));
        clp.topMargin = Ui.dp(this, 10);
        c.addView(chart, clp);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            boolean today = i == 6;
            LinearLayout col = Ui.col(this);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            chart.addView(col, Ui.lp(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

            TextView num = Ui.tv(this, counts[i] == 0 ? "" : String.valueOf(counts[i]),
                    10, p.hint, false);
            num.setGravity(Gravity.CENTER);
            col.addView(num);

            View bar = new View(this);
            int h = counts[i] == 0 ? 3 : Math.max(6, (int) (86f * counts[i] / Math.max(1, max)));
            bar.setBackground(Ui.grad(this,
                    today ? p.accent : p.accentSoft(0x99),
                    today ? p.accent2 : p.accentSoft(0x55), 4));
            LinearLayout.LayoutParams blp = Ui.lp(Ui.dp(this, 20), Ui.dp(this, h));
            blp.topMargin = Ui.dp(this, 4);
            col.addView(bar, blp);

            TextView w = Ui.tv(this, today ? "今天" : "周" + TodoStore.weekLabel(keys[i]),
                    10, today ? p.accent : p.hint, today);
            w.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams wlp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            wlp.topMargin = Ui.dp(this, 6);
            col.addView(w, wlp);

            col.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String k = keys[idx];
                    android.widget.Toast.makeText(StatsActivity.this,
                            TodoStore.prettyDate(k) + " · 完成 " + counts[idx] + " 项",
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
        return c;
    }

    // ---------------- 母级 ----------------

    private View groupCard() {
        LinearLayout c = card();
        c.addView(label("按母级看今天"));

        List<Todo> todos = TodoStore.getTodos(this, TodoStore.todayKey());
        Map<String, int[]> groups = new LinkedHashMap<>();
        for (Todo t : todos) {
            String g = t.l1 == null || t.l1.length() == 0 ? "未分类" : t.l1;
            int[] v = groups.get(g);
            if (v == null) {
                v = new int[]{0, 0};
                groups.put(g, v);
            }
            v[1]++;
            if (t.done) v[0]++;
        }

        if (groups.isEmpty()) {
            TextView empty = Ui.tv(this, "今天还没有任务", 13, p.hint, false);
            Ui.pad(empty, this, 0, 10, 0, 2);
            c.addView(empty);
            return c;
        }

        for (Map.Entry<String, int[]> e : groups.entrySet()) {
            int done = e.getValue()[0];
            int size = e.getValue()[1];

            LinearLayout row = Ui.row(this);
            LinearLayout.LayoutParams rlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = Ui.dp(this, 12);
            c.addView(row, rlp);

            TextView name = Ui.tv(this, e.getKey(), 13.5f, p.ink, false);
            row.addView(name, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView cnt = Ui.tv(this, done + "/" + size, 11.5f,
                    done == size ? p.accent : p.hint, false);
            row.addView(cnt);

            LinearLayout track = Ui.row(this);
            track.setBackground(Ui.solid(this, p.soft, 3));
            LinearLayout.LayoutParams tlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                    Ui.dp(this, 5));
            tlp.topMargin = Ui.dp(this, 6);
            c.addView(track, tlp);
            if (done > 0) {
                View fill = new View(this);
                fill.setBackground(Ui.solid(this, p.accent, 3));
                track.addView(fill, Ui.lp(0, Ui.dp(this, 5), done));
            }
            if (size - done > 0) {
                track.addView(new View(this), Ui.lp(0, Ui.dp(this, 5), size - done));
            }
        }
        return c;
    }

    // ---------------- 明细 ----------------

    private View detailCard() {
        LinearLayout c = card();
        c.addView(label("每条任务"));

        List<Todo> todos = TodoStore.getTodos(this, TodoStore.todayKey());
        List<Todo> list = new ArrayList<>();
        for (Todo t : todos) {
            if (t.repeat != Todo.REPEAT_NONE) list.add(t);
        }
        for (Todo t : todos) {
            if (t.repeat == Todo.REPEAT_NONE) list.add(t);
        }

        if (list.isEmpty()) {
            TextView empty = Ui.tv(this, "还没有可统计的任务", 13, p.hint, false);
            Ui.pad(empty, this, 0, 10, 0, 2);
            c.addView(empty);
            return c;
        }

        for (Todo t : list) {
            LinearLayout row = Ui.row(this);
            LinearLayout.LayoutParams rlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = Ui.dp(this, 12);
            c.addView(row, rlp);

            LinearLayout left = Ui.col(this);
            row.addView(left, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            String title = t.l2 != null && t.l2.length() > 0 ? t.l2 : t.l1;
            left.addView(Ui.tv(this, title, 13.5f, p.ink, false));

            StringBuilder meta = new StringBuilder();
            if (t.repeat == Todo.REPEAT_DAILY) meta.append("每天");
            else if (t.repeat == Todo.REPEAT_LONG) meta.append("长期");
            else meta.append(TodoStore.prettyDate(t.date));
            meta.append(" · 完成 ").append(TodoStore.checkCount(this, t.id)).append(" 次");
            TextView m = Ui.tv(this, meta.toString(), 11, p.hint, false);
            Ui.margin(m, this, 0, 3, 0, 0);
            left.addView(m);

            int sec = t.liveSec();
            TextView dur = Ui.tv(this, sec > 0 ? Todo.human(sec) : "—", 12,
                    sec > 0 ? p.accent : p.hint, false);
            row.addView(dur);
        }
        return c;
    }
}
