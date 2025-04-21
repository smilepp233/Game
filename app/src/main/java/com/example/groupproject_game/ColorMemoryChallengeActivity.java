package com.example.groupproject_game;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ColorMemoryChallengeActivity extends AppCompatActivity {
    private List<View> colorButtons;
    private List<Integer> colorSequence;
    private List<Integer> buttonColors;
    private List<Integer> playerSequence;
    private TextView instructionText, scoreText, timerText;
    private int currentScore = 0, sequenceLength = 3;
    private CountDownTimer gameTimer;
    private CountDownTimer readyTimer; // Ready countdown timer
    private boolean isGameActive = false; // Flag to track if game is active
    private static final int MAX_SEQUENCE_LENGTH = 10; // Maximum sequence length
    private static final int MAX_ROUNDS = 8; // Maximum rounds
    private int currentRound = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_memory);

        initializeViews();
        // Start the 5-second ready countdown before the game starts
        startReadyCountdown();
    }

    private void initializeViews() {
        colorButtons = new ArrayList<>();
        int[] buttonIds = {R.id.colorBtn1, R.id.colorBtn2, R.id.colorBtn3, R.id.colorBtn4};

        instructionText = findViewById(R.id.instructionText);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);

        for (int buttonId : buttonIds) {
            View btn = findViewById(buttonId);
            btn.setOnClickListener(v -> handleColorPress(btn));
            colorButtons.add(btn);
        }
    }

    /**
     * Starts a 5-second ready countdown before showing the color sequence.
     */
    private void startReadyCountdown() {
        // Reset game state
        isGameActive = false;
        currentRound = 1;
        sequenceLength = 3;
        currentScore = 0;
        scoreText.setText("Score: 0");
        
        // Cancel any existing timers
        if (readyTimer != null) {
            readyTimer.cancel();
        }
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        instructionText.setText("Get Ready! 5");
        readyTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Calculate remaining seconds (add 1 to show "5" at start)
                int secondsRemaining = (int) (millisUntilFinished / 1000) + 1;
                instructionText.setText("Get Ready! " + secondsRemaining);
            }

            @Override
            public void onFinish() {
                instructionText.setText("Watch the color sequence!");
                isGameActive = true;
                startNewRound();
            }
        }.start();
    }

    /**
     * Assign colors to buttons from the same set used in the sequence.
     */
    private void assignButtonColors() {
        buttonColors = new ArrayList<>();
        int[] possibleColors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        for (int color : possibleColors) {
            buttonColors.add(color);
        }
        Collections.shuffle(buttonColors); // Randomize color assignment
    }

    private void startNewRound() {
        // Safety check to prevent infinite rounds
        if (!isGameActive || currentRound > MAX_ROUNDS || sequenceLength > MAX_SEQUENCE_LENGTH) {
            completeGame();
            return;
        }
        
        colorSequence = generateColorSequence(sequenceLength);
        playerSequence = new ArrayList<>();
        assignButtonColors(); // Assign colors to buttons
        showColorSequence();
        startGameTimer(20000);
    }

    /**
     * Generate a random sequence using only the four button colors.
     */
    private List<Integer> generateColorSequence(int length) {
        List<Integer> colors = new ArrayList<>();
        // Use only these four colors to match the buttons
        int[] possibleColors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            colors.add(possibleColors[random.nextInt(possibleColors.length)]);
        }
        return colors;
    }

    private void showColorSequence() {
        // Disable player input during sequence display
        disablePlayerInput();
        
        // Display the sequence one by one on the corresponding buttons.
        for (int i = 0; i < colorSequence.size(); i++) {
            int finalI = i;
            View btn = colorButtons.get(finalI % colorButtons.size()); // Prevent index out of bounds
            btn.postDelayed(() -> {
                if (!isGameActive) return; // Skip if game no longer active
                btn.setBackgroundColor(colorSequence.get(finalI));
                btn.postDelayed(() -> {
                    if (!isGameActive) return;
                    btn.setBackgroundColor(Color.LTGRAY);
                }, 500);
            }, i * 800);
        }

        // After the sequence is shown, allow the player to input their sequence.
        colorButtons.get(0).postDelayed(() -> {
            if (!isGameActive) return; // Skip if game no longer active
            instructionText.setText("Repeat the color sequence!");
            for (int i = 0; i < colorButtons.size(); i++) {
                colorButtons.get(i).setBackgroundColor(buttonColors.get(i));
            }
            enablePlayerInput();
        }, colorSequence.size() * 800 + 200); // Add slight delay for better UX
    }

    private void handleColorPress(View button) {
        if (!isGameActive) return; // Ignore input if game is not active
        
        int buttonIndex = colorButtons.indexOf(button);
        int pressedColor = buttonColors.get(buttonIndex); // Use the assigned color
        playerSequence.add(pressedColor);

        // Flash the button color briefly to show the selection
        button.setBackgroundColor(pressedColor);
        button.postDelayed(() -> {
            // Reset the button's background to its assigned color after the flash.
            if (isGameActive) {
                button.setBackgroundColor(buttonColors.get(buttonIndex));
            }
        }, 200);

        if (playerSequence.size() == colorSequence.size()) {
            checkSequence();
        }
    }

    private int getBackgroundColor(View view) {
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor();
        }
        return Color.LTGRAY;
    }

    /**
     * Check the player's sequence against the generated sequence.
     * If correct, start a delay before starting the next round.
     */
    private void checkSequence() {
        if (!isGameActive) return;
        
        disablePlayerInput(); // Disable input during evaluation
        
        if (playerSequence.equals(colorSequence)) {
            // Correct sequence
            currentScore += 15;
            scoreText.setText("Score: " + currentScore);
            sequenceLength++;
            currentRound++;
            
            // Show appropriate message based on progress
            String message = currentRound > MAX_ROUNDS || sequenceLength > MAX_SEQUENCE_LENGTH ? 
                "Congratulations! You've completed the final round!" : 
                "Correct! Get ready for the next round...";
                
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            
            // Cancel any existing timer
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            
            // Delay for 3 seconds before starting the next round
            new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (!isGameActive) {
                        this.cancel();
                        return;
                    }
                    instructionText.setText("Next round in " + (millisUntilFinished / 1000) + "s");
                }
                @Override
                public void onFinish() {
                    if (!isGameActive) return;
                    startNewRound();
                }
            }.start();
        } else {
            // Incorrect sequence
            gameOver("You entered the wrong sequence!");
        }
    }

    private void startGameTimer(long milliseconds) {
        if (gameTimer != null) gameTimer.cancel();

        gameTimer = new CountDownTimer(milliseconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isGameActive) {
                    this.cancel();
                    return;
                }
                timerText.setText("Time: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                if (isGameActive) {
                    gameOver("Time's up!");
                }
            }
        }.start();
    }

    private void gameOver(String message) {
        if (!isGameActive) return;
        
        isGameActive = false;
        if (gameTimer != null) gameTimer.cancel();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over")
               .setMessage(message + "\nYour score: " + currentScore)
               .setCancelable(false)
               .setPositiveButton("Play Again", (dialog, id) -> startReadyCountdown())
               .setNegativeButton("Exit", (dialog, id) -> finish());
        
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void completeGame() {
        if (!isGameActive) return;
        
        isGameActive = false;
        if (gameTimer != null) gameTimer.cancel();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations!")
               .setMessage("You've completed all the levels!\nFinal score: " + currentScore)
               .setCancelable(false)
               .setPositiveButton("Play Again", (dialog, id) -> startReadyCountdown())
               .setNegativeButton("Exit", (dialog, id) -> finish());
        
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void enablePlayerInput() {
        for (View btn : colorButtons) {
            btn.setEnabled(true);
        }
    }
    
    private void disablePlayerInput() {
        for (View btn : colorButtons) {
            btn.setEnabled(false);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game when activity is not in foreground
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (readyTimer != null) {
            readyTimer.cancel();
        }
        isGameActive = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up timers
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (readyTimer != null) {
            readyTimer.cancel();
        }
    }
}
