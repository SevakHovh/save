package sevak.hovhannisyan.myproject.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.OpenRouterRequest;
import sevak.hovhannisyan.myproject.api.OpenRouterResponse;
import sevak.hovhannisyan.myproject.api.OpenRouterService;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.ui.adapter.ChatAdapter;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class AiAssistantFragment extends Fragment {

    private static final String TAG = "AiAssistantFragment";
    
    // CURRENT API KEY
    private static final String OPENROUTER_API_KEY = "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";
    
    // ACTIVE MODEL - Change this to a ':free' model if you don't have credits
    private static final String CLAUDE_MODEL = "anthropic/claude-3.5-sonnet"; 
    private static final String FREE_FALLBACK = "google/gemini-flash-1.5-8b"; // Very fast and usually cheaper/free
    private static final String VISION_MODEL = "qwen/qwen-2-vl-72b-instruct";

    private enum AiMode {
        REGULAR, BILL_DETECTOR
    }

    @Inject
    OpenRouterService openRouterService;

    private MainViewModel viewModel;
    private RecyclerView rvChatMessages;
    private EditText etUserInput;
    private MaterialButton btnSend;
    private MaterialButton btnAiMode;
    private ChatAdapter chatAdapter;
    private NumberFormat currencyFormat;
    private AiMode currentMode = AiMode.REGULAR;

    private double lastDetectedPrice = 0.0;
    private String lastDetectedCategory = "Unknown";

    private Double currentBalance = 0.0;
    private Double currentIncome = 0.0;
    private Double currentExpense = 0.0;
    private Map<String, Object> userData;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processBillWithQwen(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        etUserInput = view.findViewById(R.id.et_user_input);
        btnSend = view.findViewById(R.id.btn_send);
        btnAiMode = view.findViewById(R.id.btn_ai_mode);

        chatAdapter = new ChatAdapter();
        rvChatMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChatMessages.setAdapter(chatAdapter);

        chatAdapter.addMessage(new ChatMessage("SAVE AI Online. Intelligence Mode active.", false));

        observeData();
        setupButtons();
    }

    private void setupButtons() {
        btnAiMode.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), btnAiMode);
            popup.getMenu().add("Intelligence Mode");
            popup.getMenu().add("Receipt Scanner");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Intelligence Mode")) {
                    setAiMode(AiMode.REGULAR);
                } else {
                    setAiMode(AiMode.BILL_DETECTOR);
                }
                return true;
            });
            popup.show();
        });

        btnSend.setOnClickListener(v -> {
            if (currentMode == AiMode.REGULAR) {
                handleAiIntelligence();
            } else {
                getContent.launch("image/*");
            }
        });
    }

    private void setAiMode(AiMode mode) {
        currentMode = mode;
        if (mode == AiMode.REGULAR) {
            btnAiMode.setText("Intelligence");
            btnAiMode.setIconResource(android.R.drawable.ic_menu_info_details);
            etUserInput.setVisibility(View.VISIBLE);
            btnSend.setText("Ask AI");
        } else {
            btnAiMode.setText("Scanner");
            btnAiMode.setIconResource(android.R.drawable.ic_menu_camera);
            etUserInput.setVisibility(View.GONE);
            btnSend.setText("Scan Bill");
        }
    }

    private void handleAiIntelligence() {
        String userInput = etUserInput.getText().toString().trim();
        if (userInput.isEmpty()) return;

        chatAdapter.addMessage(new ChatMessage(userInput, true));
        etUserInput.setText("");
        
        // Smart Save Detection
        if ((userInput.toLowerCase().contains("save it") || userInput.toLowerCase().contains("add it")) && lastDetectedPrice > 0) {
            saveDetectedBill();
            return;
        }

        chatAdapter.addMessage(new ChatMessage("Synchronizing with Neural Core...", false));
        rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        runMainBrain(userInput, CLAUDE_MODEL);
    }

    private void saveDetectedBill() {
        Transaction t = new Transaction();
        t.setAmount(lastDetectedPrice);
        t.setCategory(lastDetectedCategory);
        t.setType("EXPENSE");
        t.setDate(new Date());
        t.setDescription("SAVE AI Scan (" + lastDetectedCategory + ")");
        viewModel.insertTransaction(t);
        
        lastDetectedPrice = 0.0;
        chatAdapter.addMessage(new ChatMessage("Neural Data Synchronized. Transaction logged.", false));
        rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void runMainBrain(String userInput, String modelId) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String context = String.format(Locale.US, "CONTEXT: [Date: %s] [Bal: %.2f] [Inc: %.2f] [Exp: %.2f]", 
                dateStr, currentBalance, currentIncome, currentExpense);
        
        String prompt = "Professional Financial Assistant. Context: " + context + ". User Input: " + userInput;

        OpenRouterRequest request = new OpenRouterRequest(modelId, 
                Collections.singletonList(new OpenRouterRequest.Message("user", prompt)));

        openRouterService.analyzeBill("Bearer " + OPENROUTER_API_KEY, "https://saveapp.ai", "SAVE AI", request)
            .enqueue(new Callback<OpenRouterResponse>() {
                @Override
                public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                        displayFinalAnswer(response.body().getChoices().get(0).getMessage().getContent());
                    } else if (response.code() == 402 && !modelId.equals(FREE_FALLBACK)) {
                        // If 402 on Claude, try to fallback to a free/cheaper model automatically
                        runMainBrain(userInput, FREE_FALLBACK);
                    } else {
                        handleErrorResponse(response);
                    }
                }

                @Override
                public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                    handleError("Strategic core link failed.");
                }
            });
    }

    private void handleErrorResponse(Response<?> response) {
        String msg = "System Alert: Code " + response.code();
        if (response.code() == 402) msg = "Neural Credits Depleted. Please top up your OpenRouter account to use Claude.";
        handleError(msg);
    }

    private void displayFinalAnswer(String answer) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (chatAdapter.getItemCount() > 0) chatAdapter.removeLastMessage();
            chatAdapter.addMessage(new ChatMessage(answer, false));
            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        });
    }

    private void handleError(String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (chatAdapter.getItemCount() > 0) chatAdapter.removeLastMessage();
            chatAdapter.addMessage(new ChatMessage(msg, false));
            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        });
    }

    private void processBillWithQwen(Uri uri) {
        chatAdapter.addMessage(new ChatMessage("Neural Scanning Active...", false));
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 800, (int)(800.0 * bitmap.getHeight() / bitmap.getWidth()), true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            List<OpenRouterRequest.Content> contents = new ArrayList<>();
            contents.add(OpenRouterRequest.Content.text("Return JSON ONLY: {\"total\":0.0,\"category\":\"\"}"));
            contents.add(OpenRouterRequest.Content.image(base64Image));

            OpenRouterRequest request = new OpenRouterRequest(VISION_MODEL, 
                Collections.singletonList(new OpenRouterRequest.Message("user", contents)));

            openRouterService.analyzeBill("Bearer " + OPENROUTER_API_KEY, "https://saveapp.ai", "SAVE AI", request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (chatAdapter.getItemCount() > 0) chatAdapter.removeLastMessage();
                            if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                                parseAiJsonResponse(response.body().getChoices().get(0).getMessage().getContent());
                            } else {
                                handleErrorResponse(response);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (chatAdapter.getItemCount() > 0) chatAdapter.removeLastMessage();
                            chatAdapter.addMessage(new ChatMessage("Visual link failure.", false));
                        });
                    }
                });
        } catch (Exception e) {
            chatAdapter.addMessage(new ChatMessage("Scanner initialization error.", false));
        }
    }

    private void parseAiJsonResponse(String content) {
        try {
            String jsonPart = content;
            if (content.contains("{") && content.contains("}")) {
                jsonPart = content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1);
            }
            JSONObject json = new JSONObject(jsonPart);
            lastDetectedPrice = json.optDouble("total", 0.0);
            lastDetectedCategory = json.optString("category", "Unknown");
            chatAdapter.addMessage(new ChatMessage("Neural Scan Verified:\nAmount: " + currencyFormat.format(lastDetectedPrice) + "\nCategory: " + lastDetectedCategory + "\n\nSay 'save it' to log this.", false));
        } catch (Exception e) {
            chatAdapter.addMessage(new ChatMessage("Neural extraction failed.", false));
        }
    }

    private void observeData() {
        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> currentBalance = balance != null ? balance : 0.0);
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> currentIncome = income != null ? income : 0.0);
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> currentExpense = expense != null ? expense : 0.0);
        viewModel.getUserData().observe(getViewLifecycleOwner(), data -> userData = data);
    }
}
