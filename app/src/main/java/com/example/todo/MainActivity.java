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

import static com.example.todo.Theme.*;

public class MainActivity extends Activity {

    private Ui ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new Ui(this);
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
        nav.setElevation(ui.dp(8));
        String[] icons = {"🏠", "📅", "✅", "📊"};
        String[] labels = {"الرئيسية", "الجدول اليومي", "المهام", "الإحصائيات"};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            boolean active = i == 1;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.addView(ui.text(icons[i], 20, active ? CORAL : TEXT2, false));
            TextView lb = ui.text(labels[i], 12, active ? CORAL : TEXT2, active);
            lb.setGravity(Gravity.CENTER);
            item.addView(lb);
            View bar = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setColor(active ? CORAL : Color.TRANSPARENT);
            g.setCornerRadius(ui.dp(2));
            bar.setBackground(g);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ui.dp(40), ui.dp(3));
            barLp.topMargin = ui.dp(4);
            item.addView(bar, barLp);
            item.setOnClickListener(v -> {
                if (index != 1) Toast.makeText(this, "قريباً", Toast.LENGTH_SHORT).show();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, ui.dp(64), 1f));
        }
        return nav;
    }
}
