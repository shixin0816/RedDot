package com.reddot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 捕获跳转目标：读剪贴板里的链接，确认后存到某条待办上。
 *
 * 关键点：Android 10 起，只有拿到焦点的应用才能读剪贴板。
 * 所以这里不在 onCreate 里读，而是等面板弹出后轮询重试，
 * 并且给一个可长按粘贴的输入框 + 「重新读取」按钮兜底。
 */
public class CaptureActivity extends Activity {

    private static final int RETRY_MAX = 14;
    private static final int RETRY_GAP = 200;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Palette p;
    private long pendingId = -1;
    private Todo target;
    private Dialog sheet;
    private EditText etUrl;
    private TextView sourceView;
    private TextView hintView;

    private boolean saved = false;
    private boolean userEdited = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Palette.get(this);

        pendingId = TodoStore.getPending(this);
        if (pendingId != -1) {
            target = TodoStore.get(this, pendingId);
        }
        if (target == null) {
            // 没有正在捕获的待办：让用户就地选一条，而不是直接赶人走
            handler.postDelayed(this::chooseTarget, 150);
            return;
        }
        buildSheet();
        // 面板已经显示，焦点大概还没到位，稍等一下再读，读不到就重试
        handler.postDelayed(() -> grab(0), 150);
    }

    // ---------------------------------------------------------------- 读剪贴板

    private void grab(final int attempt) {
        if (saved || userEdited || etUrl == null) return;

        String url = Clip.url(this);
        if (url != null) {
            fillLink(url);
            return;
        }
        if (attempt < RETRY_MAX) {
            handler.postDelayed(() -> grab(attempt + 1), RETRY_GAP);
        } else {
            showEmpty();
        }
    }

    private void fillLink(String url) {
        if (etUrl == null) return;
        etUrl.setText(url);
        etUrl.setSelection(url.length());
        String name = TargetUtil.appNameFromUrl(url);
        if (sourceView != null) {
            sourceView.setText("来源 · " + name);
            sourceView.setVisibility(View.VISIBLE);
        }
        setHint("链接已填入。确认没问题就保存；不对可以长按输入框手动粘贴。", p.sub);
    }

    private void showEmpty() {
        String rawText = Clip.raw(this);
        if (rawText != null) {
            // 有内容但没有链接：说明复制的东西不对
            String head = rawText.length() > 30 ? rawText.substring(0, 30) + "…" : rawText;
            setHint("剪贴板里是「" + head + "」，没找到链接。\n请在目标页点「分享 → 复制链接」，再点「重新读取」。", p.danger);
        } else {
            setHint("读不到剪贴板。可能是系统限制了剪贴板访问：\n1. 在目标页点「分享 → 复制链接」（只截屏不算）\n2. 长按下面的输入框手动粘贴\n3. 或者点「重新读取」再试一次", p.danger);
        }
    }

    private void setHint(String s, int color) {
        if (hintView == null) return;
        hintView.setText(s);
        hintView.setTextColor(color);
    }

    // ---------------------------------------------------------------- 界面

    private void buildSheet() {
        LinearLayout root = Ui.col(this);
        root.setBackground(Ui.stroke(this, p.sheet, p.sheetStroke, 1, 24));
        Ui.pad(root, this, 18, 16, 18, 18);

        LinearLayout head = Ui.row(this);
        head.addView(Ui.tv(this, "捕获跳转目标", 17, p.ink, true),
                Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView close = Ui.tv(this, "×", 20, p.hint, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 11));
        head.addView(close, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));
        close.setOnClickListener(v -> cancel());
        root.addView(head);

        TextView path = Ui.tv(this, "存为「" + target.path() + "」", 12, p.sub, false);
        LinearLayout.LayoutParams plp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Ui.dp(this, 6);
        root.addView(path, plp);

        sourceView = Ui.tv(this, "", 11, p.accent, false);
        sourceView.setBackground(Ui.stroke(this, p.accentSoft(0x1F), p.accentSoft(0x3A), 1, 8));
        Ui.pad(sourceView, this, 9, 4, 9, 4);
        sourceView.setVisibility(View.GONE);
        LinearLayout.LayoutParams slp = Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 12);
        root.addView(sourceView, slp);

        etUrl = Ui.editText(this, p, "", "https://…（也可以长按这里手动粘贴）", 3);
        LinearLayout.LayoutParams etlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        etlp.topMargin = Ui.dp(this, 10);
        root.addView(etUrl, etlp);
        etUrl.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b2, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                userEdited = true;
            }
        });

        hintView = Ui.tv(this, "正在读取剪贴板…", 11, p.hint, false);
        hintView.setLineSpacing(Ui.dp(this, 3), 1f);
        LinearLayout.LayoutParams hlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(this, 10);
        root.addView(hintView, hlp);

        TextView save = Ui.tv(this, "保存为跳转目标", 15, p.btnInk, true);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.grad(this, p.accent, p.accent2, 14));
        LinearLayout.LayoutParams savelp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 50));
        savelp.topMargin = Ui.dp(this, 14);
        root.addView(save, savelp);
        save.setOnClickListener(v -> save());

        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams rlp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(this, 10);
        root.addView(row, rlp);

        TextView reread = Ui.tv(this, "重新读取剪贴板", 13, p.ink, false);
        reread.setGravity(Gravity.CENTER);
        reread.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 13));
        LinearLayout.LayoutParams relp = Ui.lp(0, Ui.dp(this, 44), 1);
        row.addView(reread, relp);
        reread.setOnClickListener(v -> {
            userEdited = false;
            setHint("正在重新读取…", p.hint);
            grab(0);
        });

        TextView cancelBtn = Ui.tv(this, "取消", 13, p.sub, false);
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setBackground(Ui.stroke(this, p.soft, p.softLine, 1, 13));
        LinearLayout.LayoutParams calp = Ui.lp(0, Ui.dp(this, 44), 1);
        calp.leftMargin = Ui.dp(this, 10);
        row.addView(cancelBtn, calp);
        cancelBtn.setOnClickListener(v -> cancel());

        sheet = Ui.sheet(this, root);
        sheet.setOnDismissListener(d -> {
            if (!saved) finishCapture();
        });
        sheet.show();
    }

    /** 没有待办在捕获时，先选一条 */
    private void chooseTarget() {
        final List<Todo> all = TodoStore.getTodos(this);
        if (all.isEmpty()) {
            toast("还没有待办，先去 RedDot 里加一条");
            finish();
            return;
        }
        // 今天的排前面
        String today = TodoStore.todayKey();
        final List<Todo> ordered = new ArrayList<>();
        for (Todo t : all) if (today.equals(t.date)) ordered.add(t);
        for (Todo t : all) if (!today.equals(t.date)) ordered.add(t);

        String[] labels = new String[ordered.size()];
        for (int i = 0; i < ordered.size(); i++) {
            Todo t = ordered.get(i);
            labels[i] = t.path() + (t.hasTarget() ? "　· 已有目标" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle("把链接存到哪条待办？")
                .setItems(labels, (d, which) -> {
                    target = ordered.get(which);
                    pendingId = target.id;
                    TodoStore.setPending(this, pendingId);
                    buildSheet();
                    handler.postDelayed(() -> grab(0), 150);
                })
                .setNegativeButton("取消", (d, w) -> finish())
                .setOnCancelListener(d -> finish())
                .show();
    }

    // ---------------------------------------------------------------- 保存

    private void save() {
        String text = etUrl == null ? "" : etUrl.getText().toString().trim();
        String url = TargetUtil.extractUrl(text);
        if (url == null) {
            setHint("这里还不是一个完整链接，要 http:// 或 https:// 开头。", p.danger);
            toast("没识别出链接");
            return;
        }
        TodoStore.setTargetUrl(this, pendingId, url, TargetUtil.appNameFromUrl(url));
        TodoStore.clearPending(this);
        String pkg = TargetUtil.resolvePackage(this, url);
        if (pkg != null) Recents.touch(this, pkg);
        saved = true;
        toast("已保存跳转目标");
        backToNormal();
        if (sheet != null) sheet.dismiss();
        finish();
    }

    private void cancel() {
        if (sheet != null) sheet.dismiss();
        finishCapture();
        finish();
    }

    private void finishCapture() {
        TodoStore.clearPending(this);
        backToNormal();
    }

    private void backToNormal() {
        try {
            Intent i = new Intent(this, OverlayService.class)
                    .putExtra(OverlayService.CMD, OverlayService.CMD_NORMAL);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
