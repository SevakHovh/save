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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import dagger.hilt.android.AndroidEntryPoint;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

/**
 * Fragment displaying all transactions with ability to add new ones.
 */
@AndroidEntryPoint
public class TransactionsFragment extends Fragment {
    
    private MainViewModel viewModel;
    private RecyclerView rvTransactions;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddTransaction;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        rvTransactions = view.findViewById(R.id.rv_transactions);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction);
        
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        // TODO: Set adapter for transactions list
        
        fabAddTransaction.setOnClickListener(v -> {
            // TODO: Open add transaction dialog/fragment
        });
        
        observeViewModel();
    }
    
    private void observeViewModel() {
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
                // TODO: Update RecyclerView adapter
            }
        });
    }
}
