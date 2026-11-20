package com.reddot.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** 桌面上的半透明红点 */
public class BubbleView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean capture = false;
    private Palette p;

    public BubbleView(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        dot.setColor(Color.WHITE);
        p = Palette.get(c);
    }

    public void setCaptureMode(boolean c) {
        capture = c;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - dp(2);

        fill.setShader(new android.graphics.RadialGradient(
                cx - r * 0.3f, cy - r * 0.35f, r * 1.6f,
                new int[]{p.dot1, p.dot2, p.dot3},
                new float[]{0f, 0.55f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        fill.setAlpha(capture ? 238 : 212);
        canvas.drawCircle(cx, cy, r, fill);
        fill.setShader(null);

        ring.setColor(0xAAFFFFFF);
        ring.setStrokeWidth(dp(1.5f));
        canvas.drawCircle(cx, cy, r, ring);

        dot.setColor(0x55FFFFFF);
        canvas.drawCircle(cx, cy, r * 0.38f, dot);
        dot.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, r * 0.24f, dot);

        if (capture) {
            ring.setStrokeWidth(dp(1.4f));
            canvas.drawCircle(cx, cy, r * 0.48f, ring);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
