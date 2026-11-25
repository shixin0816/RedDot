package com.reddot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EditActivity extends Activity {

    private Palette p;
    private Todo todo;
    private long todoId;
    private String dateKeyValue;

    private EditText etL1, etL2;
    private TextView targetView;
    private TextView dateButton;
    private TextView[] dayChips = new TextView[3];
    private LinearLayout chipsRow;

    private int repeatValue = Todo.REPEAT_NONE;
    private TextView[] repeatChips = new TextView[3];
    private LinearLayout dayPickerRow;
    private TextView repeatHint;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Palette.get(this);

        todoId = getIntent().getLongExtra("todo_id", -1);
        todo = todoId == -1 ? null : TodoStore.get(this, todoId);
        if (todo == null) {
            todo = new Todo();
            todoId = -1;
            todo.id = System.currentTimeMillis();
            todo.date = getIntent().getStringExtra("date");
            todo.l1 = getIntent().getStringExtra("l1");
            if (todo.l1 == null) todo.l1 = "";
        }
        if (TextUtils.isEmpty(todo.date)) dateKeyValue = TodoStore.todayKey();
        else dateKeyValue = todo.date;
        repeatValue = todo.repeat;

        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Todo latest = TodoStore.get(this, todo.id);
        if (latest != null) {
            todo = latest;
            if (!TextUtils.isEmpty(todo.date)) dateKeyValue = todo.date;
            updateTarget();
        }
        pickUpClipboardOnReturn();
    }

    /**
     * 用户点「去捕获链接」后可能直接回 RedDot 而没去点那个圆点。
     * 只要回到这个页面时剪贴板里已经有链接，就地弹确认，不用再去桌面找圆点。
     */
    private boolean autoClipDone = false;

    private void pickUpClipboardOnReturn() {
        if (autoClipDone || todo == null) return;
        // 新建的待办在「去捕获链接」时才会写进存储并记下 pending，所以这里按 todo.id 判断
        if (TodoStore.getPending(this) != todo.id) return;
        autoClipDone = true;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            String url = Clip.url(this);
            if (url == null) return;
            confirmLink(url);
        }, 350);
    }

    /** 从剪贴板读到链接后的确认 */
    private void confirmLink(final String url) {
        final String name = TargetUtil.appNameFromUrl(url);
        new AlertDialog.Builder(this)
                .setTitle("从剪贴板读到链接")
                .setMessage("来源：" + name + "\n" + url + "\n\n存为「" + todo.path() + "」的跳转目标？")
                .setPositiveButton("保存", (d, w) -> {
                    todo.targetUrl = url;
                    todo.targetPackage = "";
                    todo.targetName = name;
                    saveFields(false);
                    updateTarget();
                    TodoStore.clearPending(this);
                    String pkg = TargetUtil.resolvePackage(EditActivity.this, url);
                    if (pkg != null) Recents.touch(EditActivity.this, pkg);
                    toast("已保存跳转目标");
                })
                .setNegativeButton("取消", (d, w) -> TodoStore.clearPending(this))
                .setOnCancelListener(d -> TodoStore.clearPending(this))
                .show();
    }

    // ---------------- 界面 ----------------

    private void build() {
        p = Palette.get(this);
        if (Build.VERSION.SDK_INT >= 23) {
            View dv = getWindow().getDecorView();
            int flags = dv.getSystemUiVisibility();
            if (p.light) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            dv.setSystemUiVisibility(flags);
        }
        getWindow().setStatusBarColor(p.bg);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(p.bg);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(p.bg);
        Ui.pad(root, this, 16, 0, 16, 30);

        root.addView(nav());
        root.addView(fieldsCard());
        root.addView(repeatCard());
        root.addView(targetCard());
        root.addView(saveButton());

        TextView del = Ui.tv(this, "删除这条子级", 13, p.danger, false);
        del.setGravity(Gravity.CENTER);
        Ui.pad(del, this, 8, 16, 8, 8);
        del.setVisibility(todoId == -1 ? View.GONE : View.VISIBLE);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
        root.addView(del);

        sv.addView(root, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
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

        String title = todoId == -1 ? "新建待办" : "编辑子级";
        TextView t = Ui.tv(this, title, 17, p.ink, true);
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

    private EditText input(String value, String hint) {
        EditText et = new EditText(this);
        et.setText(value == null ? "" : value);
        et.setHint(hint);
        et.setTextSize(15);
        et.setTextColor(p.ink);
        et.setHintTextColor(p.hint);
        et.setBackground(Ui.stroke(this, p.input, p.inputStroke, 1, 13));
        et.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setSingleLine(true);
        return et;
    }

    private View fieldsCard() {
        LinearLayout c = card();

        c.addView(Ui.tv(this, "母级 · 分类（必填，同名的收进同一组）", 12, p.sub, false));

        etL1 = input(todo.l1, "比如：英语学习");
        LinearLayout.LayoutParams lp1 = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.topMargin = Ui.dp(this, 7);
        c.addView(etL1, lp1);
        etL1.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c2) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                paintChips();
            }
        });

        TextView gl = Ui.tv(this, "或选一个已有的母级", 10.5f, p.hint, false);
        LinearLayout.LayoutParams glp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.topMargin = Ui.dp(this, 12);
        c.addView(gl, glp);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chipsRow = Ui.row(this);
        hs.addView(chipsRow);
        LinearLayout.LayoutParams hlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(this, 8);
        c.addView(hs, hlp);
        paintChips();

        TextView l2 = Ui.tv(this, "子级 · 具体这件事", 12, p.sub, false);
        LinearLayout.LayoutParams l2lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        l2lp.topMargin = Ui.dp(this, 16);
        c.addView(l2, l2lp);

        etL2 = input(todo.l2, "比如：背 20 个单词");
        LinearLayout.LayoutParams lp2 = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = Ui.dp(this, 7);
        c.addView(etL2, lp2);
        return c;
    }

    /** 已有母级的快捷按钮 */
    private void paintChips() {
        if (chipsRow == null || etL1 == null) return;
        chipsRow.removeAllViews();
        String cur = etL1.getText().toString().trim();

        Set<String> names = new LinkedHashSet<>();
        for (Todo t : TodoStore.getTodos(this)) {
            if (!TextUtils.isEmpty(t.l1)) names.add(t.l1);
        }
        if (names.isEmpty()) {
            chipsRow.addView(Ui.tv(this, "还没有母级，直接输入就会新建一组", 11, p.hint, false));
            return;
        }
        for (final String n : names) {
            boolean on = n.equals(cur);
            TextView chip = Ui.tv(this, n, 11.5f, on ? p.accent : p.sub, false);
            chip.setBackground(Ui.stroke(this,
                    on ? p.accentSoft(0x24) : p.soft,
                    on ? p.accentSoft(0x4D) : p.softLine, 1, 8));
            Ui.pad(chip, this, 10, 5, 10, 5);
            LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(this, 6);
            chipsRow.addView(chip, lp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etL1.setText(n);
                    etL1.setSelection(etL1.getText().length());
                }
            });
        }
    }

    private View repeatCard() {
        LinearLayout c = card();
        c.addView(Ui.tv(this, "重复", 15, p.ink, true));

        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams rlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(this, 10);
        c.addView(row, rlp);

        String[] labels = {"单次", "每天", "长期"};
        for (int i = 0; i < 3; i++) {
            final int v = i;
            TextView chip = Ui.tv(this, labels[i], 13, p.sub, false);
            chip.setGravity(Gravity.CENTER);
            Ui.pad(chip, this, 10, 8, 10, 8);
            LinearLayout.LayoutParams lp = Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) lp.leftMargin = Ui.dp(this, 8);
            row.addView(chip, lp);
            repeatChips[i] = chip;
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v2) {
                    repeatValue = v;
                    updateRepeatUi();
                }
            });
        }

        // 只有「单次」需要选具体哪一天
        dayPickerRow = Ui.row(this);
        LinearLayout.LayoutParams dlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dp(this, 12);
        c.addView(dayPickerRow, dlp);

        String[] dayLabels = {"今天", "明天", "后天"};
        for (int i = 0; i < 3; i++) {
            final int off = i;
            TextView chip = Ui.tv(this, dayLabels[i], 13, p.sub, false);
            Ui.pad(chip, this, 14, 8, 14, 8);
            LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(this, 8);
            dayPickerRow.addView(chip, lp);
            dayChips[i] = chip;
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dateKeyValue = TodoStore.shiftKey(TodoStore.todayKey(), off);
                    updateDateUi();
                }
            });
        }

        dateButton = Ui.tv(this, "", 13, p.ink, false);
        dateButton.setGravity(Gravity.CENTER);
        dateButton.setBackground(Ui.stroke(this, p.input, p.inputStroke, 1, 10));
        Ui.pad(dateButton, this, 10, 9, 10, 9);
        dayPickerRow.addView(dateButton, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
            }
        });

        repeatHint = Ui.tv(this, "", 11.5f, p.hint, false);
        repeatHint.setLineSpacing(Ui.dp(this, 3), 1f);
        LinearLayout.LayoutParams hlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(this, 12);
        c.addView(repeatHint, hlp);

        updateRepeatUi();
        return c;
    }

    private void updateRepeatUi() {
        for (int i = 0; i < 3; i++) {
            TextView chip = repeatChips[i];
            if (chip == null) continue;
            boolean on = repeatValue == i;
            chip.setTextColor(on ? p.btnInk : p.sub);
            chip.setBackground(on
                    ? Ui.grad(this, p.accent, p.accent2, 10)
                    : Ui.stroke(this, p.soft, p.softLine, 1, 10));
        }
        if (dayPickerRow != null) {
            dayPickerRow.setVisibility(repeatValue == Todo.REPEAT_NONE ? View.VISIBLE : View.GONE);
        }
        if (repeatHint != null) {
            if (repeatValue == Todo.REPEAT_DAILY) {
                repeatHint.setText("每天都出现在今日待办里，完成按天单独记录。链接只绑一次，第二天不用重新粘贴。");
            } else if (repeatValue == Todo.REPEAT_LONG) {
                repeatHint.setText("一直留在列表里，勾选后保持完成，直到你手动取消。适合「整理错题本」这类持续项目。");
            } else {
                repeatHint.setText("只在选中的这一天出现。");
            }
        }
        updateDateUi();
    }

    private void updateDateUi() {
        if (dateButton == null) return;
        dateButton.setText(TodoStore.prettyDate(dateKeyValue));
        for (int i = 0; i < 3; i++) {
            TextView chip = dayChips[i];
            if (chip == null) continue;
            boolean on = TodoStore.shiftKey(TodoStore.todayKey(), i).equals(dateKeyValue);
            chip.setTextColor(on ? p.btnInk : p.sub);
            chip.setBackground(on
                    ? Ui.grad(this, p.accent, p.accent2, 10)
                    : Ui.stroke(this, p.soft, p.softLine, 1, 10));
        }
    }

    private View targetCard() {
        LinearLayout c = card();
        c.addView(Ui.tv(this, "第三级 · 跳转目标", 15, p.ink, true));

        targetView = Ui.tv(this, "", 13, p.accent, false);
        targetView.setLineSpacing(Ui.dp(this, 4), 1f);
        LinearLayout.LayoutParams tlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(this, 10);
        c.addView(targetView, tlp);
        updateTarget();

        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams rlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(this, 12);
        c.addView(row, rlp);

        TextView pick = Ui.tv(this, "选择 App", 14, p.accent, false);
        pick.setGravity(Gravity.CENTER);
        pick.setBackground(Ui.stroke(this, p.accentSoft(0x1F), p.accentSoft(0x47), 1, 13));
        LinearLayout.LayoutParams plp = Ui.lp(0, Ui.dp(this, 46), 1);
        row.addView(pick, plp);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickApp();
            }
        });

        TextView cap = Ui.tv(this, "去捕获链接", 14, p.btnInk, true);
        cap.setGravity(Gravity.CENTER);
        cap.setBackground(Ui.grad(this, p.accent, p.accent2, 13));
        LinearLayout.LayoutParams clp = Ui.lp(0, Ui.dp(this, 46), 1);
        clp.leftMargin = Ui.dp(this, 10);
        row.addView(cap, clp);
        cap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCapture();
            }
        });

        // 最稳的一条路：复制完链接回到这里点一下，直接读剪贴板
        TextView paste = Ui.tv(this, "⧉ 从剪贴板粘贴链接", 13.5f, p.ink, false);
        paste.setGravity(Gravity.CENTER);
        paste.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 13));
        LinearLayout.LayoutParams paslp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 44));
        paslp.topMargin = Ui.dp(this, 10);
        c.addView(paste, paslp);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteLink();
            }
        });

        TextView hint = Ui.tv(this,
                "复制链接的方法：在目标页点「分享」→「复制链接」。\n也可以点「分享」直接选 RedDot。",
                11, p.hint, false);
        hint.setLineSpacing(Ui.dp(this, 3), 1f);
        LinearLayout.LayoutParams hlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(this, 10);
        c.addView(hint, hlp);
        return c;
    }

    /** 粘贴链接面板：能自动读到就填好，读不到也能长按手动粘贴 */
    private void pasteLink() {
        LinearLayout root = Ui.col(this);
        root.setBackground(Ui.stroke(this, p.sheet, p.sheetStroke, 1, 24));
        Ui.pad(root, this, 18, 16, 18, 18);

        LinearLayout head = Ui.row(this);
        head.addView(Ui.tv(this, "粘贴跳转链接", 17, p.ink, true),
                Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView close = Ui.tv(this, "×", 20, p.hint, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 11));
        head.addView(close, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));
        root.addView(head);

        String fromClip = Clip.url(this);
        final TextView state = Ui.tv(this,
                fromClip != null ? "已经从剪贴板读到链接，确认后保存。" : "剪贴板里没读到链接，长按输入框手动粘贴，或先在目标页点「复制链接」。",
                11, fromClip != null ? p.sub : p.danger, false);
        state.setLineSpacing(Ui.dp(this, 3), 1f);
        LinearLayout.LayoutParams slp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 8);
        root.addView(state, slp);

        final EditText et = Ui.editText(this, p, fromClip, "https://…", 3);
        LinearLayout.LayoutParams etlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        etlp.topMargin = Ui.dp(this, 10);
        root.addView(et, etlp);

        final Dialog d = Ui.sheet(this, root);

        TextView save = Ui.tv(this, "用它作为跳转目标", 15, p.btnInk, true);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.grad(this, p.accent, p.accent2, 14));
        LinearLayout.LayoutParams savelp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 50));
        savelp.topMargin = Ui.dp(this, 14);
        root.addView(save, savelp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = TargetUtil.extractUrl(et.getText().toString());
                if (url == null) {
                    state.setText("这还不是一个完整链接，要 http:// 或 https:// 开头。");
                    state.setTextColor(p.danger);
                    return;
                }
                todo.targetUrl = url;
                todo.targetPackage = "";
                todo.targetName = TargetUtil.appNameFromUrl(url);
                saveFields(false);
                updateTarget();
                TodoStore.clearPending(EditActivity.this);
                String pkg = TargetUtil.resolvePackage(EditActivity.this, url);
                if (pkg != null) Recents.touch(EditActivity.this, pkg);
                d.dismiss();
                toast("已设为跳转目标");
            }
        });

        TextView cancelBtn = Ui.tv(this, "取消", 13, p.sub, false);
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 13));
        LinearLayout.LayoutParams calp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 44));
        calp.topMargin = Ui.dp(this, 10);
        root.addView(cancelBtn, calp);
        cancelBtn.setOnClickListener(v -> d.dismiss());

        close.setOnClickListener(v -> d.dismiss());
        d.show();
    }

    private View saveButton() {
        TextView save = Ui.tv(this, "保存待办", 15, p.btnInk, true);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.grad(this, p.accent, p.accent2, 15));
        LinearLayout.LayoutParams lp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 50));
        lp.topMargin = Ui.dp(this, 4);
        save.setLayoutParams(lp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (saveFields(true)) finish();
            }
        });
        return save;
    }

    private void updateTarget() {
        if (targetView == null) return;
        if (!TextUtils.isEmpty(todo.targetUrl)) {
            targetView.setText("↗ " + todo.targetLabel() + "\n" + todo.targetUrl);
            targetView.setTextColor(p.accent);
        } else if (!TextUtils.isEmpty(todo.targetPackage)) {
            targetView.setText("打开应用 · " + todo.targetLabel());
            targetView.setTextColor(p.accent);
        } else {
            targetView.setText("还没有设置跳转目标");
            targetView.setTextColor(p.hint);
        }
    }

    private boolean saveFields(boolean check) {
        String l1 = etL1.getText().toString().trim();
        String l2 = etL2.getText().toString().trim();
        if (check && l1.length() == 0) {
            toast("先填母级，比如「英语学习」");
            return false;
        }
        if (check && l2.length() == 0) {
            toast("再填子级，具体要做什么");
            return false;
        }
        todo.l1 = l1;
        todo.l2 = l2;
        todo.date = dateKeyValue;
        todo.repeat = repeatValue;
        TodoStore.save(this, todo);
        return true;
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            java.text.SimpleDateFormat f =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            cal.setTime(f.parse(dateKeyValue));
        } catch (Exception ignored) {
        }
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d);
            dateKeyValue = TodoStore.keyOf(picked);
            updateDateUi();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ---------------------------------------------------------------- 选 App

    private static class AppEntry {
        String pkg;
        String label;
        android.graphics.drawable.Drawable icon;
        int recency = -1;   // -1 表示没在「最近跳转」里
    }

    /** 取出手机上所有可启动的应用，最近用过的排前面 */
    private List<AppEntry> loadApps() {
        List<String> recent = Recents.list(this);
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> all;
        try {
            all = pm.queryIntentActivities(main, 0);
        } catch (Exception e) {
            all = new ArrayList<>();
        }

        java.util.LinkedHashMap<String, AppEntry> map = new java.util.LinkedHashMap<>();
        for (ResolveInfo ri : all) {
            if (ri == null || ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (getPackageName().equals(pkg)) continue;
            if (map.containsKey(pkg)) continue;      // 一个应用只留一条
            AppEntry e = new AppEntry();
            e.pkg = pkg;
            try {
                e.label = String.valueOf(ri.loadLabel(pm));
            } catch (Exception ex) {
                e.label = pkg;
            }
            try {
                e.icon = ri.loadIcon(pm);
            } catch (Exception ex) {
                e.icon = null;
            }
            e.recency = recent.indexOf(pkg);
            map.put(pkg, e);
        }

        List<AppEntry> out = new ArrayList<>(map.values());
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                boolean ra = a.recency >= 0;
                boolean rb = b.recency >= 0;
                if (ra && rb) return a.recency - b.recency;
                if (ra != rb) return ra ? -1 : 1;
                return collator.compare(a.label, b.label);
            }
        });
        return out;
    }

    private void pickApp() {
        final List<AppEntry> apps = loadApps();

        LinearLayout root = Ui.col(this);
        root.setBackground(Ui.stroke(this, p.sheet, p.sheetStroke, 1, 24));
        Ui.pad(root, this, 18, 16, 18, 16);

        LinearLayout head = Ui.row(this);
        head.addView(Ui.tv(this, "选择要跳转的应用", 17, p.ink, true),
                Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView close = Ui.tv(this, "×", 20, p.hint, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 11));
        head.addView(close, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));
        root.addView(head);

        final EditText search = Ui.editText(this, p, "", "搜索应用名或包名", 1);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setSingleLine(true);
        LinearLayout.LayoutParams slp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 10);
        root.addView(search, slp);

        final ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        final LinearLayout listBox = Ui.col(this);
        sv.addView(listBox, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams svlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 330));
        svlp.topMargin = Ui.dp(this, 8);
        root.addView(sv, svlp);

        final Dialog d = Ui.sheet(this, root);
        close.setOnClickListener(v -> d.dismiss());

        paintAppList(listBox, apps, "", d);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b2, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                paintAppList(listBox, apps, s.toString(), d);
            }
        });

        d.show();
    }

    private void paintAppList(LinearLayout box, List<AppEntry> apps, String keyword, Dialog d) {
        box.removeAllViews();
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.US);
        boolean searching = k.length() > 0;

        List<AppEntry> recentHits = new ArrayList<>();
        List<AppEntry> rest = new ArrayList<>();
        for (AppEntry a : apps) {
            if (searching) {
                String hay = (a.label + " " + a.pkg).toLowerCase(Locale.US);
                if (!hay.contains(k)) continue;
                rest.add(a);
            } else if (a.recency >= 0) {
                recentHits.add(a);
            } else {
                rest.add(a);
            }
        }

        if (!searching && !recentHits.isEmpty()) {
            box.addView(sectionLabel("最近跳转"));
            for (AppEntry a : recentHits) box.addView(appRow(a, d));
            box.addView(sectionLabel("全部应用"));
        }
        for (AppEntry a : rest) box.addView(appRow(a, d));

        if (recentHits.isEmpty() && rest.isEmpty()) {
            TextView empty = Ui.tv(this, searching ? "没找到这个应用" : "没读到应用列表", 12, p.hint, false);
            empty.setGravity(Gravity.CENTER);
            Ui.pad(empty, this, 8, 26, 8, 26);
            box.addView(empty);
        }
    }

    private TextView sectionLabel(String text) {
        TextView t = Ui.tv(this, text, 10.5f, p.hint, false);
        Ui.pad(t, this, 4, 12, 4, 5);
        return t;
    }

    private View appRow(final AppEntry a, final Dialog d) {
        LinearLayout row = Ui.row(this);
        Ui.pad(row, this, 4, 9, 4, 9);
        row.setBackgroundColor(Color.TRANSPARENT);

        android.widget.ImageView ic = new android.widget.ImageView(this);
        if (a.icon != null) ic.setImageDrawable(a.icon);
        row.addView(ic, Ui.lp(Ui.dp(this, 30), Ui.dp(this, 30)));

        TextView name = Ui.tv(this, a.label, 14, p.ink, false);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(name, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Ui.margin(name, this, 12, 0, 6, 0);

        boolean current = a.pkg.equals(todo.targetPackage);
        if (current || a.recency >= 0) {
            TextView tag = Ui.tv(this, current ? "当前" : "最近", 9.5f,
                    current ? p.btnInk : p.accent, false);
            tag.setGravity(Gravity.CENTER);
            tag.setBackground(current
                    ? Ui.grad(this, p.accent, p.accent2, 7)
                    : Ui.stroke(this, p.accentSoft(0x1F), p.accentSoft(0x3A), 1, 7));
            Ui.pad(tag, this, 7, 2, 7, 2);
            row.addView(tag);
        }

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                todo.targetPackage = a.pkg;
                todo.targetUrl = "";
                todo.targetName = a.label;
                saveFields(false);
                updateTarget();
                Recents.touch(EditActivity.this, a.pkg);
                d.dismiss();
                toast("已设为跳转目标 · " + a.label);
            }
        });
        return row;
    }

    private void startCapture() {
        if (etL1.getText().toString().trim().length() == 0) {
            toast("先填母级，比如「英语学习」");
            return;
        }
        if (!saveFields(false)) return;
        TodoStore.setPending(this, todo.id);

        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("还没有悬浮窗权限")
                    .setMessage("捕获模式要在别的应用上面显示圆点，所以需要悬浮窗权限。\n\n也可以不用它：在小红书 / B站里点「分享」，选 RedDot，链接会直接存进来。")
                    .setPositiveButton("去授权", (d, w) -> {
                        try {
                            startActivity(new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton("知道了", null)
                    .show();
            return;
        }

        Intent svc = new Intent(this, OverlayService.class)
                .putExtra(OverlayService.CMD, OverlayService.CMD_CAPTURE);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
        else startService(svc);

        Toast.makeText(this,
                "捕获已开启：去目标页面 → 复制链接（或分享给 RedDot）→ 点一下圆点确认",
                Toast.LENGTH_LONG).show();

        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除这条子级")
                .setMessage("确定删除「" + (TextUtils.isEmpty(todo.l2) ? todo.l1 : todo.l2) + "」吗？")
                .setPositiveButton("删除", (d, w) -> {
                    TodoStore.delete(this, todo.id);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
