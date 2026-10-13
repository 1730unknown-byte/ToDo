package com.example.todo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(10, 14, 26);
    private static final int CORAL = Color.rgb(255, 92, 107);
    private static final int TEXT2 = Color.rgb(150, 160, 185);

    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        // الشاشة الحالية: الجدول اليومي (ملف مستقل)
        final ScheduleScreen schedule = new ScheduleScreen(this);
        column.addView(schedule, new LinearLayout.LayoutParams(-1, 0, 1f));

        // الشريط السفلي
        final LinearLayout nav = buildNav();
        column.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        // احترام حواف الشاشة
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            schedule.setTopInset(insets.getSystemWindowInsetTop());
            nav.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        setContentView(root);
    }

    private LinearLayout buildNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.rgb(15, 20, 36));
        nav.setElevation(dp(8));
        String[] icons = {"🏠", "📅", "✅", "📊"};
        String[] labels = {"الرئيسية", "الجدول اليومي", "المهام", "الإحصائيات"};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            boolean active = i == 1;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.addView(text(icons[i], 20, active ? CORAL : TEXT2, false));
            TextView lb = text(labels[i], 12, active ? CORAL : TEXT2, active);
            lb.setGravity(Gravity.CENTER);
            item.addView(lb);
            View bar = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setColor(active ? CORAL : Color.TRANSPARENT);
            g.setCornerRadius(dp(2));
            bar.setBackground(g);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(40), dp(3));
            barLp.topMargin = dp(4);
            item.addView(bar, barLp);
            item.setOnClickListener(v -> {
                if (index != 1) Toast.makeText(this, "قريباً", Toast.LENGTH_SHORT).show();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(64), 1f));
        }
        return nav;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private int dp(int v) {
        return (int) (v * density + 0.5f);
    }
}
