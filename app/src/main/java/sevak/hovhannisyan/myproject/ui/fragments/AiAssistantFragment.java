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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

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
import sevak.hovhannisyan.myproject.BuildConfig;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
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

    private static final String MY_TAG = "AiAssistant";
    
    private static final String KEY = BuildConfig.OPENROUTER_API_KEY;
    private static final String CHAT_MODEL = "openai/gpt-4o-mini"; 
    private static final String FREE_MODEL = "google/gemini-flash-1.5-8b:free"; 

    @Inject
    OpenRouterService router;

    private MainViewModel mainVm;
    private RecyclerView chatList;
    private EditText input;
    private View send;
    private MaterialButton toggle;
    private View btnHistory;
    private ChipGroup chips;
    private ChatAdapter adapter;
    private NumberFormat money;
    
    private boolean scannerActive = false;
    private long lastSessionId = -1L;

    // Detected receipt data
    private double foundAmt = 0.0;
    private String foundCat = "Unknown";

    // App data for AI context
    private Double bal = 0.0;
    private Double inc = 0.0;
    private Double exp = 0.0;
    private List<Transaction> history = new ArrayList<>();
    private List<FinnhubResponse> marketData = new ArrayList<>();
    private Map<String, Object> userProfile;

    private final ActivityResultLauncher<String> imgPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    runScan(uri, CHAT_MODEL);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        money = NumberFormat.getCurrencyInstance(Locale.US);

        chatList = view.findViewById(R.id.chat_recycler);
        input = view.findViewById(R.id.et_message);
        send = view.findViewById(R.id.btn_send);
        toggle = view.findViewById(R.id.btn_mode_toggle);
        chips = view.findViewById(R.id.suggestion_chips);
        btnHistory = view.findViewById(R.id.btn_chat_history);

        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        chatList.setLayoutManager(layoutManager);
        chatList.setAdapter(adapter);

        // Session observation
        mainVm.getCurrentSessionId().observe(getViewLifecycleOwner(), id -> {
            if (id == -1L) {
                // If no session, create a default one
                mainVm.createNewSession("New Chat " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), null);
            }
            lastSessionId = id;
        });

        mainVm.getCurrentSessionMessages().observe(getViewLifecycleOwner(), msgs -> {
            if (msgs != null) {
                adapter.setMessages(new ArrayList<>(msgs));
                if (!msgs.isEmpty()) {
                    chatList.scrollToPosition(msgs.size() - 1);
                } else if (lastSessionId != -1L) {
                    // Only post greeting for a brand new session
                    post(new ChatMessage(getString(R.string.ai_online), false));
                }
            }
        });

        btnHistory.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_aiAssistantFragment_to_chatHistoryFragment));

        startSync();
        handleButtons();
        handleChips();
    }

    private void handleButtons() {
        toggle.setOnClickListener(v -> {
            PopupMenu pop = new PopupMenu(requireContext(), toggle);
            pop.getMenu().add(getString(R.string.ai_mode_intelligence));
            pop.getMenu().add(getString(R.string.ai_mode_scanner));
            pop.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals(getString(R.string.ai_mode_intelligence))) {
                    setMode(false);
                } else {
                    setMode(true);
                }
                return true;
            });
            pop.show();
        });

        send.setOnClickListener(v -> {
            if (!scannerActive) {
                String text = input.getText().toString().trim();
                talkToAi(text);
            } else {
                imgPicker.launch("image/*");
            }
        });
    }

    private void handleChips() {
        if (chips == null) return;
        
        View summary = chips.findViewById(R.id.chip_summary);
        if (summary != null) summary.setOnClickListener(v -> talkToAi(getString(R.string.prompt_summary)));
        
        View tips = chips.findViewById(R.id.chip_tips);
        if (tips != null) tips.setOnClickListener(v -> talkToAi(getString(R.string.prompt_tips)));
        
        View status = chips.findViewById(R.id.chip_status);
        if (status != null) status.setOnClickListener(v -> talkToAi(getString(R.string.prompt_status)));
        
        View market = chips.findViewById(R.id.chip_market);
        if (market != null) market.setOnClickListener(v -> talkToAi(getString(R.string.prompt_market)));
    }

    private void setMode(boolean scan) {
        scannerActive = scan;
        if (!scan) {
            toggle.setIconResource(android.R.drawable.ic_menu_info_details);
            input.setVisibility(View.VISIBLE);
            if (chips != null) chips.setVisibility(View.VISIBLE);
        } else {
            toggle.setIconResource(android.R.drawable.ic_menu_camera);
            input.setVisibility(View.GONE);
            if (chips != null) chips.setVisibility(View.GONE);
        }
    }

    private void talkToAi(String text) {
        if (text.isEmpty()) return;

        post(new ChatMessage(text, true));
        input.setText("");
        
        String low = text.toLowerCase().trim();
        if ((isMatch(low, "save it") || isMatch(low, "add it") 
                || isMatch(low, "պահպանել") || isMatch(low, "ավելացնել")
                || isMatch(low, "сохранить") || isMatch(low, "добавить")) 
                && foundAmt > 0) {
            saveResult();
            return;
        }

        post(new ChatMessage(getString(R.string.ai_synchronizing), false));
        callRouter(text, CHAT_MODEL);
    }

    private boolean isMatch(String s, String target) {
        if (s.contains(target)) return true;
        if (target.length() > 4) {
            String root = target.substring(0, target.length() - 2);
            return s.contains(root);
        }
        return false;
    }

    private void saveResult() {
        Transaction t = new Transaction();
        t.setAmount(foundAmt);
        t.setCategory(foundCat);
        t.setType(TransactionType.EXPENSE);
        t.setDate(new Date());
        t.setDescription("SAVE AI Scan (" + foundCat + ")");
        
        mainVm.addNewTransaction(t);
        
        foundAmt = 0.0;
        post(new ChatMessage(getString(R.string.ai_ledger_updated), false));
    }

    private void callRouter(String p, String model) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String date = sdf.format(new Date());
        
        StringBuilder hist = new StringBuilder("TRANSACTION HISTORY:\n");
        int c = 0;
        List<Transaction> list = new ArrayList<>(history);
        Collections.reverse(list);
        for (Transaction t : list) {
            if (c >= 20) break;
            hist.append(String.format("- %s: %s %.2f (%s) - %s\n", 
                sdf.format(t.getDate()), t.getType(), t.getAmount(), t.getCategory(), t.getDescription()));
            c++;
        }

        StringBuilder pulse = new StringBuilder("\nMARKET PULSE (LIVE PRICES):\n");
        if (marketData != null && !marketData.isEmpty()) {
            for (FinnhubResponse q : marketData) {
                pulse.append(String.format("- %s: $%.2f (Change: %.2f%%)\n", q.getSymbol(), q.getCurrentPrice(), q.getPercentChange()));
            }
        } else {
            pulse.append("- Live market data is currently unavailable.\n");
        }

        String user = "N/A";
        if (userProfile != null) {
            user = String.format("Salary: %s, Fixed: %s, Goal: %s",
                userProfile.getOrDefault("salary", 0),
                userProfile.getOrDefault("fixedExpenses", 0),
                userProfile.getOrDefault("goalAmount", 0));
        }

        String prompt = String.format(Locale.US, 
            "Professional Advisor. Current Status: [Date: %s] [Bal: %.2f] [Inc: %.2f] [Exp: %.2f]\n" +
            "Profile: %s\n%s\n%s\n" +
            "Respond in the same language as the user. Use the market pulse info if asked about stocks/crypto. Be helpful.",
            date, bal, inc, exp, user, hist.toString(), pulse.toString());
        
        List<OpenRouterRequest.Message> msgs = new ArrayList<>();
        msgs.add(new OpenRouterRequest.Message("system", prompt));
        
        List<ChatMessage> chatContext = mainVm.getCurrentSessionMessages().getValue();
        if (chatContext != null) {
            int start = Math.max(0, chatContext.size() - 6);
            for (int i = start; i < chatContext.size(); i++) {
                ChatMessage cm = chatContext.get(i);
                if (cm.getType() == ChatMessage.Type.TEXT && !cm.getText().equals(getString(R.string.ai_synchronizing))) {
                    msgs.add(new OpenRouterRequest.Message(cm.isUser() ? "user" : "assistant", cm.getText()));
                }
            }
        }
        
        msgs.add(new OpenRouterRequest.Message("user", p));

        OpenRouterRequest req = new OpenRouterRequest(model, msgs);

        router.analyzeBill("Bearer " + KEY, "https://saveapp.ai", "SAVE AI", req)
            .enqueue(new Callback<OpenRouterResponse>() {
                @Override
                public void onResponse(@NonNull Call<OpenRouterResponse> call, @NonNull Response<OpenRouterResponse> res) {
                    if (res.isSuccessful() && res.body() != null && !res.body().getChoices().isEmpty()) {
                        finish(res.body().getChoices().get(0).getMessage().getContent());
                    } else if (res.code() >= 400 && !model.equals(FREE_MODEL)) {
                        callRouter(p, FREE_MODEL);
                    } else {
                        toast("Server error: " + res.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OpenRouterResponse> call, @NonNull Throwable t) {
                    if (!model.equals(FREE_MODEL)) callRouter(p, FREE_MODEL);
                    else toast("Connection failed.");
                }
            });
    }

    private void finish(String s) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            mainVm.removeLastChatMessage();
            post(new ChatMessage(s, false));
        });
    }

    private void runScan(Uri uri, String model) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            if (model.equals(CHAT_MODEL)) {
                post(new ChatMessage(getString(R.string.ai_scanning_highres), false));
            }
        });
        
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            
            if (b == null) {
                post(new ChatMessage("Bad image file.", false));
                return;
            }
            
            int target = 1024;
            Bitmap sm = Bitmap.createScaledBitmap(b, target, (int)((float)target * b.getHeight() / b.getWidth()), true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sm.compress(Bitmap.CompressFormat.JPEG, 80, out);
            String b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);

            String cats = String.format("Cats: %s, %s, %s, %s, %s, %s",
                    getString(R.string.cat_food), getString(R.string.cat_transport), getString(R.string.cat_shopping),
                    getString(R.string.cat_entertainment), getString(R.string.cat_health), getString(R.string.cat_utilities));

            List<OpenRouterRequest.Content> list = new ArrayList<>();
            String instructions = "Receipt scan. JSON only: {\"total\": 0.0, \"category\": \"Cat\"}. EN, RU, AM. Keywords: 'Ընդամենը', 'Գումար', 'Итого', 'Total'. Categories: " + cats;
            
            list.add(OpenRouterRequest.Content.text(instructions));
            list.add(OpenRouterRequest.Content.image(b64));

            OpenRouterRequest req = new OpenRouterRequest(model, 
                Collections.singletonList(new OpenRouterRequest.Message("user", list)));

            router.analyzeBill("Bearer " + KEY, "https://saveapp.ai", "SAVE AI", req)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OpenRouterResponse> call, @NonNull Response<OpenRouterResponse> res) {
                        if (res.isSuccessful() && res.body() != null && !res.body().getChoices().isEmpty()) {
                            parse(res.body().getChoices().get(0).getMessage().getContent());
                        } else {
                            if (!model.equals(FREE_MODEL)) runScan(uri, FREE_MODEL);
                            else toast("Scan failed.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OpenRouterResponse> call, @NonNull Throwable t) {
                        if (!model.equals(FREE_MODEL)) runScan(uri, FREE_MODEL);
                        else toast("Link error.");
                    }
                });
        } catch (Exception e) {
            toast("Error.");
        }
    }

    private void parse(String s) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                mainVm.removeLastChatMessage();
                
                String part = s;
                if (s.contains("```json")) {
                    part = s.substring(s.indexOf("```json") + 7, s.lastIndexOf("```"));
                } else if (s.contains("{") && s.contains("}")) {
                    part = s.substring(s.indexOf("{"), s.lastIndexOf("}") + 1);
                }
                
                JSONObject jobj = new JSONObject(part.trim());
                foundAmt = jobj.optDouble("total", 0.0);
                foundCat = jobj.optString("category", "Unknown");
                
                String hint = getString(R.string.ai_save_command_hint);
                String msg = getString(R.string.ai_scan_verified, money.format(foundAmt), foundCat, hint);
                post(new ChatMessage(msg, false));
            } catch (Exception e) {
                post(new ChatMessage("Error parsing.", false));
            }
        });
    }

    private void post(ChatMessage m) {
        mainVm.addChatMessage(m);
    }

    private void toast(String s) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show());
        }
    }

    private void startSync() {
        mainVm.getBalance().observe(getViewLifecycleOwner(), val -> bal = val != null ? val : 0.0);
        mainVm.getTotalIncome().observe(getViewLifecycleOwner(), val -> inc = val != null ? val : 0.0);
        mainVm.getTotalExpense().observe(getViewLifecycleOwner(), val -> exp = val != null ? val : 0.0);
        mainVm.getUserData().observe(getViewLifecycleOwner(), data -> userProfile = data);
        mainVm.getAllTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list != null) history = list;
        });
        mainVm.getMarketData().observe(getViewLifecycleOwner(), q -> {
            if (q != null) marketData = q;
        });
    }
}
