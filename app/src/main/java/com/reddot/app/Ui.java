package com.reddot.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 一些建界面的小工具，避免到处写重复代码 */
public class Ui {

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable solid(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable stroke(Context c, int fill, int strokeColor, int strokeDp, float radiusDp) {
        GradientDrawable g = solid(c, fill, radiusDp);
        g.setStroke(dp(c, strokeDp), strokeColor);
        return g;
    }

    public static GradientDrawable oval(Context c, int fill, int strokeColor, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        if (strokeDp > 0) g.setStroke(dp(c, strokeDp), strokeColor);
        return g;
    }

    public static GradientDrawable grad(Context c, int c1, int c2, float radiusDp) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{c1, c2});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable gradCornerTop(Context c, int c1, int c2, float radiusDp) {
        GradientDrawable g = grad(c, c1, c2, radiusDp);
        g.setCornerRadii(new float[]{
                dp(c, radiusDp), dp(c, radiusDp), dp(c, radiusDp), dp(c, radiusDp), 0, 0, 0, 0});
        return g;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView tv(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    public static void margin(View v, Context c, float l, float t, float r, float b) {
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) raw;
            p.leftMargin = dp(c, l);
            p.topMargin = dp(c, t);
            p.rightMargin = dp(c, r);
            p.bottomMargin = dp(c, b);
            v.setLayoutParams(p);
        }
    }

    public static View spacer(Context c, float h) {
        View v = new View(c);
        v.setLayoutParams(lp(1, dp(c, h)));
        return v;
    }

    public static View line(Context c, int color) {
        View v = new View(c);
        v.setBackgroundColor(color);
        v.setLayoutParams(lp(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    public static void pad(View v, Context c, float l, float t, float r, float b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    /** 底部弹出的圆角面板，背景透明、带遮罩，内容自己塞进去 */
    public static Dialog sheet(Context c, View content) {
        Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(content);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.6f);
            w.getDecorView().setPadding(dp(c, 14), 0, dp(c, 14), dp(c, 14));
        }
        d.setCanceledOnTouchOutside(true);
        return d;
    }

    /** 深色输入框，长按可以粘贴 */
    public static EditText editText(Context c, Palette p, String value, String hint, int lines) {
        EditText et = new EditText(c);
        et.setText(value == null ? "" : value);
        et.setHint(hint);
        et.setTextSize(13);
        et.setTextColor(p.ink);
        et.setHintTextColor(p.hint);
        et.setBackground(stroke(c, p.input, p.inputStroke, 1, 13));
        et.setPadding(dp(c, 13), dp(c, 11), dp(c, 13), dp(c, 11));
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setSingleLine(lines <= 1);
        if (lines > 1) {
            et.setMaxLines(lines);
            et.setVerticalScrollBarEnabled(true);
        }
        et.setSelectAllOnFocus(false);
        return et;
    }
}
