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

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
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

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        rvMarket.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MarketAdapter();
        rvMarket.setAdapter(adapter);

        setupChips();
        observeViewModel();

        // Default: Load Stocks
        viewModel.fetchStocks();
    }

    private void setupChips() {
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_stocks) {
                viewModel.fetchStocks();
            } else if (id == R.id.chip_crypto) {
                viewModel.fetchCrypto();
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
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            rvMarket.setAlpha(isLoading ? 0.5f : 1.0f);
        });

        viewModel.getMarketError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {
        private List<FinnhubResponse> quotes = new ArrayList<>();

        void setQuotes(List<FinnhubResponse> quotes) {
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
            FinnhubResponse quote = quotes.get(position);
            
            if (holder.tvSymbol != null) holder.tvSymbol.setText(quote.getSymbol());
            if (holder.tvPrice != null) holder.tvPrice.setText(String.format("$%.2f", quote.getCurrentPrice()));
            if (holder.tvChange != null) holder.tvChange.setText(String.format("%.2f%%", quote.getPercentChange()));
            
            // Logic to show/hide details based on availability
            boolean hasDetails = false;

            if (quote.getOpenPrice() != 0) {
                if (holder.tvOpen != null) {
                    holder.tvOpen.setText(String.format("%.2f", quote.getOpenPrice()));
                    ((View)holder.tvOpen.getParent()).setVisibility(View.VISIBLE);
                }
                hasDetails = true;
            } else if (holder.tvOpen != null) {
                ((View)holder.tvOpen.getParent()).setVisibility(View.GONE);
            }

            if (quote.getHighPrice() != 0) {
                if (holder.tvHigh != null) {
                    holder.tvHigh.setText(String.format("%.2f", quote.getHighPrice()));
                    ((View)holder.tvHigh.getParent()).setVisibility(View.VISIBLE);
                }
                hasDetails = true;
            } else if (holder.tvHigh != null) {
                ((View)holder.tvHigh.getParent()).setVisibility(View.GONE);
            }

            if (quote.getLowPrice() != 0) {
                if (holder.tvLow != null) {
                    holder.tvLow.setText(String.format("%.2f", quote.getLowPrice()));
                    ((View)holder.tvLow.getParent()).setVisibility(View.VISIBLE);
                }
                hasDetails = true;
            } else if (holder.tvLow != null) {
                ((View)holder.tvLow.getParent()).setVisibility(View.GONE);
            }

            if (quote.getPreviousClose() != 0) {
                if (holder.tvPrevClose != null) {
                    holder.tvPrevClose.setText(String.format("%.2f", quote.getPreviousClose()));
                    ((View)holder.tvPrevClose.getParent()).setVisibility(View.VISIBLE);
                }
                hasDetails = true;
            } else if (holder.tvPrevClose != null) {
                ((View)holder.tvPrevClose.getParent()).setVisibility(View.GONE);
            }

            String volume = quote.getVolume();
            if (volume != null && !volume.equals("-") && !volume.equals("0")) {
                if (holder.tvVolume != null) {
                    holder.tvVolume.setText(volume);
                    ((View)holder.tvVolume.getParent()).setVisibility(View.VISIBLE);
                }
                hasDetails = true;
            } else if (holder.tvVolume != null) {
                ((View)holder.tvVolume.getParent()).setVisibility(View.GONE);
            }

            if (quote.getChange() != 0) {
                if (holder.tvChangeAbs != null) {
                    holder.tvChangeAbs.setText(String.format("%.2f", quote.getChange()));
                    ((View)holder.tvChangeAbs.getParent()).setVisibility(View.VISIBLE);
                }
                hasDetails = true;
            } else if (holder.tvChangeAbs != null) {
                ((View)holder.tvChangeAbs.getParent()).setVisibility(View.GONE);
            }

            // Hide the entire detail section if no extra info is available
            if (holder.divider != null) holder.divider.setVisibility(hasDetails ? View.VISIBLE : View.GONE);
            if (holder.gridLayout != null) holder.gridLayout.setVisibility(hasDetails ? View.VISIBLE : View.GONE);

            if (quote.getChange() < 0) {
                if (holder.tvChange != null) holder.tvChange.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
                if (holder.tvChangeAbs != null) holder.tvChangeAbs.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
            } else {
                if (holder.tvChange != null) holder.tvChange.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
                if (holder.tvChangeAbs != null) holder.tvChangeAbs.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
            }
        }

        @Override
        public int getItemCount() {
            return quotes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSymbol, tvPrice, tvChange;
            TextView tvOpen, tvHigh, tvLow, tvPrevClose, tvVolume, tvChangeAbs;
            View divider, gridLayout;

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
                
                divider = itemView.findViewById(R.id.market_divider);
                gridLayout = itemView.findViewById(R.id.gridLayout);
            }
        }
    }
}
