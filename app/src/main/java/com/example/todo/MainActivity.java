package com.example.todo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // ===== الألوان =====
    static final int BG = Color.rgb(10, 14, 26);
    static final int CARD = Color.rgb(24, 29, 46);
    static final int CARD_BLUE = Color.rgb(22, 32, 58);
    static final int CORAL = Color.rgb(255, 92, 107);
    static final int BLUE = Color.rgb(90, 120, 200);
    static final int TEXT2 = Color.rgb(150, 160, 185);
    static final int GREEN = Color.rgb(70, 210, 120);
    static final int ORANGE = Color.rgb(255, 170, 60);

    // ===== بيانات ثابتة =====
    static final String[] DAYS = {"الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
    static final String[] MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
    static final String[] CATS = {"مهمة أساسية", "دراسة / تعلم", "عمل", "تطوير الذات", "صحة ولياقة"};
    static final String[] ICONS = {"☀️", "📋", "💻", "📖", "🏋️"};
    static final int[] CAT_BG = {Color.rgb(110, 30, 50), Color.rgb(80, 50, 150), Color.rgb(35, 50, 140),
            Color.rgb(20, 95, 85), Color.rgb(20, 105, 95)};
    static final int[] CAT_FG = {Color.rgb(255, 130, 150), Color.rgb(205, 175, 255), Color.rgb(160, 185, 255),
            Color.rgb(110, 230, 200), Color.rgb(110, 230, 200)};
    static final String[] PRI = {"منخفضة", "متوسطة", "عالية"};
    static final String[] PRI_SYM = {"↓ ", "– ", "↑ "};
    static final int[] PRI_COL = {GREEN, ORANGE, CORAL};

    static class Task {
        String date, title;
        int cat, pri, start, end; // الوقت بالدقائق من منتصف الليل
    }

    private final ArrayList<Task> all = new ArrayList<>();
    private final Calendar[] week = new Calendar[7];
    private int sel;
    private float density;
    private SharedPreferences prefs;

    private TextView dateText;
    private LinearLayout stripLayout, listLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        load();

        // أسبوع اليوم (من الأحد إلى السبت)
        Calendar today = Calendar.getInstance();
        sel = today.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) today.clone();
            c.add(Calendar.DAY_OF_YEAR, i - sel);
            week[i] = c;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        // ===== الرأس: العنوان والتاريخ =====
        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(8));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("تفاصيل الجدول اليومي", 24, Color.WHITE, true);
        dateText = text("", 15, TEXT2, false);
        titles.addView(title);
        titles.addView(dateText);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView todayBtn = text("📅", 20, Color.WHITE, false);
        todayBtn.setGravity(Gravity.CENTER);
        todayBtn.setBackground(rounded(Color.argb(60, 255, 92, 107), 14));
        todayBtn.setOnClickListener(v -> {
            sel = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
            render();
        });
        header.addView(todayBtn, new LinearLayout.LayoutParams(dp(44), dp(44)));
        column.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // ===== شريط الأيام =====
        stripLayout = new LinearLayout(this);
        stripLayout.setOrientation(LinearLayout.HORIZONTAL);
        stripLayout.setPadding(dp(10), dp(8), dp(10), dp(8));
        column.addView(stripLayout, new LinearLayout.LayoutParams(-1, -2));

        // ===== المحتوى القابل للتمرير =====
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(14), 0, dp(14), dp(110));
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listLayout, new ViewGroup.LayoutParams(-1, -2));
        column.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // ===== الشريط السفلي =====
        final LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.rgb(15, 20, 36));
        nav.setElevation(dp(8));
        String[] navIcons = {"🏠", "📅", "✅", "📊"};
        String[] navLabels = {"الرئيسية", "الجدول اليومي", "المهام", "الإحصائيات"};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            boolean active = i == 1;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.addView(text(navIcons[i], 20, active ? CORAL : TEXT2, false));
            TextView lb = text(navLabels[i], 12, active ? CORAL : TEXT2, active);
            lb.setGravity(Gravity.CENTER);
            item.addView(lb);
            View bar = new View(this);
            bar.setBackground(rounded(active ? CORAL : Color.TRANSPARENT, 2));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(40), dp(3));
            barLp.topMargin = dp(4);
            item.addView(bar, barLp);
            item.setOnClickListener(v -> {
                if (index != 1) Toast.makeText(this, "قريباً", Toast.LENGTH_SHORT).show();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(64), 1f));
        }
        column.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        // ===== زر إضافة مهمة =====
        LinearLayout fabBox = new LinearLayout(this);
        fabBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        fabBox.setOrientation(LinearLayout.HORIZONTAL);
        fabBox.setGravity(Gravity.CENTER_VERTICAL);
        TextView fab = text("+", 32, Color.WHITE, false);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(CORAL);
        fab.setBackground(fabBg);
        fab.setElevation(dp(10));
        fabBox.addView(fab, new LinearLayout.LayoutParams(dp(58), dp(58)));
        TextView fabLabel = text("إضافة مهمة", 15, Color.WHITE, true);
        fabLabel.setPadding(dp(12), 0, 0, 0);
        fabBox.addView(fabLabel);
        fabBox.setOnClickListener(v -> showAddDialog());
        final FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.LEFT);
        fabLp.leftMargin = dp(16);
        fabLp.bottomMargin = dp(80);
        root.addView(fabBox, fabLp);

        // ===== احترام حواف الشاشة =====
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            header.setPadding(dp(20), top + dp(16), dp(20), dp(8));
            nav.setPadding(0, 0, 0, bottom);
            fabLp.bottomMargin = dp(80) + bottom;
            fabBox.setLayoutParams(fabLp);
            return insets;
        });

        setContentView(root);
        render();
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
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, dp(10), 0, dp(10));
            GradientDrawable bg = rounded(active ? CORAL : CARD, 14);
            if (!active) bg.setStroke(dp(1), Color.rgb(40, 48, 72));
            cell.setBackground(bg);
            if (active) cell.setElevation(dp(8));
            TextView name = text(DAYS[i], 11, active ? Color.WHITE : TEXT2, false);
            name.setGravity(Gravity.CENTER);
            TextView num = text(String.valueOf(week[i].get(Calendar.DAY_OF_MONTH)), 19, Color.WHITE, true);
            num.setGravity(Gravity.CENTER);
            cell.addView(name);
            cell.addView(num);
            cell.setOnClickListener(v -> {
                sel = idx;
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            stripLayout.addView(cell, lp);
        }
    }

    private void renderList() {
        listLayout.removeAllViews();
        List<Task> list = dayTasks();

        // الملخص
        int taskMin = 0, freeMin = 0, cursor = -1;
        for (Task t : list) {
            taskMin += t.end - t.start;
            if (cursor >= 0 && t.start > cursor) freeMin += t.start - cursor;
            cursor = Math.max(cursor, t.end);
        }
        int span = taskMin + freeMin;
        int taskPct = span == 0 ? 0 : Math.round(100f * taskMin / span);
        int freePct = span == 0 ? 0 : 100 - taskPct;

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(10), dp(14), dp(10), dp(14));
        GradientDrawable sbg = rounded(CARD, 16);
        sbg.setStroke(dp(1), Color.rgb(40, 48, 72));
        summary.setBackground(sbg);
        summary.addView(summaryCol("⏱", "إجمالي وقت المهام", hm(taskMin) + " س", "(" + taskPct + "%)", CORAL),
                new LinearLayout.LayoutParams(0, -2, 1f));
        summary.addView(divider());
        summary.addView(summaryCol("📋", "عدد المهام", String.valueOf(list.size()), "مهام", CORAL),
                new LinearLayout.LayoutParams(0, -2, 1f));
        summary.addView(divider());
        summary.addView(summaryCol("🕐", "إجمالي وقت الفراغ", hm(freeMin) + " س", "(" + freePct + "%)", BLUE),
                new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.setMargins(0, dp(6), 0, dp(16));
        listLayout.addView(summary, slp);

        if (list.isEmpty()) {
            TextView empty = text("لا توجد مهام في هذا اليوم\nاضغط + لإضافة مهمة", 16, TEXT2, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(60), 0, 0);
            listLayout.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        // الخط الزمني
        cursor = -1;
        for (Task t : list) {
            if (cursor >= 0 && t.start > cursor) listLayout.addView(freeRow(cursor, t.start));
            listLayout.addView(taskRow(t));
            cursor = Math.max(cursor, t.end);
        }
    }

    private View summaryCol(String emoji, String label, String value, String sub, int color) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);

        TextView ic = text(emoji, 15, Color.WHITE, false);
        ic.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Color.argb(50, Color.red(color), Color.green(color), Color.blue(color)));
        g.setStroke(dp(1), color);
        ic.setBackground(g);
        c.addView(ic, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(8), 0, dp(4), 0);
        t.addView(text(label, 10, TEXT2, false));
        t.addView(text(value, 15, Color.WHITE, true));
        t.addView(text(sub, 11, BLUE, false));
        c.addView(t);
        return c;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(40, 48, 72));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(1), dp(44)));
        return v;
    }

    // ===== صف مهمة =====
    private View taskRow(final Task t) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.addView(timeCol(t.start, t.end), new LinearLayout.LayoutParams(dp(50), -1));

        // العقدة على الخط الزمني
        FrameLayout node = new FrameLayout(this);
        View line = new View(this);
        line.setBackgroundColor(CORAL);
        node.addView(line, new FrameLayout.LayoutParams(dp(2), -1, Gravity.CENTER_HORIZONTAL));
        TextView circle = text(ICONS[t.cat], 17, Color.WHITE, false);
        circle.setGravity(Gravity.CENTER);
        GradientDrawable cg = new GradientDrawable();
        cg.setShape(GradientDrawable.OVAL);
        cg.setColor(CORAL);
        circle.setBackground(cg);
        node.addView(circle, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));
        row.addView(node, new LinearLayout.LayoutParams(dp(44), -1));

        // البطاقة (LTR لتطابق التصميم)
        LinearLayout card = new LinearLayout(this);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(0, dp(12), dp(12), dp(12));
        GradientDrawable cbg = rounded(CARD, 16);
        cbg.setStroke(dp(1), Color.rgb(45, 52, 78));
        card.setBackground(cbg);

        View accent = new View(this);
        accent.setBackground(rounded(CORAL, 3));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(4), -1);
        alp.setMargins(dp(2), dp(2), dp(10), dp(2));
        card.addView(accent, alp);

        TextView icon = text(ICONS[t.cat], 24, Color.WHITE, false);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable ig = new GradientDrawable();
        ig.setShape(GradientDrawable.OVAL);
        ig.setColor(Color.argb(90, 255, 92, 107));
        icon.setBackground(ig);
        card.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(6), 0);
        info.addView(text(t.title, 17, Color.WHITE, true));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        TextView chip = text(CATS[t.cat], 11, CAT_FG[t.cat], false);
        chip.setBackground(rounded(CAT_BG[t.cat], 10));
        chip.setPadding(dp(10), dp(2), dp(10), dp(2));
        chips.addView(chip);
        TextView pri = text(PRI_SYM[t.pri] + PRI[t.pri], 12, PRI_COL[t.pri], false);
        pri.setPadding(dp(10), 0, 0, 0);
        chips.addView(pri);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.topMargin = dp(4);
        info.addView(chips, clp);

        info.addView(text("🕐 " + clock(t.start) + " - " + clock(t.end), 13, Color.rgb(190, 200, 225), false));
        info.addView(text("مدة المهمة: " + dur(t.end - t.start), 12, TEXT2, false));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        card.addView(text("›", 22, CORAL, true));
        card.setOnClickListener(v -> confirmDelete(t));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, -2, 1f);
        cardLp.bottomMargin = dp(10);
        row.addView(card, cardLp);
        return row;
    }

    // ===== صف وقت فراغ =====
    private View freeRow(int from, int to) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(timeCol(from, to), new LinearLayout.LayoutParams(dp(50), -1));

        FrameLayout node = new FrameLayout(this);
        View line = new View(this);
        line.setBackgroundColor(BLUE);
        node.addView(line, new FrameLayout.LayoutParams(dp(2), -1, Gravity.CENTER_HORIZONTAL));
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(dp(10), dp(10), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        top.topMargin = dp(8);
        FrameLayout.LayoutParams bot = new FrameLayout.LayoutParams(dp(10), dp(10), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        bot.bottomMargin = dp(18);
        node.addView(dot(), top);
        node.addView(dot(), bot);
        row.addView(node, new LinearLayout.LayoutParams(dp(44), -1));

        LinearLayout card = new LinearLayout(this);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable cbg = rounded(CARD_BLUE, 16);
        cbg.setStroke(dp(1), Color.rgb(45, 60, 100));
        card.setBackground(cbg);

        String emoji = from >= 18 * 60 ? "🌙" : (from >= 12 * 60 && from < 14 * 60 ? "🍽" : "☕");
        String sub = from >= 18 * 60 ? "وقت شخصي" : (from >= 12 * 60 && from < 14 * 60 ? "غداء واستراحة" : "راحة قصيرة");
        TextView icon = text(emoji, 20, Color.WHITE, false);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable ig = new GradientDrawable();
        ig.setShape(GradientDrawable.OVAL);
        ig.setColor(Color.rgb(28, 40, 76));
        ig.setStroke(dp(1), BLUE);
        icon.setBackground(ig);
        card.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(6), 0);
        info.addView(text("وقت فراغ", 16, Color.WHITE, true));
        info.addView(text(sub, 13, Color.rgb(140, 170, 230), false));
        info.addView(text("المدة: " + dur(to - from), 12, TEXT2, false));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView pill = text("🕐 " + (to - from) / 60 + ":" + String.format(Locale.US, "%02d", (to - from) % 60),
                13, Color.WHITE, false);
        pill.setBackground(rounded(Color.argb(0, 0, 0, 0), 14));
        GradientDrawable pg = rounded(Color.rgb(20, 28, 52), 14);
        pg.setStroke(dp(1), BLUE);
        pill.setBackground(pg);
        pill.setPadding(dp(10), dp(5), dp(10), dp(5));
        card.addView(pill);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, -2, 1f);
        cardLp.bottomMargin = dp(10);
        row.addView(card, cardLp);
        return row;
    }

    private View dot() {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(BLUE);
        v.setBackground(g);
        return v;
    }

    private View timeCol(int start, int end) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView a = text(clock(start), 13, Color.rgb(190, 200, 225), false);
        TextView b = text(clock(end), 13, Color.rgb(190, 200, 225), false);
        b.setPadding(0, 0, 0, dp(10));
        col.addView(a);
        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        col.addView(b);
        return col;
    }

    // ===== نافذة الإضافة =====
    private void showAddDialog() {
        List<Task> today = dayTasks();
        int s = today.isEmpty() ? 8 * 60 : Math.min(today.get(today.size() - 1).end, 23 * 60);
        final int[] st = {s};
        final int[] en = {Math.min(s + 60, 23 * 60 + 59)};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), 0);

        final EditText input = new EditText(this);
        input.setHint("عنوان المهمة");
        input.setSingleLine(true);
        form.addView(input);

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        final TextView startBtn = pickerButton("من: " + clock(st[0]));
        final TextView endBtn = pickerButton("إلى: " + clock(en[0]));
        startBtn.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> {
            st[0] = h * 60 + m;
            startBtn.setText("من: " + clock(st[0]));
        }, st[0] / 60, st[0] % 60, true).show());
        endBtn.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> {
            en[0] = h * 60 + m;
            endBtn.setText("إلى: " + clock(en[0]));
        }, en[0] / 60, en[0] % 60, true).show());
        times.addView(startBtn, new LinearLayout.LayoutParams(0, -2, 1f));
        times.addView(endBtn, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.topMargin = dp(12);
        form.addView(times, tlp);

        final Spinner catSp = new Spinner(this);
        ArrayAdapter<String> ca = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATS);
        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        catSp.setAdapter(ca);
        form.addView(catSp);

        final Spinner priSp = new Spinner(this);
        ArrayAdapter<String> pa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PRI);
        pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priSp.setAdapter(pa);
        priSp.setSelection(1);
        form.addView(priSp);

        ScrollView sv = new ScrollView(this);
        sv.addView(form);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("مهمة جديدة")
                .setView(sv)
                .setPositiveButton("إضافة", null)
                .setNegativeButton("إلغاء", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "اكتب عنوان المهمة", Toast.LENGTH_SHORT).show();
                return;
            }
            if (en[0] <= st[0]) {
                Toast.makeText(this, "وقت النهاية يجب أن يكون بعد البداية", Toast.LENGTH_SHORT).show();
                return;
            }
            Task t = new Task();
            t.date = dateKey(week[sel]);
            t.title = name;
            t.cat = catSp.getSelectedItemPosition();
            t.pri = priSp.getSelectedItemPosition();
            t.start = st[0];
            t.end = en[0];
            all.add(t);
            save();
            render();
            dialog.dismiss();
        });
    }

    private TextView pickerButton(String label) {
        TextView b = text(label, 15, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(CORAL, 12));
        b.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void confirmDelete(final Task t) {
        new AlertDialog.Builder(this)
                .setTitle(t.title)
                .setMessage("حذف هذه المهمة؟")
                .setPositiveButton("حذف", (d, w) -> {
                    all.remove(t);
                    save();
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    // ===== البيانات =====
    private List<Task> dayTasks() {
        String key = dateKey(week[sel]);
        ArrayList<Task> list = new ArrayList<>();
        for (Task t : all) if (t.date.equals(key)) list.add(t);
        Collections.sort(list, (a, b) -> a.start - b.start);
        return list;
    }

    private String dateKey(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Task t : all) {
                JSONObject o = new JSONObject();
                o.put("date", t.date);
                o.put("title", t.title);
                o.put("cat", t.cat);
                o.put("pri", t.pri);
                o.put("start", t.start);
                o.put("end", t.end);
                arr.put(o);
            }
            prefs.edit().putString("schedule", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void load() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("schedule", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Task t = new Task();
                t.date = o.getString("date");
                t.title = o.getString("title");
                t.cat = o.getInt("cat");
                t.pri = o.getInt("pri");
                t.start = o.getInt("start");
                t.end = o.getInt("end");
                all.add(t);
            }
        } catch (Exception ignored) {
        }
    }

    // ===== أدوات مساعدة =====
    private String clock(int m) {
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    private String hm(int m) {
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    private String dur(int m) {
        int h = m / 60, r = m % 60;
        if (h > 0 && r > 0) return h + " ساعة " + r + " دقيقة";
        if (h > 0) return h + " ساعة";
        return r + " دقيقة";
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

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }
}
