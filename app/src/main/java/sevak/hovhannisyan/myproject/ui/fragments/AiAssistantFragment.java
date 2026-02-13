package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import dagger.hilt.android.AndroidEntryPoint;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

/**
 * Fragment for AI Assistant chat interface.
 * Placeholder for Gemini API integration.
 */
@AndroidEntryPoint
public class AiAssistantFragment extends Fragment {
    
    private MainViewModel viewModel;
    private RecyclerView rvChatMessages;
    private EditText etUserInput;
    private MaterialButton btnSend;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        etUserInput = view.findViewById(R.id.et_user_input);
        btnSend = view.findViewById(R.id.btn_send);
        
        rvChatMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        // TODO: Set adapter for chat messages
        
        btnSend.setOnClickListener(v -> {
            String question = etUserInput.getText().toString().trim();
            if (!question.isEmpty()) {
                // TODO: Send question to AI service
                etUserInput.setText("");
            }
        });
    }
}
