package sevak.hovhannisyan.myproject.ui.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.databinding.FragmentProfileBinding;
import sevak.hovhannisyan.myproject.ui.adapter.RecurringTransactionAdapter;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private MainViewModel mainVm;
    private RecurringTransactionAdapter recurringAdapter;
    
    private String timeFilter = "Week";
    private List<Transaction> fullList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentProfileBinding.inflate(inf, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupChartStyle();
        setupRecurringList();
        initObservers();
        initListeners();
    }

    private void setupRecurringList() {
        recurringAdapter = new RecurringTransactionAdapter(rt -> {
            mainVm.deleteRecurringTransaction(rt);
        });
        binding.rvRecurringTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecurringTransactions.setAdapter(recurringAdapter);
    }

    private void initListeners() {
        binding.btnSaveFinancialInfo.setOnClickListener(v -> {
            String s = binding.etSalary.getText().toString();
            try {
                double sal = s.isEmpty() ? 0 : Double.parseDouble(s);
                mainVm.saveProfileInfo(sal, 0); 
                Toast.makeText(requireContext(), R.string.financial_info_saved, Toast.LENGTH_SHORT).show();
            } catch (Exception err) {
                Toast.makeText(requireContext(), R.string.invalid_numbers, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSettingsProfile.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.settingsFragment));

        binding.chipGroupOverview.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_day_ov) timeFilter = "Day";
                else if (id == R.id.chip_week_ov) timeFilter = "Week";
                else if (id == R.id.chip_month_ov) timeFilter = "Month";
                updateChart(fullList);
            }
        });

        binding.btnAddRecurring.setOnClickListener(v -> showAddRecurringDialog());
    }

    private void showAddRecurringDialog() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_recurring, null);
        TextInputEditText etDesc = v.findViewById(R.id.et_recurring_desc);
        TextInputEditText etAmount = v.findViewById(R.id.et_recurring_amount);
        TextInputEditText etPeriod = v.findViewById(R.id.et_recurring_period);
        AutoCompleteTextView spinnerType = v.findViewById(R.id.spinner_type);
        AutoCompleteTextView spinnerCat = v.findViewById(R.id.spinner_category);

        String typeIncome = getString(R.string.income_type);
        String typeExpense = getString(R.string.expense_type);
        String[] types = {typeExpense, typeIncome};
        
        String[] cats = {
            getString(R.string.cat_utilities), getString(R.string.cat_debt), getString(R.string.cat_taxes), 
            getString(R.string.cat_food), getString(R.string.cat_transport), 
            getString(R.string.cat_shopping), getString(R.string.cat_other)
        };

        spinnerType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, types));
        spinnerCat.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cats));
        
        spinnerType.setText(typeExpense, false);
        spinnerCat.setText(getString(R.string.cat_other), false);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_fixed_transaction)
                .setView(v)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String desc = etDesc.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String periodStr = etPeriod.getText().toString().trim();
                    String selectedType = spinnerType.getText().toString();
                    String cat = spinnerCat.getText().toString();

                    if (desc.isEmpty() || amtStr.isEmpty() || periodStr.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_fill_all, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        RecurringTransaction rt = new RecurringTransaction();
                        rt.setDescription(desc);
                        rt.setAmount(Double.parseDouble(amtStr));
                        rt.setPeriodDays(Integer.parseInt(periodStr));
                        
                        // Map localized string back to constant
                        if (selectedType.equals(typeIncome)) {
                            rt.setType(TransactionType.INCOME);
                        } else {
                            rt.setType(TransactionType.EXPENSE);
                        }
                        
                        rt.setCategory(cat);
                        mainVm.addRecurringTransaction(rt);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), R.string.invalid_numbers, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupChartStyle() {
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setExtraOffsets(5, 10, 5, 5);
        binding.pieChart.setDragDecelerationFrictionCoef(0.95f);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setTransparentCircleRadius(61f);
        binding.pieChart.setHoleRadius(58f);

        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int color = dark ? Color.WHITE : Color.BLACK;
        
        binding.pieChart.setEntryLabelColor(color);
        binding.pieChart.setEntryLabelTextSize(12f);
        binding.pieChart.setCenterText(getString(R.string.spending_overview));
        binding.pieChart.setCenterTextColor(color);
        binding.pieChart.setCenterTextSize(16f);
    }

    private void initObservers() {
        mainVm.getUserData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                Object s = data.get("salary");
                if (s != null) binding.etSalary.setText(String.valueOf(s));
            }
        });

        mainVm.getAllTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                fullList = list;
                updateChart(list);
            }
        });

        mainVm.getRecurringTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                recurringAdapter.setList(list);
            }
        });
    }

    private void updateChart(List<Transaction> list) {
        if (list == null || list.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoStats.setVisibility(View.VISIBLE);
            return;
        }

        long threshold = getStartTime(timeFilter);
        Map<String, Double> categoryMap = new HashMap<>();
        boolean hasData = false;

        for (Transaction t : list) {
            if (TransactionType.EXPENSE.equals(t.getType()) && t.getDate().getTime() >= threshold) {
                hasData = true;
                categoryMap.put(t.getCategory(), categoryMap.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        if (!hasData) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoStats.setVisibility(View.VISIBLE);
            binding.tvNoStats.setText(R.string.no_expense_data);
            return;
        }

        binding.pieChart.setVisibility(View.VISIBLE);
        binding.tvNoStats.setVisibility(View.GONE);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (dark) {
            colors.add(Color.parseColor("#00B0FF"));
            colors.add(Color.parseColor("#00E5FF"));
            colors.add(Color.parseColor("#7C4DFF"));
            colors.add(Color.parseColor("#1DE9B6"));
            colors.add(Color.parseColor("#FF4081"));
        } else {
            for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
            for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        }
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(dark ? Color.WHITE : Color.BLACK);

        binding.pieChart.setData(data);
        binding.pieChart.animateY(800);
        binding.pieChart.invalidate();
    }

    private long getStartTime(String filter) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (filter.equals("Day")) return cal.getTimeInMillis();
        if (filter.equals("Month")) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            return cal.getTimeInMillis();
        }
        cal.add(Calendar.DAY_OF_YEAR, -7);
        return cal.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
