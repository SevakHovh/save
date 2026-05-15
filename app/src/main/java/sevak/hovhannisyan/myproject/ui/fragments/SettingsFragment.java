package sevak.hovhannisyan.myproject.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

import sevak.hovhannisyan.myproject.LoginActivity;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

public class SettingsFragment extends Fragment {

    private MainViewModel mainVm;
    private MaterialButton backBtn, themeBtn, langBtn, resetBtn, clearBtn, infoBtn, logoutBtn;
    private MaterialSwitch notifySwitch;
    private LinearLayout infoLayout;
    private TextView emailText;
    private SharedPreferences prefs;
    
    private static final String PREF_FILE = "goal_prefs";
    private static final String NOTIFY_KEY = "notifications_enabled";

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        mainVm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        prefs = requireContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        // Use 'state' here because that is what I named the parameter
        super.onViewCreated(view, state);

        // Wiring up the UI elements
        backBtn = view.findViewById(R.id.btn_back);
        themeBtn = view.findViewById(R.id.btn_change_theme);
        langBtn = view.findViewById(R.id.btn_change_language);
        resetBtn = view.findViewById(R.id.btn_reset_password);
        clearBtn = view.findViewById(R.id.btn_clear_data);
        infoBtn = view.findViewById(R.id.btn_personal_info);
        logoutBtn = view.findViewById(R.id.btn_sign_out);
        notifySwitch = view.findViewById(R.id.switch_notifications);
        infoLayout = view.findViewById(R.id.layout_personal_info);
        emailText = view.findViewById(R.id.tv_user_email);

        backBtn.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        themeBtn.setOnClickListener(v -> pickTheme());
        
        langBtn.setOnClickListener(v -> pickLanguage());

        infoBtn.setOnClickListener(v -> {
            if (infoLayout.getVisibility() == View.VISIBLE) {
                infoLayout.setVisibility(View.GONE);
                infoBtn.setText(R.string.account_details_show);
            } else {
                infoLayout.setVisibility(View.VISIBLE);
                infoBtn.setText(R.string.account_details_hide);
            }
        });

        resetBtn.setOnClickListener(v -> doPasswordReset());

        clearBtn.setOnClickListener(v -> confirmClear());

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // Load the toggle state
        boolean on = prefs.getBoolean(NOTIFY_KEY, true);
        notifySwitch.setChecked(on);

        notifySwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(NOTIFY_KEY, isChecked).apply();
            String msg = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // Show user email
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            emailText.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }
    }

    private void pickTheme() {
        String[] options = {getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_dialog_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    else if (which == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                })
                .show();
    }

    private void pickLanguage() {
        String[] list = {getString(R.string.language_en), getString(R.string.language_ru), getString(R.string.language_hy)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_dialog_title)
                .setItems(list, (dialog, which) -> {
                    String code = "en";
                    if (which == 1) code = "ru";
                    else if (which == 2) code = "hy";
                    updateLocale(code);
                })
                .show();
    }

    private void updateLocale(String code) {
        Locale l = new Locale(code);
        Locale.setDefault(l);
        Configuration c = new Configuration();
        c.setLocale(l);
        requireContext().getResources().updateConfiguration(c, requireContext().getResources().getDisplayMetrics());
        
        SharedPreferences.Editor ed = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit();
        ed.putString("My_Lang", code);
        ed.apply();

        requireActivity().recreate();
    }

    private void doPasswordReset() {
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        if (email != null) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Reset link sent!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Data")
                .setMessage("Delete everything? This cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mainVm.clearAllData();
                    Toast.makeText(getContext(), "Done.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
