package com.example.groupproject_game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private Button loginRedirectButton;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UserManager
        userManager = new UserManager(this);

        // Initialize UI components
        usernameInput = findViewById(R.id.editTextNewUsername);
        passwordInput = findViewById(R.id.editTextNewPassword);
        confirmPasswordInput = findViewById(R.id.editTextConfirmPassword);
        registerButton = findViewById(R.id.buttonRegister);
        loginRedirectButton = findViewById(R.id.buttonLoginRedirect);

        // Register button click listener
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();

                // Validate input
                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check password match
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Attempt registration
                UserManager.RegistrationResult regResult = userManager.registerUser(username, password);
                if (regResult.success) {
                    // Successful registration
                    Toast.makeText(RegisterActivity.this,
                        "Registration Successful!",
                        Toast.LENGTH_SHORT).show();

                    // Navigate to Login Activity
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Failed registration
                    Toast.makeText(RegisterActivity.this,
                        regResult.message,  // Use the message from RegistrationResult
                        Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Redirect to Login Activity
        loginRedirectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}