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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.ChatSession;
import sevak.hovhannisyan.myproject.ui.adapter.ChatSessionAdapter;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class ChatHistoryFragment extends Fragment {

    private MainViewModel mainVm;
    private RecyclerView rvHistory;
    private ChatSessionAdapter adapter;
    private TextView tvEmpty;
    private MaterialButton btnNewChat;
    private View btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_chat_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        rvHistory = view.findViewById(R.id.rv_chat_history);
        tvEmpty = view.findViewById(R.id.tv_empty_history);
        btnNewChat = view.findViewById(R.id.btn_new_chat);
        btnBack = view.findViewById(R.id.btn_back_history);

        adapter = new ChatSessionAdapter(this::openSession, this::deleteSession);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        mainVm.getChatSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
                adapter.setSessions(sessions);
            }
        });

        btnNewChat.setOnClickListener(v -> {
            mainVm.createNewSession("New Conversation", id -> {
                requireActivity().runOnUiThread(() -> {
                    Navigation.findNavController(v).navigateUp();
                });
            });
        });

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void openSession(ChatSession session) {
        mainVm.setCurrentSessionId(session.getId());
        Navigation.findNavController(requireView()).navigateUp();
    }

    private void deleteSession(ChatSession session) {
        mainVm.deleteSession(session.getId());
    }
}
