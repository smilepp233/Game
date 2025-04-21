package com.example.groupproject_game;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MathSpeedChallengeActivity extends AppCompatActivity {
    private TextView equationText, timerText, scoreText;
    private ProgressBar readyProgressBar;
    private MaterialButton[] answerButtons;
    private int correctAnswer, currentScore = 0;
    private CountDownTimer gameTimer;
    private CountDownTimer readyTimer;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_speed);

        initializeViews();
        // Start the 5-second ready countdown before the game starts
        startReadyCountdown();
    }

    private void initializeViews() {
        equationText = findViewById(R.id.equationText);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        readyProgressBar = findViewById(R.id.readyProgressBar);

        answerButtons = new MaterialButton[]{
            findViewById(R.id.btnAnswer1),
            findViewById(R.id.btnAnswer2),
            findViewById(R.id.btnAnswer3),
            findViewById(R.id.btnAnswer4)
        };

        for (int i = 0; i < answerButtons.length; i++) {
            final int buttonIndex = i;
            answerButtons[i].setOnClickListener(v -> checkAnswer(buttonIndex));
        }

        userManager = new UserManager(this);
    }

    /**
     * Starts a 5-second ready countdown with a progress bar.
     */
    private void startReadyCountdown() {
        // Display "Get Ready!" instead of an equation
        equationText.setText("Get Ready!");
        // Reset the progress bar (assumes max is set to 5 in XML)
        readyProgressBar.setProgress(0);

        readyTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsPassed = (int) ((5000 - millisUntilFinished) / 1000);
                readyProgressBar.setProgress(secondsPassed);
            }

            @Override
            public void onFinish() {
                readyProgressBar.setProgress(5);
                startNewRound();
            }
        }.start();
    }

    private void startNewRound() {
        generateEquation();
        startGameTimer(10000); // 10 seconds per round
    }

    private void generateEquation() {
        Random random = new Random();
        int difficulty = currentScore / 50 + 1; // Increase difficulty with score

        int num1 = random.nextInt(difficulty * 10) + 1;
        int num2 = random.nextInt(difficulty * 10) + 1;

        // Randomly choose an operation
        int operation = random.nextInt(3);
        String equation;
        switch (operation) {
            case 0: // Addition
                correctAnswer = num1 + num2;
                equation = num1 + " + " + num2 + " = ?";
                break;
            case 1: // Subtraction
                correctAnswer = num1 - num2;
                equation = num1 + " - " + num2 + " = ?";
                break;
            default: // Multiplication
                correctAnswer = num1 * num2;
                equation = num1 + " × " + num2 + " = ?";
                break;
        }

        equationText.setText(equation);
        setAnswerOptions(correctAnswer);
    }

    private void setAnswerOptions(int correctAnswer) {
        Random random = new Random();
        List<Integer> options = new ArrayList<>();
        options.add(correctAnswer);

        // Generate 3 incorrect options
        while (options.size() < 4) {
            int wrongAnswer = correctAnswer + random.nextInt(10) - 5;
            if (!options.contains(wrongAnswer)) {
                options.add(wrongAnswer);
            }
        }

        // Shuffle options and set button texts
        Collections.shuffle(options);
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(String.valueOf(options.get(i)));
        }
    }

    private void checkAnswer(int buttonIndex) {
        int selectedAnswer = Integer.parseInt(answerButtons[buttonIndex].getText().toString());
        if (selectedAnswer == correctAnswer) {
            // Correct answer: Increase score and start a new round
            currentScore += 10;
            scoreText.setText("Score: " + currentScore);
            startNewRound();
        } else {
            gameOver();
        }
    }

    private void startGameTimer(long milliseconds) {
        if (gameTimer != null) gameTimer.cancel();

        gameTimer = new CountDownTimer(milliseconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                gameOver();
            }
        }.start();
    }

    private void gameOver() {
        if (gameTimer != null) gameTimer.cancel();
        Toast.makeText(this, "Game Over! Score: " + currentScore, Toast.LENGTH_LONG).show();
        // Send score to database if a user is logged in
        UserManager.User currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            // Example: update stage 3 progress with the current score; adjust stage/time as needed
            userManager.updateStageProgress(currentUser.username, 3, currentScore, 10000);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) gameTimer.cancel();
        if (readyTimer != null) readyTimer.cancel();
    }
}
