package sevak.hovhannisyan.myproject;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class GoalActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private TextView tvGoalTitle, tvCurrentSavings, tvRemainingAmount, tvPercentage, tvDailyReq;
    private CircularProgressIndicator progressGoal;
    private MaterialButton btnUpdateGoal;
    private ImageButton btnBack;
    private CalendarView calendarView;
    private NumberFormat currencyFormat;
    
    private List<Long> excludedDates = new ArrayList<>();

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
        progressGoal = findViewById(R.id.progress_goal_circular);
        btnUpdateGoal = findViewById(R.id.btn_update_goal);
        btnBack = findViewById(R.id.btn_back);
        calendarView = findViewById(R.id.calendar_view);

        // Prevent selecting past dates in the UI
        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        btnBack.setOnClickListener(v -> finish());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long selectedDate = cal.getTimeInMillis();

            // Set today at midnight for comparison
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

        btnUpdateGoal.setOnClickListener(v -> {
            Toast.makeText(this, "Please update goal from Dashboard to reset timeline.", Toast.LENGTH_LONG).show();
        });
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

        if (endTime > System.currentTimeMillis()) {
            long diffInMillis = endTime - System.currentTimeMillis();
            long totalDays = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;
            
            long validDays = totalDays;
            for (Long excluded : excludedDates) {
                if (excluded > System.currentTimeMillis() && excluded < endTime) {
                    validDays--;
                }
            }
            
            if (validDays > 0) {
                double daily = remaining / validDays;
                tvDailyReq.setText("Save " + currencyFormat.format(daily) + " / day");
                tvDailyReq.setVisibility(View.VISIBLE);
            } else {
                tvDailyReq.setText("Timeline tight!");
            }
        } else {
            tvDailyReq.setVisibility(View.GONE);
        }
    }
}
