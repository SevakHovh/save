package sevak.hovhannisyan.myproject;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class GoalActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private TextView tvGoalTitle, tvCurrentSavings, tvRemainingAmount, tvPercentage, tvDailyReq, tvDailyStatus;
    private CircularProgressIndicator progressGoal;
    private MaterialButton btnUpdateGoal, btnQuickSave, btnCustomSave;
    private ImageButton btnBack;
    private CalendarView calendarView;
    private NumberFormat currencyFormat;
    
    private List<Long> excludedDates = new ArrayList<>();
    private List<Long> completedDates = new ArrayList<>();
    private double currentDailyRequirement = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        initViews();
        observeViewModel();
    }

    private void initViews() {
        tvGoalTitle = findViewById(R.id.tv_goal_title);
        tvCurrentSavings = findViewById(R.id.tv_current_savings);
        tvRemainingAmount = findViewById(R.id.tv_remaining_amount);
        tvPercentage = findViewById(R.id.tv_percentage);
        tvDailyReq = findViewById(R.id.tv_daily_requirement);
        tvDailyStatus = findViewById(R.id.tv_daily_status);
        progressGoal = findViewById(R.id.progress_goal_circular);
        btnUpdateGoal = findViewById(R.id.btn_update_goal);
        btnQuickSave = findViewById(R.id.btn_quick_save);
        btnCustomSave = findViewById(R.id.btn_custom_save);
        btnBack = findViewById(R.id.btn_back);
        calendarView = findViewById(R.id.calendar_view);

        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        btnBack.setOnClickListener(v -> finish());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long selectedDate = cal.getTimeInMillis();

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            if (selectedDate < today.getTimeInMillis()) {
                Toast.makeText(this, "Cannot modify past dates", Toast.LENGTH_SHORT).show();
                return;
            }

            if (excludedDates.contains(selectedDate)) {
                excludedDates.remove(selectedDate);
                Toast.makeText(this, "Day included in plan", Toast.LENGTH_SHORT).show();
            } else {
                excludedDates.add(selectedDate);
                Toast.makeText(this, "Day excluded from plan", Toast.LENGTH_SHORT).show();
            }
            viewModel.updateExcludedDates(excludedDates);
        });

        btnQuickSave.setOnClickListener(v -> {
            if (currentDailyRequirement > 0) {
                addTransaction(currentDailyRequirement, "Daily Savings (Quick Save)");
                viewModel.markDateAsCompleted(getTodayMidnight());
            } else {
                Toast.makeText(this, "No daily requirement set", Toast.LENGTH_SHORT).show();
            }
        });

        btnCustomSave.setOnClickListener(v -> showCustomSaveDialog());

        btnUpdateGoal.setOnClickListener(v -> {
            Toast.makeText(this, "Please update goal from Dashboard to reset timeline.", Toast.LENGTH_LONG).show();
        });
    }

    private void showCustomSaveDialog() {
        TextInputEditText etAmount = new TextInputEditText(this);
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setHint("Enter amount");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Custom Save")
                .setView(etAmount)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = etAmount.getText().toString();
                    if (!val.isEmpty()) {
                        double amount = Double.parseDouble(val);
                        addTransaction(amount, "Daily Savings (Custom)");
                        if (amount >= currentDailyRequirement) {
                            viewModel.markDateAsCompleted(getTodayMidnight());
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addTransaction(double amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(TransactionType.INCOME);
        transaction.setCategory("Savings");
        transaction.setDescription(description);
        transaction.setDate(new Date());
        viewModel.insertTransaction(transaction);
        Toast.makeText(this, "Saved " + currencyFormat.format(amount), Toast.LENGTH_SHORT).show();
    }

    private long getTodayMidnight() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTimeInMillis();
    }

    private void observeViewModel() {
        viewModel.getUserData().observe(this, data -> {
            if (data != null && data.containsKey("goalAmount")) {
                double goal = Double.parseDouble(String.valueOf(data.get("goalAmount")));
                double startBalance = data.containsKey("goalStartBalance") ? Double.parseDouble(String.valueOf(data.get("goalStartBalance"))) : 0.0;
                long endTime = data.containsKey("goalEndTime") ? (long) data.get("goalEndTime") : 0;
                
                if (data.containsKey("excludedDates")) {
                    Object excluded = data.get("excludedDates");
                    if (excluded instanceof List) {
                        excludedDates = (List<Long>) excluded;
                    }
                }
                
                if (data.containsKey("completedDates")) {
                    Object completed = data.get("completedDates");
                    if (completed instanceof List) {
                        completedDates = (List<Long>) completed;
                    }
                }

                tvGoalTitle.setText("Strategic Goal: " + currencyFormat.format(goal));
                updateUI(goal, startBalance, viewModel.getBalance().getValue() != null ? viewModel.getBalance().getValue() : 0.0, endTime);
            }
        });

        viewModel.getBalance().observe(this, balance -> {
            Map<String, Object> data = viewModel.getUserData().getValue();
            if (data != null && data.containsKey("goalAmount")) {
                double goal = Double.parseDouble(String.valueOf(data.get("goalAmount")));
                double startBalance = data.containsKey("goalStartBalance") ? Double.parseDouble(String.valueOf(data.get("goalStartBalance"))) : 0.0;
                long endTime = data.containsKey("goalEndTime") ? (long) data.get("goalEndTime") : 0;
                updateUI(goal, startBalance, balance != null ? balance : 0.0, endTime);
            }
        });
    }

    private void updateUI(double goal, double startBalance, double currentBalance, long endTime) {
        if (goal <= 0) return;

        double netSavings = Math.max(0, currentBalance - startBalance);
        double remaining = Math.max(0, goal - netSavings);
        
        tvCurrentSavings.setText(currencyFormat.format(netSavings) + " saved");
        tvRemainingAmount.setText(currencyFormat.format(remaining) + " to go");

        int progress = (int) ((netSavings / goal) * 100);
        progressGoal.setProgress(Math.min(progress, 100));
        tvPercentage.setText(Math.min(progress, 100) + "%");

        long todayMidnight = getTodayMidnight();
        boolean isCompletedToday = completedDates.contains(todayMidnight);

        if (endTime > System.currentTimeMillis()) {
            long diffInMillis = endTime - todayMidnight;
            long totalDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            if (totalDays <= 0) totalDays = 1;
            
            long validDays = 0;
            for (int i = 0; i < totalDays; i++) {
                Calendar check = Calendar.getInstance();
                check.setTimeInMillis(todayMidnight);
                check.add(Calendar.DAY_OF_YEAR, i);
                long checkTime = check.getTimeInMillis();
                if (!excludedDates.contains(checkTime) && !completedDates.contains(checkTime)) {
                    validDays++;
                }
            }
            
            if (validDays > 0) {
                currentDailyRequirement = remaining / validDays;
                tvDailyReq.setText("Save " + currencyFormat.format(currentDailyRequirement) + " / day");
                tvDailyReq.setVisibility(View.VISIBLE);
            } else {
                currentDailyRequirement = 0;
                tvDailyReq.setText("Target reached or no days left!");
            }
        } else {
            tvDailyReq.setVisibility(View.GONE);
        }

        if (isCompletedToday) {
            tvDailyStatus.setVisibility(View.VISIBLE);
            tvDailyStatus.setText("Today's goal completed!");
            btnQuickSave.setEnabled(false);
        } else {
            tvDailyStatus.setVisibility(View.GONE);
            btnQuickSave.setEnabled(true);
        }
    }
}
