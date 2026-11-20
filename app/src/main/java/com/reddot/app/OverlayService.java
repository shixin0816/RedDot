package com.reddot.app;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class OverlayService extends Service {

    public static final String CMD = "cmd";
    public static final String CMD_SHOW = "show";
    public static final String CMD_HIDE = "hide";
    public static final String CMD_CAPTURE = "capture";
    public static final String CMD_NORMAL = "normal";

    private WindowManager wm;
    private NotificationManager nm;
    private BubbleView bubble;
    private WindowManager.LayoutParams lp;
    private boolean captureMode = false;
    private String bubbleTheme = "";

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "reddot", "RedDot 桌面圆点", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.setDescription("保持圆点显示，点按通知可恢复被隐藏的圆点");
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());

        SharedPreferences sp = TodoStore.sp(this);
        String cmd = intent == null ? null : intent.getStringExtra(CMD);
        boolean enabled = sp.getBoolean(TodoStore.K_ENABLED, true);

        if (CMD_HIDE.equals(cmd)) {
            sp.edit().putBoolean(TodoStore.K_BUBBLE, false).apply();
            captureMode = false;
            removeBubble();
        } else {
            if (CMD_SHOW.equals(cmd)) {
                sp.edit().putBoolean(TodoStore.K_BUBBLE, true).apply();
            }
            if (CMD_CAPTURE.equals(cmd)) captureMode = true;
            if (CMD_NORMAL.equals(cmd)) captureMode = false;

            boolean want = enabled
                    && Settings.canDrawOverlays(this)
                    && sp.getBoolean(TodoStore.K_BUBBLE, true);
            if (want && bubble != null && !Palette.get(this).id.equals(bubbleTheme)) {
                removeBubble();
            }
            if (want) addBubble();
            else removeBubble();
        }

        if (bubble != null) bubble.setCaptureMode(captureMode);
        nm.notify(1, buildNotification());
        return START_STICKY;
    }

    // ---------------- 圆点 ----------------

    private int windowType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void addBubble() {
        if (bubble != null) return;
        if (!Settings.canDrawOverlays(this)) return;

        float density = getResources().getDisplayMetrics().density;
        int size = (int) (density * 56);

        bubble = new BubbleView(this);
        bubbleTheme = Palette.get(this).id;
        lp = new WindowManager.LayoutParams(
                size, size,
                windowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        lp.x = dm.widthPixels - size - (int) (density * 24);
        lp.y = dm.heightPixels * 2 / 5;

        bubble.setCaptureMode(captureMode);
        bubble.setOnTouchListener(new TouchHandler());
        try {
            wm.addView(bubble, lp);
        } catch (Exception e) {
            bubble = null;
        }
    }

    private void removeBubble() {
        if (bubble != null) {
            try {
                wm.removeView(bubble);
            } catch (Exception ignored) {
            }
            bubble = null;
        }
    }

    private class TouchHandler implements View.OnTouchListener {

        private float downRawX, downRawY;
        private int startX, startY;

        private final GestureDetector detector = new GestureDetector(
                OverlayService.this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                TodoStore.sp(OverlayService.this)
                        .edit().putBoolean(TodoStore.K_BUBBLE, false).apply();
                removeBubble();
                nm.notify(1, buildNotification());
                Toast.makeText(OverlayService.this,
                        "圆点已隐藏，点通知可以恢复", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (captureMode) {
                    startActivity(new Intent(OverlayService.this, CaptureActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    showTodoPopup();
                }
                return true;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            detector.onTouchEvent(ev);
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = lp.x;
                    startY = lp.y;
                    downRawX = ev.getRawX();
                    downRawY = ev.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    lp.x = startX + (int) (ev.getRawX() - downRawX);
                    lp.y = startY + (int) (ev.getRawY() - downRawY);
                    try {
                        wm.updateViewLayout(bubble, lp);
                    } catch (Exception ignored) {
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    // ---------------- 今日待办弹窗 ----------------

    private View buildTitle(String text) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        box.setPadding(pad, pad, pad, pad);

        View dot = new View(this);
        dot.setBackgroundResource(R.drawable.bg_dot);
        int dotSize = (int) (getResources().getDisplayMetrics().density * 9);
        box.addView(dot, new LinearLayout.LayoutParams(dotSize, dotSize));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(19);
        tv.setTextColor(getResources().getColor(R.color.text_main));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.leftMargin = (int) (getResources().getDisplayMetrics().density * 9);
        box.addView(tv, tvLp);

        return box;
    }

    private void showTodoPopup() {
        try {
            TodoSheetDialog.show(this, windowType());
        } catch (Exception e) {
            Toast.makeText(this, "打开待办面板失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    // ---------------- 通知 ----------------

    private Notification buildNotification() {
        boolean visible = TodoStore.sp(this).getBoolean(TodoStore.K_BUBBLE, true);

        Intent i = new Intent(this, MainActivity.class)
                .putExtra("show_bubble", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, piFlags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "reddot")
                : new Notification.Builder(this);

        String text;
        if (!visible) {
            text = "圆点已隐藏 · 点按恢复";
        } else if (captureMode) {
            text = "捕获模式：复制链接后点一下红点";
        } else {
            text = "单击红点看今日待办，双击隐藏";
        }

        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("RedDot 正在运行")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true)
                .setShowWhen(false);

        if (Build.VERSION.SDK_INT >= 26) {
            b.setColor(Palette.get(this).accent);
        }
        return b.build();
    }

    @Override
    public void onDestroy() {
        removeBubble();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
