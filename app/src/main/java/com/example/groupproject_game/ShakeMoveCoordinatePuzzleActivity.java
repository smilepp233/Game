package com.example.groupproject_game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.groupproject_game.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ShakeMoveCoordinatePuzzleActivity - A physics-based puzzle game,
 * integrating accelerometer and increasing difficulty levels.
 */
public class ShakeMoveCoordinatePuzzleActivity extends AppCompatActivity implements SensorEventListener {

    // 常量
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final long SHAKE_COOLDOWN_MS = 600;
    private static final int GAME_DURATION_MS = 30000;
    private static final float FRICTION = 0.97f;
    private static final float RESTITUTION = 0.8f;
    private static final int MAX_LEVEL = 5;
    private static final int BASE_SCORE = 200;
    private static final int TIME_BONUS_MULTIPLIER = 2;
    private static final int LEVEL_BONUS_MULTIPLIER = 50;
    private static final int SHAKE_BONUS_MULTIPLIER = 25;
    private static final int MOVE_PENALTY = 1;
    private static final float DECOY_SPEED_INCREASE = 0.2f;
    private static final int SPECIAL_EFFECT_CHANCE = 20; // 20% chance for special effects

    // 移除重复的声明，只保留一个计时器变量
    private CountDownTimer gameTimer;

    // 游戏状态
    private enum GameState { READY, PLAYING, PAUSED, COMPLETE, FAILED }
    private GameState currentState = GameState.READY;

    // UI元素
    private ImageView puzzlePiece;
    private List<ImageView> decoyPieces = new ArrayList<>();
    private ImageView targetSlot;
    private TextView scoreText, timerText, levelText, shakesText;
    private View shakeFlash;
    private ConstraintLayout gameLayout;

    // 游戏机制
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private float dX, dY, velocityX, velocityY;
    private int score = 0;
    private int moves = 0;
    private int level = 1;
    private int maxLevel = 5;
    private int shakesRemaining = 3;
    private boolean isPieceGrabbed = false;
    private Random random = new Random();

    // 服务

    private Vibrator vibrator;
    private CountDownTimer timer;
    private Handler physicsHandler = new Handler(Looper.getMainLooper());
    private Runnable physicsTick = new Runnable() {
        @Override
        public void run() {
            if (currentState == GameState.PLAYING) {
                updatePhysics();
            }
            // 即使不在PLAYING状态也要继续调度，以保持线程活跃
            physicsHandler.postDelayed(this, 16); // ~60fps
        }
    };

