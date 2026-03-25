package sevak.hovhannisyan.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    @Inject
    FirebaseAuth mAuth;

    private EditText etEmail, etPassword;
    private TextInputLayout tilEmail;
    private Button btnLogin, btnCheckVerification, btnResend, btnBack;
    private TextView tvForgotPassword;
    private LinearLayout llCredentials, llVerification;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tilEmail = findViewById(R.id.til_email);
        
        btnLogin = findViewById(R.id.btn_login);
        btnCheckVerification = findViewById(R.id.btn_check_verification);
        btnResend = findViewById(R.id.btn_resend_email);
        btnBack = findViewById(R.id.btn_back_to_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        
        llCredentials = findViewById(R.id.ll_credentials);
        llVerification = findViewById(R.id.ll_verification);

        btnLogin.setOnClickListener(v -> loginUser());
        
        btnCheckVerification.setOnClickListener(v -> checkEmailVerification());
        
        btnResend.setOnClickListener(v -> resendVerificationEmail());
        
        btnBack.setOnClickListener(v -> {
            llVerification.setVisibility(View.GONE);
            llCredentials.setVisibility(View.VISIBLE);
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.tv_register_prompt).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            return;
        } else {
            tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                navigateToMain();
                            } else {
                                sendVerificationEmail(user);
                            }
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Log.e(TAG, "Login error: " + error);
                        Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        final EditText resetEmailInput = new EditText(this);
        resetEmailInput.setHint("Email Address");
        resetEmailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Add margins to the EditText in the dialog
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(64, 20, 64, 0);
        resetEmailInput.setLayoutParams(params);
        container.addView(resetEmailInput);
        
        // Pre-fill email if user already typed it in the login field
        String currentEmail = etEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            resetEmailInput.setText(currentEmail);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email to receive a password reset link.")
                .setView(container)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = resetEmailInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Password reset email sent successfully to " + email);
                                        Toast.makeText(LoginActivity.this, "Reset link sent! Please check your inbox.", Toast.LENGTH_LONG).show();
                                    } else {
                                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                        Log.e(TAG, "Failed to send reset email: " + error);
                                        Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(LoginActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Verification email sent to " + user.getEmail());
                Toast.makeText(this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                llCredentials.setVisibility(View.GONE);
                llVerification.setVisibility(View.VISIBLE);
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Log.e(TAG, "Failed to send verification email: " + error);
                Toast.makeText(this, "Failed to send verification email: " + error, Toast.LENGTH_LONG).show();
                llCredentials.setVisibility(View.GONE);
                llVerification.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    Toast.makeText(this, "Verified! Welcome back.", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    Toast.makeText(this, "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            sendVerificationEmail(user);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
