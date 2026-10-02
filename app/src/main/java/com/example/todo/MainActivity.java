package com.example.todo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // الألوان
    private static final int BG = Color.rgb(20, 20, 20);
    private static final int CARD = Color.rgb(36, 36, 40);
    private static final int PRIMARY = Color.rgb(98, 70, 234);
    private static final int GREEN = Color.rgb(46, 184, 114);
    private static final int GRAY = Color.rgb(140, 140, 150);

    private static class Task {
        String text;
        boolean done;

        Task(String text) {
            this.text = text;
        }
    }

    private final ArrayList<Task> tasks = new ArrayList<>();
    private LinearLayout listLayout;
    private TextView counter;
    private float density;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE);
        load();
        getWindow().setStatusBarColor(PRIMARY);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // ===== الجذر =====
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ===== 1) الشريط العلوي =====
        final TextView topBar = new TextView(this);
        topBar.setText("ToDo");
        topBar.setTextSize(22);
        topBar.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.setTextColor(Color.WHITE);
        topBar.setGravity(Gravity.CENTER);
        topBar.setBackgroundColor(PRIMARY);
        topBar.setElevation(dp(6));
        topBar.setPadding(0, dp(16), 0, dp(16));
        column.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== 2) قسم "مهامي اليوم" =====
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(24), dp(20), dp(8));

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("مهامي اليوم");
        sectionTitle.setTextSize(20);
        sectionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sectionTitle.setTextColor(Color.WHITE);
        header.addView(sectionTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        counter = new TextView(this);
        counter.setTextSize(14);
        counter.setTextColor(Color.WHITE);
        counter.setBackground(rounded(PRIMARY, 12));
        counter.setPadding(dp(12), dp(4), dp(12), dp(4));
        header.addView(counter);

        column.addView(header);

        // ===== 3) قائمة المهام =====
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(16), dp(8), dp(16), dp(100));
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ===== 4) زر + إضافة مهمة =====
        final Button addBtn = new Button(this);
        addBtn.setText("+ إضافة مهمة");
        addBtn.setAllCaps(false);
        addBtn.setTextSize(17);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setBackground(rounded(PRIMARY, 28));
        addBtn.setElevation(dp(8));
        addBtn.setOnClickListener(v -> showAddDialog());
        final FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56), Gravity.BOTTOM);
        btnLp.setMargins(dp(20), 0, dp(20), dp(20));
        root.addView(addBtn, btnLp);

        // ===== 7) مناسب للهاتف: احترام حواف الشاشة (الشريط العلوي/السفلي) =====
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            topBar.setPadding(0, top + dp(16), 0, dp(16));
            btnLp.bottomMargin = bottom + dp(20);
            addBtn.setLayoutParams(btnLp);
            return insets;
        });

        setContentView(root);
        render();
    }

    // ===== رسم القائمة من جديد =====
    private void render() {
        listLayout.removeAllViews();

        int doneCount = 0;
        for (Task t : tasks) if (t.done) doneCount++;
        counter.setText(doneCount + " / " + tasks.size());

        if (tasks.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد مهام بعد\nاضغط على \"+ إضافة مهمة\" للبدء");
            empty.setTextSize(16);
            empty.setTextColor(GRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(80), 0, 0);
            listLayout.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }

        for (final Task task : tasks) {
            listLayout.addView(buildCard(task));
        }
    }

    // ===== 5) + 6) بطاقة المهمة وحالة الإكمال =====
    private View buildCard(final Task task) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(CARD, 16));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        // دائرة الحالة
        TextView check = new TextView(this);
        check.setGravity(Gravity.CENTER);
        check.setTextSize(15);
        check.setTextColor(Color.WHITE);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        if (task.done) {
            circle.setColor(GREEN);
            circle.setStroke(dp(2), GREEN);
            check.setText("✓");
        } else {
            circle.setColor(Color.TRANSPARENT);
            circle.setStroke(dp(2), GRAY);
        }
        check.setBackground(circle);
        card.addView(check, new LinearLayout.LayoutParams(dp(28), dp(28)));

        // نص المهمة
        TextView label = new TextView(this);
        label.setText(task.text);
        label.setTextSize(17);
        if (task.done) {
            label.setTextColor(GRAY);
            label.setPaintFlags(label.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            label.setTextColor(Color.WHITE);
        }
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelLp.setMargins(dp(14), 0, dp(8), 0);
        card.addView(label, labelLp);

        // زر الحذف
        TextView delete = new TextView(this);
        delete.setText("✕");
        delete.setTextSize(18);
        delete.setTextColor(GRAY);
        delete.setGravity(Gravity.CENTER);
        delete.setOnClickListener(v -> {
            tasks.remove(task);
            save();
            render();
        });
        card.addView(delete, new LinearLayout.LayoutParams(dp(40), dp(40)));

        // الضغط على البطاقة = إكمال/إلغاء الإكمال
        card.setOnClickListener(v -> {
            task.done = !task.done;
            save();
            render();
        });

        return card;
    }

    // ===== نافذة إضافة مهمة =====
    private void showAddDialog() {
        final EditText input = new EditText(this);
        input.setHint("اكتب المهمة هنا");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout box = new FrameLayout(this);
        box.setPadding(dp(20), dp(12), dp(20), 0);
        box.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("مهمة جديدة")
                .setView(box)
                .setPositiveButton("إضافة", (d, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        tasks.add(0, new Task(text));
                        save();
                        render();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    // ===== الحفظ والتحميل =====
    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("text", t.text);
                o.put("done", t.done);
                arr.put(o);
            }
            prefs.edit().putString("tasks", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void load() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("tasks", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Task t = new Task(o.getString("text"));
                t.done = o.optBoolean("done", false);
                tasks.add(t);
            }
        } catch (Exception ignored) {
        }
    }

    // ===== أدوات مساعدة =====
    private int dp(int value) {
        return (int) (value * density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }
}
