package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class MarketFragment extends Fragment {

    private MainViewModel mainVm;
    private RecyclerView list;
    private ProgressBar bar;
    private ImageButton back;
    private ChipGroup chips;
    private MarketAdapter myAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_market, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        list = view.findViewById(R.id.rv_market_full);
        bar = view.findViewById(R.id.pb_market_full);
        back = view.findViewById(R.id.btn_back);
        chips = view.findViewById(R.id.chip_group_market);

        if (back != null) {
            back.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        list.setLayoutManager(new LinearLayoutManager(getContext()));
        myAdapter = new MarketAdapter();
        list.setAdapter(myAdapter);

        // Setup the top selection chips
        if (chips != null) {
            chips.setOnCheckedStateChangeListener((group, ids) -> {
                if (ids.isEmpty()) return;
                int id = ids.get(0);
                if (id == R.id.chip_stocks) {
                    mainVm.fetchStocks();
                } else if (id == R.id.chip_crypto) {
                    mainVm.fetchCrypto();
                }
            });
        }

        // Observer for market results
        mainVm.getMarketData().observe(getViewLifecycleOwner(), quotes -> {
            if (quotes != null) {
                myAdapter.setData(quotes);
            }
        });

        // Loading state
        mainVm.getIsMarketLoading().observe(getViewLifecycleOwner(), loading -> {
            if (bar != null) bar.setVisibility(loading ? View.VISIBLE : View.GONE);
            list.setAlpha(loading ? 0.4f : 1.0f);
        });

        // Initial fetch
        mainVm.fetchStocks();
    }

    private static class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.Holder> {
        private List<FinnhubResponse> data = new ArrayList<>();

        void setData(List<FinnhubResponse> quotes) {
            this.data = quotes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_market, p, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int i) {
            FinnhubResponse r = data.get(i);
            
            h.sym.setText(r.getSymbol());
            h.pr.setText(String.format(Locale.US, "$%.2f", r.getCurrentPrice()));
            h.ch.setText(String.format(Locale.US, "%.2f%%", r.getPercentChange()));
            
            // Check for extra data fields
            boolean details = false;

            if (r.getOpenPrice() != 0) {
                h.op.setText(String.format(Locale.US, "%.2f", r.getOpenPrice()));
                ((View)h.op.getParent()).setVisibility(View.VISIBLE);
                details = true;
            } else {
                ((View)h.op.getParent()).setVisibility(View.GONE);
            }

            if (r.getHighPrice() != 0) {
                h.hi.setText(String.format(Locale.US, "%.2f", r.getHighPrice()));
                ((View)h.hi.getParent()).setVisibility(View.VISIBLE);
                details = true;
            } else {
                ((View)h.hi.getParent()).setVisibility(View.GONE);
            }

            if (r.getLowPrice() != 0) {
                h.lo.setText(String.format(Locale.US, "%.2f", r.getLowPrice()));
                ((View)h.lo.getParent()).setVisibility(View.VISIBLE);
                details = true;
            } else {
                ((View)h.lo.getParent()).setVisibility(View.GONE);
            }

            if (r.getPreviousClose() != 0) {
                h.pc.setText(String.format(Locale.US, "%.2f", r.getPreviousClose()));
                ((View)h.pc.getParent()).setVisibility(View.VISIBLE);
                details = true;
            } else {
                ((View)h.pc.getParent()).setVisibility(View.GONE);
            }

            String vol = r.getVolume();
            if (vol != null && !vol.equals("-") && !vol.equals("0")) {
                h.vo.setText(vol);
                ((View)h.vo.getParent()).setVisibility(View.VISIBLE);
                details = true;
            } else {
                ((View)h.vo.getParent()).setVisibility(View.GONE);
            }

            if (r.getChange() != 0) {
                h.ca.setText(String.format(Locale.US, "%.2f", r.getChange()));
                ((View)h.ca.getParent()).setVisibility(View.VISIBLE);
                details = true;
            } else {
                ((View)h.ca.getParent()).setVisibility(View.GONE);
            }

            // UI visibility logic
            h.div.setVisibility(details ? View.VISIBLE : View.GONE);
            h.grid.setVisibility(details ? View.VISIBLE : View.GONE);

            int color = r.getChange() < 0 ? R.color.expense_red : R.color.income_green;
            h.ch.setTextColor(ContextCompat.getColor(h.itemView.getContext(), color));
            h.ca.setTextColor(ContextCompat.getColor(h.itemView.getContext(), color));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView sym, pr, ch, op, hi, lo, pc, vo, ca;
            View div, grid;

            Holder(View v) {
                super(v);
                sym = v.findViewById(R.id.tv_symbol);
                pr = v.findViewById(R.id.tv_price);
                ch = v.findViewById(R.id.tv_change);
                op = v.findViewById(R.id.tv_open);
                hi = v.findViewById(R.id.tv_high);
                lo = v.findViewById(R.id.tv_low);
                pc = v.findViewById(R.id.tv_prev_close);
                vo = v.findViewById(R.id.tv_volume);
                ca = v.findViewById(R.id.tv_change_abs);
                div = v.findViewById(R.id.market_divider);
                grid = v.findViewById(R.id.gridLayout);
            }
        }
    }
}
