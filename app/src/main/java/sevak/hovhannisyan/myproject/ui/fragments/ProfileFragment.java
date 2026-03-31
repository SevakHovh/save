package sevak.hovhannisyan.myproject.ui.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private MainViewModel viewModel;
    private TextView tvNoStats;
    private PieChart pieChart;

    private TextInputEditText etSalary;
    private TextInputEditText etFixedExpenses;
    private MaterialButton btnSaveFinancialInfo;
    private MaterialButton btnSettings;
    private MaterialButton btnPersonalInfo;
    private View layoutPersonalInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        tvNoStats = view.findViewById(R.id.tv_no_stats);
        pieChart = view.findViewById(R.id.pie_chart);

        etSalary = view.findViewById(R.id.et_salary);
        etFixedExpenses = view.findViewById(R.id.et_fixed_expenses);
        btnSaveFinancialInfo = view.findViewById(R.id.btn_save_financial_info);
        btnSettings = view.findViewById(R.id.btn_settings_profile);
        btnPersonalInfo = view.findViewById(R.id.btn_personal_info);
        layoutPersonalInfo = view.findViewById(R.id.layout_personal_info);

        observeFinancialInfo();
        setupPieChart();
        observeTransactions();

        btnSaveFinancialInfo.setOnClickListener(v -> saveFinancialInfo());
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.settingsFragment);
            });
        }

        if (btnPersonalInfo != null) {
            btnPersonalInfo.setOnClickListener(v -> {
                if (layoutPersonalInfo.getVisibility() == View.VISIBLE) {
                    layoutPersonalInfo.setVisibility(View.GONE);
                    btnPersonalInfo.setText(R.string.account_details_show);
                } else {
                    layoutPersonalInfo.setVisibility(View.VISIBLE);
                    btnPersonalInfo.setText(R.string.account_details_hide);
                }
            });
        }
    }

    private void observeFinancialInfo() {
        viewModel.getUserData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                Object salary = data.get("salary");
                Object expenses = data.get("fixedExpenses");
                
                if (salary != null) etSalary.setText(String.valueOf(salary));
                if (expenses != null) etFixedExpenses.setText(String.valueOf(expenses));
            }
        });
    }

    private void saveFinancialInfo() {
        String salaryStr = etSalary.getText().toString();
        String expensesStr = etFixedExpenses.getText().toString();

        try {
            double salary = salaryStr.isEmpty() ? 0 : Double.parseDouble(salaryStr);
            double expenses = expensesStr.isEmpty() ? 0 : Double.parseDouble(expensesStr);

            viewModel.saveUserFinancialData(salary, expenses);
            Toast.makeText(requireContext(), R.string.financial_info_saved, Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), R.string.invalid_numbers, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(61f);
        
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int labelColor = isDarkMode ? Color.WHITE : Color.BLACK;
        
        pieChart.setEntryLabelColor(labelColor);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setCenterText(getString(R.string.spending_overview));
        pieChart.setCenterTextColor(labelColor);
        pieChart.setCenterTextSize(18f);
        pieChart.setHoleRadius(58f);
    }

    private void observeTransactions() {
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                pieChart.setVisibility(View.GONE);
                tvNoStats.setVisibility(View.VISIBLE);
            } else {
                updateChartData(transactions);
            }
        });
    }

    private void updateChartData(List<Transaction> transactions) {
        Map<String, Double> categoryTotals = new HashMap<>();
        boolean hasExpenses = false;

        for (Transaction transaction : transactions) {
            if ("EXPENSE".equals(transaction.getType())) {
                hasExpenses = true;
                String category = transaction.getCategory();
                double amount = transaction.getAmount();
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
            }
        }

        if (!hasExpenses) {
            pieChart.setVisibility(View.GONE);
            tvNoStats.setVisibility(View.VISIBLE);
            tvNoStats.setText(R.string.no_expense_data);
            return;
        }

        pieChart.setVisibility(View.VISIBLE);
        tvNoStats.setVisibility(View.GONE);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, getString(R.string.categories));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        ArrayList<Integer> colors = new ArrayList<>();
        
        if (isDarkMode) {
            // High-contrast, vibrant colors for Dark Mode (Blues, Cyans, Purples)
            colors.add(Color.parseColor("#00B0FF")); // Light Blue 
            colors.add(Color.parseColor("#00E5FF")); // Cyan
            colors.add(Color.parseColor("#7C4DFF")); // Deep Purple
            colors.add(Color.parseColor("#1DE9B6")); // Teal
            colors.add(Color.parseColor("#FF4081")); // Pink (Accent)
        } else {
            // Classic palette for Light Mode
            for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
            for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        }
        
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(isDarkMode ? Color.WHITE : Color.BLACK);

        pieChart.setData(data);
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }
}
