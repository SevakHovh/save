package sevak.hovhannisyan.myproject.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.LoginActivity;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.di.AppModule;
import sevak.hovhannisyan.myproject.ui.ThemeManager;
import sevak.hovhannisyan.myproject.ui.viewmodel.MainViewModel;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    @Inject
    ThemeManager themeManager;

    @Inject
    FirebaseAuth mAuth;

    @Inject
    @Named(AppModule.GOAL_PREFS)
    SharedPreferences goalPrefs;

    private MainViewModel viewModel;

    private MaterialButton btnBack;
    private MaterialButton btnChangeTheme;
    private MaterialButton btnPersonalInfo;
    private MaterialButton btnSignOut;
    private MaterialButton btnResetPassword;
    private MaterialButton btnClearData;
    private MaterialSwitch switchNotifications;
    private LinearLayout layoutPersonalInfo;
    private TextView tvUserEmail;

    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        btnBack = view.findViewById(R.id.btn_back);
        btnChangeTheme = view.findViewById(R.id.btn_change_theme);
        btnPersonalInfo = view.findViewById(R.id.btn_personal_info);
        btnSignOut = view.findViewById(R.id.btn_sign_out);
        btnResetPassword = view.findViewById(R.id.btn_reset_password);
        btnClearData = view.findViewById(R.id.btn_clear_data);
        switchNotifications = view.findViewById(R.id.switch_notifications);
        layoutPersonalInfo = view.findViewById(R.id.layout_personal_info);
        tvUserEmail = view.findViewById(R.id.tv_user_email);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        btnChangeTheme.setOnClickListener(v -> showThemeDialog());

        btnPersonalInfo.setOnClickListener(v -> {
            if (layoutPersonalInfo.getVisibility() == View.VISIBLE) {
                layoutPersonalInfo.setVisibility(View.GONE);
                btnPersonalInfo.setText("Account Details");
            } else {
                layoutPersonalInfo.setVisibility(View.VISIBLE);
                btnPersonalInfo.setText("Hide Account Details");
            }
        });

        btnResetPassword.setOnClickListener(v -> handlePasswordReset());

        btnClearData.setOnClickListener(v -> showClearDataConfirmation());

        // Load saved notification preference
        boolean isEnabled = goalPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        switchNotifications.setChecked(isEnabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            goalPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
            String msg = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        setupUserInfo();
    }

    private void setupUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
        }
    }

    private void handlePasswordReset() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Password reset email sent to " + user.getEmail(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showClearDataConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Transactions?")
                .setMessage("This will permanently delete your transaction history. Note: Your starting balance/salary settings will remain.")
                .setPositiveButton("Clear Transactions", (dialog, which) -> {
                    viewModel.clearAllTransactions();
                    Toast.makeText(requireContext(), "Transactions cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeDialog() {
        String[] themes = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
        };

        int currentMode = themeManager.getSavedMode();
        int checkedItem = 2; 
        if (currentMode == ThemeManager.MODE_LIGHT) checkedItem = 0;
        else if (currentMode == ThemeManager.MODE_DARK) checkedItem = 1;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    int mode;
                    switch (which) {
                        case 0: mode = ThemeManager.MODE_LIGHT; break;
                        case 1: mode = ThemeManager.MODE_DARK; break;
                        default: mode = ThemeManager.MODE_SYSTEM; break;
                    }
                    themeManager.setTheme(mode);
                    dialog.dismiss();
                })
                .show();
    }
}
