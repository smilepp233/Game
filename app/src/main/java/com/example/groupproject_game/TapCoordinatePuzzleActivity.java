package com.example.groupproject_game;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.bumptech.glide.Glide;
import com.example.groupproject_game.model.Block;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TapCoordinatePuzzleActivity extends AppCompatActivity {

    private CustomPuzzleView puzzleView;

    // UI元素
    private TextView movesTextView;
    private TextView scoreTextView;
    private TextView timerTextView;
    private TextView levelTextView;
    private Button hintButton;
    private Button resetButton;
    private Button pauseButton;
    private FrameLayout gameCompleteOverlay;
    private TextView completeScoreText;
    private TextView completeMoveText;
    private Button nextLevelButton;
    private Button backToMenuButton;

    // 游戏状态
    private boolean isPaused = false;
    private int currentLevel = 1;
    private int score = 0;
    private int movesCount = 0;
    private int timeRemaining = 60;
    private static final int MAX_LEVEL = 3;
    private Random random = new Random();

    // 倒计时器
    private CountDownTimer gameTimer;

    // 配置
    private static final int BASE_SCORE = 100;
    private static final int MOVE_PENALTY = 5;
    private static final int TIME_BONUS_MULTIPLIER = 10;

    // Example puzzle size
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 4;

    // Board array: -1 means empty; otherwise store block ID
    private int[][] board = new int[NUM_ROWS][NUM_COLS];

    // Keep a list of blocks
    private List<Block> blocks = new ArrayList<>();

    // 新增方法，设置目标位置
    private void setTargetPosition(int row, int col) {
        // 保存目标位置信息以便重置时使用
        targetRow = row;
        targetCol = col;
    }

    // 成员变量，保存目标位置
    private int targetRow = -1;
    private int targetCol = -1;

    // 添加标志防止重复触发
    private boolean isLevelCompleteHandling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_coordinate_puzzle);

        // 初始化UI元素
        initializeUI();

        // 设置游戏
        setupGame();

        // 开始游戏
        startGame();
        
        // 添加一条启动日志
        Log.d("GameActivity", "游戏已启动，当前关卡: " + currentLevel);
    }

    private void initializeUI() {
        // 获取文本视图引用
        movesTextView = findViewById(R.id.movesTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        timerTextView = findViewById(R.id.timerTextView);
        levelTextView = findViewById(R.id.levelTextView);

        // 获取按钮引用
        hintButton = findViewById(R.id.hintButton);
        resetButton = findViewById(R.id.resetButton);
        pauseButton = findViewById(R.id.pauseButton);

        // 获取游戏完成叠加层及相关控件
        gameCompleteOverlay = findViewById(R.id.gameCompleteOverlay);
        completeScoreText = findViewById(R.id.completeScoreText);
        completeMoveText = findViewById(R.id.completeMoveText);
        nextLevelButton = findViewById(R.id.nextLevelButton);
        backToMenuButton = findViewById(R.id.backToMenuButton);

        // 设置初始文本
        updateUIText();

        // 设置按钮点击监听器
        setupButtonListeners();
    }

    private void updateUIText() {
        movesTextView.setText("Moves: " + movesCount);
        scoreTextView.setText("Score: " + score);
        timerTextView.setText("Time: " + timeRemaining + "s");
        levelTextView.setText("Level: " + currentLevel);
    }

    private void setupButtonListeners() {
        // 测试按钮 - 直接完成当前关卡并进入下一关
        hintButton.setText("Debug Button"); // 更改按钮文本
        hintButton.setOnClickListener(v -> {
            // 切换不同的调试功能，根据当前状态
            if (gameCompleteOverlay.getVisibility() == View.VISIBLE) {
                // 如果完成界面已显示，隐藏它并进入下一关
                Toast.makeText(this, "Forcing next level...", Toast.LENGTH_SHORT).show();
                gameCompleteOverlay.setVisibility(View.GONE);
                goToNextLevel();
            } else {
                // 显示一条明确的消息
                Toast.makeText(this, "Debug: simulating level complete", Toast.LENGTH_SHORT).show();
                
                // 停止计时器
                if (gameTimer != null) {
                    gameTimer.cancel();
                }
                
                // 触发关卡完成
                onLevelComplete();
            }
        });

        // 重置按钮
        resetButton.setOnClickListener(v -> {
            // 直接重置关卡，避免对话框确认
            Toast.makeText(this, "Resetting level...", Toast.LENGTH_SHORT).show();
            resetLevel();
        });

        // 暂停/继续按钮
        pauseButton.setOnClickListener(v -> {
            togglePause();
        });

        // 返回主菜单按钮
        backToMenuButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupGame() {
        // 初始化板块数组为-1（空）
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                board[r][c] = -1;
            }
        }

        // 清除之前的block列表
        blocks.clear();

        // 创建关卡布局
        createLevelLayout(currentLevel);

        // 创建拼图视图
        puzzleView = new CustomPuzzleView(this, blocks, board, NUM_ROWS, NUM_COLS);

        // 设置移动监听器
        puzzleView.setOnMoveListener(newMovesCount -> {
            movesCount = newMovesCount;
            movesTextView.setText("Moves: " + movesCount);
        });

        // 设置拼图完成监听器
        puzzleView.setOnPuzzleSolvedListener(() -> {
            // 直接调用关卡完成方法
            onLevelComplete();
        });

        // 将拼图视图添加到布局容器中
        FrameLayout puzzleContainer = findViewById(R.id.puzzleContainer);
        puzzleContainer.removeAllViews(); // 移除之前的视图（如果有）
        puzzleContainer.addView(puzzleView);
        
        // 确保拼图状态被重置
        if (puzzleView != null) {
            puzzleView.resetPuzzleSolvedState();
        }
    }

    private void createLevelLayout(int level) {
        // 根据关卡创建不同难度的拼图布局
        switch (level) {
            case 1:
                // 简单关卡
                // 一个2x2的方块，从row=0,col=1开始，可以在两个方向移动
                Block block0 = new Block(0, 0, 1, 2, 2, true, true);
                block0.setAsGoalBlock(); // 将其设置为目标方块
                placeBlockOnBoard(block0);
                blocks.add(block0);

                // 一个竖直的1x2方块，在row=2,col=0，只能垂直移动
                Block block1 = new Block(1, 2, 0, 1, 2, false, true);
                placeBlockOnBoard(block1);
                blocks.add(block1);

                // 另一个竖直的1x2方块，在row=3,col=2
                Block block2 = new Block(2, 3, 2, 1, 2, false, true);
                placeBlockOnBoard(block2);
                blocks.add(block2);

                // 设置目标位置
                setTargetPosition(3, 1);
                break;

            case 2:
                // 中等关卡
                // 更多方块，更复杂的布局
                Block blockA = new Block(0, 0, 0, 2, 2, true, true);
                blockA.setAsGoalBlock(); // 将其设置为目标方块
                placeBlockOnBoard(blockA);
                blocks.add(blockA);

                Block blockB = new Block(1, 2, 0, 1, 3, false, true);
                placeBlockOnBoard(blockB);
                blocks.add(blockB);

                Block blockC = new Block(2, 0, 2, 2, 1, true, false);
                placeBlockOnBoard(blockC);
                blocks.add(blockC);

                Block blockD = new Block(3, 3, 2, 1, 2, false, true);
                placeBlockOnBoard(blockD);
                blocks.add(blockD);

                Block blockE = new Block(4, 2, 3, 1, 1, true, true);
                placeBlockOnBoard(blockE);
                blocks.add(blockE);

                // 设置目标位置
                setTargetPosition(3, 0);
                break;

            case 3:
                // 困难关卡
                // 更复杂的布局
                Block goalBlock = new Block(0, 0, 0, 2, 2, true, true);
                goalBlock.setAsGoalBlock(); // 将其设置为目标方块
                placeBlockOnBoard(goalBlock);
                blocks.add(goalBlock);

                for (int i = 1; i < 7; i++) {
                    int width = random.nextInt(2) + 1;
                    int height = (width == 1) ? random.nextInt(2) + 1 : 1;
                    int row = random.nextInt(NUM_ROWS - height);
                    int col = random.nextInt(NUM_COLS - width);

                    // 确保没有重叠
                    boolean overlap = false;
                    for (int r = row; r < row + height; r++) {
                        for (int c = col; c < col + width; c++) {
                            if (board[r][c] != -1) {
                                overlap = true;
                                break;
                            }
                        }
                        if (overlap) break;
                    }

                    if (!overlap) {
                        Block block = new Block(i, row, col, width, height, true, true);
                        placeBlockOnBoard(block);
                        blocks.add(block);
                    }
                }

                // 设置目标位置
                setTargetPosition(3, 2);
                break;
        }
    }

    private void startGame() {
        // 重置移动计数
        movesCount = 0;

        // 启动游戏计时器
        startTimer();

        // 设置目标位置到拼图视图
        if (puzzleView != null && targetRow >= 0 && targetCol >= 0) {
            puzzleView.setTargetPosition(targetRow, targetCol);
            // 确保拼图状态被重置
            puzzleView.resetPuzzleSolvedState();
        }

        // 更新UI
        updateUIText();

        // 游戏准备就绪
        isPaused = false;
        pauseButton.setText("Pause");
    }

    private void startTimer() {
        // 取消之前的计时器（如果存在）
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // 设置游戏时间（秒）
        timeRemaining = 60;

        // 创建新的计时器
        gameTimer = new CountDownTimer(timeRemaining * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = (int) (millisUntilFinished / 1000);
                timerTextView.setText("Time: " + timeRemaining + "s");

                // 在时间不多时改变计时器颜色
                if (timeRemaining <= 10) {
                    timerTextView.setTextColor(
                        getResources().getColor(android.R.color.holo_red_light));
                }
            }

            @Override
            public void onFinish() {
                timeRemaining = 0;
                timerTextView.setText("Time's up!");
                onGameOver();
            }
        };

        // 开始计时
        gameTimer.start();
    }

    private void togglePause() {
        if (isPaused) {
            // 继续游戏
            resumeGame();
        } else {
            // 暂停游戏
            pauseGame();
        }
    }

    private void pauseGame() {
        // 取消计时器
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // 更新状态和UI
        isPaused = true;
        pauseButton.setText("Resume");

        // 显示暂停对话框
        new AlertDialog.Builder(this)
            .setTitle("Game Paused")
            .setMessage("Take a break! Click Resume when you're ready to continue.")
            .setPositiveButton("Resume", (dialog, which) -> resumeGame())
            .setCancelable(false)
            .show();
    }

    private void resumeGame() {
        // 重新启动计时器
        startTimer();

        // 更新状态和UI
        isPaused = false;
        pauseButton.setText("Pause");
    }

    private void resetLevel() {
        // 清除游戏状态
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        // 清除关卡完成覆盖层（如果可见）
        gameCompleteOverlay.setVisibility(View.GONE);
        
        // 重置移动次数
        movesCount = 0;
        
        // 重置时间
        timeRemaining = 60;
        
        // 清除旧的拼图视图
        FrameLayout puzzleContainer = findViewById(R.id.puzzleContainer);
        puzzleContainer.removeAllViews();
        
        // 重置board数组
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                board[r][c] = -1;
            }
        }
        
        // 清除blocks列表
        blocks.clear();
        
        // 重新设置游戏
        setupGame();
        
        // 先更新UI，再开始游戏
        updateUIText();
        
        // 开始游戏
        startGame();
        
        Toast.makeText(this, "Level " + currentLevel + " reset!", Toast.LENGTH_SHORT).show();
    }

    private void showHint() {
        // 显示提示（简单实现）
        Toast.makeText(this, "Try to move the blocks to solve the puzzle!", Toast.LENGTH_SHORT)
            .show();
    }

    private void onLevelComplete() {
        // 记录日志
        Log.d("GameActivity", "关卡完成被触发! 当前关卡: " + currentLevel);
        
        // 防止重复触发
        if (isLevelCompleteHandling) {
            Log.d("GameActivity", "已经在处理关卡完成，忽略此次调用");
            return;
        }
        isLevelCompleteHandling = true;
        
        // 停止计时器
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // 计算最终得分
        int levelScore = calculateScore();
        score += levelScore;

        // 更新UI
        scoreTextView.setText("Score: " + score);

        // 根据是否还有下一关来设置按钮
        if (currentLevel < MAX_LEVEL) {
            // 显示庆祝信息
            Toast.makeText(this, "Level " + currentLevel + " completed! Moving to next level...", Toast.LENGTH_SHORT).show();
            
            // 短暂显示关卡完成界面，然后自动进入下一关
            gameCompleteOverlay.setVisibility(View.VISIBLE);
            completeScoreText.setText("Score: " + score);
            completeMoveText.setText("Moves: " + movesCount);
            
            // 使用动画效果
            gameCompleteOverlay.setAlpha(0f);
            gameCompleteOverlay.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
            
            // 延迟2秒后自动进入下一关，让玩家有时间看到分数
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 隐藏覆盖层
                if (gameCompleteOverlay != null) {
                    gameCompleteOverlay.setVisibility(View.GONE);
                    // 直接进入下一关
                    goToNextLevel();
                }
            }, 2000); // 2秒后自动进入下一关
        } else {
            // 这是最后一关，显示游戏完成界面
            gameCompleteOverlay.setVisibility(View.VISIBLE);
            completeScoreText.setText("Final Score: " + score);
            completeMoveText.setText("Game Completed!");
            nextLevelButton.setText("Back to Menu");
            
            // 清除旧的监听器，防止重复绑定
            nextLevelButton.setOnClickListener(null); 
            nextLevelButton.setOnClickListener(v -> finish());
            
            // 使用动画效果
            gameCompleteOverlay.setAlpha(0f);
            gameCompleteOverlay.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
                
            // 显示祝贺消息
            Toast.makeText(this, "Congratulations! You completed all levels!", Toast.LENGTH_LONG).show();
        }
    }
    
    // 创建一个新方法，简化调用
    private void goToNextLevel() {
        Log.d("GameActivity", "准备进入下一关");
        
        try {
            // 检查活动是否已被销毁
            if (isFinishing() || isDestroyed()) {
                Log.e("GameActivity", "活动已结束，无法进入下一关");
                return;
            }
            
            // 重置标志
            isLevelCompleteHandling = false;
            
            // 增加关卡
            currentLevel++;
    
            if (currentLevel <= MAX_LEVEL) {
                // 清除当前游戏状态
                if (gameTimer != null) {
                    gameTimer.cancel();
                }
                
                // 重置移动次数，但保留得分
                movesCount = 0;
                timeRemaining = 60; // 重置时间
                
                // 清除旧的拼图视图
                FrameLayout puzzleContainer = findViewById(R.id.puzzleContainer);
                puzzleContainer.removeAllViews();
                
                // 处理旧的board数组
                for (int r = 0; r < NUM_ROWS; r++) {
                    for (int c = 0; c < NUM_COLS; c++) {
                        board[r][c] = -1;
                    }
                }
                
                // 清除旧的blocks列表
                blocks.clear();
                
                // 重新设置游戏
                setupGame();
                
                // 更新UI
                updateUIText();
                
                // 开始新游戏
                startGame();
                
                // 显示关卡信息
                Toast.makeText(this, "Level " + currentLevel + " started!", Toast.LENGTH_SHORT).show();
                
                Log.d("GameActivity", "已进入关卡: " + currentLevel);
            } else {
                // 所有关卡完成
                Toast.makeText(this, "Congratulations! You completed all levels!", Toast.LENGTH_LONG)
                    .show();
                
                // 可以返回主菜单或显示最终得分等
                new AlertDialog.Builder(this)
                    .setTitle("Game Completed")
                    .setMessage("You have completed all levels with a total score of " + score + "!")
                    .setPositiveButton("Back to Menu", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            }
        } catch (Exception e) {
            Log.e("GameActivity", "进入下一关时出错: " + e.getMessage());
            // 尝试简单重置当前关卡
            try {
                resetLevel();
            } catch (Exception ex) {
                // 如果连重置都失败，则退出活动
                Log.e("GameActivity", "重置关卡失败: " + ex.getMessage());
                finish();
            }
        }
    }

    private void onGameOver() {
        // 显示游戏结束对话框
        new AlertDialog.Builder(this)
            .setTitle("Time's Up!")
            .setMessage("Your score: " + score)
            .setPositiveButton("Try Again", (dialog, which) -> resetLevel())
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private int calculateScore() {
        // 基础分数
        int levelScore = BASE_SCORE;

        // 时间奖励
        int timeBonus = timeRemaining * TIME_BONUS_MULTIPLIER;

        // 移动惩罚
        int movePenalty = movesCount * MOVE_PENALTY;

        // 总分
        return levelScore + timeBonus - movePenalty;
    }

    /**
     * Helper to place a block onto the board array.
     */
    private void placeBlockOnBoard(Block block) {
        for (int r = block.row; r < block.row + block.height; r++) {
            for (int c = block.col; c < block.col + block.width; c++) {
                board[r][c] = block.id;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 如果游戏正在进行，暂停游戏
        if (!isPaused && gameTimer != null) {
            pauseGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消计时器
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        
        // 清除所有可能的引用
        puzzleView = null;
        blocks.clear();
    }
}