    // 添加新的游戏状态
    private boolean isSpecialEffectActive = false;
    private int consecutiveHits = 0;
    private float currentDecoySpeed = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shake_move_puzzle);

        // 初始化服务
        //userManager = new UserManager(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initializeUI();
        setupGameObjects();
        showTutorial();

        // 在创建后立即启动物理引擎，但使用post而不是postDelayed
        physicsHandler.post(physicsTick);
    }

    private void initializeUI() {
        gameLayout = findViewById(R.id.gameLayout);
        puzzlePiece = findViewById(R.id.movableObject);
        targetSlot = findViewById(R.id.realTarget);
        decoyPieces.add(findViewById(R.id.fakeObject));

        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        shakeFlash = findViewById(R.id.shakeFlash);
        levelText = findViewById(R.id.levelText);

        // 如果找不到关卡UI，则创建一个
        if (levelText == null) {
            levelText = new TextView(this);
            levelText.setText("Level: " + level);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            levelText.setLayoutParams(params);
            levelText.setTextColor(Color.WHITE);
            levelText.setTextSize(18);
            if (gameLayout != null) {
                gameLayout.addView(levelText);
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) levelText.getLayoutParams();
                layoutParams.topToBottom = R.id.timerText;
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams.setMargins(16, 8, 0, 0);
                levelText.setLayoutParams(layoutParams);
            }
        }

        // 如果不存在则添加摇晃计数器UI元素
        shakesText = findViewById(R.id.shakesText);
        if (shakesText == null) {
            shakesText = new TextView(this);
            shakesText.setText("Shakes: " + shakesRemaining);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            shakesText.setLayoutParams(params);
            shakesText.setTextColor(Color.WHITE);
            shakesText.setTextSize(18);
            if (gameLayout != null) {
                gameLayout.addView(shakesText);
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) shakesText.getLayoutParams();
                layoutParams.topToBottom = R.id.scoreText;
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams.setMargins(16, 8, 0, 0);
                shakesText.setLayoutParams(layoutParams);
            }
        }
    }

    private void setupGameObjects() {
        // 设置视觉吸引力的位图
        puzzlePiece.setImageBitmap(createPuzzlePieceBitmap());
        targetSlot.setImageBitmap(createTargetSlotBitmap());

        // 固定目标位置 - 只在游戏开始时随机放置一次
        targetSlot.post(() -> {
            // 获取父视图尺寸
            int parentWidth = ((View)targetSlot.getParent()).getWidth();
            int parentHeight = ((View)targetSlot.getParent()).getHeight();
            
            // 放置在屏幕上部区域的随机位置
            targetSlot.setX(random.nextInt(parentWidth - targetSlot.getWidth()));
            targetSlot.setY(random.nextInt(parentHeight/3) + parentHeight/10);
            
            // 确保不要添加任何动画到目标
            targetSlot.clearAnimation();
        });

        for (ImageView decoy : decoyPieces) {
            decoy.setImageBitmap(createDecoyPieceBitmap());
            animateDecoyMovement(decoy);
        }

        // 高级触摸处理和物理效果
        puzzlePiece.setOnTouchListener(new View.OnTouchListener() {
            private float lastTouchX, lastTouchY;
            private long lastTouchTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (currentState != GameState.PLAYING) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        lastTouchTime = System.currentTimeMillis();

                        isPieceGrabbed = true;
                        view.animate()
                            .scaleX(1.15f)
                            .scaleY(1.15f)
                            .setDuration(150)
                            .setInterpolator(new AnticipateOvershootInterpolator())
                            .start();

                        // 触感反馈
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // 计算物理速度
                        long currentTime = System.currentTimeMillis();
                        float timeElapsed = (currentTime - lastTouchTime) / 1000f;
                        if (timeElapsed > 0) {
                            velocityX = (event.getRawX() - lastTouchX) / timeElapsed;
                            velocityY = (event.getRawY() - lastTouchY) / timeElapsed;
                        }

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        lastTouchTime = currentTime;

                        // 移动拼图块并检查边界
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        newX = Math.max(0, Math.min(newX, getWindow().getDecorView().getWidth() - view.getWidth()));
                        newY = Math.max(0, Math.min(newY, getWindow().getDecorView().getHeight() - view.getHeight()));

                        view.setX(newX);
                        view.setY(newY);

                        moves++;
                        checkCollisions();
                        break;

                    case MotionEvent.ACTION_UP:
                        isPieceGrabbed = false;
                        view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new BounceInterpolator())
                            .start();

                        // 应用动量
                        velocityX *= 1.2f;
                        velocityY *= 1.2f;
                        break;
                }
                return true;
            }
        });
    }

    private void updatePhysics() {
        if (isPieceGrabbed) return;

        // 只有当拼图块在移动时才应用物理效果
        if (Math.abs(velocityX) > 0.1 || Math.abs(velocityY) > 0.1) {
            runOnUiThread(() -> {
                // 根据速度更新位置
                float newX = puzzlePiece.getX() + velocityX * 0.016f;
                float newY = puzzlePiece.getY() + velocityY * 0.016f;

                // 应用边界碰撞
                float width = getWindow().getDecorView().getWidth();
                float height = getWindow().getDecorView().getHeight();

                if (newX < 0) {
                    newX = 0;
                    velocityX = -velocityX * RESTITUTION;
                    provideBoundaryFeedback();
                } else if (newX > width - puzzlePiece.getWidth()) {
                    newX = width - puzzlePiece.getWidth();
                    velocityX = -velocityX * RESTITUTION;
                    provideBoundaryFeedback();
                }

                if (newY < 0) {
                    newY = 0;
                    velocityY = -velocityY * RESTITUTION;
                    provideBoundaryFeedback();
                } else if (newY > height - puzzlePiece.getHeight()) {
                    newY = height - puzzlePiece.getHeight();
                    velocityY = -velocityY * RESTITUTION;
                    provideBoundaryFeedback();
                }

                // 应用摩擦力
                velocityX *= FRICTION;
                velocityY *= FRICTION;

                // 更新位置
                puzzlePiece.setX(newX);
                puzzlePiece.setY(newY);

                checkCollisions();
            });
        }
    }

    private void provideBoundaryFeedback() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void checkCollisions() {
        // 检查与目标的碰撞
        if (isViewOverlapping(puzzlePiece, targetSlot)) {
            onTargetReached();
            return; // 如果到达目标，不再检查其他碰撞
        }

        // 检查与诱饵的碰撞
        for (ImageView decoy : decoyPieces) {
            if (decoy.getVisibility() == View.VISIBLE && isViewOverlapping(puzzlePiece, decoy)) {
                onDecoyCollision(decoy);
                break; // 只处理一次碰撞
            }
        }
    }

    private boolean isViewOverlapping(View firstView, View secondView) {
        if (firstView == null || secondView == null) return false;
        
        int[] firstLoc = new int[2];
        int[] secondLoc = new int[2];
        firstView.getLocationOnScreen(firstLoc);
        secondView.getLocationOnScreen(secondLoc);
        
        // 计算视图的边界
        int firstLeft = firstLoc[0];
        int firstTop = firstLoc[1];
        int firstRight = firstLeft + firstView.getWidth();
        int firstBottom = firstTop + firstView.getHeight();
        
        int secondLeft = secondLoc[0];
        int secondTop = secondLoc[1];
        int secondRight = secondLeft + secondView.getWidth();
        int secondBottom = secondTop + secondView.getHeight();
        
        // 检查是否有重叠
        return !(firstRight < secondLeft || 
                 firstLeft > secondRight || 
                 firstBottom < secondTop || 
                 firstTop > secondBottom);
    }

    private void onTargetReached() {
        // 关闭计时器
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // 停止玩家移动
        velocityX = 0;
        velocityY = 0;
        isPieceGrabbed = false;

        // 计算分数和奖励
        int timeBonus = (int)((GAME_DURATION_MS / 100) * (timerText.getText().toString().contains("Time") ? 
            Float.parseFloat(timerText.getText().toString().replaceAll("[^\\d.]", "")) / 30.0f : 0));
        timeBonus *= TIME_BONUS_MULTIPLIER;
        
        int movesPenalty = Math.min(moves * MOVE_PENALTY, 100);
        int levelBonus = level * LEVEL_BONUS_MULTIPLIER;
        int shakeBonus = shakesRemaining * SHAKE_BONUS_MULTIPLIER;
        
        // 连击奖励
        consecutiveHits++;
        int comboBonus = consecutiveHits * 10;
        
        int stageScore = BASE_SCORE + timeBonus + levelBonus + shakeBonus + comboBonus - movesPenalty;
        score += stageScore;
        
        // 更新UI
        int finalTimeBonus = timeBonus;
        runOnUiThread(() -> {
            scoreText.setText("Score: " + score);
            
            // 锁定拼图块到目标位置
            puzzlePiece.animate()
                .x(targetSlot.getX() + (targetSlot.getWidth() - puzzlePiece.getWidth()) / 2)
                .y(targetSlot.getY() + (targetSlot.getHeight() - puzzlePiece.getHeight()) / 2)
                .setDuration(200)
                .start();
            
            // 显示高级视觉效果
            showTargetReachedEffect();
            
            // 显示得分详情
            Toast.makeText(this, 
                "Time Bonus: " + finalTimeBonus + "\n" +
                "Level Bonus: " + levelBonus + "\n" +
                "Shake Bonus: " + shakeBonus + "\n" +
                "Combo Bonus: " + comboBonus + "\n" +
                "Total: " + stageScore, 
                Toast.LENGTH_LONG).show();
        });
        
        // 延迟一点时间以展示视觉效果
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 庆祝成功
            celebrateSuccess();
            
            // 进入下一关或完成游戏
            if (level < MAX_LEVEL) {
                level++;
                prepareNextLevel();
            } else {
                completeGame();
            }
        }, 800);
    }

    private void showTargetReachedEffect() {
        // 为目标和拼图创建闪光效果
        View flashOverlay = new View(this);
        flashOverlay.setBackgroundColor(Color.WHITE);
        flashOverlay.setAlpha(0.8f);
        
        // 添加到布局
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        gameLayout.addView(flashOverlay, params);
        
        // 闪光动画
        flashOverlay.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction(() -> gameLayout.removeView(flashOverlay))
            .start();
        
        // 目标和拼图旋转动画
        targetSlot.animate()
            .rotation(targetSlot.getRotation() + 360)
            .setDuration(600)
            .start();
        
        puzzlePiece.animate()
            .rotation(puzzlePiece.getRotation() + 360)
            .setDuration(600)
            .start();
    }

    private void celebrateSuccess() {
        // 触感反馈
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 50, 100, 50, 100, 150};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }
        }
        
        // 视觉反馈
        runOnUiThread(() -> {
            // 缩放动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(puzzlePiece, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(puzzlePiece, "scaleY", 1f, 1.2f, 1f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY);
            animSet.setDuration(300);
            animSet.start();
            
            // 随机特殊效果
            if (random.nextInt(100) < SPECIAL_EFFECT_CHANCE) {
                activateSpecialEffect();
            }
        });
    }

    private void activateSpecialEffect() {
        isSpecialEffectActive = true;
        
        // 随机选择特殊效果
        int effect = random.nextInt(3);
        switch (effect) {
            case 0: // 时间暂停
                if (gameTimer != null) {
                    gameTimer.cancel();
                    new Handler().postDelayed(() -> {
                        startGame();
                        isSpecialEffectActive = false;
                    }, 2000);
                }
                break;
            case 1: // 诱饵减速
                currentDecoySpeed = 0.5f;
                new Handler().postDelayed(() -> {
                    currentDecoySpeed = 1.0f;
                    isSpecialEffectActive = false;
                }, 3000);
                break;
            case 2: // 额外摇晃次数
                shakesRemaining += 2;
                updateShakesDisplay();
                isSpecialEffectActive = false;
                break;
        }
    }

    private void prepareNextLevel() {
        // 显示关卡过渡
        runOnUiThread(() -> {
            // 创建关卡过渡动画
            View levelTransition = new View(this);
            levelTransition.setBackgroundColor(Color.parseColor("#3F51B5"));
            levelTransition.setAlpha(0f);
            
            // 使用临时变量名避免与类成员变量冲突
            TextView levelTransitionText = new TextView(this);
            levelTransitionText.setText("LEVEL " + level);
            levelTransitionText.setTextSize(40);
            levelTransitionText.setTextColor(Color.WHITE);
            levelTransitionText.setGravity(android.view.Gravity.CENTER);
            levelTransitionText.setAlpha(0f);
            
            // 添加到布局
            ConstraintLayout.LayoutParams bgParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            );
            gameLayout.addView(levelTransition, bgParams);
            
            ConstraintLayout.LayoutParams textParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            textParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            textParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            gameLayout.addView(levelTransitionText, textParams);
            
            // 淡入动画
            levelTransition.animate()
                .alpha(0.9f)
                .setDuration(400)
                .start();
            
            levelTransitionText.animate()
                .alpha(1f)
                .setDuration(400)
                .start();
            
            // 重置拼图块
            resetPuzzlePiece();
            
            // 增加难度
            increaseDifficulty();
            
            // 更新摇晃次数
            shakesRemaining = 2 + level;
            updateShakesDisplay();
            
            // 设置新的目标位置
            int parentWidth = ((View)targetSlot.getParent()).getWidth();
            int parentHeight = ((View)targetSlot.getParent()).getHeight();
            targetSlot.setX(random.nextInt(parentWidth - targetSlot.getWidth()));
            targetSlot.setY(random.nextInt(parentHeight/3) + parentHeight/10);
            
            // 更新主UI显示当前关卡
            if (levelText != null) {
                levelText.setText("Level: " + level);
            }
            
            // 淡出动画和开始新关卡
            new Handler().postDelayed(() -> {
                levelTransition.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        gameLayout.removeView(levelTransition);
                        gameLayout.removeView(levelTransitionText);
                        
                        if (currentState != GameState.COMPLETE) {
                            startGame();
                        }
                    })
                    .start();
                
                levelTransitionText.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .start();
            }, 1500);
        });
    }

    private void increaseDifficulty() {
        // 根据关卡添加更多诱饵
        addDecoys(level);
        
        // 增加诱饵速度
        currentDecoySpeed += DECOY_SPEED_INCREASE;
        
        // 更新现有诱饵的动画
        for (ImageView decoy : decoyPieces) {
            decoy.clearAnimation();
            animateDecoyMovement(decoy);
        }
    }

    private void addDecoys(int count) {
        // 根据关卡添加额外的诱饵
        for (int i = decoyPieces.size(); i < Math.min(count + 1, 5); i++) {
            ImageView newDecoy = new ImageView(this);
            newDecoy.setImageBitmap(createDecoyPieceBitmap());

            // 添加到布局并随机定位
            addViewToLayout(newDecoy);
            positionRandomly(newDecoy);

            animateDecoyMovement(newDecoy);
            decoyPieces.add(newDecoy);
        }
    }

    private void addViewToLayout(View view) {
        // 添加视图到约束布局
        if (gameLayout != null) {
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            view.setLayoutParams(params);
            gameLayout.addView(view);
        }
    }

    private void positionRandomly(View view) {
        view.post(() -> {
            int parentWidth = ((View)view.getParent()).getWidth();
            int parentHeight = ((View)view.getParent()).getHeight();

            view.setX(random.nextInt(parentWidth - view.getWidth()));
            view.setY(random.nextInt(parentHeight - view.getHeight()));
        });
    }

    private void animateDecoyMovement(ImageView decoy) {
        float baseSpeed = 1500f + (level * 500f);
        baseSpeed *= currentDecoySpeed;
        
        // 复杂路径移动
        Path path = new Path();
        path.addCircle(0, 0, 100 + random.nextInt(100), Path.Direction.CW);
        
        ObjectAnimator pathAnimator = ObjectAnimator.ofFloat(decoy, View.X, View.Y, path);
        pathAnimator.setDuration((long)(baseSpeed + random.nextInt(1000)));
        pathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        ObjectAnimator rotation = ObjectAnimator.ofFloat(decoy, "rotation", 0, 360);
        rotation.setDuration(2000);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(pathAnimator, rotation);
        animSet.start();
    }

    private void resetPuzzlePiece() {
        moves = 0;
        velocityX = 0;
        velocityY = 0;

        // 位于屏幕底部
        puzzlePiece.post(() -> {
            puzzlePiece.setX(getWindow().getDecorView().getWidth() / 2f - puzzlePiece.getWidth() / 2f);
            puzzlePiece.setY(getWindow().getDecorView().getHeight() - puzzlePiece.getHeight() - 100);
        });
    }

    private void updateShakesDisplay() {
        if (shakesText != null) {
            shakesText.setText("Shakes: " + shakesRemaining);
        }
    }

    private void completeGame() {
        currentState = GameState.COMPLETE;

        // 保存进度
        //UserManager.User currentUser = userManager.getCurrentUser();
        //if (currentUser != null) {
        //    userManager.updateStageProgress(currentUser.username, 4, score, GAME_DURATION_MS);
        //}
        //
        // 显示完成对话框
        new AlertDialog.Builder(this)
            .setTitle("Game Completed!")
            .setMessage("Congratulations! You've completed all levels!\n\nFinal score: " + score)
            .setPositiveButton("Awesome!", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void startGame() {
        currentState = GameState.PLAYING;

        // 设置倒计时器并提供视觉反馈
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        gameTimer = new CountDownTimer(GAME_DURATION_MS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (currentState == GameState.PLAYING) {
                    runOnUiThread(() -> {
                        if (timerText != null) {
                            timerText.setText(String.format("Time: %.1fs", millisUntilFinished / 1000f));
                            // 当时间不多时的视觉警告
                            if (millisUntilFinished < 5000) {
                                timerText.setTextColor(Color.RED);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFinish() {
                if (currentState == GameState.PLAYING) {
                    runOnUiThread(() -> {
                        if (timerText != null) {
                            timerText.setText("Time's up!");
                            onGameOver();
                        }
                    });
                }
            }
        }.start();
    }
    private void onGameOver() {
        currentState = GameState.FAILED;

        // 保存当前进度
        //UserManager.User currentUser = userManager.getCurrentUser();
        //if (currentUser != null) {
        //    userManager.updateStageProgress(currentUser.username, 4, score, GAME_DURATION_MS);
        //}
        //
        // 显示游戏结束对话框
        new AlertDialog.Builder(this)
            .setTitle("Time's Up!")
            .setMessage("Your final score: " + score)
            .setPositiveButton("Try Again", (dialog, which) -> {
                resetGame();
                startGame();
            })
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .show();
    }

    private void resetGame() {
        level = 1;
        score = 0;
        moves = 0;
        shakesRemaining = 3;
        currentDecoySpeed = 1.0f; // 重置诱饵速度
        consecutiveHits = 0; // 重置连击数

        // 清除多余的诱饵
        for (int i = decoyPieces.size() - 1; i > 0; i--) {
            ImageView decoy = decoyPieces.remove(i);
            if (decoy != null && decoy.getParent() != null) {
                ((ViewGroup)decoy.getParent()).removeView(decoy);
            }
        }

        // 将UI更新放在主线程
        runOnUiThread(() -> {
            scoreText.setText("Score: 0");
            if (levelText != null) {
                levelText.setText("Level: " + level);
            }
            resetPuzzlePiece();
            updateShakesDisplay();
        });
    }

    private void showTutorial() {
        new AlertDialog.Builder(this)
            .setTitle("Puzzle Challenge")
            .setMessage("Complete all levels by dragging the puzzle piece to the target!\n\n" +
                       "Shake your device to temporarily clear decoys.\n\n" +
                       "Be careful, collisions will reduce your score!")
            .setPositiveButton("Start Game", (dialog, which) -> {
                resetGame();
                startGame();
            })
            .setCancelable(false)
            .show();
    }

    // 创建彩色拼图块位图
    private Bitmap createPuzzlePieceBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 渐变填充
        RadialGradient gradient = new RadialGradient(
            40, 40, 35,
            new int[]{Color.parseColor("#FF5722"), Color.parseColor("#FFC107")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        canvas.drawCircle(40, 40, 35, paint);

        // 边框
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawCircle(40, 40, 35, paint);

        // 添加标签
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(16);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Puzzle", 40, 45, paint);

        return bitmap;
    }

    // 创建目标槽位图
    private Bitmap createTargetSlotBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 渐变背景
        LinearGradient gradient = new LinearGradient(
            0, 0, 60, 60,
            Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        canvas.drawRect(5, 5, 55, 55, paint);

        // 边框
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawRect(5, 5, 55, 55, paint);

        // 添加标签
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(14);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Target", 30, 35, paint);

        return bitmap;
    }

    // 创建诱饵位图
    private Bitmap createDecoyPieceBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 渐变填充
        RadialGradient gradient = new RadialGradient(
            50, 50, 40,
            new int[]{Color.GRAY, Color.DKGRAY},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        canvas.drawCircle(50, 50, 40, paint);

        // 边框
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(50, 50, 40, paint);

        // 添加标签
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(16);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Decoy", 50, 55, paint);

        return bitmap;
    }

    // 传感器事件处理
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER ||
            currentState != GameState.PLAYING ||
            shakesRemaining <= 0) return;

        long currentTime = System.currentTimeMillis();
        // 添加冷却检查以避免过度处理
        if (currentTime - lastShakeTime <= SHAKE_COOLDOWN_MS) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            lastShakeTime = currentTime;

            // 使用Handler将UI操作发送到主线程
            runOnUiThread(() -> {
                shakesRemaining--;
                updateShakesDisplay();

                // 闪烁效果指示摇晃动作
                shakeFlash.setVisibility(View.VISIBLE);
                shakeFlash.setAlpha(1f);
                shakeFlash.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> shakeFlash.setVisibility(View.GONE))
                    .start();

                // 暂时隐藏所有诱饵
                for (ImageView decoy : decoyPieces) {
                    decoy.animate().alpha(0f).setDuration(300).start();
                }

                // 2秒后恢复诱饵
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    for (ImageView decoy : decoyPieces) {
                        decoy.animate().alpha(1f).setDuration(300).start();
                    }
                }, 2000);

                // 触感反馈
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(200);
                    }
                }

                Toast.makeText(ShakeMoveCoordinatePuzzleActivity.this, "Decoys cleared! Keep going!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 不需要处理
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (currentState == GameState.PAUSED) {
            startGame(); // 重新开始计时器
            currentState = GameState.PLAYING;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (currentState == GameState.PLAYING) {
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            currentState = GameState.PAUSED;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }

        // 确保移除所有回调
        if (physicsHandler != null) {
            physicsHandler.removeCallbacksAndMessages(null);
        }

        // 释放传感器
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // 释放震动器
        if (vibrator != null) {
            vibrator = null;
        }
    }

    // 处理后退按钮
    @Override
    public void onBackPressed() {
        if (currentState == GameState.PLAYING) {
            // 暂停游戏并询问用户是否要退出
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            currentState = GameState.PAUSED;

            new AlertDialog.Builder(this)
                .setTitle("Exit Game")
                .setMessage("Are you sure you want to exit? Current progress will be saved.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("Continue Playing", (dialog, which) -> {
                    startGame(); // 重新开始计时器
                    currentState = GameState.PLAYING;
                })
                .setCancelable(false)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    private void onDecoyCollision(ImageView decoy) {
        // 分数惩罚
        score = Math.max(0, score - 15);
        consecutiveHits = 0; // 重置连击
        
        runOnUiThread(() -> {
            scoreText.setText("Score: " + score);
            
            // 视觉和触感反馈
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(puzzlePiece, "scaleX", 1f, 0.8f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(puzzlePiece, "scaleY", 1f, 0.8f, 1f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY);
            animSet.setDuration(300);
            animSet.start();
            
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(200);
                }
            }
            
            // 显示碰撞提示
            Toast.makeText(this, "Collision! -15 points", Toast.LENGTH_SHORT).show();
        });
        
        // 排斥物理效果
        float centerX1 = puzzlePiece.getX() + puzzlePiece.getWidth() / 2;
        float centerY1 = puzzlePiece.getY() + puzzlePiece.getHeight() / 2;
        float centerX2 = decoy.getX() + decoy.getWidth() / 2;
        float centerY2 = decoy.getY() + decoy.getHeight() / 2;
        
        float repulsionX = centerX1 - centerX2;
        float repulsionY = centerY1 - centerY2;
        float magnitude = (float) Math.sqrt(repulsionX * repulsionX + repulsionY * repulsionY);
        
        if (magnitude > 0) {
            velocityX = repulsionX / magnitude * 800;
            velocityY = repulsionY / magnitude * 800;
        }
    }
}