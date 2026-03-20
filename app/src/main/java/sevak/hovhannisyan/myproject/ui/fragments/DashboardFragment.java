package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import dagger.hilt.android.AndroidEntryPoint;

import java.text.NumberFormat;
import java.util.Locale;

import javax.inject.Inject;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.StockResponse;
import sevak.hovhannisyan.myproject.ui.GoalManager;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

/**
 * Dashboard fragment displaying financial overview and market data.
 */
@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    @Inject
    GoalManager goalManager;

    private MainViewModel viewModel;
    private TextView tvBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private TextView tvGoalDaily;
    private TextView tvGoalProgress;
    private LinearProgressIndicator progressGoal;
    private EditText etGoalAmount;
    private MaterialButton btnSetGoal;
    private MaterialButton btnSettings;
    
    private MaterialCardView cardMarket;
    private LinearLayout layoutMarketItems;
    private ProgressBar pbMarket;

    private NumberFormat currencyFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
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
            tvGoalDaily = view.findViewById(R.id.tv_goal_daily);
            tvGoalProgress = view.findViewById(R.id.tv_goal_progress);
            progressGoal = view.findViewById(R.id.progress_goal);
            etGoalAmount = view.findViewById(R.id.et_goal_amount);
            btnSetGoal = view.findViewById(R.id.btn_set_goal);
            btnSettings = view.findViewById(R.id.btn_settings);
            
            cardMarket = view.findViewById(R.id.card_market);
            layoutMarketItems = view.findViewById(R.id.layout_market_items);
            pbMarket = view.findViewById(R.id.pb_market);

            setupGoalCard();
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
                // Show only the first 2 items in dashboard as a preview
                int limit = Math.min(quotes.size(), 2);
                for (int i = 0; i < limit; i++) {
                    addMarketItem(quotes.get(i));
                }
            }
        });
    }

    private void addMarketItem(StockResponse.GlobalQuote quote) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_market, layoutMarketItems, false);
        TextView tvSymbol = itemView.findViewById(R.id.tv_symbol);
        TextView tvPrice = itemView.findViewById(R.id.tv_price);
        TextView tvChange = itemView.findViewById(R.id.tv_change);

        tvSymbol.setText(quote.getSymbol());
        tvPrice.setText("$" + quote.getPrice());
        
        String change = quote.getChangePercent();
        tvChange.setText(change);
        
        if (change != null) {
            if (change.startsWith("-")) {
                tvChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
            } else {
                tvChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
            }
        }
        
        layoutMarketItems.addView(itemView);
    }

    private void setupGoalCard() {
        if (goalManager == null || etGoalAmount == null) return;

        double savedGoal = goalManager.getGoalAmount();
        if (savedGoal > 0) {
            etGoalAmount.setText(String.valueOf(savedGoal));
            updateDailyRequired(savedGoal);
        } else {
            if (tvGoalDaily != null) tvGoalDaily.setText("");
            if (tvGoalProgress != null) tvGoalProgress.setText("");
            if (progressGoal != null) progressGoal.setProgress(0);
        }

        if (btnSetGoal != null) {
            btnSetGoal.setOnClickListener(v -> {
                String text = etGoalAmount.getText().toString().trim();
                if (text.isEmpty()) {
                    etGoalAmount.setError(getString(R.string.goal_hint));
                    return;
                }
                try {
                    double goal = Double.parseDouble(text);
                    if (goal <= 0) {
                        etGoalAmount.setError(getString(R.string.goal_hint));
                        return;
                    }
                    goalManager.saveGoalAmount(goal);
                    updateDailyRequired(goal);
                    Double currentBalance = viewModel.getBalance().getValue();
                    updateGoalProgress(currentBalance != null ? currentBalance : 0.0);
                } catch (NumberFormatException e) {
                    etGoalAmount.setError(getString(R.string.goal_hint));
                }
            });
        }
    }

    private void updateDailyRequired(double goal) {
        if (tvGoalDaily == null) return;
        double perDay = goal / 30.0;
        String text = getString(R.string.goal_daily_prefix)
                + currencyFormat.format(perDay)
                + getString(R.string.goal_daily_suffix);
        tvGoalDaily.setText(text);
    }

    private void updateGoalProgress(double balance) {
        if (goalManager == null || tvGoalProgress == null || progressGoal == null) return;
        double goal = goalManager.getGoalAmount();
        if (goal <= 0) {
            tvGoalProgress.setText("");
            progressGoal.setProgress(0);
            return;
        }
        double ratio = balance / goal;
        if (ratio < 0) ratio = 0;
        if (ratio > 1) ratio = 1;
        int percent = (int) Math.round(ratio * 100);
        String text = getString(R.string.goal_progress_prefix)
                + percent + "%"
                + getString(R.string.goal_progress_suffix);
        tvGoalProgress.setText(text);
        progressGoal.setProgress(percent);
    }
}
