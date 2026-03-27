package sevak.hovhannisyan.myproject.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;

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
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.ui.adapter.ChatAdapter;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class AiAssistantFragment extends Fragment {

    private static final String TAG = "AiAssistantFragment";
    
    // API KEY - Ensure this is the latest key from your OpenRouter dashboard
    private static final String OPENROUTER_API_KEY = "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";
    
    // Primary Models
    private static final String INTELLIGENCE_MODEL = "openai/gpt-4o-mini"; 
    private static final String VISION_MODEL = "qwen/qwen-2-vl-72b-instruct";
    private static final String FREE_FALLBACK = "google/gemini-flash-1.5-8b:free"; 

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

        chatAdapter.addMessage(new ChatMessage("SAVE AI Online. GPT-4o-mini & Qwen-VL Active.", false));

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
        
        if ((userInput.toLowerCase().contains("save it") || userInput.toLowerCase().contains("add it")) && lastDetectedPrice > 0) {
            saveDetectedBill();
            return;
        }

        chatAdapter.addMessage(new ChatMessage("Processing with GPT-4o-mini...", false));
        rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        runMainBrain(userInput, INTELLIGENCE_MODEL);
    }

    private void saveDetectedBill() {
        Transaction t = new Transaction();
        t.setAmount(lastDetectedPrice);
        t.setCategory(lastDetectedCategory);
        t.setType(TransactionType.EXPENSE);
        t.setDate(new Date());
        t.setDescription("SAVE AI Scan (" + lastDetectedCategory + ")");
        
        viewModel.insertTransaction(t);
        
        lastDetectedPrice = 0.0;
        chatAdapter.addMessage(new ChatMessage("Transaction logged to database.", false));
        rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void runMainBrain(String userInput, String modelId) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String context = String.format(Locale.US, "CONTEXT: [Date: %s] [Bal: %.2f] [Inc: %.2f] [Exp: %.2f]", 
                dateStr, currentBalance, currentIncome, currentExpense);
        
        List<OpenRouterRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenRouterRequest.Message("system", "You are a Professional Financial Assistant. Context: " + context));
        messages.add(new OpenRouterRequest.Message("user", userInput));

        OpenRouterRequest request = new OpenRouterRequest(modelId, messages);

        openRouterService.analyzeBill("Bearer " + OPENROUTER_API_KEY, "https://saveapp.ai", "SAVE AI", request)
            .enqueue(new Callback<OpenRouterResponse>() {
                @Override
                public void onResponse(@NonNull Call<OpenRouterResponse> call, @NonNull Response<OpenRouterResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                        displayFinalAnswer(response.body().getChoices().get(0).getMessage().getContent());
                    } else {
                        handleErrorResponse(response);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OpenRouterResponse> call, @NonNull Throwable t) {
                    handleError("Network failure: " + t.getMessage());
                }
            });
    }

    private void handleErrorResponse(Response<?> response) {
        String errorBody = "";
        try {
            if (response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (Exception ignored) {}
        
        Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
        
        String msg;
        if (response.code() == 401) {
            msg = "Authentication Failed (401). Please check your OpenRouter API Key.";
        } else if (response.code() == 402) {
            msg = "Insufficient Credits (402). Please top up your OpenRouter account.";
        } else if (response.code() == 429) {
            msg = "Rate limit reached. Please wait a moment.";
        } else {
            msg = "AI Error " + response.code() + ": " + (errorBody.length() > 50 ? "Request failed" : errorBody);
        }
        
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
        chatAdapter.addMessage(new ChatMessage("Analyzing receipt with Qwen-VL...", false));
        rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) {
                handleError("Failed to decode image.");
                return;
            }
            
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 800, (int)(800.0 * bitmap.getHeight() / bitmap.getWidth()), true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            List<OpenRouterRequest.Content> contents = new ArrayList<>();
            contents.add(OpenRouterRequest.Content.text("Identify total amount and primary category from this receipt. Return RAW JSON ONLY: {\"total\":0.0,\"category\":\"\"}"));
            contents.add(OpenRouterRequest.Content.image(base64Image));

            OpenRouterRequest request = new OpenRouterRequest(VISION_MODEL, 
                Collections.singletonList(new OpenRouterRequest.Message("user", contents)));

            openRouterService.analyzeBill("Bearer " + OPENROUTER_API_KEY, "https://saveapp.ai", "SAVE AI", request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OpenRouterResponse> call, @NonNull Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                            parseAiJsonResponse(response.body().getChoices().get(0).getMessage().getContent());
                        } else {
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OpenRouterResponse> call, @NonNull Throwable t) {
                        handleError("Scanner failure: " + t.getMessage());
                    }
                });
        } catch (Exception e) {
            handleError("Scanner error: " + e.getMessage());
        }
    }

    private void parseAiJsonResponse(String content) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                if (chatAdapter.getItemCount() > 0) chatAdapter.removeLastMessage();
                
                String jsonPart = content;
                if (content.contains("```json")) {
                    jsonPart = content.substring(content.indexOf("```json") + 7, content.lastIndexOf("```"));
                } else if (content.contains("{") && content.contains("}")) {
                    jsonPart = content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1);
                }
                
                JSONObject json = new JSONObject(jsonPart.trim());
                lastDetectedPrice = json.optDouble("total", 0.0);
                lastDetectedCategory = json.optString("category", "Unknown");
                
                String message = "Receipt Analyzed:\nAmount: " + currencyFormat.format(lastDetectedPrice) + 
                                "\nCategory: " + lastDetectedCategory + "\n\nType 'save it' to log this transaction.";
                chatAdapter.addMessage(new ChatMessage(message, false));
                rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            } catch (Exception e) {
                chatAdapter.addMessage(new ChatMessage("Could not parse receipt data. AI Response: " + content, false));
                rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }

    private void observeData() {
        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> currentBalance = balance != null ? balance : 0.0);
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> currentIncome = income != null ? income : 0.0);
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> currentExpense = expense != null ? expense : 0.0);
        viewModel.getUserData().observe(getViewLifecycleOwner(), data -> userData = data);
    }
}
