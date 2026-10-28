package com.mowtiie.dueit.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mowtiie.dueit.data.database.DueItDatabase;
import com.mowtiie.dueit.databinding.ActivityRegisterBinding;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    ActivityRegisterBinding binding;
    DueItDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new DueItDatabase(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> {
            String username = Objects.requireNonNull(binding.usernameInput.getText()).toString().trim();
            String email = Objects.requireNonNull(binding.emailInput.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.passwordInput.getText()).toString();
            String confirmPassword = Objects.requireNonNull(binding.confirmPasswordInput.getText()).toString();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "All fields are required!", Toast.LENGTH_SHORT).show();
            } else {
                if (!username.matches("[A-Za-z0-9_.]{3,30}")) {
                    Toast.makeText(RegisterActivity.this, "Username must be 3-30 letters, numbers, dots or underscores!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (email.length() > 100 || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(RegisterActivity.this, "Enter a valid email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 16 || password.length() > 64) {
                    Toast.makeText(RegisterActivity.this, "Password must be 16-64 characters!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Passwords must match!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (database.register(username, email, password)) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "User already exists!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
