package com.example.groupproject_game;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.groupproject_game.model.Block;
import java.util.ArrayList;
import java.util.List;

public class SlidingBlockPuzzleActivity extends AppCompatActivity {

    private CustomPuzzleView puzzleView;
    private TextView movesTextView;
    private TextView levelTextView;
    private Button resetButton;

    // Puzzle size
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 4;

    // Target position for goal block (block ID 0)
    private static final int TARGET_ROW = 3;
    private static final int TARGET_COL = 2;

    // Board array: -1 means empty; otherwise store block ID
    private int[][] board = new int[NUM_ROWS][NUM_COLS];

    // List of blocks
    private List<Block> blocks = new ArrayList<>();

    // Instance of UserManager to update score into DB
    private UserManager userManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sliding_puzzle);

        movesTextView = findViewById(R.id.movesTextView);
        levelTextView = findViewById(R.id.levelTextView);
        resetButton = findViewById(R.id.resetButton);

        levelTextView.setText("Level 1");

        // Initialize UserManager (ensure a user is logged in before playing)
        userManager = new UserManager(this);

        resetButton.setOnClickListener(v -> resetPuzzle());

        setupPuzzle();
    }

    private void setupPuzzle() {
        blocks.clear();
        // Initialize board to -1 (empty)
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                board[r][c] = -1;
            }
        }

        // Create puzzle blocks
        Block goalBlock = new Block(0, 0, 1, 2, 2, true, true, Color.RED);
        goalBlock.isGoalBlock = true;
        placeBlockOnBoard(goalBlock);
        blocks.add(goalBlock);

        Block block1 = new Block(1, 2, 0, 1, 2, false, true, Color.BLUE);
        placeBlockOnBoard(block1);
        blocks.add(block1);

        Block block2 = new Block(2, 3, 2, 1, 2, false, true, Color.GREEN);
        placeBlockOnBoard(block2);
        blocks.add(block2);

        Block block3 = new Block(3, 2, 3, 1, 1, true, false, Color.YELLOW);
        placeBlockOnBoard(block3);
        blocks.add(block3);

        puzzleView = new CustomPuzzleView(this, blocks, board, NUM_ROWS, NUM_COLS);
        puzzleView.setTargetPosition(TARGET_ROW, TARGET_COL);

        // Update moves count when a move is made.
        puzzleView.setOnMoveListener(movesCount ->
            movesTextView.setText("Moves: " + movesCount)
        );

        // When puzzle is solved, update the score in the DB and return to MainActivity.
        puzzleView.setOnPuzzleSolvedListener(() -> {
            Toast.makeText(this, "Puzzle Solved! Great job!", Toast.LENGTH_LONG).show();

            // Retrieve the current user and update the score.
            UserManager.User currentUser = userManager.getCurrentUser();
            if (currentUser != null) {
                // Here we assume that fewer moves is better, but you can adjust the score calculation as needed.
                int score = Integer.parseInt(movesTextView.getText().toString().replaceAll("\\D+", ""));
                // For example, use stage 3 for the puzzle and a dummy time value (e.g., 0).
                userManager.updateStageProgress(currentUser.username, 3, score, 0);
                Log.d("SlidingBlockPuzzle", "Updated score for " + currentUser.username + ": " + score);
            } else {
                Log.d("SlidingBlockPuzzle", "No logged-in user, score not saved.");
            }

            // Optionally, add a delay here to let the user see the success message before returning.
            // Then finish the activity, which returns to MainActivity.
            finish();
        });

        FrameLayout puzzleContainer = findViewById(R.id.puzzleContainer);
        puzzleContainer.removeAllViews();
        puzzleContainer.addView(puzzleView);
    }

    /**
     * Helper method to place a block on the board.
     */
    private void placeBlockOnBoard(Block block) {
        for (int r = block.row; r < block.row + block.height; r++) {
            for (int c = block.col; c < block.col + block.width; c++) {
                board[r][c] = block.id;
            }
        }
    }

    /**
     * Resets the puzzle to its initial state.
     */
    private void resetPuzzle() {
        setupPuzzle();
        movesTextView.setText("Moves: 0");
    }

    @Override
    public void onBackPressed() {
        // When back is pressed, simply finish this activity to return to MainActivity.
        finish();
    }
}
