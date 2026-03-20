package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

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

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.StockResponse;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class MarketFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView rvMarket;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ChipGroup chipGroup;
    private MarketAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        rvMarket = view.findViewById(R.id.rv_market_full);
        progressBar = view.findViewById(R.id.pb_market_full);
        btnBack = view.findViewById(R.id.btn_back);
        chipGroup = view.findViewById(R.id.chip_group_market);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        rvMarket.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MarketAdapter();
        rvMarket.setAdapter(adapter);

        setupChips();
        observeViewModel();

        // Default: Load Stocks
        viewModel.fetchStocks();
    }

    private void setupChips() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_stocks) {
                viewModel.fetchStocks();
            } else if (id == R.id.chip_crypto) {
                viewModel.fetchCrypto();
            } else if (id == R.id.chip_indices) {
                viewModel.fetchIndices();
            } else if (id == R.id.chip_forex) {
                viewModel.fetchForex();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getMarketData().observe(getViewLifecycleOwner(), quotes -> {
            if (quotes != null) {
                adapter.setQuotes(quotes);
            }
        });

        viewModel.getIsMarketLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            rvMarket.setAlpha(isLoading ? 0.5f : 1.0f);
        });
    }

    private static class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {
        private List<StockResponse.GlobalQuote> quotes = new ArrayList<>();

        void setQuotes(List<StockResponse.GlobalQuote> quotes) {
            this.quotes = quotes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_market, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StockResponse.GlobalQuote quote = quotes.get(position);
            holder.tvSymbol.setText(quote.getSymbol());
            holder.tvPrice.setText("$" + (quote.getPrice() != null ? quote.getPrice() : "N/A"));
            holder.tvChange.setText(quote.getChangePercent());
            
            holder.tvOpen.setText(quote.getOpen());
            holder.tvHigh.setText(quote.getHigh());
            holder.tvLow.setText(quote.getLow());
            holder.tvPrevClose.setText(quote.getPreviousClose());
            holder.tvVolume.setText(quote.getVolume());
            holder.tvChangeAbs.setText(quote.getChange());

            String change = quote.getChangePercent();
            if (change != null && change.startsWith("-")) {
                holder.tvChange.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
                holder.tvChangeAbs.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
            } else {
                holder.tvChange.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
                holder.tvChangeAbs.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
            }
        }

        @Override
        public int getItemCount() {
            return quotes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSymbol, tvPrice, tvChange;
            TextView tvOpen, tvHigh, tvLow, tvPrevClose, tvVolume, tvChangeAbs;

            ViewHolder(View itemView) {
                super(itemView);
                tvSymbol = itemView.findViewById(R.id.tv_symbol);
                tvPrice = itemView.findViewById(R.id.tv_price);
                tvChange = itemView.findViewById(R.id.tv_change);
                tvOpen = itemView.findViewById(R.id.tv_open);
                tvHigh = itemView.findViewById(R.id.tv_high);
                tvLow = itemView.findViewById(R.id.tv_low);
                tvPrevClose = itemView.findViewById(R.id.tv_prev_close);
                tvVolume = itemView.findViewById(R.id.tv_volume);
                tvChangeAbs = itemView.findViewById(R.id.tv_change_abs);
            }
        }
    }
}
