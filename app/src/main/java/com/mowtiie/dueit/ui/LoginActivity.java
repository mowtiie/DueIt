package com.mowtiie.dueit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mowtiie.dueit.data.DueItDatabase;
import com.mowtiie.dueit.databinding.ActivityLoginBinding;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    DueItDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new DueItDatabase(this);

        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        binding.btnLogin.setOnClickListener(v -> {
            String username = Objects.requireNonNull(binding.usernameInput.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.passwordInput.getText()).toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Both fields are required!", Toast.LENGTH_SHORT).show();
            } else {
                int studentId = database.login(username, password);
                if (studentId != -1) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                    mainIntent.putExtra(MainActivity.EXTRA_STUDENT_ID, studentId);
                    mainIntent.putExtra(MainActivity.EXTRA_USERNAME, username);
                    startActivity(mainIntent);
                    finish();
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}