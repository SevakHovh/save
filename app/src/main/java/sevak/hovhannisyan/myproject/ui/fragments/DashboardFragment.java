package sevak.hovhannisyan.myproject.ui.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import sevak.hovhannisyan.myproject.GoalActivity;
import sevak.hovhannisyan.myproject.GoalReceiver;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.data.repository.UserRepository;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private static final String LOG_TAG = "DashboardFragment";

    private MainViewModel mainVm;
    
    private TextView balanceTxt, incomeTxt, expenseTxt, goalTxt, dateTxt, timeTxt;
    private LinearProgressIndicator goalBar;
    private EditText goalInput;
    private MaterialButton setGoalBtn;

    private MaterialCardView balanceCard, incomeCard, expenseCard, marketCard, goalCard;
    private LinearLayout marketContainer;
    private ProgressBar marketProgress;

    private NumberFormat moneyFmt;
    private double profileSalary = 0.0;
    private double profileFixedExp = 0.0;
    private long goalEndTime = -1;
    
    private int reminderHour = 20;
    private int reminderMinute = 0;

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        moneyFmt = NumberFormat.getCurrencyInstance(Locale.US);
        
        SharedPreferences p = requireContext().getSharedPreferences("goal_prefs", Context.MODE_PRIVATE);
        reminderHour = p.getInt("reminder_hour", 20);
        reminderMinute = p.getInt("reminder_minute", 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        try {
            balanceTxt = view.findViewById(R.id.tv_balance);
            incomeTxt = view.findViewById(R.id.tv_total_income);
            expenseTxt = view.findViewById(R.id.tv_total_expense);
            goalTxt = view.findViewById(R.id.tv_goal_progress);
            goalBar = view.findViewById(R.id.progress_goal);
            goalInput = view.findViewById(R.id.et_goal_amount);
            setGoalBtn = view.findViewById(R.id.btn_set_goal);
            dateTxt = view.findViewById(R.id.tv_target_date_dashboard);
            timeTxt = view.findViewById(R.id.tv_notification_time);

            balanceCard = view.findViewById(R.id.card_balance);
            incomeCard = view.findViewById(R.id.card_income);
            expenseCard = view.findViewById(R.id.card_expense);
            marketCard = view.findViewById(R.id.card_market);
            goalCard = view.findViewById(R.id.card_goal);
            
            marketContainer = view.findViewById(R.id.layout_market_items);
            marketProgress = view.findViewById(R.id.pb_market);

            timeTxt.setText(String.format(getCurrentLocale(), "Reminder: %02d:%02d", reminderHour, reminderMinute));

            startObserving();
            initClickListeners();

            mainVm.fetchMarketData();
        } catch (Exception e) {
            Log.e(LOG_TAG, "View setup failed: " + e.getMessage());
        }
    }

    private Locale getCurrentLocale() {
        return getResources().getConfiguration().getLocales().get(0);
    }

    private void initClickListeners() {
        if (marketCard != null) {
            marketCard.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_marketFragment));
        }

        if (incomeCard != null) incomeCard.setOnClickListener(v -> popupIncomeDialog());
        if (expenseCard != null) expenseCard.setOnClickListener(v -> popupExpenseDialog());

        if (balanceCard != null) {
            balanceCard.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_balanceStatsFragment));
        }
        
        if (goalCard != null) {
            goalCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), GoalActivity.class)));
        }

        if (dateTxt != null) {
            dateTxt.setOnClickListener(v -> {
                CalendarConstraints c = new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())
                        .build();

                MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.select_end_date)
                        .setCalendarConstraints(c)
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();
                        
                picker.addOnPositiveButtonClickListener(time -> {
                    // Adjust UTC midnight from picker to local midnight to avoid timezone mismatch
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(time);
                    
                    Calendar local = Calendar.getInstance();
                    local.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
                    goalEndTime = local.getTimeInMillis();
                    
                    SimpleDateFormat f = new SimpleDateFormat("MMM dd, yyyy", getCurrentLocale());
                    dateTxt.setText(getString(R.string.target_date_prefix, f.format(new Date(goalEndTime))));
                });
                picker.show(getParentFragmentManager(), "DATE_PICKER");
            });
        }

        if (timeTxt != null) {
            timeTxt.setOnClickListener(v -> {
                TimePickerDialog timePicker = new TimePickerDialog(requireContext(), (view, hour, minute) -> {
                    reminderHour = hour;
                    reminderMinute = minute;
                    timeTxt.setText(String.format(getCurrentLocale(), "Reminder: %02d:%02d", hour, minute));
                    
                    requireContext().getSharedPreferences("goal_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putInt("reminder_hour", hour)
                            .putInt("reminder_minute", minute)
                            .apply();
                }, reminderHour, reminderMinute, true);
                timePicker.show();
            });
        }

        if (setGoalBtn != null) {
            setGoalBtn.setOnClickListener(v -> {
                String val = goalInput.getText().toString().trim();
                if (val.isEmpty()) {
                    goalInput.setError(getString(R.string.required));
                    return;
                }
                if (goalEndTime == -1) {
                    Toast.makeText(getContext(), R.string.select_target_date, Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    double num = Double.parseDouble(val);
                    if (num <= 0) {
                        goalInput.setError(getString(R.string.invalid_amount));
                        return;
                    }
                    mainVm.saveGoal(num, goalEndTime);
                    setupAlarm();
                    Toast.makeText(getContext(), R.string.goal_set_success, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    goalInput.setError(getString(R.string.invalid_amount));
                }
            });
        }
    }

    private void setupAlarm() {
        Context ctx = requireContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, GoalReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, reminderHour);
        cal.set(Calendar.MINUTE, reminderMinute);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (am != null) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    private void popupIncomeDialog() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_income, null);
        TextInputEditText input = v.findViewById(R.id.et_income_amount);
        MaterialButton salaryBtn = v.findViewById(R.id.btn_add_salary);

        salaryBtn.setText(getString(R.string.add_salary_format, moneyFmt.format(profileSalary)));

        AlertDialog d = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_income)
                .setView(v)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String s = input.getText().toString();
                    if (!s.isEmpty()) record(Double.parseDouble(s), "Manual Income", TransactionType.INCOME, "Income");
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        salaryBtn.setOnClickListener(btn -> {
            if (profileSalary > 0) {
                record(profileSalary, "Monthly Salary", TransactionType.INCOME, "Income");
                d.dismiss();
            } else {
                Toast.makeText(requireContext(), "Set salary in Profile first", Toast.LENGTH_SHORT).show();
            }
        });

        d.show();
    }

    private void popupExpenseDialog() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null);
        TextInputEditText amountInput = v.findViewById(R.id.et_expense_amount);
        AutoCompleteTextView catSpinner = v.findViewById(R.id.spinner_category);
        TextInputLayout customCatLayout = v.findViewById(R.id.til_custom_category);
        TextInputEditText customCatInput = v.findViewById(R.id.et_custom_category);
        MaterialButton fixedBtn = v.findViewById(R.id.btn_add_fixed_expenses);

        String[] cats = {
                getString(R.string.cat_food), getString(R.string.cat_transport), getString(R.string.cat_shopping),
                getString(R.string.cat_entertainment), getString(R.string.cat_health), getString(R.string.cat_utilities),
                getString(R.string.cat_other), getString(R.string.cat_custom)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cats);
        catSpinner.setAdapter(adapter);

        catSpinner.setOnItemClickListener((p, view, pos, id) -> {
            String s = (String) p.getItemAtPosition(pos);
            customCatLayout.setVisibility(getString(R.string.cat_custom).equals(s) ? View.VISIBLE : View.GONE);
        });

        fixedBtn.setText(getString(R.string.pay_fixed_format, moneyFmt.format(profileFixedExp)));

        AlertDialog d = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_expense)
                .setView(v)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String amt = amountInput.getText().toString();
                    String cat = catSpinner.getText().toString();
                    if (getString(R.string.cat_custom).equals(cat)) cat = customCatInput.getText().toString().trim();
                    if (cat.isEmpty()) cat = getString(R.string.cat_other);
                    if (!amt.isEmpty()) record(Double.parseDouble(amt), "Manual Expense", TransactionType.EXPENSE, cat);
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        fixedBtn.setOnClickListener(btn -> {
            if (profileFixedExp > 0) {
                record(profileFixedExp, "Monthly Fixed Expenses", TransactionType.EXPENSE, "Utilities");
                d.dismiss();
            } else {
                Toast.makeText(requireContext(), "Set fixed expenses in Profile first", Toast.LENGTH_SHORT).show();
            }
        });

        d.show();
    }

    private void record(double amt, String note, String type, String cat) {
        Transaction t = new Transaction();
        t.setAmount(amt);
        t.setType(type);
        t.setCategory(cat);
        t.setDescription(note);
        t.setDate(new Date());

        mainVm.addNewTransaction(t);
        Toast.makeText(requireContext(), note + " recorded!", Toast.LENGTH_SHORT).show();
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

    private void startObserving() {
        if (mainVm == null) return;

        mainVm.getBalance().observe(getViewLifecycleOwner(), b -> {
            if (balanceTxt != null) {
                double v = b != null ? b : 0.0;
                balanceTxt.setText(moneyFmt.format(v));
                refreshProgress(v);
            }
        });

        mainVm.getTotalIncome().observe(getViewLifecycleOwner(), i -> {
            if (incomeTxt != null) incomeTxt.setText(moneyFmt.format(i != null ? i : 0.0));
        });

        mainVm.getTotalExpense().observe(getViewLifecycleOwner(), e -> {
            if (expenseTxt != null) expenseTxt.setText(moneyFmt.format(e != null ? e : 0.0));
        });

        mainVm.getIsMarketLoading().observe(getViewLifecycleOwner(), loading -> {
            if (marketProgress != null) marketProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
        
        mainVm.getMarketData().observe(getViewLifecycleOwner(), list -> {
            if (marketContainer != null && list != null) {
                marketContainer.removeAllViews();
                int max = Math.min(list.size(), 4);
                for (int i = 0; i < max; i++) addSmallMarketView(list.get(i));
            }
        });

        mainVm.getUserData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                if (data.containsKey(UserRepository.FIELD_GOAL_AMOUNT)) {
                    double g = parseDouble(data.get(UserRepository.FIELD_GOAL_AMOUNT));
                    goalInput.setText(String.valueOf(g));
                    Double current = mainVm.getBalance().getValue();
                    refreshProgress(current != null ? current : 0.0);
                }
                if (data.containsKey(UserRepository.FIELD_GOAL_END_TIME)) {
                    goalEndTime = parseLong(data.get(UserRepository.FIELD_GOAL_END_TIME));
                    if (goalEndTime > 0) {
                        SimpleDateFormat f = new SimpleDateFormat("MMM dd, yyyy", getCurrentLocale());
                        if (dateTxt != null) dateTxt.setText(getString(R.string.target_date_prefix, f.format(new Date(goalEndTime))));
                    }
                }
                if (data.containsKey(UserRepository.FIELD_SALARY)) profileSalary = parseDouble(data.get(UserRepository.FIELD_SALARY));
                if (data.containsKey(UserRepository.FIELD_FIXED_EXPENSES)) profileFixedExp = parseDouble(data.get(UserRepository.FIELD_FIXED_EXPENSES));
            }
        });
    }

    private void addSmallMarketView(FinnhubResponse r) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_market, marketContainer, false);
        TextView symbol = v.findViewById(R.id.tv_symbol);
        TextView price = v.findViewById(R.id.tv_price);
        TextView change = v.findViewById(R.id.tv_change);

        symbol.setText(r.getSymbol());
        price.setText(String.format(getCurrentLocale(), "$%.2f", r.getCurrentPrice()));
        
        double pc = r.getPercentChange();
        change.setText(String.format(getCurrentLocale(), "%.2f%%", pc));
        change.setTextColor(ContextCompat.getColor(requireContext(), pc < 0 ? R.color.expense_red : R.color.income_green));

        View grid = v.findViewById(R.id.gridLayout);
        if (grid != null) grid.setVisibility(View.GONE);
        View line = v.findViewById(R.id.market_divider);
        if (line != null) line.setVisibility(View.GONE);
        
        marketContainer.addView(v);
    }

    private void refreshProgress(double balance) {
        Map<String, Object> data = mainVm.getUserData().getValue();
        if (data == null || !data.containsKey(UserRepository.FIELD_GOAL_AMOUNT) || !data.containsKey(UserRepository.FIELD_GOAL_START_BALANCE)) return;
        
        try {
            double goal = parseDouble(data.get(UserRepository.FIELD_GOAL_AMOUNT));
            double start = parseDouble(data.get(UserRepository.FIELD_GOAL_START_BALANCE));
            
            if (goal > 0) {
                double saved = balance - start;
                int p = (int) ((saved / goal) * 100);
                if (goalBar != null) goalBar.setProgress(Math.max(0, Math.min(p, 100)));
                if (goalTxt != null) goalTxt.setText(getString(R.string.goal_achieved_prefix, Math.max(0, p)));
            }
        } catch (Exception ignored) {}
    }
}
