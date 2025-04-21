package com.example.groupproject_game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerRedirectButton;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UserManager
        userManager = new UserManager(this);

        // Initialize UI components
        usernameInput = findViewById(R.id.editTextUsername);
        passwordInput = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerRedirectButton = findViewById(R.id.buttonRegisterRedirect);

        // Login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                // Validate input
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                        "Please enter username and password",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Attempt login
                UserManager.LoginResult loginResult = userManager.loginUser(username, password);
                if (loginResult.success) {
                    // Successful login
                    Toast.makeText(LoginActivity.this,
                        "Login Successful!",
                        Toast.LENGTH_SHORT).show();

                    // Navigate to MainActivity and pass the username to display it there
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();
                } else {
                    // Failed login
                    Toast.makeText(LoginActivity.this,
                        loginResult.message,  // Use the message from LoginResult
                        Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Redirect to Register Activity
        registerRedirectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
