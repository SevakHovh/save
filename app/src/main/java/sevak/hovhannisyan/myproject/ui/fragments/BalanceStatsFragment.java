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

    private MainViewModel mainVm;
    private LineChart trendChart;
    private BarChart incomeBar, expenseBar;
    private ChipGroup timeChips;
    private ImageButton backArrow;
    
    private List<Transaction> myData = new ArrayList<>();
    private String filter = "Week";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_balance_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // UI setup
        trendChart = view.findViewById(R.id.line_chart);
        incomeBar = view.findViewById(R.id.bar_chart_income);
        expenseBar = view.findViewById(R.id.bar_chart_expense);
        timeChips = view.findViewById(R.id.chip_group_time);
        backArrow = view.findViewById(R.id.btn_back);

        backArrow.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        configLineChart();
        configBarChart(incomeBar, R.color.income_green);
        configBarChart(expenseBar, R.color.expense_red);
        
        // Time filter logic
        timeChips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_day) filter = "Day";
            else if (id == R.id.chip_week) filter = "Week";
            else if (id == R.id.chip_month) filter = "Month";
            else if (id == R.id.chip_year) filter = "Year";
            
            refreshCharts();
        });

        // Watch data
        mainVm.getAllTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                this.myData = list;
                refreshCharts();
            }
        });
    }

    private void configLineChart() {
        trendChart.getDescription().setEnabled(false);
        trendChart.setDrawGridBackground(false);
        trendChart.getAxisRight().setEnabled(false);
        trendChart.getLegend().setEnabled(false);
        
        XAxis x = trendChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        
        YAxis y = trendChart.getAxisLeft();
        y.setDrawGridLines(true);
        y.setDrawZeroLine(true);
    }

    private void configBarChart(BarChart bc, int colorRes) {
        bc.getDescription().setEnabled(false);
        bc.setDrawGridBackground(false);
        bc.getAxisRight().setEnabled(false);
        bc.getLegend().setEnabled(false);
        bc.setDrawBarShadow(false);
        bc.setDrawValueAboveBar(true);

        XAxis x = bc.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);

        YAxis y = bc.getAxisLeft();
        y.setDrawGridLines(true);
        y.setAxisMinimum(0f);
    }

    private void refreshCharts() {
        if (myData.isEmpty()) return;

        long start = 0;
        SimpleDateFormat fmt;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (filter.equals("Day")) {
            start = c.getTimeInMillis();
            fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else if (filter.equals("Month")) {
            c.set(Calendar.DAY_OF_MONTH, 1);
            start = c.getTimeInMillis();
            fmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        } else if (filter.equals("Year")) {
            c.set(Calendar.DAY_OF_YEAR, 1);
            start = c.getTimeInMillis();
            fmt = new SimpleDateFormat("MMM", Locale.getDefault());
        } else {
            c.add(Calendar.DAY_OF_YEAR, -7);
            start = c.getTimeInMillis();
            fmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        }

        Map<Long, Double> incMap = new TreeMap<>();
        Map<Long, Double> expMap = new TreeMap<>();

        for (Transaction t : myData) {
            if (t.getDate().getTime() >= start) {
                long key = createKey(t.getDate(), filter);
                if (TransactionType.INCOME.equals(t.getType())) {
                    incMap.put(key, incMap.getOrDefault(key, 0.0) + t.getAmount());
                } else {
                    expMap.put(key, expMap.getOrDefault(key, 0.0) + t.getAmount());
                }
            }
        }

        List<Entry> lineInc = new ArrayList<>();
        List<Entry> lineExp = new ArrayList<>();
        List<BarEntry> barInc = new ArrayList<>();
        List<BarEntry> barExp = new ArrayList<>();
        List<String> names = new ArrayList<>();

        List<Long> keys = new ArrayList<>(incMap.keySet());
        for (Long k : expMap.keySet()) {
            if (!keys.contains(k)) keys.add(k);
        }
        Collections.sort(keys);

        for (int i = 0; i < keys.size(); i++) {
            long k = keys.get(i);
            double iVal = incMap.getOrDefault(k, 0.0);
            double eVal = expMap.getOrDefault(k, 0.0);
            
            lineInc.add(new Entry(i, (float) iVal));
            lineExp.add(new Entry(i, (float) eVal));
            barInc.add(new BarEntry(i, (float) iVal));
            barExp.add(new BarEntry(i, (float) eVal));
            names.add(fmt.format(new Date(k)));
        }

        ValueFormatter vf = new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                int pos = (int) v;
                return (pos >= 0 && pos < names.size()) ? names.get(pos) : "";
            }
        };

        trendChart.getXAxis().setValueFormatter(vf);
        incomeBar.getXAxis().setValueFormatter(vf);
        expenseBar.getXAxis().setValueFormatter(vf);

        LineDataSet set1 = makeLineSet(lineInc, "Income", ContextCompat.getColor(requireContext(), R.color.income_green));
        LineDataSet set2 = makeLineSet(lineExp, "Expenses", ContextCompat.getColor(requireContext(), R.color.expense_red));
        trendChart.setData(new LineData(set1, set2));
        trendChart.animateY(800);
        trendChart.invalidate();

        fillBar(incomeBar, barInc, R.color.income_green);
        fillBar(expenseBar, barExp, R.color.expense_red);
    }

    private void fillBar(BarChart bc, List<BarEntry> e, int color) {
        BarDataSet set = new BarDataSet(e, bc == incomeBar ? "Income" : "Expenses");
        set.setColor(ContextCompat.getColor(requireContext(), color));
        set.setDrawValues(true);
        set.setValueTextSize(9f);
        
        bc.setData(new BarData(set));
        bc.setFitBars(true);
        bc.animateY(800);
        bc.invalidate();
    }

    private LineDataSet makeLineSet(List<Entry> e, String label, int color) {
        LineDataSet set = new LineDataSet(e, label);
        set.setColor(color);
        set.setLineWidth(2.5f);
        set.setCircleColor(color);
        set.setCircleRadius(3.5f);
        set.setDrawCircleHole(true);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(80, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT}
        );
        set.setFillDrawable(g);
        
        return set;
    }

    private long createKey(Date d, String f) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        
        if (f.equals("Year")) {
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
        } else if (!f.equals("Day")) {
            c.set(Calendar.HOUR_OF_DAY, 0);
        }
        return c.getTimeInMillis();
    }
}
