package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import java.text.NumberFormat;
import java.util.Locale;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.ui.ThemeManager;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

/**
 * Dashboard fragment displaying financial overview and recent transactions.
 */
@AndroidEntryPoint
public class DashboardFragment extends Fragment {
    
    private MainViewModel viewModel;
    private TextView tvBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private RecyclerView rvRecentTransactions;
    private MaterialButton btnSettings;
    
    private NumberFormat currencyFormat;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        tvBalance = view.findViewById(R.id.tv_balance);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
        btnSettings = view.findViewById(R.id.btn_settings);
        
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        // TODO: Set adapter for recent transactions

        setupSettingsButton();
        observeViewModel();
    }
    
    private void observeViewModel() {
        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                tvBalance.setText(currencyFormat.format(balance));
            } else {
                tvBalance.setText(currencyFormat.format(0.0));
            }
        });
        
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            if (income != null) {
                tvTotalIncome.setText(currencyFormat.format(income));
            } else {
                tvTotalIncome.setText(currencyFormat.format(0.0));
            }
        });
        
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> {
            if (expense != null) {
                tvTotalExpense.setText(currencyFormat.format(expense));
            } else {
                tvTotalExpense.setText(currencyFormat.format(0.0));
            }
        });
        
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            // TODO: Update RecyclerView adapter with recent transactions
            // For now, just show the first 5 transactions
        });
    }

    private void setupSettingsButton() {
        btnSettings.setOnClickListener(v -> {
            // Simple dialog to choose theme
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.theme_dialog_title)
                    .setItems(new CharSequence[]{
                                    getString(R.string.theme_light),
                                    getString(R.string.theme_dark)
                            },
                            (dialog, which) -> {
                                if (which == 0) {
                                    ThemeManager.setTheme(requireContext(), ThemeManager.MODE_LIGHT);
                                } else if (which == 1) {
                                    ThemeManager.setTheme(requireContext(), ThemeManager.MODE_DARK);
                                }
                                // recreate activity to apply theme instantly
                                requireActivity().recreate();
                            })
                    .show();
        });
    }
}
