package com.reddot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

/** 从小红书 / B站 分享链接到 RedDot 时走这里 */
public class ShareActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        String text = null;
        try {
            text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        } catch (Exception ignored) {
        }

        final String url = TargetUtil.extractUrl(text);
        if (url == null) {
            Toast.makeText(this, "分享内容里没找到链接", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String name = TargetUtil.appNameFromUrl(url);
        long pending = TodoStore.getPending(this);
        final Todo pendingTodo = pending == -1 ? null : TodoStore.get(this, pending);

        if (pendingTodo != null) {
            new AlertDialog.Builder(this)
                    .setTitle("保存跳转目标")
                    .setMessage("来源：" + name + "\n" + url + "\n\n存为「" + pendingTodo.path() + "」的跳转目标？")
                    .setPositiveButton("保存", (d, w) -> saveTo(pendingTodo.id, url, name))
                    .setNegativeButton("取消", (d, w) -> finish())
                    .setOnCancelListener(d -> finish())
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("存成新待办")
                    .setMessage("来源：" + name + "\n" + url + "\n\n会新建一条待办「" + name + "」并把这个链接设为它的跳转目标。")
                    .setPositiveButton("创建", (d, w) -> {
                        Todo t = new Todo();
                        t.id = System.currentTimeMillis();
                        t.l1 = name;
                        t.targetName = name;
                        t.targetUrl = url;
                        TodoStore.save(this, t);
                        Toast.makeText(this, "已保存到 RedDot", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", (d, w) -> finish())
                    .setOnCancelListener(d -> finish())
                    .show();
        }
    }

    private void saveTo(long id, String url, String name) {
        TodoStore.setTargetUrl(this, id, url, name);
        TodoStore.clearPending(this);

        Intent i = new Intent(this, OverlayService.class)
                .putExtra(OverlayService.CMD, OverlayService.CMD_NORMAL);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);

        Toast.makeText(this, "已保存跳转目标", Toast.LENGTH_SHORT).show();
        finish();
    }
}
