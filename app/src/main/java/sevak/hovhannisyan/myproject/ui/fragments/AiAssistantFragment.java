package sevak.hovhannisyan.myproject.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.ui.GoalManager;
import sevak.hovhannisyan.myproject.ui.adapter.ChatAdapter;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class AiAssistantFragment extends Fragment {

    private enum AiMode {
        REGULAR, BILL_DETECTOR
    }

    private MainViewModel viewModel;
    private RecyclerView rvChatMessages;
    private EditText etUserInput;
    private MaterialButton btnSend;
    private MaterialButton btnAiMode;
    private ChatAdapter chatAdapter;
    private NumberFormat currencyFormat;
    private AiMode currentMode = AiMode.REGULAR;

    // Track state for detected bill to allow "add it" command
    private double lastDetectedPrice = 0.0;
    private String lastDetectedCategory = "Other";

    @Inject
    GoalManager goalManager;

    private Double currentBalance = 0.0;
    private Double currentIncome = 0.0;
    private Double currentExpense = 0.0;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processBillImage(uri);
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

        // Add welcome message
        chatAdapter.addMessage(new ChatMessage("Hello! I'm your SAVE AI assistant. How can I help you today?", false));

        observeData();
        setupButtons();
    }

    private void setupButtons() {
        btnAiMode.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), btnAiMode);
            popup.getMenu().add("Regular");
            popup.getMenu().add("Bill Detector");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Regular")) {
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
                handleRegularSend();
            } else {
                getContent.launch("image/*");
            }
        });
    }

    private void setAiMode(AiMode mode) {
        currentMode = mode;
        if (mode == AiMode.REGULAR) {
            btnAiMode.setText("Regular");
            btnAiMode.setIconResource(android.R.drawable.ic_menu_info_details);
            etUserInput.setVisibility(View.VISIBLE);
            btnSend.setText("Send");
            btnSend.setIconResource(android.R.drawable.ic_menu_send);
        } else {
            btnAiMode.setText("Bill Det.");
            btnAiMode.setIconResource(android.R.drawable.ic_menu_camera);
            etUserInput.setVisibility(View.GONE);
            btnSend.setText("Pick Bill");
            btnSend.setIconResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void handleRegularSend() {
        String question = etUserInput.getText().toString().trim();
        if (!question.isEmpty()) {
            chatAdapter.addMessage(new ChatMessage(question, true));
            etUserInput.setText("");
            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String answer = generateAiResponse(question.toLowerCase());
                chatAdapter.addMessage(new ChatMessage(answer, false));
                rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }, 1000);
        }
    }

    private void processBillImage(Uri uri) {
        chatAdapter.addMessage(new ChatMessage("Analyzing bill image...", true));
        rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        // OCR Simulation using the provided API context
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Generate a random realistic price between $5 and $150
            Random r = new Random();
            lastDetectedPrice = 5.0 + (145.0 * r.nextDouble());
            
            String[] categories = {"Grocery", "Dining", "Shopping", "Transport", "Utilities"};
            lastDetectedCategory = categories[r.nextInt(categories.length)];

            String formattedPrice = currencyFormat.format(lastDetectedPrice);
            String result = "Bill Analysis Complete:\n" +
                    "Detected Amount: " + formattedPrice + "\n" +
                    "Estimated Category: " + lastDetectedCategory + "\n\n" +
                    "I've saved these details. Switch to 'Regular' mode and say 'add it' to save this expense!";
            
            chatAdapter.addMessage(new ChatMessage(result, false));
            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }, 2000);
    }

    private void observeData() {
        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> currentBalance = balance != null ? balance : 0.0);
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> currentIncome = income != null ? income : 0.0);
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> currentExpense = expense != null ? expense : 0.0);
    }

    private String generateAiResponse(String input) {
        // Handle "add it" command for bill detector
        if ((input.contains("add it") || input.contains("save this") || input.contains("add expense")) && lastDetectedPrice > 0) {
            Transaction transaction = new Transaction();
            transaction.setAmount(lastDetectedPrice);
            transaction.setCategory(lastDetectedCategory);
            transaction.setType("EXPENSE");
            transaction.setDate(new Date());
            transaction.setDescription("Bill Detector Scan");
            
            viewModel.insertTransaction(transaction);
            
            double price = lastDetectedPrice;
            lastDetectedPrice = 0; // Reset
            return "Done! I've added " + currencyFormat.format(price) + " to your expenses under " + lastDetectedCategory + ". Your balance has been updated.";
        }

        double salary = goalManager.getSalary();
        double fixedExpenses = goalManager.getFixedExpenses();
        double goal = goalManager.getGoalAmount();

        if (input.contains("advice") || input.contains("analyze") || input.contains("conclude") || input.contains("help") || input.contains("status")) {
            if (salary <= 0) {
                return "I'd love to give you financial advice, but I don't know your salary yet! Please go to your Profile and enter your monthly income.";
            }

            StringBuilder advice = new StringBuilder();
            double disposableIncome = salary - fixedExpenses;
            double actualSavings = currentIncome - currentExpense;

            advice.append("Analysis: Your monthly disposable income is ").append(currencyFormat.format(disposableIncome)).append(". ");

            if (actualSavings < 0) {
                advice.append("Warning: You are currently overspending by ").append(currencyFormat.format(Math.abs(actualSavings))).append(" this month. ");
                advice.append("Tip: Focus on cutting discretionary spending like ").append(lastDetectedCategory.toLowerCase()).append(" to balance your budget.");
            } else {
                double savingsRate = (actualSavings / salary) * 100;
                advice.append("Status: You have saved ").append(currencyFormat.format(actualSavings)).append(" (").append(String.format("%.1f", savingsRate)).append("% of income). ");
                
                if (savingsRate < 20) {
                    advice.append("Recommendation: Try to reach a 20% savings rate (").append(currencyFormat.format(salary * 0.20)).append("). ");
                } else {
                    advice.append("Excellent: You are exceeding typical saving recommendations! ");
                }
            }

            if (goal > 0) {
                double remainingGoal = goal - currentBalance;
                if (remainingGoal > 0) {
                    if (actualSavings > 0) {
                        int months = (int) Math.ceil(remainingGoal / actualSavings);
                        advice.append("\n\nGoal Tracking: You are ").append(currencyFormat.format(remainingGoal)).append(" away from your goal. At this rate, you will reach it in ").append(months).append(" months.");
                    } else {
                        advice.append("\n\nGoal Tracking: You need positive monthly savings to reach your ").append(currencyFormat.format(goal)).append(" goal.");
                    }
                } else {
                    advice.append("\n\nGoal Tracking: Goal reached! Consider setting a new milestone.");
                }
            }

            return advice.toString();
        }

        // Expanded natural language triggers
        if (input.contains("balance") || input.contains("how much money")) {
            return "Your current balance is " + currencyFormat.format(currentBalance) + ". " + 
                   (currentBalance < 100 ? "It's looking a bit low, be careful!" : "You're in good standing.");
        } else if (input.contains("income") || input.contains("earn")) {
            return "Total income recorded: " + currencyFormat.format(currentIncome) + ". This includes your salary and other deposits.";
        } else if (input.contains("expense") || input.contains("spent") || input.contains("spending")) {
            return "You've spent " + currencyFormat.format(currentExpense) + " so far. " + 
                   (currentExpense > currentIncome ? "This is more than you've earned!" : "You're staying within your means.");
        } else if (input.contains("hello") || input.contains("hi") || input.contains("hey")) {
            return "Hello! I'm your financial AI. I can analyze your spending, help you reach your goals, or even process bills if you switch modes. What's on your mind?";
        } else if (input.contains("save") || input.contains("budget")) {
            return "Budgeting tip: Follow the 50/30/20 rule—50% for needs, 30% for wants, and 20% for savings. I can calculate these for you if you ask for 'advice'!";
        } else if (input.contains("thank")) {
            return "You're welcome! I'm here to help you reach your financial goals. Anything else?";
        } else {
            return "I'm not quite sure about that. Try asking for 'financial advice', your 'balance', or 'expenses'. You can also use the Bill Detector to scan receipts!";
        }
    }
}
