package sevak.hovhannisyan.myproject.ui.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dagger.hilt.android.AndroidEntryPoint;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import sevak.hovhannisyan.myproject.GoalActivity;
import sevak.hovhannisyan.myproject.GoalReceiver;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private MainViewModel viewModel;
    private TextView tvBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private TextView tvGoalProgress;
    private LinearProgressIndicator progressGoal;
    private EditText etGoalAmount;
    private MaterialButton btnSetGoal;
    private MaterialButton btnSettings;
    private TextView tvTargetDate;

    private MaterialCardView cardBalance;
    private MaterialCardView cardIncome;
    private MaterialCardView cardExpense;
    private MaterialCardView cardMarket;
    private MaterialCardView cardGoal;
    private LinearLayout layoutMarketItems;
    private ProgressBar pbMarket;

    private NumberFormat currencyFormat;
    private double currentProfileSalary = 0.0;
    private double currentProfileFixedExpenses = 0.0;
    private long selectedEndTime = -1;

    private final String[] expenseCategories = {"Food", "Transport", "Shopping", "Entertainment", "Health", "Utilities", "Other", "Custom..."};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            tvBalance = view.findViewById(R.id.tv_balance);
            tvTotalIncome = view.findViewById(R.id.tv_total_income);
            tvTotalExpense = view.findViewById(R.id.tv_total_expense);
            tvGoalProgress = view.findViewById(R.id.tv_goal_progress);
            progressGoal = view.findViewById(R.id.progress_goal);
            etGoalAmount = view.findViewById(R.id.et_goal_amount);
            btnSetGoal = view.findViewById(R.id.btn_set_goal);
            btnSettings = view.findViewById(R.id.btn_settings);
            tvTargetDate = view.findViewById(R.id.tv_target_date_dashboard);

            cardBalance = view.findViewById(R.id.card_balance);
            cardIncome = view.findViewById(R.id.card_income);
            cardExpense = view.findViewById(R.id.card_expense);
            cardMarket = view.findViewById(R.id.card_market);
            cardGoal = view.findViewById(R.id.card_goal);
            
            layoutMarketItems = view.findViewById(R.id.layout_market_items);
            pbMarket = view.findViewById(R.id.pb_market);

            observeViewModel();
            setupButtons();

            viewModel.fetchMarketData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage());
        }
    }

    private void setupButtons() {
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_settingsFragment);
            });
        }

        if (cardMarket != null) {
            cardMarket.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_marketFragment);
            });
        }

        if (cardIncome != null) {
            cardIncome.setOnClickListener(v -> showIncomeMenu());
        }

        if (cardExpense != null) {
            cardExpense.setOnClickListener(v -> showExpenseMenu());
        }

        if (cardBalance != null) {
            cardBalance.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_balanceStatsFragment);
            });
        }
        
        if (cardGoal != null) {
            cardGoal.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), GoalActivity.class);
                startActivity(intent);
            });
        }

        if (tvTargetDate != null) {
            tvTargetDate.setOnClickListener(v -> {
                CalendarConstraints constraints = new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())
                        .build();

                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Target Date")
                        .setCalendarConstraints(constraints)
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();
                        
                datePicker.addOnPositiveButtonClickListener(selection -> {
                    selectedEndTime = selection;
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    tvTargetDate.setText("Target: " + sdf.format(new Date(selection)));
                });
                datePicker.show(getParentFragmentManager(), "DATE_PICKER");
            });
        }

        if (btnSetGoal != null) {
            btnSetGoal.setOnClickListener(v -> {
                String text = etGoalAmount.getText().toString().trim();
                if (text.isEmpty()) {
                    etGoalAmount.setError("Required");
                    return;
                }
                if (selectedEndTime == -1) {
                    Toast.makeText(getContext(), "Please select a target date", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    double goal = Double.parseDouble(text);
                    if (goal <= 0) {
                        etGoalAmount.setError("Invalid amount");
                        return;
                    }
                    viewModel.saveGoalAmount(goal, selectedEndTime);
                    scheduleDailyNotification();
                    Toast.makeText(getContext(), "Goal set with deadline!", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    etGoalAmount.setError("Invalid amount");
                }
            });
        }
    }

    private void scheduleDailyNotification() {
        Context context = requireContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, GoalReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 20); // 8:00 PM
        calendar.set(Calendar.MINUTE, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    private void showIncomeMenu() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_income, null);
        TextInputEditText etIncomeAmount = dialogView.findViewById(R.id.et_income_amount);
        MaterialButton btnAddSalary = dialogView.findViewById(R.id.btn_add_salary);

        btnAddSalary.setText("Add Salary (" + currencyFormat.format(currentProfileSalary) + ")");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Income")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    String amountStr = etIncomeAmount.getText().toString();
                    if (!amountStr.isEmpty()) {
                        addTransaction(Double.parseDouble(amountStr), "Manual Income", TransactionType.INCOME, "Income");
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        btnAddSalary.setOnClickListener(v -> {
            if (currentProfileSalary > 0) {
                addTransaction(currentProfileSalary, "Monthly Salary", TransactionType.INCOME, "Income");
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Set salary in Profile first", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showExpenseMenu() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null);
        TextInputEditText etExpenseAmount = dialogView.findViewById(R.id.et_expense_amount);
        AutoCompleteTextView spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        TextInputLayout tilCustomCategory = dialogView.findViewById(R.id.til_custom_category);
        TextInputEditText etCustomCategory = dialogView.findViewById(R.id.et_custom_category);
        MaterialButton btnAddFixed = dialogView.findViewById(R.id.btn_add_fixed_expenses);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, expenseCategories);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if ("Custom...".equals(selected)) {
                tilCustomCategory.setVisibility(View.VISIBLE);
            } else {
                tilCustomCategory.setVisibility(View.GONE);
            }
        });

        btnAddFixed.setText("Pay Fixed (" + currencyFormat.format(currentProfileFixedExpenses) + ")");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Expense")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    String amountStr = etExpenseAmount.getText().toString();
                    String category = spinnerCategory.getText().toString();
                    
                    if ("Custom...".equals(category)) {
                        category = etCustomCategory.getText().toString().trim();
                    }

                    if (category.isEmpty()) category = "Other";
                    
                    if (!amountStr.isEmpty()) {
                        addTransaction(Double.parseDouble(amountStr), "Manual Expense", TransactionType.EXPENSE, category);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        btnAddFixed.setOnClickListener(v -> {
            if (currentProfileFixedExpenses > 0) {
                addTransaction(currentProfileFixedExpenses, "Monthly Fixed Expenses", TransactionType.EXPENSE, "Utilities");
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Set fixed expenses in Profile first", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addTransaction(double amount, String description, String type, String category) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setDescription(description);
        transaction.setDate(new Date());

        viewModel.insertTransaction(transaction);
        Toast.makeText(requireContext(), description + " recorded!", Toast.LENGTH_SHORT).show();
    }

    private void observeViewModel() {
        if (viewModel == null) return;

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            if (tvBalance != null) {
                double val = balance != null ? balance : 0.0;
                tvBalance.setText(currencyFormat.format(val));
                updateGoalProgress(val);
            }
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            if (tvTotalIncome != null) {
                tvTotalIncome.setText(currencyFormat.format(income != null ? income : 0.0));
            }
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> {
            if (tvTotalExpense != null) {
                tvTotalExpense.setText(currencyFormat.format(expense != null ? expense : 0.0));
            }
        });

        viewModel.getIsMarketLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (pbMarket != null) {
                pbMarket.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
        
        viewModel.getMarketData().observe(getViewLifecycleOwner(), quotes -> {
            if (layoutMarketItems != null && quotes != null) {
                layoutMarketItems.removeAllViews();
                int limit = Math.min(quotes.size(), 4);
                for (int i = 0; i < limit; i++) {
                    addMarketItem(quotes.get(i));
                }
            }
        });

        viewModel.getUserData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                if (data.containsKey("goalAmount")) {
                    Object goalObj = data.get("goalAmount");
                    if (goalObj != null) {
                        double goal = Double.parseDouble(String.valueOf(goalObj));
                        etGoalAmount.setText(String.valueOf(goal));
                        Double currentBalance = viewModel.getBalance().getValue();
                        updateGoalProgress(currentBalance != null ? currentBalance : 0.0);
                    }
                }
                if (data.containsKey("goalEndTime")) {
                    selectedEndTime = Long.parseLong(String.valueOf(data.get("goalEndTime")));
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    if (tvTargetDate != null) tvTargetDate.setText("Target: " + sdf.format(new Date(selectedEndTime)));
                }
                if (data.containsKey("salary")) {
                    Object salaryObj = data.get("salary");
                    if (salaryObj != null) {
                        currentProfileSalary = Double.parseDouble(String.valueOf(salaryObj));
                    }
                }
                if (data.containsKey("fixedExpenses")) {
                    Object expObj = data.get("fixedExpenses");
                    if (expObj != null) {
                        currentProfileFixedExpenses = Double.parseDouble(String.valueOf(expObj));
                    }
                }
            }
        });
    }

    private void addMarketItem(FinnhubResponse quote) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_market, layoutMarketItems, false);
        TextView tvSymbol = itemView.findViewById(R.id.tv_symbol);
        TextView tvPrice = itemView.findViewById(R.id.tv_price);
        TextView tvChange = itemView.findViewById(R.id.tv_change);

        tvSymbol.setText(quote.getSymbol());
        tvPrice.setText(String.format("$%.2f", quote.getCurrentPrice()));
        
        double percentChange = quote.getPercentChange();
        tvChange.setText(String.format("%.2f%%", percentChange));
        
        if (percentChange < 0) {
            tvChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else {
            tvChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        }

        View detailGrid = itemView.findViewById(R.id.gridLayout);
        if (detailGrid != null) detailGrid.setVisibility(View.GONE);
        View divider = itemView.findViewById(R.id.market_divider);
        if (divider != null) divider.setVisibility(View.GONE);
        
        layoutMarketItems.addView(itemView);
    }

    private void updateGoalProgress(double currentBalance) {
        Map<String, Object> data = viewModel.getUserData().getValue();
        if (data == null || !data.containsKey("goalAmount") || !data.containsKey("goalStartBalance")) return;
        
        try {
            double goal = Double.parseDouble(String.valueOf(data.get("goalAmount")));
            double startBalance = Double.parseDouble(String.valueOf(data.get("goalStartBalance")));
            
            if (goal > 0) {
                double netSavings = currentBalance - startBalance;
                int progress = (int) ((netSavings / goal) * 100);
                if (progressGoal != null) progressGoal.setProgress(Math.max(0, Math.min(progress, 100)));
                if (tvGoalProgress != null) tvGoalProgress.setText(Math.max(0, progress) + "% of Goal Achieved (Net Savings)");
            }
        } catch (NumberFormatException ignored) {}
    }
}
