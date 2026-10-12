package com.example.todo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import static com.example.todo.Theme.*;

/** شاشة الجدول اليومي: الرأس وشريط الأيام والقائمة */
public class ScheduleScreen extends FrameLayout {

    private final Activity ctx;
    private final Ui ui;
    private final TaskStore store;
    private final TimelineViews timeline;
    private final AddTaskDialog addDialog;

    private final Calendar[] week = new Calendar[7];
    private int sel;

    private LinearLayout header, stripLayout, listLayout;
    private TextView dateText;

    public ScheduleScreen(Activity activity) {
        super(activity);
        ctx = activity;
        ui = new Ui(ctx);
        store = new TaskStore(ctx);
        timeline = new TimelineViews(ctx, ui, this::confirmDelete);
        addDialog = new AddTaskDialog(ctx, ui);
        setBackgroundColor(BG);
        setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // أسبوع اليوم (من الأحد إلى السبت)
        Calendar today = Calendar.getInstance();
        sel = today.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) today.clone();
            c.add(Calendar.DAY_OF_YEAR, i - sel);
            week[i] = c;
        }

        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        addView(column, new FrameLayout.LayoutParams(-1, -1));

        // ===== الرأس: العنوان والتاريخ =====
        header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(ui.dp(20), ui.dp(16), ui.dp(20), ui.dp(8));

        LinearLayout titles = new LinearLayout(ctx);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(ui.text("تفاصيل الجدول اليومي", 24, Color.WHITE, true));
        dateText = ui.text("", 15, TEXT2, false);
        titles.addView(dateText);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView todayBtn = ui.text("📅", 20, Color.WHITE, false);
        todayBtn.setGravity(Gravity.CENTER);
        todayBtn.setBackground(ui.rounded(Color.argb(60, 255, 92, 107), 14));
        todayBtn.setOnClickListener(v -> {
            sel = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
            render();
        });
        header.addView(todayBtn, new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)));
        column.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // ===== شريط الأيام =====
        stripLayout = new LinearLayout(ctx);
        stripLayout.setOrientation(LinearLayout.HORIZONTAL);
        stripLayout.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(8));
        column.addView(stripLayout, new LinearLayout.LayoutParams(-1, -2));

        // ===== المحتوى القابل للتمرير =====
        ScrollView scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        scroll.setPadding(ui.dp(14), 0, ui.dp(14), ui.dp(100));
        listLayout = new LinearLayout(ctx);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listLayout, new ViewGroup.LayoutParams(-1, -2));
        column.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // ===== زر إضافة مهمة =====
        LinearLayout fabBox = new LinearLayout(ctx);
        fabBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        fabBox.setOrientation(LinearLayout.HORIZONTAL);
        fabBox.setGravity(Gravity.CENTER_VERTICAL);
        TextView fab = ui.text("+", 32, Color.WHITE, false);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(CORAL);
        fab.setBackground(fabBg);
        fab.setElevation(ui.dp(10));
        fabBox.addView(fab, new LinearLayout.LayoutParams(ui.dp(58), ui.dp(58)));
        TextView fabLabel = ui.text("إضافة مهمة", 15, Color.WHITE, true);
        fabLabel.setPadding(ui.dp(12), 0, 0, 0);
        fabBox.addView(fabLabel);
        fabBox.setOnClickListener(v -> addDialog.show(dayTasks(), TaskStore.dateKey(week[sel]), t -> {
            store.add(t);
            render();
        }));
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.LEFT);
        fabLp.leftMargin = ui.dp(16);
        fabLp.bottomMargin = ui.dp(16);
        addView(fabBox, fabLp);

        render();
    }

    /** تستدعيها MainActivity لإبعاد الرأس عن شريط الحالة */
    public void setTopInset(int topPx) {
        header.setPadding(ui.dp(20), topPx + ui.dp(16), ui.dp(20), ui.dp(8));
    }

    // ===== الرسم =====
    private void render() {
        Calendar d = week[sel];
        dateText.setText("📅 " + DAYS[sel] + " " + d.get(Calendar.DAY_OF_MONTH) + " "
                + MONTHS[d.get(Calendar.MONTH)] + " " + d.get(Calendar.YEAR));
        renderStrip();
        renderList();
    }

    private void renderStrip() {
        stripLayout.removeAllViews();
        Calendar now = Calendar.getInstance();
        // الإضافة من السبت إلى الأحد لأن الاتجاه من اليمين لليسار
        for (int i = 6; i >= 0; i--) {
            final int idx = i;
            boolean active = i == sel;
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, ui.dp(10), 0, ui.dp(10));
            GradientDrawable bg = ui.rounded(active ? CORAL : CARD, 14);
            if (!active) bg.setStroke(ui.dp(1), Color.rgb(40, 48, 72));
            cell.setBackground(bg);
            if (active) cell.setElevation(ui.dp(8));
            TextView name = ui.text(DAYS[i], 11, active ? Color.WHITE : TEXT2, false);
            name.setGravity(Gravity.CENTER);
            TextView num = ui.text(String.valueOf(week[i].get(Calendar.DAY_OF_MONTH)), 19, Color.WHITE, true);
            num.setGravity(Gravity.CENTER);
            cell.addView(name);
            cell.addView(num);
            cell.setOnClickListener(v -> {
                sel = idx;
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(ui.dp(3), 0, ui.dp(3), 0);
            stripLayout.addView(cell, lp);
        }
    }

    private void renderList() {
        listLayout.removeAllViews();
        List<Task> list = dayTasks();

        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.setMargins(0, ui.dp(6), 0, ui.dp(16));
        listLayout.addView(timeline.summary(list), slp);

        if (list.isEmpty()) {
            TextView empty = ui.text("لا توجد مهام في هذا اليوم\nاضغط + لإضافة مهمة", 16, TEXT2, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, ui.dp(60), 0, 0);
            listLayout.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        int cursor = -1;
        for (Task t : list) {
            if (cursor >= 0 && t.start > cursor) listLayout.addView(timeline.freeRow(cursor, t.start));
            listLayout.addView(timeline.taskRow(t));
            cursor = Math.max(cursor, t.end);
        }
    }

    private void confirmDelete(final Task t) {
        new AlertDialog.Builder(ctx)
                .setTitle(t.title)
                .setMessage("حذف هذه المهمة؟")
                .setPositiveButton("حذف", (d, w) -> {
                    store.remove(t);
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private List<Task> dayTasks() {
        return store.forDate(TaskStore.dateKey(week[sel]));
    }
}
