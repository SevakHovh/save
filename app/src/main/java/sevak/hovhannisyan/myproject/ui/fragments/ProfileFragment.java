package sevak.hovhannisyan.myproject.ui.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.databinding.FragmentProfileBinding;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private MainViewModel mainVm;
    
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
        initObservers();
        initListeners();
    }

    private void initListeners() {
        binding.btnSaveFinancialInfo.setOnClickListener(v -> {
            String s = binding.etSalary.getText().toString();
            String e = binding.etFixedExpenses.getText().toString();
            try {
                double sal = s.isEmpty() ? 0 : Double.parseDouble(s);
                double exp = e.isEmpty() ? 0 : Double.parseDouble(e);
                mainVm.saveProfileInfo(sal, exp);
                Toast.makeText(requireContext(), R.string.financial_info_saved, Toast.LENGTH_SHORT).show();
            } catch (Exception err) {
                Toast.makeText(requireContext(), R.string.invalid_numbers, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSettingsProfile.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.settingsFragment));

        // Fixed: Updated to match the ChipGroup and Chip IDs in fragment_profile.xml
        binding.chipGroupOverview.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_day_ov) timeFilter = "Day";
                else if (id == R.id.chip_week_ov) timeFilter = "Week";
                else if (id == R.id.chip_month_ov) timeFilter = "Month";
                updateChart(fullList);
            }
        });
        
        // Removed references to btnPersonalInfo and layoutPersonalInfo as they don't exist in fragment_profile.xml
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
                Object e = data.get("fixedExpenses");
                if (s != null) binding.etSalary.setText(String.valueOf(s));
                if (e != null) binding.etFixedExpenses.setText(String.valueOf(e));
            }
        });

        mainVm.getAllTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                fullList = list;
                updateChart(list);
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
            if ("EXPENSE".equals(t.getType()) && t.getDate().getTime() >= threshold) {
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
