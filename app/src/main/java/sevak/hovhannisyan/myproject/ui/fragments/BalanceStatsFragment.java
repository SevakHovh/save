package sevak.hovhannisyan.myproject.ui.fragments;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class BalanceStatsFragment extends Fragment {

    private MainViewModel viewModel;
    private LineChart lineChart;
    private BarChart barChart;
    private ChipGroup chipGroupTime;
    private ImageButton btnBack;
    
    private List<Transaction> allTransactions = new ArrayList<>();
    private String currentTimeFilter = "Week";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_balance_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        lineChart = view.findViewById(R.id.line_chart);
        barChart = view.findViewById(R.id.bar_chart);
        chipGroupTime = view.findViewById(R.id.chip_group_time);
        btnBack = view.findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        setupLineChartStyle();
        setupBarChartStyle();
        setupChips();
        observeTransactions();
    }

    private void setupLineChartStyle() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawZeroLine(true);
    }

    private void setupBarChartStyle() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
    }

    private void setupChips() {
        chipGroupTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_day) currentTimeFilter = "Day";
            else if (id == R.id.chip_week) currentTimeFilter = "Week";
            else if (id == R.id.chip_month) currentTimeFilter = "Month";
            else if (id == R.id.chip_year) currentTimeFilter = "Year";
            
            updateChart();
        });
    }

    private void observeTransactions() {
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                this.allTransactions = transactions;
                updateChart();
            }
        });
    }

    private void updateChart() {
        if (allTransactions.isEmpty()) return;

        long startTime = 0;
        SimpleDateFormat sdf;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (currentTimeFilter) {
            case "Day":
                startTime = cal.getTimeInMillis();
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                break;
            case "Month":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startTime = cal.getTimeInMillis();
                sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                break;
            case "Year":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                startTime = cal.getTimeInMillis();
                sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                break;
            case "Week":
            default:
                cal.add(Calendar.DAY_OF_YEAR, -7);
                startTime = cal.getTimeInMillis();
                sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                break;
        }

        Map<Long, Double> incomeData = new TreeMap<>();
        Map<Long, Double> expenseData = new TreeMap<>();

        for (Transaction t : allTransactions) {
            if (t.getDate().getTime() >= startTime) {
                long key = getGroupKey(t.getDate(), currentTimeFilter);
                if (TransactionType.INCOME.equals(t.getType())) {
                    incomeData.put(key, incomeData.getOrDefault(key, 0.0) + t.getAmount());
                } else {
                    expenseData.put(key, expenseData.getOrDefault(key, 0.0) + t.getAmount());
                }
            }
        }

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        List<Long> allKeys = new ArrayList<>(incomeData.keySet());
        for (Long k : expenseData.keySet()) {
            if (!allKeys.contains(k)) allKeys.add(k);
        }
        Collections.sort(allKeys);

        for (int i = 0; i < allKeys.size(); i++) {
            long key = allKeys.get(i);
            double inc = incomeData.getOrDefault(key, 0.0);
            double exp = expenseData.getOrDefault(key, 0.0);
            
            incomeEntries.add(new Entry(i, (float) inc));
            expenseEntries.add(new Entry(i, (float) exp));
            barEntries.add(new BarEntry(i, (float) exp));
            labels.add(sdf.format(new Date(key)));
        }

        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
            }
        };

        lineChart.getXAxis().setValueFormatter(formatter);
        barChart.getXAxis().setValueFormatter(formatter);

        // Line Chart Data
        LineDataSet incomeSet = createLineDataSet(incomeEntries, "Income", ContextCompat.getColor(requireContext(), R.color.income_green));
        LineDataSet expenseSet = createLineDataSet(expenseEntries, "Expenses", ContextCompat.getColor(requireContext(), R.color.expense_red));
        lineChart.setData(new LineData(incomeSet, expenseSet));
        lineChart.animateY(1000);
        lineChart.invalidate();

        // Bar Chart Data (Histogram)
        BarDataSet barDataSet = new BarDataSet(barEntries, "Expenses");
        barDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        barDataSet.setDrawValues(true);
        barDataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label, int color) {
        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setLineWidth(3f);
        set.setCircleColor(color);
        set.setCircleRadius(4f);
        set.setDrawCircleHole(true);
        set.setCircleHoleRadius(2f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT}
        );
        set.setFillDrawable(gradient);
        
        return set;
    }

    private long getGroupKey(Date date, String filter) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        if ("Day".equals(filter)) {
            // Group by hour
        } else if ("Year".equals(filter)) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            // Group by day
            cal.set(Calendar.HOUR_OF_DAY, 0);
        }
        return cal.getTimeInMillis();
    }
}
