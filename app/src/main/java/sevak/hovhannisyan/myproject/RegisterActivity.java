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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    @Inject
    FirebaseAuth mAuth;

    private EditText etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilEmail;
    private Button btnRegister, btnCheckVerification, btnResend, btnBack;
    private LinearLayout llDetails, llVerification;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilEmail = findViewById(R.id.til_email);

        btnRegister = findViewById(R.id.btn_register);
        btnCheckVerification = findViewById(R.id.btn_check_verification);
        btnResend = findViewById(R.id.btn_resend_email);
        btnBack = findViewById(R.id.btn_back_to_register);

        llDetails = findViewById(R.id.ll_details);
        llVerification = findViewById(R.id.ll_verification);

        btnRegister.setOnClickListener(v -> registerUser());
        
        btnCheckVerification.setOnClickListener(v -> checkEmailVerification());
        
        btnResend.setOnClickListener(v -> resendVerificationEmail());
        
        btnBack.setOnClickListener(v -> {
            llVerification.setVisibility(View.GONE);
            llDetails.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.tv_login_prompt).setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            return;
        } else {
            tilEmail.setError(null);
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Log.e(TAG, "Registration error: " + error);
                        Toast.makeText(RegisterActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Verification email sent to " + user.getEmail());
                Toast.makeText(RegisterActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                llDetails.setVisibility(View.GONE);
                llVerification.setVisibility(View.VISIBLE);
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Log.e(TAG, "Failed to send verification email: " + error);
                Toast.makeText(RegisterActivity.this, "Failed to send verification email: " + error, Toast.LENGTH_LONG).show();
                // Even if sending fails, show the verification screen so they can try resending
                llDetails.setVisibility(View.GONE);
                llVerification.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    Toast.makeText(RegisterActivity.this, "Email verified! Welcome.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
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
}
