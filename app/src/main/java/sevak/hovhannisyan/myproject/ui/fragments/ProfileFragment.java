package sevak.hovhannisyan.myproject.ui.fragments;

import android.content.Intent;
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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.LoginActivity;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.ui.GoalManager;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject
    FirebaseAuth mAuth;

    @Inject
    GoalManager goalManager;

    private MainViewModel viewModel;
    private TextView tvUserEmail;
    private TextView tvNoStats;
    private MaterialButton btnSignOut;
    private PieChart pieChart;

    private TextInputEditText etSalary;
    private TextInputEditText etFixedExpenses;
    private MaterialButton btnSaveFinancialInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvNoStats = view.findViewById(R.id.tv_no_stats);
        btnSignOut = view.findViewById(R.id.btn_sign_out);
        pieChart = view.findViewById(R.id.pie_chart);

        etSalary = view.findViewById(R.id.et_salary);
        etFixedExpenses = view.findViewById(R.id.et_fixed_expenses);
        btnSaveFinancialInfo = view.findViewById(R.id.btn_save_financial_info);

        setupUserInfo();
        setupFinancialInfo();
        setupPieChart();
        observeTransactions();

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        btnSaveFinancialInfo.setOnClickListener(v -> saveFinancialInfo());
    }

    private void setupUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
        }
    }

    private void setupFinancialInfo() {
        double salary = goalManager.getSalary();
        double fixedExpenses = goalManager.getFixedExpenses();
        
        if (salary > 0) etSalary.setText(String.valueOf(salary));
        if (fixedExpenses > 0) etFixedExpenses.setText(String.valueOf(fixedExpenses));
    }

    private void saveFinancialInfo() {
        String salaryStr = etSalary.getText().toString();
        String expensesStr = etFixedExpenses.getText().toString();

        try {
            double salary = salaryStr.isEmpty() ? 0 : Double.parseDouble(salaryStr);
            double expenses = expensesStr.isEmpty() ? 0 : Double.parseDouble(expensesStr);

            goalManager.saveSalary(salary);
            goalManager.saveFixedExpenses(expenses);

            Toast.makeText(requireContext(), "Financial info saved!", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
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
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setCenterText("Expenses");
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
            tvNoStats.setText("No expense data to display");
            return;
        }

        pieChart.setVisibility(View.VISIBLE);
        tvNoStats.setVisibility(View.GONE);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }
}
