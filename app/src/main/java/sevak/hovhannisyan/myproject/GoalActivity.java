package sevak.hovhannisyan.myproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.data.repository.UserRepository;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class GoalActivity extends AppCompatActivity {

    private static final String TAG = "GoalActivity";
    private MainViewModel mainVm;
    private TextView titleLabel, savingsLabel, remainingLabel, percentLabel, dailyReqLabel, dailyStatusLabel;
    private CircularProgressIndicator goalCircle;
    private MaterialButton updateBtn, quickBtn, customBtn;
    private ImageButton backBtn;
    private CalendarView calView;
    private NumberFormat moneyFmt;
    
    private List<Long> noSaveDates = new ArrayList<>();
    private List<Long> doneDates = new ArrayList<>();
    private double reqPerDay = 0.0;
    private double currentLeft = 0.0;
    private boolean goalExists = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences p = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String l = p.getString("My_Lang", "");
        if (!l.isEmpty()) {
            Locale locale = new Locale(l);
            Locale.setDefault(locale);
            Configuration conf = new Configuration();
            conf.setLocale(locale);
            super.attachBaseContext(newBase.createConfigurationContext(conf));
        } else {
            super.attachBaseContext(newBase);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        mainVm = new ViewModelProvider(this).get(MainViewModel.class);
        moneyFmt = NumberFormat.getCurrencyInstance(Locale.US);

        initUi();
        syncData();
    }

    private void initUi() {
        titleLabel = findViewById(R.id.tv_goal_title);
        savingsLabel = findViewById(R.id.tv_current_savings);
        remainingLabel = findViewById(R.id.tv_remaining_amount);
        percentLabel = findViewById(R.id.tv_percentage);
        dailyReqLabel = findViewById(R.id.tv_daily_requirement);
        dailyStatusLabel = findViewById(R.id.tv_daily_status);
        goalCircle = findViewById(R.id.progress_goal_circular);
        updateBtn = findViewById(R.id.btn_update_goal);
        quickBtn = findViewById(R.id.btn_quick_save);
        customBtn = findViewById(R.id.btn_custom_save);
        backBtn = findViewById(R.id.btn_back);
        calView = findViewById(R.id.calendar_view);

        calView.setMinDate(System.currentTimeMillis() - 1000);

        backBtn.setOnClickListener(v -> finish());

        calView.setOnDateChangeListener((v, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            long time = c.getTimeInMillis();

            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);

            if (time < now.getTimeInMillis()) {
                Toast.makeText(this, "Can't change the past", Toast.LENGTH_SHORT).show();
                return;
            }

            if (noSaveDates.contains(time)) {
                noSaveDates.remove(time);
                Toast.makeText(this, "Day included!", Toast.LENGTH_SHORT).show();
            } else {
                noSaveDates.add(time);
                Toast.makeText(this, "Day skipped!", Toast.LENGTH_SHORT).show();
            }
            mainVm.updateNoSaveDays(noSaveDates);
        });

        quickBtn.setOnClickListener(v -> {
            if (reqPerDay > 0) {
                recordMoney(reqPerDay, "Daily Goal (Quick)");
                mainVm.markDayDone(getMidnight());
            } else {
                if (!goalExists) {
                    Toast.makeText(this, "No goal set yet", Toast.LENGTH_SHORT).show();
                } else if (currentLeft <= 0) {
                    Toast.makeText(this, "Goal already achieved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Today is skipped or no valid days left.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        customBtn.setOnClickListener(v -> {
            TextInputEditText input = new TextInputEditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint(R.string.goal_hint);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.custom_save)
                    .setView(input)
                    .setPositiveButton(R.string.add, (dialog, which) -> {
                        String s = input.getText().toString();
                        if (!s.isEmpty()) {
                            try {
                                double amt = Double.parseDouble(s);
                                recordMoney(amt, "Daily Goal (Custom)");
                                if (amt >= reqPerDay && reqPerDay > 0) mainVm.markDayDone(getMidnight());
                            } catch (Exception e) {
                                Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        updateBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Check Dashboard to reset the goal.", Toast.LENGTH_LONG).show();
        });
    }

    private void recordMoney(double amt, String note) {
        Transaction t = new Transaction();
        t.setAmount(amt);
        t.setType(TransactionType.INCOME);
        t.setCategory("Savings");
        t.setDescription(note);
        t.setDate(new Date());
        mainVm.addNewTransaction(t);
        Toast.makeText(this, "Saved " + moneyFmt.format(amt), Toast.LENGTH_SHORT).show();
    }

    private long getMidnight() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private long parseLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return (long) Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0L;
        }
    }

    private void syncData() {
        mainVm.getUserData().observe(this, data -> {
            if (data != null && data.containsKey(UserRepository.FIELD_GOAL_AMOUNT)) {
                double g = parseDouble(data.get(UserRepository.FIELD_GOAL_AMOUNT));
                Log.d(TAG, "SyncData: goalAmount = " + g);
                if (g > 0) {
                    goalExists = true;
                    double start = parseDouble(data.get(UserRepository.FIELD_GOAL_START_BALANCE));
                    long end = parseLong(data.get(UserRepository.FIELD_GOAL_END_TIME));
                    
                    noSaveDates = safeLongList(data.get(UserRepository.FIELD_EXCLUDED_DATES));
                    doneDates = safeLongList(data.get(UserRepository.FIELD_COMPLETED_DATES));

                    titleLabel.setText(getString(R.string.strategic_goal_prefix, moneyFmt.format(g)));
                    refresh(g, start, mainVm.getBalance().getValue() != null ? mainVm.getBalance().getValue() : 0.0, end);
                } else {
                    goalExists = false;
                    Log.d(TAG, "SyncData: goalAmount is 0 or less");
                }
            } else {
                goalExists = false;
                Log.d(TAG, "SyncData: data is null or doesn't contain goalAmount");
            }
        });

        mainVm.getBalance().observe(this, b -> {
            Map<String, Object> data = mainVm.getUserData().getValue();
            if (data != null && data.containsKey(UserRepository.FIELD_GOAL_AMOUNT)) {
                double g = parseDouble(data.get(UserRepository.FIELD_GOAL_AMOUNT));
                if (g > 0) {
                    double start = parseDouble(data.get(UserRepository.FIELD_GOAL_START_BALANCE));
                    long end = parseLong(data.get(UserRepository.FIELD_GOAL_END_TIME));
                    refresh(g, start, b != null ? b : 0.0, end);
                }
            }
        });
    }

    private List<Long> safeLongList(Object obj) {
        List<Long> list = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                list.add(parseLong(item));
            }
        }
        return list;
    }

    private void refresh(double goal, double start, double current, long end) {
        if (goal <= 0) {
            goalExists = false;
            return;
        }
        goalExists = true;

        double net = Math.max(0, current - start);
        double left = Math.max(0, goal - net);
        currentLeft = left;
        
        savingsLabel.setText(getString(R.string.goal_total_saved_format, moneyFmt.format(net)));
        remainingLabel.setText(getString(R.string.goal_remaining_format, moneyFmt.format(left)));

        int p = (int) ((net / goal) * 100);
        goalCircle.setProgress(Math.min(p, 100));
        percentLabel.setText(Math.min(p, 100) + "%");

        long mid = getMidnight();
        boolean doneToday = doneDates.contains(mid);

        if (end >= mid && left > 0) {
            long diffMs = end - mid;
            long totalDays = TimeUnit.MILLISECONDS.toDays(diffMs) + 1;
            if (totalDays <= 0) totalDays = 1;
            
            long valid = 0;
            for (int i = 0; i < totalDays; i++) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(mid);
                c.add(Calendar.DAY_OF_YEAR, i);
                long t = c.getTimeInMillis();
                if (!noSaveDates.contains(t) && !doneDates.contains(t)) valid++;
            }
            
            if (valid > 0) {
                reqPerDay = left / valid;
                dailyReqLabel.setText(getString(R.string.goal_daily_format, moneyFmt.format(reqPerDay)));
                dailyReqLabel.setVisibility(View.VISIBLE);
            } else {
                reqPerDay = 0;
                dailyReqLabel.setText(getString(R.string.goal_reached));
            }
        } else {
            reqPerDay = 0;
            dailyReqLabel.setVisibility(View.GONE);
        }

        if (doneToday) {
            dailyStatusLabel.setVisibility(View.VISIBLE);
            dailyStatusLabel.setText(getString(R.string.today_goal_completed));
            quickBtn.setEnabled(false);
        } else {
            dailyStatusLabel.setVisibility(View.GONE);
            quickBtn.setEnabled(true);
        }
    }
}
