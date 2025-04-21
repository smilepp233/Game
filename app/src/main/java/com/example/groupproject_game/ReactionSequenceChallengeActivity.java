package com.example.groupproject_game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReactionSequenceChallengeActivity extends AppCompatActivity {
    private List<MaterialButton> sequenceButtons;
    private List<Integer> correctSequence;
    private List<Integer> playerSequence;
    private MaterialTextView instructionText, scoreText, timerText;
    private ConstraintLayout gameLayout;
    private int currentScore = 0;
    private int sequenceLength = 3;
    private CountDownTimer gameTimer;
    private static final String TAG = "ReactionSequence";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction_sequence);

        gameLayout = findViewById(R.id.gameLayout);
        instructionText = findViewById(R.id.instructionText);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);

        setupSequenceButtons();
        // Show a "Get Ready" message and delay the start by 5 seconds
        instructionText.setText("Get Ready...");
        gameLayout.postDelayed(() -> startNewRound(), 5000);
    }

    private void setupSequenceButtons() {
        sequenceButtons = new ArrayList<>();
        int[] buttonIds = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4};

        for (int i = 0; i < buttonIds.length; i++) {
            MaterialButton btn = findViewById(buttonIds[i]);
            final int index = i;
            btn.setOnClickListener(v -> handleButtonPress(index));
            sequenceButtons.add(btn);
            btn.setEnabled(false); // Disabled until sequence is shown
        }
    }

    private void startNewRound() {
        correctSequence = generateRandomSequence(sequenceLength);
        playerSequence = new ArrayList<>();

        // Reset button colors (if using backgroundTint, consider caching color state lists)
        for (MaterialButton btn : sequenceButtons) {
            btn.setBackgroundTintList(getColorStateList(R.color.primary));
        }

        Log.d(TAG, "New round. Correct sequence: " + correctSequence.toString());
        showSequence();
        startGameTimer(15000); // 15 seconds per round
    }

    private List<Integer> generateRandomSequence(int length) {
        List<Integer> sequence = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sequence.add(random.nextInt(4)); // 0-3 for four buttons
        }
        return sequence;
    }

    /**
     * Displays the correct sequence by creating a sequential AnimatorSet.
     */
    private void showSequence() {
        instructionText.setText("Watch the sequence!");
        disablePlayerInput();

        AnimatorSet sequenceAnimator = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();

        // Create an animation for each button in the sequence
        for (int i = 0; i < correctSequence.size(); i++) {
            MaterialButton btn = sequenceButtons.get(correctSequence.get(i));

            // Scale-up animation
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btn, "scaleX", 1f, 1.2f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btn, "scaleY", 1f, 1.2f);
            // Scale-down animation
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btn, "scaleX", 1.2f, 1f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btn, "scaleY", 1.2f, 1f);

            scaleUpX.setDuration(150);
            scaleUpY.setDuration(150);
            scaleDownX.setDuration(150);
            scaleDownY.setDuration(150);

            AnimatorSet btnAnim = new AnimatorSet();
            btnAnim.playSequentially(scaleUpX, scaleUpY, scaleDownX, scaleDownY);
            btnAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            // Stagger each button’s animation by 500ms
            btnAnim.setStartDelay(i * 500);
            animators.add(btnAnim);
        }

        sequenceAnimator.playTogether(animators);
        sequenceAnimator.start();

        sequenceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                instructionText.setText("Repeat the sequence!");
                enablePlayerInput();
            }
        });
    }

    private void handleButtonPress(int buttonIndex) {
        playerSequence.add(buttonIndex);
        // Provide immediate feedback by reusing the same highlight animation
        animateButtonHighlight(sequenceButtons.get(buttonIndex));

        if (playerSequence.size() == correctSequence.size()) {
            checkSequence();
        }
    }

    /**
     * A simplified version of the highlight animation for a button.
     * This method is reused for both sequence display and player input feedback.
     */
    private void animateButtonHighlight(MaterialButton btn) {
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btn, "scaleX", 1f, 1.1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btn, "scaleY", 1f, 1.1f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btn, "scaleX", 1.1f, 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btn, "scaleY", 1.1f, 1f);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(scaleUpX, scaleUpY, scaleDownX, scaleDownY);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void checkSequence() {
        Log.d(TAG, "Player sequence: " + playerSequence.toString() + " | Correct sequence: " + correctSequence.toString());
        if (playerSequence.equals(correctSequence)) {
            currentScore += 10;
            scoreText.setText("Score: " + currentScore);
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            sequenceLength++;
            // Delay starting the new round to let the player see the updated score
            gameLayout.postDelayed(() -> startNewRound(), 500);
        } else {
            Toast.makeText(this, "Wrong sequence!\nCorrect: " + correctSequence.toString() +
                "\nYour input: " + playerSequence.toString(), Toast.LENGTH_LONG).show();
            gameOver();
        }
    }

    private void startGameTimer(long milliseconds) {
        if (gameTimer != null) {
            gameTimer.cancel();
        }

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
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        for (MaterialButton btn : sequenceButtons) {
            btn.setEnabled(false);
        }
        Toast.makeText(this, "Game Over! Score: " + currentScore, Toast.LENGTH_LONG).show();
        finish();
    }

    private void disablePlayerInput() {
        for (MaterialButton btn : sequenceButtons) {
            btn.setEnabled(false);
        }
    }

    private void enablePlayerInput() {
        for (MaterialButton btn : sequenceButtons) {
            btn.setEnabled(true);
        }
    }
}
