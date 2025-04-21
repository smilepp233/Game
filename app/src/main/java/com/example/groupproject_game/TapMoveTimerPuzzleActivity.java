package com.example.groupproject_game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Random;

public class TapMoveTimerPuzzleActivity extends AppCompatActivity {
    private static final String TAG = "TapMoveTimer";
    
    // Game objects
    private ImageView movableObject;
    private ImageView realTarget;
    private ImageView fakeTarget;
    private ImageView obstacleObject;
    private ArrayList<ImageView> collectibles = new ArrayList<>();
    
    // UI elements
    private TextView timerText;
    private TextView moveCountText;
    private TextView levelText;
    private TextView scoreText;
    private ProgressBar levelProgressBar;
    private FrameLayout gameContainer;
    private Button pauseButton;
    
    // Game state
    private CountDownTimer gameTimer;
    private int moveCount = 0;
    private int currentLevel = 1;
    private int maxLevel = 5;
    private int totalScore = 0;
    private int collectiblesCollected = 0;
    private int timeRemainingMs = 30000;
    private boolean isTrickyMode = false;
    private boolean isPaused = false;
    private boolean isGyroscopeMode = false;
    private String difficulty = "normal"; // easy, normal, hard
    private Random random = new Random();
    
    // Game resources
    private UserManager userManager;
    private MediaPlayer soundCollect;
    private MediaPlayer soundWin;
    private MediaPlayer soundFail;

