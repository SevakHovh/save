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

    private MainViewModel viewModel;
    private MaterialButton btnBack, btnChangeTheme, btnChangeLanguage, btnResetPassword, btnClearData, btnPersonalInfo, btnSignOut;
    private MaterialSwitch switchNotifications;
    private LinearLayout layoutPersonalInfo;
    private TextView tvUserEmail;
    private SharedPreferences goalPrefs;
    private static final String PREFS_NAME = "goal_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        goalPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btn_back);
        btnChangeTheme = view.findViewById(R.id.btn_change_theme);
        btnChangeLanguage = view.findViewById(R.id.btn_change_language);
        btnResetPassword = view.findViewById(R.id.btn_reset_password);
        btnClearData = view.findViewById(R.id.btn_clear_data);
        btnPersonalInfo = view.findViewById(R.id.btn_personal_info);
        btnSignOut = view.findViewById(R.id.btn_sign_out);
        switchNotifications = view.findViewById(R.id.switch_notifications);
        layoutPersonalInfo = view.findViewById(R.id.layout_personal_info);
        tvUserEmail = view.findViewById(R.id.tv_user_email);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        btnChangeTheme.setOnClickListener(v -> showThemeDialog());
        
        btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());

        btnPersonalInfo.setOnClickListener(v -> {
            if (layoutPersonalInfo.getVisibility() == View.VISIBLE) {
                layoutPersonalInfo.setVisibility(View.GONE);
                btnPersonalInfo.setText(R.string.account_details_show);
            } else {
                layoutPersonalInfo.setVisibility(View.VISIBLE);
                btnPersonalInfo.setText(R.string.account_details_hide);
            }
        });

        btnResetPassword.setOnClickListener(v -> handlePasswordReset());

        btnClearData.setOnClickListener(v -> showClearDataConfirmation());

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        boolean isEnabled = goalPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        switchNotifications.setChecked(isEnabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            goalPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
            String msg = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            tvUserEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }
    }

    private void showThemeDialog() {
        String[] themes = {getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_dialog_title)
                .setItems(themes, (dialog, which) -> {
                    if (which == 0) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    else if (which == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                })
                .show();
    }

    private void showLanguageDialog() {
        String[] langs = {getString(R.string.language_en), getString(R.string.language_ru), getString(R.string.language_hy)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_dialog_title)
                .setItems(langs, (dialog, which) -> {
                    String langCode = "en";
                    if (which == 1) langCode = "ru";
                    else if (which == 2) langCode = "hy";
                    setLocale(langCode);
                })
                .show();
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());
        
        SharedPreferences.Editor editor = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit();
        editor.putString("My_Lang", langCode);
        editor.apply();

        requireActivity().recreate();
    }

    private void handlePasswordReset() {
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        if (email != null) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showClearDataConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Data")
                .setMessage("Are you sure you want to delete all transactions? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.clearAllTransactions();
                    Toast.makeText(getContext(), "All data cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
