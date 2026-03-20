package sevak.hovhannisyan.myproject.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.ui.ThemeManager;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    @Inject
    ThemeManager themeManager;

    private MaterialButton btnChangeTheme;
    private MaterialButton btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnChangeTheme = view.findViewById(R.id.btn_change_theme);
        btnBack = view.findViewById(R.id.btn_back);

        btnChangeTheme.setOnClickListener(v -> {
            showThemeSelectionDialog();
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Navigation.findNavController(v).navigateUp();
            });
        }
    }

    private void showThemeSelectionDialog() {
        String[] options = {getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system)};
        int checkedItem = -1;
        int currentMode = themeManager.getSavedMode();
        
        if (currentMode == ThemeManager.MODE_LIGHT) checkedItem = 0;
        else if (currentMode == ThemeManager.MODE_DARK) checkedItem = 1;
        else if (currentMode == ThemeManager.MODE_SYSTEM) checkedItem = 2;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    int mode;
                    if (which == 0) mode = ThemeManager.MODE_LIGHT;
                    else if (which == 1) mode = ThemeManager.MODE_DARK;
                    else mode = ThemeManager.MODE_SYSTEM;
                    
                    themeManager.setTheme(mode);
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .show();
    }
}