    // Add a new field to track if the game is in a winning state
    private boolean isWinning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_move_timer_puzzle);

        // Initialize UI elements
        initializeViews();
        
        // Initialize game resources
        initializeResources();
        
        // Load level configuration
        loadLevelConfiguration();
        
        // Show level start dialog
        showLevelStartDialog();
    }
    
    private void initializeViews() {
        // Get game objects
        movableObject = findViewById(R.id.movableObject);
        realTarget = findViewById(R.id.realTarget);
        fakeTarget = findViewById(R.id.fakeTarget);
        obstacleObject = findViewById(R.id.obstacleObject);
        gameContainer = findViewById(R.id.gameContainer);
        
        // Get UI elements
        timerText = findViewById(R.id.timerText);
        moveCountText = findViewById(R.id.moveCountText);
        levelText = findViewById(R.id.levelText);
        scoreText = findViewById(R.id.scoreText);
        levelProgressBar = findViewById(R.id.levelProgressBar);
        pauseButton = findViewById(R.id.pauseButton);
        
        // Set pause button click event
        pauseButton.setOnClickListener(v -> togglePause());
        pauseButton.setText("Pause");
        
        // Set movable object click event
        movableObject.setOnClickListener(v -> handleObjectClick());
        
        // Set object drag event
        movableObject.setOnTouchListener((v, event) -> {
            if (isPaused) return false;
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (difficulty.equals("hard")) return false; // Hard mode doesn't allow dragging
                    
                    v.setX(event.getRawX() - v.getWidth() / 2);
                    v.setY(event.getRawY() - v.getHeight() / 2 - getStatusBarHeight());
                    moveCount++;
                    moveCountText.setText("Moves: " + moveCount);
                    checkCollisions();
                    return true;
            }
            return false;
        });
        
        // Update initial UI texts to English
        timerText.setText("Time: 30s");
        moveCountText.setText("Moves: 0");
        levelText.setText("Level: " + currentLevel);
        scoreText.setText("Score: " + totalScore);
    }
    
    private void initializeResources() {
        // Initialize user manager
        userManager = new UserManager(this);
        
        // Initialize sound effects
        soundCollect = MediaPlayer.create(this, R.raw.collect_sound);
        soundWin = MediaPlayer.create(this, R.raw.win_sound);
        soundFail = MediaPlayer.create(this, R.raw.fail_sound);
        
        // Set local image resources
        movableObject.setImageResource(R.drawable.coin);
        realTarget.setImageResource(R.drawable.treasure);
        fakeTarget.setImageResource(R.drawable.fake_target);
        
        // Create obstacle and set to invisible
        if (obstacleObject != null) {
            obstacleObject.setImageResource(R.drawable.obstacle);
            obstacleObject.setVisibility(View.INVISIBLE);
        }
        
        // Set initial object animation
        movableObject.animate().rotation(360f).setDuration(1000).start();
    }
    
    private void loadLevelConfiguration() {
        isWinning = false;
        Log.d(TAG, "Loading level configuration for level " + currentLevel);
        
        // Check game difficulty settings
        SharedPreferences prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);
        difficulty = prefs.getString("difficulty", "normal");
        
        // Check if using gyroscope mode
        isGyroscopeMode = prefs.getBoolean("gyroscope_mode", false);
        
        // Adjust game parameters based on current level and difficulty
        switch (currentLevel) {
            case 1:
                timeRemainingMs = 30000;
                break;
            case 2:
                timeRemainingMs = 25000;
                spawnCollectibles(2);
                break;
            case 3:
                timeRemainingMs = 20000;
                spawnCollectibles(3);
                if (!difficulty.equals("easy")) {
                    obstacleObject.setVisibility(View.VISIBLE);
                    moveObjectToRandomPosition(obstacleObject);
                }
                break;
            case 4:
                timeRemainingMs = 18000;
                spawnCollectibles(4);
                if (!difficulty.equals("easy")) {
                    obstacleObject.setVisibility(View.VISIBLE);
                    moveObjectToRandomPosition(obstacleObject);
                }
                break;
            case 5:
                timeRemainingMs = 15000;
                spawnCollectibles(5);
                if (!difficulty.equals("easy")) {
                    obstacleObject.setVisibility(View.VISIBLE);
                    moveObjectToRandomPosition(obstacleObject);
                }
                break;
        }
        
        // Adjust time based on difficulty
        if (difficulty.equals("easy")) {
            timeRemainingMs += 10000;
        } else if (difficulty.equals("hard")) {
            timeRemainingMs -= 5000;
        }
        
        // Update UI
        levelText.setText("Level: " + currentLevel);
        scoreText.setText("Score: " + totalScore);
        levelProgressBar.setMax(maxLevel);
        levelProgressBar.setProgress(currentLevel - 1);
        
        // Reset game state
        moveCount = 0;
        moveCountText.setText("Moves: 0");
        timerText.setText("Time: " + (timeRemainingMs / 1000) + "s");
        collectiblesCollected = 0;
        isTrickyMode = false;
        
        Log.d(TAG, "Level " + currentLevel + " configured with " + timeRemainingMs + "ms and difficulty " + difficulty);
    }
    
    private void startGame() {
        Log.d(TAG, "Starting game at level " + currentLevel);
        
        // Move objects to random positions
        moveObjectToRandomPosition(movableObject);
        moveObjectToRandomPosition(realTarget);
        moveObjectToRandomPosition(fakeTarget);
        
        // Start game timer
        startGameTimer();
    }
    
    private void startGameTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        gameTimer = new CountDownTimer(timeRemainingMs, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingMs = (int) millisUntilFinished;
                timerText.setText("Time: " + (millisUntilFinished / 1000) + "." + ((millisUntilFinished % 1000) / 100) + "s");
                
                // Enter tricky mode when less than 1/3 of time remains
                if (millisUntilFinished < timeRemainingMs/3 && !isTrickyMode) {
                    enterTrickyMode();
                }
            }

            @Override
            public void onFinish() {
                timerText.setText("Time's Up!");
                soundFail.start();
                showGameOverDialog("Too slow! Time's up!", false);
            }
        }.start();
    }
    
    private void togglePause() {
        if (isPaused) {
            // Resume game
            isPaused = false;
            pauseButton.setText("Pause");
            startGameTimer();
        } else {
            // Pause game
            isPaused = true;
            pauseButton.setText("Resume");
            if (gameTimer != null) {
                gameTimer.cancel();
            }
        }
    }
    
    private void handleObjectClick() {
        if (isPaused || isWinning) return;
        
        moveCount++;
        moveCountText.setText("Moves: " + moveCount);
        
        if (isOverlapping(movableObject, realTarget)) {
            winLevel();
            return;
        } else if (isOverlapping(movableObject, fakeTarget)) {
            Toast.makeText(this, "Oops! That's a trap!", Toast.LENGTH_SHORT).show();
            movableObject.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                moveObjectToRandomPosition(movableObject);
                movableObject.setAlpha(1f);
            }).start();
            return;
        } else if (obstacleObject != null && obstacleObject.getVisibility() == View.VISIBLE && 
                isOverlapping(movableObject, obstacleObject)) {
            Toast.makeText(this, "Hit an obstacle! Be careful!", Toast.LENGTH_SHORT).show();
            // Time reduction when hitting obstacle
            timeRemainingMs -= 2000;
            if (timeRemainingMs < 0) timeRemainingMs = 0;
            
            // Vibration effect
            ObjectAnimator shakeX = ObjectAnimator.ofFloat(movableObject, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
            shakeX.setDuration(500);
            shakeX.start();
            return;
        } else {
            moveObjectToRandomPosition(movableObject);
            Toast.makeText(this, "Keep chasing it!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkCollisions() {
        // Skip collision detection if we're already winning
        if (isWinning) {
            return;
        }
        
        // DEBUG - Check how many collectibles are available
        Log.d(TAG, "Checking collisions with " + collectibles.size() + " collectibles");
        
        // Check collectible collisions
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            ImageView collectible = collectibles.get(i);
            if (isOverlapping(movableObject, collectible)) {
                Log.d(TAG, "Collectible collision detected at index " + i);
                collectCollectible(collectible, i);
            }
        }
        
        // Check main target collision - this can trigger win
        if (isOverlapping(movableObject, realTarget)) {
            winLevel();
            return; // Exit early after winning
        }
        
        // Check fake target collision
        if (isOverlapping(movableObject, fakeTarget)) {
            Toast.makeText(this, "Oops! That's a trap!", Toast.LENGTH_SHORT).show();
            movableObject.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                moveObjectToRandomPosition(movableObject);
                movableObject.setAlpha(1f);
            }).start();
            return; // Exit after handling fake target collision
        }
        
        // Check obstacle collision
        if (obstacleObject != null && obstacleObject.getVisibility() == View.VISIBLE && 
                isOverlapping(movableObject, obstacleObject)) {
            Toast.makeText(this, "Hit an obstacle! Be careful!", Toast.LENGTH_SHORT).show();
            // Time reduction when hitting obstacle
            timeRemainingMs -= 2000;
            if (timeRemainingMs < 0) timeRemainingMs = 0;
            
            // Vibration effect
            ObjectAnimator shakeX = ObjectAnimator.ofFloat(movableObject, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
            shakeX.setDuration(500);
            shakeX.start();
        }
    }
    
    private void collectCollectible(ImageView collectible, int index) {
        Log.d(TAG, "Collecting collectible at index " + index);
        
        // Play sound
        if (soundCollect != null) {
            soundCollect.start();
        }
        
        // Collection animation
        collectible.animate()
            .scaleX(0f)
            .scaleY(0f)
            .rotation(360f)
            .setDuration(300)
            .withEndAction(() -> {
                try {
                    gameContainer.removeView(collectible);
                    if (index >= 0 && index < collectibles.size()) {
                        collectibles.remove(index);
                    }
                    Log.d(TAG, "Collectible removed. Remaining: " + collectibles.size());
                } catch (Exception e) {
                    Log.e(TAG, "Error removing collectible: " + e.getMessage());
                }
            }).start();
        
        // Increase score
        totalScore += 50;
        scoreText.setText("Score: " + totalScore);
        
        // Add time
        timeRemainingMs += 2000;
        
        collectiblesCollected++;
        
        // Show effects
        showFloatingText("+50 pts", collectible.getX(), collectible.getY(), Color.YELLOW);
        showFloatingText("+2 sec", collectible.getX(), collectible.getY() - 50, Color.GREEN);
    }
    
    private void moveObjectToRandomPosition(ImageView imageView) {
        if (imageView == null || gameContainer == null) {
            Log.e(TAG, "Cannot move null object or gameContainer is null");
            return;
        }
        
        try {
            int padding = 50;
            
            // Ensure gameContainer has been laid out
            if (gameContainer.getWidth() <= 0 || gameContainer.getHeight() <= 0) {
                Log.w(TAG, "Game container has invalid dimensions. Using screen dimensions.");
                float maxX = getResources().getDisplayMetrics().widthPixels - imageView.getWidth() - padding;
                float maxY = getResources().getDisplayMetrics().heightPixels - imageView.getHeight() - padding;
                
                float newX = padding + random.nextFloat() * maxX;
                float newY = padding + random.nextFloat() * maxY;
                
                imageView.setX(newX);
                imageView.setY(newY);
                return;
            }
            
            float maxX = gameContainer.getWidth() - imageView.getWidth() - padding;
            float maxY = gameContainer.getHeight() - imageView.getHeight() - padding;
            
            if (maxX <= 0 || maxY <= 0) {
                Log.w(TAG, "Invalid maxX or maxY. Using default position.");
                imageView.setX(gameContainer.getWidth() / 2f);
                imageView.setY(gameContainer.getHeight() / 2f);
                return;
            }
            
            float newX = padding + random.nextFloat() * maxX;
            float newY = padding + random.nextFloat() * maxY;
            
            // Ensure objects don't overlap
            int maxAttempts = 10;
            int attempts = 0;
            while (attempts < maxAttempts && isPositionOverlappingAnyObject(imageView, newX, newY)) {
                newX = padding + random.nextFloat() * maxX;
                newY = padding + random.nextFloat() * maxY;
                attempts++;
            }
            
            // Animation effect
            if (imageView == movableObject && isTrickyMode) {
                // Faster in tricky mode
                imageView.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            } else {
                imageView.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error moving object: " + e.getMessage());
        }
    }
    
    private boolean isPositionOverlappingAnyObject(ImageView imageToCheck, float x, float y) {
        // Create a temporary bounding box
        float left = x;
        float top = y;
        float right = left + imageToCheck.getWidth();
        float bottom = top + imageToCheck.getHeight();
        
        // Check overlap with each game object
        if (imageToCheck != realTarget && isRectOverlapping(left, top, right, bottom, realTarget)) {
            return true;
        }
        
        if (imageToCheck != fakeTarget && isRectOverlapping(left, top, right, bottom, fakeTarget)) {
            return true;
        }
        
        if (imageToCheck != movableObject && isRectOverlapping(left, top, right, bottom, movableObject)) {
            return true;
        }
        
        if (obstacleObject != null && obstacleObject.getVisibility() == View.VISIBLE && 
                imageToCheck != obstacleObject && isRectOverlapping(left, top, right, bottom, obstacleObject)) {
            return true;
        }
        
        // Check overlap with collectibles
        for (ImageView collectible : collectibles) {
            if (imageToCheck != collectible && isRectOverlapping(left, top, right, bottom, collectible)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isRectOverlapping(float left1, float top1, float right1, float bottom1, ImageView view2) {
        float left2 = view2.getX();
        float top2 = view2.getY();
        float right2 = left2 + view2.getWidth();
        float bottom2 = top2 + view2.getHeight();
        
        return !(right1 < left2 || right2 < left1 || bottom1 < top2 || bottom2 < top1);
    }
    
    private void enterTrickyMode() {
        isTrickyMode = true;
        Toast.makeText(this, "Tricky Mode: It's speeding up!", Toast.LENGTH_SHORT).show();
        
        // Effects
        fakeTarget.animate().scaleX(1.2f).scaleY(1.2f).setDuration(500).start();
        
        // Change timer color
        timerText.setTextColor(Color.RED);
        
        // Start moving targets in hard mode
        if (difficulty.equals("hard")) {
            new Handler().postDelayed(this::moveTargetsRandomly, 1000);
        }
    }
    
    private void moveTargetsRandomly() {
        if (isTrickyMode && !isPaused) {
            moveObjectToRandomPosition(fakeTarget);
            
            // In hard mode, real target also moves
            if (difficulty.equals("hard") && random.nextBoolean()) {
                moveObjectToRandomPosition(realTarget);
            }
            
            // Move obstacles
            if (obstacleObject != null && obstacleObject.getVisibility() == View.VISIBLE) {
                moveObjectToRandomPosition(obstacleObject);
            }
            
            // Continue moving
            new Handler().postDelayed(this::moveTargetsRandomly, 2000);
        }
    }
    
    private void spawnCollectibles(int count) {
        Log.d(TAG, "Spawning " + count + " collectibles");
        
        // Clear existing collectibles
        for (ImageView collectible : collectibles) {
            gameContainer.removeView(collectible);
        }
        collectibles.clear();
        
        // Create new collectibles
        for (int i = 0; i < count; i++) {
            ImageView collectible = new ImageView(this);
            collectible.setImageResource(R.drawable.star);
            
            // Set size
            int size = (int) (getResources().getDisplayMetrics().density * 30);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            collectible.setLayoutParams(params);
            
            // Add to container
            gameContainer.addView(collectible);
            collectibles.add(collectible);
            
            // Move to random position
            moveObjectToRandomPosition(collectible);
            
            // Add pulse animation with ValueAnimator
            ValueAnimator scaleXAnimator = ValueAnimator.ofFloat(1f, 1.2f, 1f);
            scaleXAnimator.addUpdateListener(animation -> {
                collectible.setScaleX((float) animation.getAnimatedValue());
            });
            scaleXAnimator.setDuration(1000);
            scaleXAnimator.setRepeatCount(ValueAnimator.INFINITE);
            
            ValueAnimator scaleYAnimator = ValueAnimator.ofFloat(1f, 1.2f, 1f);
            scaleYAnimator.addUpdateListener(animation -> {
                collectible.setScaleY((float) animation.getAnimatedValue());
            });
            scaleYAnimator.setDuration(1000);
            scaleYAnimator.setRepeatCount(ValueAnimator.INFINITE);
            
            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleXAnimator, scaleYAnimator);
            animSet.start();
        }
        
        Log.d(TAG, "Created " + collectibles.size() + " collectibles");
    }
    
    private boolean isOverlapping(ImageView obj, ImageView target) {
        if (obj == null || target == null) return false;
        
        float objX = obj.getX();
        float objY = obj.getY();
        float objRight = objX + obj.getWidth();
        float objBottom = objY + obj.getHeight();
        
        float targetX = target.getX();
        float targetY = target.getY();
        float targetRight = targetX + target.getWidth();
        float targetBottom = targetY + target.getHeight();
        
        // Simple rectangle collision detection
        return objX < targetRight &&
               objRight > targetX &&
               objY < targetBottom &&
               objBottom > targetY;
    }
    
    private void winLevel() {
        // Prevent multiple calls to winLevel()
        if (isWinning) {
            Log.d(TAG, "Win already in progress, ignoring duplicate call");
            return;
        }
        
        isWinning = true;
        Log.d(TAG, "Level " + currentLevel + " completed!");
        
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        if (soundWin != null) {
            soundWin.start();
        }
        
        // Calculate score
        int levelScore = calculateLevelScore();
        totalScore += levelScore;
        
        // Effects
        realTarget.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .rotation(360f)
            .setDuration(1000)
            .start();
        
        showWinDialog(levelScore);
    }
    
    private int calculateLevelScore() {
        // Base score
        int baseScore = 100;
        
        // Bonus based on remaining time
        int timeBonus = (int)(timeRemainingMs / 1000) * 10;
        
        // Penalty based on moves
        int movePenalty = Math.min(50, moveCount);
        
        // Difficulty multiplier
        float difficultyMultiplier = 1.0f;
        switch (difficulty) {
            case "easy":
                difficultyMultiplier = 0.8f;
                break;
            case "normal":
                difficultyMultiplier = 1.0f;
                break;
            case "hard":
                difficultyMultiplier = 1.5f;
                break;
        }
        
        // Collectible bonuses already calculated when collected
        
        return (int)((baseScore + timeBonus - movePenalty) * difficultyMultiplier);
    }
    
    private void showLevelStartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Level " + currentLevel)
               .setMessage("Ready to start level " + currentLevel + "\n" +
                          "Goal: Move the coin to the treasure chest\n" +
                          "Collect stars for extra points and time\n" +
                          "Difficulty: " + getDifficultyName())
               .setCancelable(false)
               .setPositiveButton("Start", (dialog, id) -> startGame());
        
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void showWinDialog(int levelScore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations!")
               .setMessage("You completed level " + currentLevel + "!\n" +
                          "Level score: " + levelScore + "\n" +
                          "Total score: " + totalScore + "\n" +
                          "Stars collected: " + collectiblesCollected)
               .setCancelable(false);
        
        if (currentLevel < maxLevel) {
            builder.setPositiveButton("Next Level", (dialog, id) -> {
                Log.d(TAG, "Advancing to level " + (currentLevel + 1));
                currentLevel++;
                isWinning = false;
                loadLevelConfiguration();
                showLevelStartDialog();
            });
        } else {
            builder.setPositiveButton("Complete Game", (dialog, id) -> {
                saveGameResults();
                finish();
            });
        }
        
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void showGameOverDialog(String message, boolean canRetry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over")
               .setMessage(message + "\nTotal Score: " + totalScore)
               .setCancelable(false);
        
        if (canRetry) {
            builder.setPositiveButton("Retry", (dialog, id) -> {
                isWinning = false;
                loadLevelConfiguration();
                showLevelStartDialog();
            });
        }
        
        builder.setNegativeButton("Exit", (dialog, id) -> {
            saveGameResults();
            finish();
        });
        
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void saveGameResults() {
        UserManager.User currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            userManager.updateStageProgress(currentUser.username, 3, totalScore, (int)(30000 - timeRemainingMs));
        }
    }
    
    private String getDifficultyName() {
        switch (difficulty) {
            case "easy":
                return "Easy";
            case "normal":
                return "Normal";
            case "hard":
                return "Hard";
            default:
                return "Normal";
        }
    }
    
    private void showFloatingText(String text, float x, float y, int color) {
        TextView floatingText = new TextView(this);
        floatingText.setText(text);
        floatingText.setTextColor(color);
        floatingText.setTextSize(16);
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        floatingText.setLayoutParams(params);
        floatingText.setX(x);
        floatingText.setY(y);
        
        gameContainer.addView(floatingText);
        
        floatingText.animate()
            .translationYBy(-100f)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction(() -> gameContainer.removeView(floatingText))
            .start();
    }
    
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        // Release media resources
        if (soundCollect != null) {
            soundCollect.release();
            soundCollect = null;
        }
        if (soundWin != null) {
            soundWin.release();
            soundWin = null;
        }
        if (soundFail != null) {
            soundFail.release();
            soundFail = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isPaused && gameTimer != null) {
            gameTimer.cancel();
            isPaused = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPaused && !isFinishing()) {
            // Show continue game dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Game Paused")
                   .setMessage("Continue playing?")
                   .setCancelable(false)
                   .setPositiveButton("Continue", (dialog, id) -> {
                       isPaused = false;
                       startGameTimer();
                   })
                   .setNegativeButton("Exit", (dialog, id) -> finish());
            
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}