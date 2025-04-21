package com.example.groupproject_game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BrainTrainerGamesActivity extends AppCompatActivity {
    // Game 1: Reaction Sequence Challenge
    public static class ReactionSequenceChallengeActivity extends AppCompatActivity {
        private List<MaterialButton> sequenceButtons;
        private List<Integer> correctSequence;
        private List<Integer> playerSequence;
        private TextView instructionText, scoreText, timerText;
        private ConstraintLayout gameLayout;
        private int currentScore = 0;
        private int sequenceLength = 3;
        private CountDownTimer gameTimer;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_reaction_sequence);

            gameLayout = findViewById(R.id.gameLayout);
            instructionText = findViewById(R.id.instructionText);
            scoreText = findViewById(R.id.scoreText);
            timerText = findViewById(R.id.timerText);

            setupSequenceButtons();
            startNewRound();
        }

        private void setupSequenceButtons() {
            sequenceButtons = new ArrayList<>();
            int[] buttonIds = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4};

            for (int i = 0; i < buttonIds.length; i++) {
                MaterialButton btn = findViewById(buttonIds[i]);
                final int index = i;
                btn.setOnClickListener(v -> handleButtonPress(index));
                sequenceButtons.add(btn);
            }
        }

        private void startNewRound() {
            // Generate random sequence
            correctSequence = generateRandomSequence(sequenceLength);
            playerSequence = new ArrayList<>();

            // Show sequence to player
            showSequence();

            // Start game timer
            startGameTimer(15000); // 15 seconds per round
        }

        private List<Integer> generateRandomSequence(int length) {
            List<Integer> sequence = new ArrayList<>();
            Random random = new Random();
            for (int i = 0; i < length; i++) {
                sequence.add(random.nextInt(4)); // 0-3 buttons
            }
            return sequence;
        }

        private void showSequence() {
            instructionText.setText("Watch the sequence!");
            for (int i = 0; i < correctSequence.size(); i++) {
                int finalI = i;
                MaterialButton btn = sequenceButtons.get(correctSequence.get(i));

                // Animate button highlight
                btn.postDelayed(() -> {
                    animateButtonHighlight(btn);
                }, i * 800);
            }

            // Enable player input after sequence display
            gameLayout.postDelayed(() -> {
                instructionText.setText("Repeat the sequence!");
                enablePlayerInput();
            }, correctSequence.size() * 800);
        }

        private void animateButtonHighlight(MaterialButton btn) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(btn, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(btn, "scaleY", 1f, 1.2f, 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(500);
            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
            animatorSet.start();
        }

        private void handleButtonPress(int buttonIndex) {
            playerSequence.add(buttonIndex);
            animateButtonHighlight(sequenceButtons.get(buttonIndex));

            // Check sequence if player has pressed same number of buttons
            if (playerSequence.size() == correctSequence.size()) {
                checkSequence();
            }
        }

        private void checkSequence() {
            if (playerSequence.equals(correctSequence)) {
                // Correct sequence
                currentScore += 10;
                scoreText.setText("Score: " + currentScore);
                sequenceLength++;
                startNewRound();
            } else {
                // Incorrect sequence
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
            finish();
        }

        private void enablePlayerInput() {
            for (MaterialButton btn : sequenceButtons) {
                btn.setEnabled(true);
            }
        }
    }

    // Game 2: Math Speed Challenge

    // Game 3: Color Memory Challenge

}