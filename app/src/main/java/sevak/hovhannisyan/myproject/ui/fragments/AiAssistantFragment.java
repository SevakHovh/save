package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.text.NumberFormat;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.ui.GoalManager;
import sevak.hovhannisyan.myproject.ui.adapter.ChatAdapter;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class AiAssistantFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView rvChatMessages;
    private EditText etUserInput;
    private MaterialButton btnSend;
    private ChatAdapter chatAdapter;
    private NumberFormat currencyFormat;

    @Inject
    GoalManager goalManager;

    private Double currentBalance = 0.0;
    private Double currentIncome = 0.0;
    private Double currentExpense = 0.0;

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

        chatAdapter = new ChatAdapter();
        rvChatMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChatMessages.setAdapter(chatAdapter);

        // Add welcome message
        chatAdapter.addMessage(new ChatMessage("Hello! I'm your SAVE AI assistant. How can I help you with your finances today?", false));

        observeData();

        btnSend.setOnClickListener(v -> {
            String question = etUserInput.getText().toString().trim();
            if (!question.isEmpty()) {
                chatAdapter.addMessage(new ChatMessage(question, true));
                etUserInput.setText("");
                rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                
                // Process AI Response with a small delay for "thinking" feel
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    String answer = generateAiResponse(question.toLowerCase());
                    chatAdapter.addMessage(new ChatMessage(answer, false));
                    rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }, 1000);
            }
        });
    }

    private void observeData() {
        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> currentBalance = balance != null ? balance : 0.0);
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> currentIncome = income != null ? income : 0.0);
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> currentExpense = expense != null ? expense : 0.0);
    }

    private String generateAiResponse(String input) {
        double salary = goalManager.getSalary();
        double fixedExpenses = goalManager.getFixedExpenses();
        double goal = goalManager.getGoalAmount();

        if (input.contains("advice") || input.contains("analyze") || input.contains("conclude") || input.contains("help")) {
            if (salary <= 0) {
                return "I'd love to give you financial advice, but I don't know your salary yet! Please go to your Profile and enter your monthly income.";
            }

            StringBuilder advice = new StringBuilder();
            double disposableIncome = salary - fixedExpenses;
            double actualSavings = currentIncome - currentExpense;

            advice.append("Based on your profile, your disposable income after fixed expenses is ").append(currencyFormat.format(disposableIncome)).append(". ");

            if (actualSavings < 0) {
                advice.append("Currently, your expenses are exceeding your income by ").append(currencyFormat.format(Math.abs(actualSavings))).append(". You should review your variable spending immediately. ");
            } else if (actualSavings < (disposableIncome * 0.2)) {
                advice.append("You are saving about ").append(currencyFormat.format(actualSavings)).append(" this month. Try to aim for 20% of your disposable income (").append(currencyFormat.format(disposableIncome * 0.2)).append(") for better financial security. ");
            } else {
                advice.append("Great job! You are saving ").append(currencyFormat.format(actualSavings)).append(", which is a healthy portion of your income. ");
            }

            if (goal > 0) {
                double remainingGoal = goal - currentBalance;
                if (remainingGoal > 0) {
                    if (actualSavings > 0) {
                        int monthsToGoal = (int) Math.ceil(remainingGoal / actualSavings);
                        advice.append("At your current rate, you'll reach your ").append(currencyFormat.format(goal)).append(" saving goal in about ").append(monthsToGoal).append(" months.");
                    } else {
                        advice.append("To reach your ").append(currencyFormat.format(goal)).append(" goal, you'll need to start saving at least some of your income each month.");
                    }
                } else {
                    advice.append("Congratulations! You have reached your saving goal.");
                }
            }

            return advice.toString();
        }

        if (input.contains("balance")) {
            return "Your current total balance is " + currencyFormat.format(currentBalance) + ".";
        } else if (input.contains("income")) {
            return "You've earned a total of " + currencyFormat.format(currentIncome) + " so far.";
        } else if (input.contains("expense") || input.contains("spent")) {
            return "Your total expenses are " + currencyFormat.format(currentExpense) + ".";
        } else if (input.contains("hello") || input.contains("hi")) {
            return "Hi there! I can help you check your balance, total income, or total expenses. You can also ask for 'advice' to get a financial analysis!";
        } else if (input.contains("save") || input.contains("goal")) {
            return "To save more, try to minimize your daily expenses and set a clear goal in the Dashboard!";
        } else {
            return "I'm not sure I understand. You can ask me about your balance, income, expenses, or ask for 'advice' based on your profile.";
        }
    }
}
