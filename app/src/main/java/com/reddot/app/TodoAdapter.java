package com.reddot.app;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class TodoAdapter extends BaseAdapter {

    public interface OnDoneListener {
        void onDone(Todo t);
    }

    private final Context ctx;
    private final List<Todo> data;
    private final OnDoneListener listener;

    public TodoAdapter(Context c, List<Todo> d) {
        this(c, d, null);
    }

    public TodoAdapter(Context c, List<Todo> d, OnDoneListener l) {
        this.ctx = c;
        this.data = d;
        this.listener = l;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int p) {
        return data.get(p);
    }

    @Override
    public long getItemId(int p) {
        return data.get(p).id;
    }

    @Override
    public View getView(int p, View convert, ViewGroup parent) {
        if (convert == null) {
            convert = LayoutInflater.from(ctx).inflate(R.layout.item_todo, parent, false);
        }
        final Todo t = data.get(p);

        TextView l1 = convert.findViewById(R.id.tv_l1);
        TextView l2 = convert.findViewById(R.id.tv_l2);
        TextView target = convert.findViewById(R.id.tv_target);
        TextView done = convert.findViewById(R.id.btn_done);

        l1.setText(t.l1);
        if (t.done) {
            l1.setPaintFlags(l1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            l1.setPaintFlags(l1.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        if (t.l2 == null || t.l2.length() == 0) {
            l2.setVisibility(View.GONE);
        } else {
            l2.setVisibility(View.VISIBLE);
            l2.setText(t.l2);
        }

        target.setText("↗ " + t.targetLabel());
        target.setTextColor(ctx.getResources().getColor(
                t.hasTarget() ? R.color.red_primary : R.color.text_hint));
        target.setBackgroundResource(t.hasTarget() ? R.drawable.bg_chip : 0);

        done.setText(t.done ? "✓" : "○");
        done.setTextColor(ctx.getResources().getColor(
                t.done ? R.color.text_hint : R.color.red_primary));

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TodoStore.toggleDone(ctx, t.id);
                t.done = !t.done;
                if (listener != null) listener.onDone(t);
                notifyDataSetChanged();
            }
        });

        return convert;
    }
}
