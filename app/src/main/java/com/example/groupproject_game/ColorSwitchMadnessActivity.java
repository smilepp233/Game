package com.example.groupproject_game;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.util.Random;

public class ColorSwitchMadnessActivity extends AppCompatActivity {
    private ImageView targetCircle;
    private ImageView colorBall;
    private TextView timerText;
    private TextView scoreText;
    private CountDownTimer gameTimer;
    private CountDownTimer colorSwitchTimer;
    private int score = 0;
    private int[] colors = {0xFFFF5722, 0xFF3F51B5, 0xFFE91E63, 0xFF4CAF50}; // Orange, Blue, Pink, Green
    private int currentTargetColor;
    private int currentBallColor;
    private UserManager userManager;
    private boolean isPenaltyMode = false;
    private long colorSwitchInterval = 3000; // default interval in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_switch_madness);

        targetCircle = findViewById(R.id.targetCircle);
        colorBall = findViewById(R.id.colorBall);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        userManager = new UserManager(this);

        // Load ball image with Glide (with placeholder and error drawable)
        Glide.with(this)
            .load("https://cdn.pixabay.com/photo/2013/07/12/12/56/ball-146497_1280.png")
            .placeholder(R.drawable.ball) // ensure you have a local ball drawable
            .error(R.drawable.ball)
            .into(colorBall);

        // Set initial colors using color filters to tint the images.
        setRandomColors();

        // Start game timer (30 seconds)
        gameTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time: " + millisUntilFinished / 1000 + "s");
                if (millisUntilFinished < 10000 && !isPenaltyMode) {
                    enterPenaltyMode();
                }
            }

            @Override
            public void onFinish() {
                timerText.setText("Time's Up!");
                Toast.makeText(ColorSwitchMadnessActivity.this,
                    "Game Over! Score: " + score, Toast.LENGTH_SHORT).show();
                finishGame();
            }
        }.start();

        // Start color switch timer with the default interval
        startColorSwitchTimer(colorSwitchInterval);

        // Ball click listener – update colors and check match
        colorBall.setOnClickListener(v -> {
            if (currentBallColor == currentTargetColor) {
                score += 10;
                scoreText.setText("Score: " + score);
                Toast.makeText(this, "Match! +10", Toast.LENGTH_SHORT).show();
                setRandomColors();
                colorBall.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
                    .withEndAction(() -> colorBall.animate().scaleX(1f).scaleY(1f).setDuration(200).start())
                    .start();
            } else {
                Toast.makeText(this, "Wrong color! Speeding up!", Toast.LENGTH_SHORT).show();
                if (colorSwitchTimer != null) {
                    colorSwitchTimer.cancel();
                }
                long newInterval = isPenaltyMode ? 500 : 2000;
                startColorSwitchTimer(newInterval);
                colorBall.animate().rotationBy(360f).setDuration(500).start();
            }
        });
    }

    // Use color filters with a PorterDuff mode to ensure proper tinting.
    private void setRandomColors() {
        Random random = new Random();
        currentTargetColor = colors[random.nextInt(colors.length)];
        currentBallColor = colors[random.nextInt(colors.length)];
        // Apply color filter to the drawable (make sure targetCircle uses android:src)
        targetCircle.setColorFilter(currentTargetColor, PorterDuff.Mode.SRC_IN);
        colorBall.setColorFilter(currentBallColor, PorterDuff.Mode.SRC_IN);
    }

    private void switchTargetColor() {
        Random random = new Random();
        currentTargetColor = colors[random.nextInt(colors.length)];
        targetCircle.setColorFilter(currentTargetColor, PorterDuff.Mode.SRC_IN);
        targetCircle.animate().rotationBy(360f).setDuration(500).start();
    }

    private void startColorSwitchTimer(long interval) {
        colorSwitchInterval = interval;
        colorSwitchTimer = new CountDownTimer(30000, colorSwitchInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                switchTargetColor();
            }
            @Override
            public void onFinish() {}
        }.start();
    }

    private void enterPenaltyMode() {
        isPenaltyMode = true;
        Toast.makeText(this, "Penalty Mode: Colors switch faster!", Toast.LENGTH_SHORT).show();
        if (colorSwitchTimer != null) {
            colorSwitchTimer.cancel();
        }
        startColorSwitchTimer(1000); // Faster switching in penalty mode
    }

    // When the game finishes, update the user score in the database.
    private void finishGame() {
        UserManager.User currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            userManager.updateStageProgress(currentUser.username, 4, score, 30000);
            Log.d("ColorSwitchMadness", "Updated score for " + currentUser.username + ": " + score);
        } else {
            Log.d("ColorSwitchMadness", "No logged-in user, score not saved.");
        }
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (colorSwitchTimer != null) {
            colorSwitchTimer.cancel();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) gameTimer.cancel();
        if (colorSwitchTimer != null) colorSwitchTimer.cancel();
    }
}
