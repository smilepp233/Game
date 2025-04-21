package com.example.groupproject_game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.groupproject_game.model.TrickElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TricksterPathActivity extends AppCompatActivity {

    // 常量
    private static final int MAX_LEVEL = 10;
    private static final int DEATH_COUNT_TO_HINT = 3; // 死亡3次后给提示
    
    // 音效播放器
    private MediaPlayer mpNani;
    private MediaPlayer mpYeehaw;
    private MediaPlayer mpLaugh;
    private MediaPlayer mpCollect;
    private MediaPlayer mpFail;
    private MediaPlayer mpWin;
    
    // 3D效果设置
    private boolean is3DMode = true;  // 3D模式开关
    private float perspectiveScale = 1.2f; // 透视缩放系数
    private int shadowOffset = 8; // 阴影偏移量
    
    // 游戏状态
    private int currentLevel = 1;
    private int score = 0;
    private int deathCount = 0;
    private boolean isControlReversed = false;
    private boolean hasReachedFakeGoal = false;
    
    // UI元素
    private TextView levelText;
    private TextView scoreText;
    private TextView messageText;
    private Button restartButton;
    private TrickGameView gameView;
    
    // 方向控制按钮
    private Button btnUp;
    private Button btnDown;
    private Button btnLeft;
    private Button btnRight;
    private Button btnJump;
    private Button btnDoubleJump; // 新增双跳按钮
    
    // 游戏对象
    private Player player;
    private List<TrickElement> elements = new ArrayList<>();
    private Random random = new Random();
    
    // 系统服务
    private Vibrator vibrator;
    private Handler handler = new Handler();
    
    // 键盘状态
    private boolean keyW = false;
    private boolean keyA = false;
    private boolean keyS = false;
    private boolean keyD = false;
    
    // 跟踪按键状态
    private boolean keyWPressed = false; // 用于检测W键的双击
    private long lastWPressTime = 0; // 上次W键按下的时间
    private static final long DOUBLE_PRESS_INTERVAL = 300; // 双击间隔时间（毫秒）
    
    // 光照系统
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private float currentLightLevel = 100; // 默认光照值
    private static final float MAX_LIGHT_LEVEL = 1000; // 最大光照级别
    private boolean isAmbientLightEnabled = true; // 环境光开关
    private int sunlightColor = Color.parseColor("#FFFFCC"); // 阳光颜色
    private float sunPosX = 0.2f; // 太阳水平位置（相对于屏幕宽度的比例）
    private float sunPosY = 0.1f; // 太阳垂直位置（相对于屏幕高度的比例）
    private float sunRadius = 80; // 太阳半径
    private float sunIntensity = 1.0f; // 阳光强度
    private float[] recentLightReadings = new float[5]; // 存储最近几次光线传感器读数
    private int readingsIndex = 0; // 读数索引
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trickster_path);
        
        // 初始化UI元素
        levelText = findViewById(R.id.levelText);
        scoreText = findViewById(R.id.scoreText);
        messageText = findViewById(R.id.messageText);
        restartButton = findViewById(R.id.restartButton);
        
        // 初始化音效播放器
        initializeSoundPlayers();
        
        // 初始化方向控制按钮
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnJump = findViewById(R.id.btnJump);
        btnDoubleJump = findViewById(R.id.btnDoubleJump); // 初始化双跳按钮
        
        // 获取系统服务
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // 初始化游戏视图
        FrameLayout gameContainer = findViewById(R.id.gameContainer);
        gameView = new TrickGameView(this);
        gameContainer.addView(gameView);
        
        // 设置按钮监听器
        restartButton.setOnClickListener(v -> restartLevel());
        
        // 添加3D模式切换按钮
        Button btn3DMode = new Button(this);
        btn3DMode.setText("3D模式");
        btn3DMode.setOnClickListener(v -> toggle3DMode());
        
        // 设置按钮样式和位置
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        params.topMargin = 10;
        params.rightMargin = 10;
        btn3DMode.setLayoutParams(params);
        
        // 设置按钮背景和样式
        btn3DMode.setBackgroundColor(Color.parseColor("#4CAF50"));
        btn3DMode.setTextColor(Color.WHITE);
        btn3DMode.setPadding(20, 10, 20, 10);
        btn3DMode.setAlpha(0.8f);
        
        gameContainer.addView(btn3DMode);
        
        // 设置方向控制按钮监听器
        setupDirectionButtons();
        
        // 初始化游戏
        initializeGame();
        
        // 添加调试消息
        Toast.makeText(this, "游戏已启动，请使用方向按钮或WASD控制", Toast.LENGTH_SHORT).show();
        
        // 确保视图可以接收按键事件
        gameView.setFocusable(true);
        gameView.setFocusableInTouchMode(true);
        gameView.requestFocus();
        
        // 初始化光照传感器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d("TricksterGame", "设备没有光线传感器");
            }
        }
        
        // 添加光照模式切换按钮
        Button btnLightMode = new Button(this);
        btnLightMode.setText("环境光");
        btnLightMode.setOnClickListener(v -> toggleAmbientLight());
        
        // 设置按钮样式和位置
        FrameLayout.LayoutParams lightBtnParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        lightBtnParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        lightBtnParams.topMargin = 70; // 放在3D模式按钮下方
        lightBtnParams.rightMargin = 10;
        btnLightMode.setLayoutParams(lightBtnParams);
        
        // 设置按钮背景和样式
        btnLightMode.setBackgroundColor(Color.parseColor("#FFC107"));
        btnLightMode.setTextColor(Color.BLACK);
        btnLightMode.setPadding(20, 10, 20, 10);
        btnLightMode.setAlpha(0.8f);
        
        gameContainer.addView(btnLightMode);
    }
    
    /**
     * 初始化所有音效播放器
     */
    private void initializeSoundPlayers() {
        mpNani = MediaPlayer.create(this, R.raw.nani);
        mpYeehaw = MediaPlayer.create(this, R.raw.yeehaw);
        mpLaugh = MediaPlayer.create(this, R.raw.laugh);
        mpCollect = MediaPlayer.create(this, R.raw.collect_sound);
        mpFail = MediaPlayer.create(this, R.raw.fail_sound);
        mpWin = MediaPlayer.create(this, R.raw.win_sound);
    }
    
    /**
     * 播放指定的音效
     * @param mediaPlayer 要播放的MediaPlayer对象
     */
    private void playSound(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            try {
                // 如果正在播放，先停止
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.prepare();
                    mediaPlayer.seekTo(0);
                }
                // 播放音效
                mediaPlayer.start();
            } catch (Exception e) {
                Log.e("TricksterGame", "播放音效失败: " + e.getMessage());
                
                // 如果出错，尝试重新创建
                if (mediaPlayer == mpNani) {
                    mpNani = MediaPlayer.create(this, R.raw.nani);
                    if (mpNani != null) mpNani.start();
                } else if (mediaPlayer == mpYeehaw) {
                    mpYeehaw = MediaPlayer.create(this, R.raw.yeehaw);
                    if (mpYeehaw != null) mpYeehaw.start();
                } else if (mediaPlayer == mpLaugh) {
                    mpLaugh = MediaPlayer.create(this, R.raw.laugh);
                    if (mpLaugh != null) mpLaugh.start();
                } else if (mediaPlayer == mpCollect) {
                    mpCollect = MediaPlayer.create(this, R.raw.collect_sound);
                    if (mpCollect != null) mpCollect.start();
                } else if (mediaPlayer == mpFail) {
                    mpFail = MediaPlayer.create(this, R.raw.fail_sound);
                    if (mpFail != null) mpFail.start();
                } else if (mediaPlayer == mpWin) {
                    mpWin = MediaPlayer.create(this, R.raw.win_sound);
                    if (mpWin != null) mpWin.start();
                }
            }
        }
    }
    
    private void initializeGame() {
        // 创建玩家
        player = new Player(100, 500, 50, 50);
        
        // 设置关卡
        setupLevel(currentLevel);
        
        // 更新UI
        updateUI();
    }
    
    private void setupLevel(int level) {
        // 清空元素列表
        elements.clear();
        
        // 根据关卡设置元素
        switch(level) {
            case 1:
                createLevel1();
                break;
            case 2:
                createLevel2();
                // 关卡2播放yeehaw音效
                playSound(mpYeehaw);
                break;
            case 3:
                createLevel3();
                // 关卡3播放nani音效，因为有隐形平台
                playSound(mpNani);
                break;
            case 4:
                createLevel4();
                // 关卡4播放yeehaw音效，因为有弹簧陷阱
                playSound(mpYeehaw);
                break;
            case 5:
                createLevel5();
                break;
            case 6:
                createLevel6();
                // 关卡6播放laugh音效，因为是疯狂关卡
                playSound(mpLaugh);
                break;
            case 7:
                createLevel7();
                // 关卡7播放yeehaw音效，因为是跳跃挑战
                playSound(mpYeehaw);
                break;
            case 8:
                createLevel8();
                // 关卡8播放nani音效，因为是迷宫陷阱
                playSound(mpNani);
                break;
            case 9:
                createLevel9();
                // 关卡9播放laugh音效，因为有反向引导
                playSound(mpLaugh);
                break;
            case 10:
                createLevel10();
                // 最终关卡播放所有音效，依次播放
                new Handler().postDelayed(() -> playSound(mpNani), 0);
                new Handler().postDelayed(() -> playSound(mpYeehaw), 1000);
                new Handler().postDelayed(() -> playSound(mpLaugh), 2000);
                break;
            default:
                createLevel1();
                break;
        }
    }
    
    private void createLevel1() {
        // 关卡1: 基础介绍
        // 添加基本平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 200, 50));
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 300, 600, 200, 50));
        
        // 添加假平台（看起来像正常平台，但会消失）
        TrickElement fakePlatform = new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 600, 600, 200, 50);
        fakePlatform.setMessage("Ha! Gotcha!");
        elements.add(fakePlatform);
        
        // 添加目标旗帜，现在是可见的
        TrickElement goalFlag = new TrickElement(TrickElement.TYPE_GOAL, 800, 500, 50, 100);
        goalFlag.isVisible = true; // 确保可见
        elements.add(goalFlag);
        
        // 添加隐藏的假目标
        TrickElement hiddenFakeGoal = new TrickElement(TrickElement.TYPE_FAKE_GOAL, 600, 400, 100, 100);
        hiddenFakeGoal.isVisible = false;
        elements.add(hiddenFakeGoal);
        
        // 添加假目标
        TrickElement fakeGoal = new TrickElement(TrickElement.TYPE_FAKE_GOAL, 800, 600, 100, 100);
        fakeGoal.setMessage("This is not the real goal!");
        elements.add(fakeGoal);
        
        // 设置玩家初始位置
        player.x = 100;
        player.y = 500;
    }
    
    private void createLevel2() {
        // 关卡2: 反向控制
        // 添加基本平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 150, 50));
        
        // 添加移动平台
        TrickElement movingPlatform = new TrickElement(TrickElement.TYPE_MOVING, 200, 550, 150, 50);
        movingPlatform.moveSpeed = 3.0f;
        movingPlatform.moveDistance = 250.0f;
        elements.add(movingPlatform);
        
        // 添加反向控制区域
        TrickElement reverseArea = new TrickElement(TrickElement.TYPE_REVERSE, 400, 550, 200, 200);
        reverseArea.setMessage("Controls reversed!");
        elements.add(reverseArea);
        
        // 添加目标旗帜
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 750, 450, 50, 100));
        
        // 添加陷阱
        TrickElement trap = new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 600, 550, 100, 50);
        trap.setMessage("Mind the gap!");
        elements.add(trap);
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel3() {
        // 关卡3: 隐形平台
        // 添加起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 150, 50));
        
        // 添加隐形平台路径
        for(int i = 0; i < 4; i++) {
            TrickElement invisiblePlatform = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 200 + i * 150, 600, 100, 50);
            invisiblePlatform.isVisible = false; // 隐形平台，只有踩上去才能看到
            elements.add(invisiblePlatform);
        }
        
        // 添加可见但是假的平台，诱导玩家走错路
        for(int i = 0; i < 3; i++) {
            TrickElement fakePlatform = new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 250 + i * 150, 500, 100, 50);
            fakePlatform.setMessage("Trust nothing!");
            elements.add(fakePlatform);
        }
        
        // 添加目标旗帜
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 800, 500, 50, 100));
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel4() {
        // 关卡4: 弹簧陷阱
        // 添加基本平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 150, 50));
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 750, 400, 150, 50));
        
        // 添加弹簧 - 看起来能弹到终点，但实际上会弹过头
        TrickElement trampoline1 = new TrickElement(TrickElement.TYPE_TRAMPOLINE, 200, 600, 100, 20);
        elements.add(trampoline1);
        
        // 添加隐形平台 - 实际正确路径
        TrickElement hiddenPlatform = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 400, 450, 150, 50);
        hiddenPlatform.isVisible = false;
        elements.add(hiddenPlatform);
        
        // 添加弹簧陷阱 - 弹到天花板上
        TrickElement trapTrampoline = new TrickElement(TrickElement.TYPE_TRAMPOLINE, 400, 400, 100, 20);
        elements.add(trapTrampoline);
        
        // 天花板 - 撞到会受伤
        TrickElement ceiling = new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 300, 100, 300, 20);
        ceiling.setMessage("Ouch! That hurt!");
        elements.add(ceiling);
        
        // 添加目标旗帜
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 800, 300, 50, 100));
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel5() {
        // 关卡5: 简单平台
        // 添加起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 200, 50));
        
        // 添加普通平台路径
        for(int i = 0; i < 4; i++) {
            TrickElement platform = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 250 + i * 200, 600, 150, 50);
            elements.add(platform);
        }
        
        // 添加最后的安全平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 900, 600, 200, 50));
        
        // 添加目标旗帜
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 950, 500, 50, 100));
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    private void createLevel6() {
        // 关卡6: 疯狂但可完成的关卡
        // 添加起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 150, 50));
        
        // 第一个滑动平台 - 玩家需要快速跳跃
        TrickElement slipperyPlatform1 = new TrickElement(TrickElement.TYPE_SLIPPERY, 200, 600, 200, 50);
        elements.add(slipperyPlatform1);
        
        // 弹簧平台 - 弹到上方隐藏平台
        TrickElement trampoline = new TrickElement(TrickElement.TYPE_TRAMPOLINE, 450, 600, 100, 20);
        elements.add(trampoline);
        
        // 隐藏的安全平台网络
        TrickElement hiddenPlatform1 = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 350, 400, 100, 20);
        hiddenPlatform1.isVisible = false;
        elements.add(hiddenPlatform1);
        
        TrickElement hiddenPlatform2 = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 500, 400, 100, 20);
        hiddenPlatform2.isVisible = false;
        elements.add(hiddenPlatform2);
        
        // 移动平台序列 - 形成一个有趣的路径
        TrickElement movingPlatform1 = new TrickElement(TrickElement.TYPE_MOVING, 650, 400, 80, 20);
        movingPlatform1.moveSpeed = 3.0f;
        movingPlatform1.moveDistance = 100.0f;
        elements.add(movingPlatform1);
        
        TrickElement movingPlatform2 = new TrickElement(TrickElement.TYPE_MOVING, 750, 350, 80, 20);
        movingPlatform2.moveSpeed = -2.5f;
        movingPlatform2.moveDistance = 80.0f;
        elements.add(movingPlatform2);
        
        // 假目标 - 诱惑玩家
        TrickElement fakeGoal = new TrickElement(TrickElement.TYPE_FAKE_GOAL, 850, 500, 50, 100);
        elements.add(fakeGoal);
        
        // 真正的目标 - 在移动平台可以到达的位置
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 850, 300, 50, 100));
        
        // 额外的隐藏安全平台 - 以防玩家失误
        TrickElement emergencyPlatform = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 600, 550, 100, 20);
        emergencyPlatform.isVisible = false;
        elements.add(emergencyPlatform);
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel7() {
        // 关卡7: 疯狂跳跃挑战
        // 起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 100, 50));
        
        // 第一阶段：交替的移动和消失平台
        TrickElement movingPlatform1 = new TrickElement(TrickElement.TYPE_MOVING, 150, 550, 80, 20);
        movingPlatform1.moveSpeed = 4.0f;
        movingPlatform1.moveDistance = 150.0f;
        elements.add(movingPlatform1);
        
        // 弹簧陷阱组合
        elements.add(new TrickElement(TrickElement.TYPE_TRAMPOLINE, 300, 500, 60, 20));
        elements.add(new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 300, 300, 100, 20));
        
        // 隐形平台路径
        TrickElement hiddenPath1 = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 400, 400, 80, 20);
        hiddenPath1.isVisible = false;
        elements.add(hiddenPath1);
        
        // 滑动平台区域
        elements.add(new TrickElement(TrickElement.TYPE_SLIPPERY, 500, 450, 150, 20));
        
        // 反向控制区域
        TrickElement reverseTrap = new TrickElement(TrickElement.TYPE_REVERSE, 550, 300, 100, 100);
        reverseTrap.setMessage("Try your luck!");
        elements.add(reverseTrap);
        
        // 高速移动平台序列
        for(int i = 0; i < 3; i++) {
            TrickElement fastPlatform = new TrickElement(TrickElement.TYPE_MOVING, 650 + i * 60, 350 - i * 30, 50, 20);
            fastPlatform.moveSpeed = 5.0f + i;
            fastPlatform.moveDistance = 100.0f;
            elements.add(fastPlatform);
        }
        
        // 假目标陷阱
        elements.add(new TrickElement(TrickElement.TYPE_FAKE_GOAL, 850, 450, 50, 100));
        
        // 真实目标 - 需要精确时机
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 900, 250, 50, 100));
        
        // 隐藏的安全网（以防万一）
        TrickElement safetyNet = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 700, 550, 100, 20);
        safetyNet.isVisible = false;
        elements.add(safetyNet);
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel8() {
        // 关卡8: 迷宫陷阱
        // 添加起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 150, 50));
        
        // 设计迷宫墙壁 - 垂直
        int[][] verticalWalls = {
            {200, 400, 20, 250},
            {400, 200, 20, 300},
            {600, 300, 20, 350},
            {800, 150, 20, 300}
        };
        
        for(int[] wall : verticalWalls) {
            elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, wall[0], wall[1], wall[2], wall[3]));
        }
        
        // 设计迷宫墙壁 - 水平
        int[][] horizontalWalls = {
            {200, 400, 200, 20},
            {400, 300, 200, 20},
            {220, 200, 180, 20},
            {600, 450, 200, 20}
        };
        
        for(int[] wall : horizontalWalls) {
            elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, wall[0], wall[1], wall[2], wall[3]));
        }
        
        // 添加一些隐形捷径
        TrickElement shortcut = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 500, 350, 100, 20);
        shortcut.isVisible = false;
        elements.add(shortcut);
        
        // 添加一些反向控制区
        TrickElement reverseTrap = new TrickElement(TrickElement.TYPE_REVERSE, 300, 500, 100, 100);
        reverseTrap.setMessage("Confused?");
        elements.add(reverseTrap);
        
        // 添加目标旗帜 - 隐藏在迷宫中
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 700, 250, 50, 100));
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel9() {
        // 关卡9: 反向引导
        // 添加起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 150, 50));
        
        // 添加一系列看似正确路径的平台 - 实际上是死路
        for(int i = 0; i < 5; i++) {
            TrickElement deceptivePath = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 200 + i * 100, 600 - i * 50, 80, 20);
            elements.add(deceptivePath);
        }
        
        // 最后一个是假平台
        TrickElement finalDeception = new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 700, 350, 80, 20);
        finalDeception.setMessage("So close, yet so far!");
        elements.add(finalDeception);
        
        // 真正的路径 - 刚开始就向下跳，看起来像是会死但实际有隐形平台
        TrickElement hiddenStart = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 150, 700, 100, 20);
        hiddenStart.isVisible = false;
        elements.add(hiddenStart);
        
        // 隐形路径继续
        for(int i = 0; i < 3; i++) {
            TrickElement hiddenPath = new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 300 + i * 150, 700, 100, 20);
            hiddenPath.isVisible = i % 2 == 0; // 间隔可见性，增加难度
            elements.add(hiddenPath);
        }
        
        // 添加回到可见区域的跳板
        TrickElement trampoline = new TrickElement(TrickElement.TYPE_TRAMPOLINE, 750, 700, 100, 20);
        elements.add(trampoline);
        
        // 添加终点平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 850, 450, 150, 50));
        
        // 添加目标旗帜
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 900, 350, 50, 100));
        
        // 添加一个障碍，防止玩家直接跳到终点
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 750, 100, 300, 20));
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void createLevel10() {
        // 关卡10: 终极挑战
        // 添加起始平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 0, 600, 100, 50));
        
        // 第一部分：消失平台速跑
        for(int i = 0; i < 4; i++) {
            TrickElement disappearingPlatform = new TrickElement(TrickElement.TYPE_FAKE_PLATFORM, 120 + i * 60, 600, 40, 20);
            elements.add(disappearingPlatform);
        }
        
        // 第二部分：滑动区域
        TrickElement slipperySection = new TrickElement(TrickElement.TYPE_SLIPPERY, 350, 600, 150, 20);
        elements.add(slipperySection);
        
        // 第三部分：反向控制区
        TrickElement reverseSection = new TrickElement(TrickElement.TYPE_REVERSE, 550, 500, 150, 150);
        reverseSection.setMessage("Final challenge begins!");
        elements.add(reverseSection);
        
        // 添加一些欺骗性的假平台
        for(int i = 0; i < 8; i++) {
            float x = 300 + random.nextFloat() * 500;
            float y = 200 + random.nextFloat() * 350;
            TrickElement deceptivePlatform = new TrickElement(
                random.nextBoolean() ? TrickElement.TYPE_FAKE_PLATFORM : TrickElement.TYPE_NORMAL_PLATFORM, 
                x, y, 40 + random.nextFloat() * 40, 20);
            deceptivePlatform.isVisible = random.nextFloat() > 0.3f; // 有30%的概率是隐形的
            elements.add(deceptivePlatform);
        }
        
        // 添加几个移动平台
        for(int i = 0; i < 3; i++) {
            TrickElement movingPlatform = new TrickElement(TrickElement.TYPE_MOVING, 300 + i * 200, 350 + i * 50, 80, 20);
            movingPlatform.moveSpeed = 2.0f + random.nextFloat() * 3.0f;
            movingPlatform.moveDistance = 100.0f + random.nextFloat() * 150.0f;
            elements.add(movingPlatform);
        }
        
        // 添加一些弹簧
        for(int i = 0; i < 2; i++) {
            TrickElement trampoline = new TrickElement(TrickElement.TYPE_TRAMPOLINE, 400 + i * 300, 500 - i * 150, 80, 20);
            elements.add(trampoline);
        }
        
        // 添加几个假目标
        for(int i = 0; i < 3; i++) {
            TrickElement fakeGoal = new TrickElement(TrickElement.TYPE_FAKE_GOAL, 250 + i * 200, 250 + i * 100, 40, 80);
            fakeGoal.setMessage("Nope, still not the end!");
            elements.add(fakeGoal);
        }
        
        // 添加终点平台
        elements.add(new TrickElement(TrickElement.TYPE_NORMAL_PLATFORM, 850, 200, 150, 50));
        
        // 添加真正的目标旗帜 - 藏在最难到达的地方
        elements.add(new TrickElement(TrickElement.TYPE_GOAL, 900, 100, 50, 100));
        
        // 设置玩家初始位置
        player.x = 50;
        player.y = 550;
    }
    
    private void updateUI() {
        levelText.setText("Level: " + currentLevel);
        scoreText.setText("Score: " + score);
    }
    
    private void restartLevel() {
        setupLevel(currentLevel);
        player.reset();
        gameView.invalidate();
    }
    
    private void playerDied() {
        deathCount++;
        vibrator.vibrate(500); // 震动反馈
        
        // 播放失败音效
        playSound(mpFail);
        
        // 显示嘲讽消息
        String[] taunts = {
            "Haha, you died again!",
            "You're really bad at this game...",
            "Try again?",
            "This is so easy, why can't you do it?"
        };
        
        messageText.setText(taunts[random.nextInt(taunts.length)]);
        
        // 如果死亡次数达到阈值，给一个提示
        if (deathCount % DEATH_COUNT_TO_HINT == 0) {
            new Handler().postDelayed(() -> {
                messageText.setText("Hint: Not everything is as it seems...");
                // 连续死亡多次后，给出提示时播放nani音效
                playSound(mpNani);
            }, 2000);
        }
        
        // 重置玩家位置
        player.reset();
    }
    
    private void nextLevel() {
        try {
            currentLevel++;
            if (currentLevel <= MAX_LEVEL) {
                // 暂停游戏循环
                handler.removeCallbacksAndMessages(null);
                
                // 确保UI更新在主线程
                runOnUiThread(() -> {
                    try {
                        // 清空当前关卡
                        elements.clear();
                        
                        // 设置新关卡
                        setupLevel(currentLevel);
                        player.reset();
                        score += 100;
                        updateUI();
                        
                        Toast.makeText(this, "Level " + currentLevel + " started!", Toast.LENGTH_SHORT).show();
                        
                        // 为安全起见，确保游戏视图获取焦点
                        if (gameView != null) {
                            gameView.postInvalidate();
                            gameView.requestFocus();
                        }
                        
                        // 延迟一点时间后重新开始游戏循环
                        new Handler().postDelayed(() -> {
                            if (gameView != null) {
                                gameView.startGameLoop();
                            }
                        }, 100);
                    } catch (Exception e) {
                        Log.e("TricksterGame", "Error setting up next level: " + e.getMessage());
                        // 如果设置下一关失败，尝试重置当前关卡
                        try {
                            currentLevel--;
                            setupLevel(currentLevel);
                            player.reset();
                            updateUI();
                        } catch (Exception ex) {
                            Log.e("TricksterGame", "Error resetting level: " + ex.getMessage());
                        }
                    }
                });
            } else {
                // 游戏通关
                runOnUiThread(() -> {
                    try {
                        new AlertDialog.Builder(this)
                            .setTitle("Game Complete!")
                            .setMessage("Congratulations! You completed all levels!\n\nFinal score: " + score)
                            .setPositiveButton("Awesome!", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                    } catch (Exception e) {
                        Log.e("TricksterGame", "Error showing completion dialog: " + e.getMessage());
                        // 如果显示对话框失败，直接结束活动
                        finish();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("TricksterGame", "Fatal error in nextLevel: " + e.getMessage());
            // 严重错误时尝试安全退出
            finish();
        }
    }
    
    /**
     * 设置方向控制按钮的监听器
     */
    private void setupDirectionButtons() {
        // 上按钮 - 跳跃
        btnUp.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下开始跳跃
                    if (!player.isJumping) {
                        player.velocityY = -15;
                        player.isJumping = true;
                        messageText.setText("Jump!");
                        // 播放yeehaw音效
                        playSound(mpYeehaw);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // 松开不做特殊处理
                    break;
            }
            return true;
        });
        
        // 下按钮
        btnDown.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 目前下键没有特殊功能
                    messageText.setText("Crouch");
                    break;
            }
            return true;
        });
        
        // 左按钮
        btnLeft.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下向左移动
                    player.velocityX = isControlReversed ? 5 : -5;
                    messageText.setText("Move Left");
                    break;
                case MotionEvent.ACTION_UP:
                    // 松开停止水平移动（如果右键未按下）
                    if (!keyD) {
                        player.velocityX = 0;
                    }
                    break;
            }
            return true;
        });
        
        // 右按钮
        btnRight.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下向右移动
                    player.velocityX = isControlReversed ? -5 : 5;
                    messageText.setText("Move Right");
                    break;
                case MotionEvent.ACTION_UP:
                    // 松开停止水平移动（如果左键未按下）
                    if (!keyA) {
                        player.velocityX = 0;
                    }
                    break;
            }
            return true;
        });
        
        // 跳跃按钮
        btnJump.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下开始跳跃
                    if (!player.isJumping) {
                        player.velocityY = -15;
                        player.isJumping = true;
                        messageText.setText("Jump!");
                        // 播放yeehaw音效
                        playSound(mpYeehaw);
                    }
                    break;
            }
            return true;
        });
        
        // 双跳按钮
        btnDoubleJump.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 如果玩家已经在跳跃中但没有使用过双跳
                    if (player.isJumping && !player.hasUsedDoubleJump) {
                        player.velocityY = -15; // 再次提供向上的推力
                        player.hasUsedDoubleJump = true; // 标记已使用双跳
                        messageText.setText("Double Jump!");
                        
                        // 播放nani音效表示惊讶的双跳
                        playSound(mpNani);
                        
                        // 视觉反馈
                        try {
                            vibrator.vibrate(50);
                        } catch (Exception e) {
                            Log.e("TricksterGame", "Vibration failed: " + e.getMessage());
                        }
                    }
                    break;
            }
            return true;
        });
    }
    
    // 直接在Activity级别处理按键事件 - 确保所有按键都正确处理
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        
        // 记录触发按键事件，用于调试
        Log.d("TricksterGame", "按键事件: " + keyCode + ", 动作: " + action);
        
        if (action == KeyEvent.ACTION_DOWN) {
            // 按键按下
            switch (keyCode) {
                case KeyEvent.KEYCODE_W:
                case KeyEvent.KEYCODE_DPAD_UP:
                    // 检测双击W键以实现双跳
                    long currentTime = System.currentTimeMillis();
                    if (keyWPressed && (currentTime - lastWPressTime < DOUBLE_PRESS_INTERVAL)) {
                        // 如果已经在跳跃中且没有使用过双跳，执行双跳
                        if (player.isJumping && !player.hasUsedDoubleJump) {
                            player.velocityY = -15;
                            player.hasUsedDoubleJump = true;
                            messageText.setText("Double Jump!");
                            
                            // 播放nani音效
                            playSound(mpNani);
                            
                            // 视觉反馈
                            try {
                                vibrator.vibrate(50);
                            } catch (Exception e) {
                                Log.e("TricksterGame", "Vibration failed: " + e.getMessage());
                            }
                        }
                    } else if (!player.isJumping) {
                        // 首次跳跃
                        player.velocityY = -15;
                        player.isJumping = true;
                        messageText.setText("Jump!");
                        
                        // 播放yeehaw音效
                        playSound(mpYeehaw);
                    }
                    
                    // 更新状态
                    keyW = true;
                    keyWPressed = true;
                    lastWPressTime = currentTime;
                    return true;
                    
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    keyA = true;
                    player.velocityX = isControlReversed ? 5 : -5;
                    messageText.setText("Move Left");
                    return true;
                    
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    keyS = true;
                    messageText.setText("Crouch");
                    return true;
                    
                case KeyEvent.KEYCODE_D:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    keyD = true;
                    player.velocityX = isControlReversed ? -5 : 5;
                    messageText.setText("Move Right");
                    return true;
            }
        } else if (action == KeyEvent.ACTION_UP) {
            // 按键释放
            switch (keyCode) {
                case KeyEvent.KEYCODE_W:
                case KeyEvent.KEYCODE_DPAD_UP:
                    keyW = false;
                    keyWPressed = false;
                    return true;
                    
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    keyA = false;
                    if (!keyD) { // 如果D键没有按下，停止水平移动
                        player.velocityX = 0;
                    } else { // 如果D键按下，应用D键的效果
                        player.velocityX = isControlReversed ? -5 : 5;
                    }
                    return true;
                    
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    keyS = false;
                    return true;
                    
                case KeyEvent.KEYCODE_D:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    keyD = false;
                    if (!keyA) { // 如果A键没有按下，停止水平移动
                        player.velocityX = 0;
                    } else { // 如果A键按下，应用A键的效果
                        player.velocityX = isControlReversed ? 5 : -5;
                    }
                    return true;
            }
        }
        
        return super.dispatchKeyEvent(event);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 通过dispatchKeyEvent处理全部按键
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 通过dispatchKeyEvent处理全部按键
        return super.onKeyUp(keyCode, event);
    }
    
    // 玩家类 - 更改为人形外观
    private class Player {
        public float x, y;
        public float width, height;
        public float velocityX, velocityY;
        public boolean isJumping;
        public boolean isDead;
        public boolean isFacingRight = true; // 玩家面向方向
        public boolean hasUsedDoubleJump = false; // 是否已使用双跳
        
        // 初始位置
        private float initialX, initialY;
        
        public Player(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.velocityX = 0;
            this.velocityY = 0;
            this.isJumping = false;
            this.isDead = false;
            this.hasUsedDoubleJump = false;
            
            this.initialX = x;
            this.initialY = y;
        }
        
        public void update() {
            // 更新面向方向
            if (velocityX > 0) {
                isFacingRight = true;
            } else if (velocityX < 0) {
                isFacingRight = false;
            }
            
            // 应用重力
            velocityY += 0.5f;
            
            // 应用速度
            x += velocityX;
            y += velocityY;
            
            // 检查是否掉出屏幕
            if (y > gameView.getHeight()) {
                isDead = true;
            }
        }
        
        public void reset() {
            x = initialX;
            y = initialY;
            velocityX = 0;
            velocityY = 0;
            isJumping = false;
            isDead = false;
            isFacingRight = true;
            hasUsedDoubleJump = false; // 重置双跳状态
        }
    }
    
    // 切换3D模式
    private void toggle3DMode() {
        is3DMode = !is3DMode;
        Toast.makeText(this, is3DMode ? "3D模式已开启" : "3D模式已关闭", Toast.LENGTH_SHORT).show();
        gameView.invalidate();
    }
    
    // 环境光开关
    private void toggleAmbientLight() {
        isAmbientLightEnabled = !isAmbientLightEnabled;
        Toast.makeText(this, isAmbientLightEnabled ? "环境光已开启" : "环境光已关闭", Toast.LENGTH_SHORT).show();
        
        // 如果关闭环境光，则设置为默认光照水平
        if (!isAmbientLightEnabled) {
            currentLightLevel = 100;
        }
    }
    
    // 光线传感器监听器
    private SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (isAmbientLightEnabled) {
                // 获取当前光线传感器读数
                float reading = event.values[0];
                
                // 将读数添加到最近读数数组
                recentLightReadings[readingsIndex] = reading;
                readingsIndex = (readingsIndex + 1) % recentLightReadings.length;
                
                // 计算平均值以平滑光照变化
                float sum = 0;
                for (float value : recentLightReadings) {
                    sum += value;
                }
                float averageReading = sum / recentLightReadings.length;
                
                // 更新当前光照水平（限制在最大值内）
                currentLightLevel = Math.min(averageReading, MAX_LIGHT_LEVEL);
                
                // 根据光照水平调整太阳强度
                sunIntensity = 0.5f + (currentLightLevel / MAX_LIGHT_LEVEL) * 0.5f;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // 不需要处理
        }
    };
    
    // 游戏视图类
    private class TrickGameView extends View {
        private Paint paint;
        private boolean isGameLoopRunning = false;
        private Runnable gameLoopRunnable;
        
        // 背景图层 - 用于视差效果
        private List<BackgroundLayer> backgroundLayers = new ArrayList<>();
        
        // 触摸控制变量
        private long lastTouchTime = 0;
        private boolean isDoubleTapWindow = false;
        private static final long DOUBLE_TAP_WINDOW = 300; // 双击检测窗口期（毫秒）
        
        // 阴影区域的渐变颜色
        private int darkAreaColor = Color.parseColor("#99000000"); // 半透明黑色
        
        public TrickGameView(Context context) {
            super(context);
            paint = new Paint();
            paint.setAntiAlias(true); // 启用抗锯齿
            
            // 初始化背景图层
            initBackgroundLayers();
            
            // 启动游戏循环
            startGameLoop();
        }
        
        private void initBackgroundLayers() {
            // 远景层
            backgroundLayers.add(new BackgroundLayer(Color.parseColor("#021033"), 0.2f));
            // 中景层
            backgroundLayers.add(new BackgroundLayer(Color.parseColor("#052244"), 0.5f));
            // 近景层
            backgroundLayers.add(new BackgroundLayer(Color.parseColor("#094266"), 0.8f));
        }
        
        public void startGameLoop() {
            // 停止任何现有的游戏循环
            stopGameLoop();
            
            // 创建新的游戏循环
            isGameLoopRunning = true;
            gameLoopRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isGameLoopRunning) {
                        try {
                            updateGame();
                            postInvalidate(); // 请求重绘
                            handler.postDelayed(this, 16);
                        } catch (Exception e) {
                            Log.e("TricksterGame", "Error in game loop: " + e.getMessage());
                            // 尝试恢复
                            handler.postDelayed(this, 100);
                        }
                    }
                }
            };
            
            // 开始游戏循环
            handler.post(gameLoopRunnable);
        }
        
        public void stopGameLoop() {
            isGameLoopRunning = false;
            if (gameLoopRunnable != null) {
                handler.removeCallbacks(gameLoopRunnable);
            }
        }
        
        private void updateGame() {
            // 不再在这里调用updatePlayerMovement
            // 我们已经在按键事件中直接设置了速度
            
            // 更新玩家
            player.update();
            
            // 更新元素
            for (TrickElement element : elements) {
                element.update();
            }
            
            // 碰撞检测
            checkCollisions();
            
            // 检查玩家状态
            if (player.isDead) {
                playerDied();
            }
        }
        
        private void checkCollisions() {
            // 检查与每个元素的碰撞
            for (TrickElement element : elements) {
                if (!element.isActive) continue;
                
                if (isColliding(player, element)) {
                    handleCollision(element);
                }
            }
        }
        
        private boolean isColliding(Player player, TrickElement element) {
            return (player.x < element.x + element.width &&
                   player.x + player.width > element.x &&
                   player.y < element.y + element.height &&
                   player.y + player.height > element.y);
        }
        
        private void handleCollision(TrickElement element) {
            switch (element.type) {
                case TrickElement.TYPE_NORMAL_PLATFORM:
                    // 站在平台上
                    if (player.velocityY > 0) {
                        player.y = element.y - player.height;
                        player.velocityY = 0;
                        player.isJumping = false;
                        player.hasUsedDoubleJump = false; // 重置双跳状态
                    }
                    break;
                    
                case TrickElement.TYPE_FAKE_PLATFORM:
                    // 假平台会消失
                    element.isActive = false;
                    if (!element.message.isEmpty()) {
                        messageText.setText(element.message);
                        new Handler().postDelayed(() -> messageText.setText(""), 2000);
                    }
                    // 播放"nani"音效，表示惊讶
                    playSound(mpNani);
                    break;
                    
                case TrickElement.TYPE_TRAMPOLINE:
                    // 弹簧会弹起玩家
                    player.velocityY = -20;
                    player.isJumping = true;
                    // 播放"yeehaw"音效，表示兴奋
                    playSound(mpYeehaw);
                    break;
                    
                case TrickElement.TYPE_REVERSE:
                    // 反向控制
                    isControlReversed = true;
                    new Handler().postDelayed(() -> isControlReversed = false, 5000);
                    if (!element.message.isEmpty()) {
                        messageText.setText(element.message);
                        new Handler().postDelayed(() -> messageText.setText(""), 2000);
                    }
                    // 播放"nani"音效，表示惊讶/困惑
                    playSound(mpNani);
                    break;
                    
                case TrickElement.TYPE_GOAL:
                    // 到达目标，确保只触发一次并防止状态混乱
                    if (element.isActive) {
                        element.isActive = false; // 防止重复触发
                        // 播放胜利音效
                        playSound(mpWin);
                        // 使用Handler确保在主线程上处理
                        new Handler(Looper.getMainLooper()).post(() -> nextLevel());
                    }
                    break;
                    
                case TrickElement.TYPE_FAKE_GOAL:
                    // 假目标，欺骗玩家
                    hasReachedFakeGoal = true;
                    element.isActive = false;
                    
                    // 显示嘲讽消息
                    if (!element.message.isEmpty()) {
                        messageText.setText(element.message);
                    } else {
                        messageText.setText("Haha, this is not the real goal!");
                    }
                    
                    // 播放嘲笑音效
                    playSound(mpLaugh);
                    
                    // 震动反馈
                    try {
                        vibrator.vibrate(300);
                    } catch (Exception e) {
                        Log.e("TricksterGame", "Vibration failed: " + e.getMessage());
                    }
                    
                    // 2秒后清除消息
                    new Handler().postDelayed(() -> messageText.setText(""), 2000);
                    break;
                    
                case TrickElement.TYPE_SLIPPERY:
                    // 滑动平台 - 玩家站在上面但无法完全控制移动
                    if (player.velocityY > 0) {
                        player.y = element.y - player.height;
                        player.velocityY = 0;
                        player.isJumping = false;
                        player.hasUsedDoubleJump = false; // 重置双跳状态
                        
                        // 添加持续的滑动效果
                        if (player.velocityX > 0) {
                            player.velocityX += 0.2f; // 加速滑行
                        } else if (player.velocityX < 0) {
                            player.velocityX -= 0.2f; // 加速滑行
                        } else {
                            // 如果没有移动，随机选择一个方向开始滑动
                            player.velocityX = (random.nextBoolean() ? 1 : -1) * 2.0f;
                        }
                        
                        // 限制最大滑动速度
                        if (player.velocityX > 8) player.velocityX = 8;
                        if (player.velocityX < -8) player.velocityX = -8;
                    }
                    break;
                    
                case TrickElement.TYPE_MOVING:
                    // 移动平台 - 玩家站在上面会跟随平台移动
                    if (player.velocityY > 0) {
                        player.y = element.y - player.height;
                        player.velocityY = 0;
                        player.isJumping = false;
                        player.hasUsedDoubleJump = false; // 重置双跳状态
                        
                        // 应用平台的移动速度给玩家
                        player.x += element.moveSpeed;
                    }
                    break;
            }
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // 绘制背景
            drawBackground(canvas);
            
            // 绘制太阳和光照效果
            drawSunAndLighting(canvas);
            
            // 更新背景图层
            for (BackgroundLayer layer : backgroundLayers) {
                layer.update(player.velocityX);
            }
            
            // 绘制元素
            for (TrickElement element : elements) {
                if (!element.isActive || !element.isVisible) continue;
                
                if (is3DMode) {
                    draw3DElement(canvas, element);
                } else {
                    // 绘制原始2D元素
                    paint.setColor(getColorForElementType(element.type));
                    
                    // 为目标旗帜绘制特殊图形
                    if (element.type == TrickElement.TYPE_GOAL) {
                        // 绘制旗杆
                        paint.setColor(Color.GRAY);
                        canvas.drawRect(element.x, element.y, 
                                       element.x + 10, 
                                       element.y + element.height, paint);
                        
                        // 绘制旗帜
                        paint.setColor(Color.GREEN);
                        canvas.drawRect(element.x + 10, element.y, 
                                       element.x + element.width, 
                                       element.y + element.height / 2, paint);
                    } else {
                        // 绘制其他元素
                        canvas.drawRect(element.x, element.y, 
                                       element.x + element.width, 
                                       element.y + element.height, paint);
                    }
                }
            }
            
            // 绘制玩家
            if (!player.isDead) {
                if (is3DMode) {
                    draw3DPlayer(canvas);
                } else {
                    // 绘制原始2D玩家
                    // 身体
                    paint.setColor(Color.CYAN);
                    canvas.drawRect(player.x, player.y + player.height/3, 
                                   player.x + player.width, 
                                   player.y + player.height, paint);
                    
                    // 头部
                    paint.setColor(Color.WHITE);
                    canvas.drawCircle(player.x + player.width/2, 
                                     player.y + player.height/6, 
                                     player.width/3, paint);
                    
                    // 眼睛
                    paint.setColor(Color.BLACK);
                    float eyeX = player.x + (player.isFacingRight ? 
                                           player.width/2 + 5 : player.width/2 - 5);
                    canvas.drawCircle(eyeX, player.y + player.height/6, 3, paint);
                }
            }
        }
        
        // 绘制太阳和光照效果
        private void drawSunAndLighting(Canvas canvas) {
            // 太阳位置
            float sunX = getWidth() * sunPosX;
            float sunY = getHeight() * sunPosY;
            
            // 绘制太阳光晕
            Paint sunGlowPaint = new Paint();
            sunGlowPaint.setStyle(Paint.Style.FILL);
            
            // 创建径向渐变作为太阳光晕
            int adjustedSunlightColor = adjustColorByLight(sunlightColor);
            RadialGradient sunGradient = new RadialGradient(
                sunX, sunY,
                sunRadius * 3 * sunIntensity,
                new int[] {adjustedSunlightColor, Color.argb(100, 255, 255, 200), Color.TRANSPARENT},
                new float[] {0.1f, 0.3f, 1.0f},
                Shader.TileMode.CLAMP
            );
            sunGlowPaint.setShader(sunGradient);
            canvas.drawCircle(sunX, sunY, sunRadius * 3 * sunIntensity, sunGlowPaint);
            
            // 绘制太阳本体
            Paint sunPaint = new Paint();
            sunPaint.setColor(adjustedSunlightColor);
            sunPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(sunX, sunY, sunRadius * 0.7f, sunPaint);
            
            // 如果光照级别较低，绘制暗区
            if (currentLightLevel < 100) {
                // 计算暗区透明度 - 光线越暗，透明度越低（越黑）
                int alpha = (int)(180 * (1 - currentLightLevel / 100));
                
                // 创建径向渐变作为光照区域
                RadialGradient lightGradient = new RadialGradient(
                    sunX, sunY,
                    getWidth() * sunIntensity,
                    Color.TRANSPARENT,
                    Color.argb(alpha, 0, 0, 0),
                    Shader.TileMode.CLAMP
                );
                
                Paint darkAreaPaint = new Paint();
                darkAreaPaint.setShader(lightGradient);
                canvas.drawRect(0, 0, getWidth(), getHeight(), darkAreaPaint);
            }
        }
        
        private void drawBackground(Canvas canvas) {
            // 绘制渐变背景
            Paint bgPaint = new Paint();
            bgPaint.setShader(new LinearGradient(0, 0, 0, getHeight(), 
                                                Color.parseColor("#000428"), 
                                                Color.parseColor("#004e92"), 
                                                Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
            
            // 绘制背景图层
            for (BackgroundLayer layer : backgroundLayers) {
                layer.draw(canvas, getWidth(), getHeight());
            }
            
            // 绘制远处的山脉轮廓
            Paint mountainPaint = new Paint();
            mountainPaint.setColor(Color.parseColor("#062e4d"));
            Path mountainPath = new Path();
            mountainPath.moveTo(0, getHeight() * 0.6f);
            
            // 创建山脉轮廓
            float segmentWidth = getWidth() / 12;
            for (int i = 0; i <= 12; i++) {
                float x = i * segmentWidth;
                float y = getHeight() * 0.6f - (float)(Math.sin(i * 0.5) * 80) - (float)(Math.random() * 40);
                mountainPath.lineTo(x, y);
            }
            
            mountainPath.lineTo(getWidth(), getHeight());
            mountainPath.lineTo(0, getHeight());
            mountainPath.close();
            
            canvas.drawPath(mountainPath, mountainPaint);
            
            // 绘制网格地板效果
            drawGridFloor(canvas);
        }
        
        // 绘制网格地板
        private void drawGridFloor(Canvas canvas) {
            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.parseColor("#2d4b60"));
            gridPaint.setStrokeWidth(2);
            gridPaint.setAlpha(100);
            
            float horizonY = getHeight() * 0.7f;
            float vanishX = getWidth() / 2;
            
            // 绘制水平线
            for (int i = 0; i < 20; i++) {
                float y = horizonY + (getHeight() - horizonY) * i / 20.0f;
                canvas.drawLine(0, y, getWidth(), y, gridPaint);
            }
            
            // 绘制垂直线 - 透视效果
            for (int i = 0; i < 20; i++) {
                float startX = vanishX - getWidth() * i / 20.0f;
                float endX = vanishX - getWidth() * i / 10.0f;
                if (startX >= 0) canvas.drawLine(startX, horizonY, 0, getHeight(), gridPaint);
                
                startX = vanishX + getWidth() * i / 20.0f;
                endX = vanishX + getWidth() * i / 10.0f;
                if (startX <= getWidth()) canvas.drawLine(startX, horizonY, getWidth(), getHeight(), gridPaint);
            }
        }
        
        // 绘制3D元素
        private void draw3DElement(Canvas canvas, TrickElement element) {
            int elementColor = getColorForElementType(element.type);
            
            // 根据当前光照调整颜色
            elementColor = adjustColorByLight(elementColor);
            
            // 计算透视缩放 - 越靠近屏幕底部的对象显得越大
            float depthFactor = 1.0f - (element.y / getHeight()) * 0.3f;
            
            // 应用3D变换
            float x = element.x;
            float y = element.y;
            float width = element.width * depthFactor;
            float height = element.height * depthFactor;
            
            // 绘制阴影
            if (element.type != TrickElement.TYPE_SLIPPERY) { // 滑动平台无阴影
                paint.setColor(Color.BLACK);
                paint.setAlpha(100);
                RectF shadowRect = new RectF(x + shadowOffset, y + shadowOffset, 
                                           x + width + shadowOffset, y + height + shadowOffset);
                canvas.drawRect(shadowRect, paint);
                paint.setAlpha(255);
            }
            
            // 根据元素类型应用不同的3D效果
            switch (element.type) {
                case TrickElement.TYPE_GOAL:
                    draw3DGoal(canvas, x, y, width, height);
                    break;
                case TrickElement.TYPE_FAKE_GOAL:
                    draw3DFakeGoal(canvas, x, y, width, height);
                    break;
                case TrickElement.TYPE_TRAMPOLINE:
                    draw3DTrampoline(canvas, x, y, width, height);
                    break;
                case TrickElement.TYPE_REVERSE:
                    draw3DReverseTrap(canvas, x, y, width, height);
                    break;
                case TrickElement.TYPE_SLIPPERY:
                    draw3DSlipperyPlatform(canvas, x, y, width, height);
                    break;
                case TrickElement.TYPE_MOVING:
                    draw3DMovingPlatform(canvas, x, y, width, height);
                    break;
                default:
                    // 普通平台和其他元素
                    paint.setColor(elementColor);
                    
                    // 绘制主体
                    canvas.drawRect(x, y, x + width, y + height, paint);
                    
                    // 绘制侧面以创建3D效果
                    paint.setColor(darker(elementColor));
                    Path sidePath = new Path();
                    sidePath.moveTo(x + width, y);
                    sidePath.lineTo(x + width + shadowOffset, y + shadowOffset);
                    sidePath.lineTo(x + width + shadowOffset, y + height + shadowOffset);
                    sidePath.lineTo(x + width, y + height);
                    sidePath.close();
                    canvas.drawPath(sidePath, paint);
                    
                    // 绘制顶面
                    paint.setColor(brighter(elementColor));
                    Path topPath = new Path();
                    topPath.moveTo(x, y);
                    topPath.lineTo(x + shadowOffset, y + shadowOffset);
                    topPath.lineTo(x + width + shadowOffset, y + shadowOffset);
                    topPath.lineTo(x + width, y);
                    topPath.close();
                    canvas.drawPath(topPath, paint);
                    break;
            }
        }
        
        // 绘制3D目标旗帜
        private void draw3DGoal(Canvas canvas, float x, float y, float width, float height) {
            // 绘制旗杆
            paint.setColor(Color.GRAY);
            canvas.drawRect(x, y, x + 10, y + height, paint);
            
            // 绘制旗杆的3D效果
            paint.setColor(darker(Color.GRAY));
            Path poleSidePath = new Path();
            poleSidePath.moveTo(x + 10, y);
            poleSidePath.lineTo(x + 10 + shadowOffset, y + shadowOffset);
            poleSidePath.lineTo(x + 10 + shadowOffset, y + height + shadowOffset);
            poleSidePath.lineTo(x + 10, y + height);
            poleSidePath.close();
            canvas.drawPath(poleSidePath, paint);
            
            // 绘制旗帜
            paint.setColor(Color.GREEN);
            canvas.drawRect(x + 10, y, x + width, y + height/2, paint);
            
            // 绘制旗帜的3D效果 - 飘动效果
            paint.setColor(darker(Color.GREEN));
            int waveCount = 3;
            Path flagPath = new Path();
            flagPath.moveTo(x + 10, y);
            
            for (int i = 0; i <= 10; i++) {
                float waveX = x + 10 + (width - 10) * i / 10.0f;
                float waveY = y + (float) Math.sin(i * waveCount * Math.PI / 10.0f) * 5;
                flagPath.lineTo(waveX, waveY);
            }
            
            for (int i = 10; i >= 0; i--) {
                float waveX = x + 10 + (width - 10) * i / 10.0f;
                float waveY = y + height/2 + (float) Math.sin(i * waveCount * Math.PI / 10.0f) * 5;
                flagPath.lineTo(waveX, waveY);
            }
            
            flagPath.close();
            canvas.drawPath(flagPath, paint);
            
            // 添加反光效果
            paint.setColor(Color.WHITE);
            paint.setAlpha(50);
            canvas.drawLine(x + 15, y + 5, x + 15, y + height/2 - 5, paint);
            paint.setAlpha(255);
        }
        
        // 绘制3D假目标
        private void draw3DFakeGoal(Canvas canvas, float x, float y, float width, float height) {
            // 与真目标类似，但有微妙的不同
            
            // 绘制旗杆
            paint.setColor(Color.GRAY);
            canvas.drawRect(x, y, x + 10, y + height, paint);
            
            // 旗杆3D效果
            paint.setColor(darker(Color.GRAY));
            Path poleSidePath = new Path();
            poleSidePath.moveTo(x + 10, y);
            poleSidePath.lineTo(x + 10 + shadowOffset, y + shadowOffset);
            poleSidePath.lineTo(x + 10 + shadowOffset, y + height + shadowOffset);
            poleSidePath.lineTo(x + 10, y + height);
            poleSidePath.close();
            canvas.drawPath(poleSidePath, paint);
            
            // 绘制假旗帜 - 略微不同的绿色
            paint.setColor(Color.parseColor("#00D000")); // 略微不同的绿色
            canvas.drawRect(x + 10, y, x + width, y + height/2, paint);
            
            // 添加一个微妙的图案以区分（但不明显）
            paint.setColor(darker(Color.parseColor("#00D000")));
            for (int i = 0; i < 3; i++) {
                canvas.drawLine(x + 15 + i * 10, y + 5, x + 15 + i * 10, y + height/2 - 5, paint);
            }
        }
        
        // 绘制3D弹簧
        private void draw3DTrampoline(Canvas canvas, float x, float y, float width, float height) {
            // 弹簧底座
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(x, y + height - 5, x + width, y + height, paint);
            
            // 弹簧线圈
            paint.setColor(Color.YELLOW);
            paint.setStrokeWidth(4);
            
            int coilCount = 5;
            float coilSpacing = (height - 5) / coilCount;
            
            for (int i = 0; i < coilCount; i++) {
                float coilY = y + i * coilSpacing;
                if (i % 2 == 0) {
                    canvas.drawLine(x, coilY, x + width, coilY, paint);
                } else {
                    canvas.drawLine(x + width, coilY, x, coilY, paint);
                }
            }
            
            // 弹簧反光效果
            paint.setColor(Color.WHITE);
            paint.setAlpha(100);
            canvas.drawCircle(x + width/2, y + height/2, width/6, paint);
            paint.setAlpha(255);
        }
        
        // 绘制3D反向控制区
        private void draw3DReverseTrap(Canvas canvas, float x, float y, float width, float height) {
            // 渐变背景
            Paint gradientPaint = new Paint();
            gradientPaint.setShader(new LinearGradient(
                x, y, x + width, y + height,
                Color.MAGENTA, Color.parseColor("#6A0DAD"),
                Shader.TileMode.CLAMP));
            
            canvas.drawRect(x, y, x + width, y + height, gradientPaint);
            
            // 添加闪烁效果
            long time = System.currentTimeMillis() % 2000;
            if (time < 1000) {
                paint.setColor(Color.WHITE);
                paint.setAlpha((int)(50 * (1 - time/1000.0f)));
                canvas.drawRect(x, y, x + width, y + height, paint);
                paint.setAlpha(255);
            }
            
            // 绘制旋转的箭头符号
            paint.setColor(Color.WHITE);
            paint.setAlpha(180);
            
            float centerX = x + width/2;
            float centerY = y + height/2;
            float arrowSize = Math.min(width, height) * 0.4f;
            
            // 旋转量基于时间
            float rotation = (System.currentTimeMillis() % 3600) / 10.0f;
            
            canvas.save();
            canvas.rotate(rotation, centerX, centerY);
            
            // 绘制圆形箭头
            Path arrowPath = new Path();
            arrowPath.moveTo(centerX, centerY - arrowSize);
            arrowPath.lineTo(centerX + arrowSize/3, centerY - arrowSize/2);
            arrowPath.lineTo(centerX, centerY - arrowSize/4);
            arrowPath.lineTo(centerX - arrowSize/3, centerY - arrowSize/2);
            arrowPath.close();
            
            canvas.drawPath(arrowPath, paint);
            canvas.restore();
            
            paint.setAlpha(255);
        }
        
        // 绘制3D滑动平台
        private void draw3DSlipperyPlatform(Canvas canvas, float x, float y, float width, float height) {
            // 创建冰面效果
            Paint icePaint = new Paint();
            icePaint.setShader(new LinearGradient(
                x, y, x + width, y + height,
                Color.parseColor("#A5F2F3"), Color.parseColor("#8EECF5"),
                Shader.TileMode.MIRROR));
            
            canvas.drawRect(x, y, x + width, y + height, icePaint);
            
            // 添加闪光效果
            paint.setColor(Color.WHITE);
            paint.setAlpha(100);
            
            // 随机闪光点
            Random random = new Random(System.currentTimeMillis() / 500);
            for (int i = 0; i < 10; i++) {
                float sparkX = x + random.nextFloat() * width;
                float sparkY = y + random.nextFloat() * height;
                canvas.drawCircle(sparkX, sparkY, 2 + random.nextFloat() * 3, paint);
            }
            
            paint.setAlpha(255);
            
            // 绘制冰面边缘
            paint.setColor(Color.parseColor("#B0E2FF"));
            canvas.drawRect(x, y, x + width, y + 3, paint);
        }
        
        // 绘制3D移动平台
        private void draw3DMovingPlatform(Canvas canvas, float x, float y, float width, float height) {
            // 主体颜色
            paint.setColor(Color.CYAN);
            canvas.drawRect(x, y, x + width, y + height, paint);
            
            // 侧面颜色
            paint.setColor(darker(Color.CYAN));
            Path sidePath = new Path();
            sidePath.moveTo(x + width, y);
            sidePath.lineTo(x + width + shadowOffset, y + shadowOffset);
            sidePath.lineTo(x + width + shadowOffset, y + height + shadowOffset);
            sidePath.lineTo(x + width, y + height);
            sidePath.close();
            canvas.drawPath(sidePath, paint);
            
            // 顶面颜色
            paint.setColor(brighter(Color.CYAN));
            Path topPath = new Path();
            topPath.moveTo(x, y);
            topPath.lineTo(x + shadowOffset, y + shadowOffset);
            topPath.lineTo(x + width + shadowOffset, y + shadowOffset);
            topPath.lineTo(x + width, y);
            topPath.close();
            canvas.drawPath(topPath, paint);
            
            // 添加移动指示器
            paint.setColor(Color.WHITE);
            float markerSize = 10;
            float markerX = x + (width - markerSize)/2 + (float)Math.sin(System.currentTimeMillis()/200.0) * (width/4);
            canvas.drawCircle(markerX, y + height/2, markerSize/2, paint);
        }
        
        // 绘制3D玩家
        private void draw3DPlayer(Canvas canvas) {
            // 调整玩家颜色为基于光照的蓝色
            int playerColor = adjustColorByLight(Color.BLUE);
            
            // 计算透视缩放
            float depthFactor = 1.0f - (player.y / getHeight()) * 0.3f;
            
            // 玩家宽高
            float width = player.width * depthFactor;
            float height = player.height * depthFactor;
            
            // 绘制阴影
            paint.setColor(Color.BLACK);
            paint.setAlpha(80);
            RectF shadowRect = new RectF(player.x + shadowOffset, player.y + shadowOffset,
                                      player.x + width + shadowOffset, player.y + height + shadowOffset);
            canvas.drawRect(shadowRect, paint);
            paint.setAlpha(255);
            
            // 绘制主体
            paint.setColor(playerColor);
            canvas.drawRect(player.x, player.y, player.x + width, player.y + height, paint);
            
            // 绘制侧面
            paint.setColor(darker(playerColor));
            Path sidePath = new Path();
            sidePath.moveTo(player.x + width, player.y);
            sidePath.lineTo(player.x + width + shadowOffset, player.y + shadowOffset);
            sidePath.lineTo(player.x + width + shadowOffset, player.y + height + shadowOffset);
            sidePath.lineTo(player.x + width, player.y + height);
            sidePath.close();
            canvas.drawPath(sidePath, paint);
            
            // 绘制顶面
            paint.setColor(brighter(playerColor));
            Path topPath = new Path();
            topPath.moveTo(player.x, player.y);
            topPath.lineTo(player.x + shadowOffset, player.y + shadowOffset);
            topPath.lineTo(player.x + width + shadowOffset, player.y + shadowOffset);
            topPath.lineTo(player.x + width, player.y);
            topPath.close();
            canvas.drawPath(topPath, paint);
            
            // 画出眼睛（根据朝向调整）
            paint.setColor(Color.WHITE);
            float eyeSize = width / 6;
            float eyeY = player.y + height / 3;
            float leftEyeX, rightEyeX;
            
            if (player.isFacingRight) {
                leftEyeX = player.x + width / 4;
                rightEyeX = player.x + width * 3 / 4;
            } else {
                rightEyeX = player.x + width / 4;
                leftEyeX = player.x + width * 3 / 4;
            }
            
            canvas.drawCircle(leftEyeX, eyeY, eyeSize, paint);
            canvas.drawCircle(rightEyeX, eyeY, eyeSize, paint);
            
            // 眼珠（黑色）
            paint.setColor(adjustColorByLight(Color.BLACK));  // 调整颜色
            float pupilOffset = player.isFacingRight ? eyeSize / 3 : -eyeSize / 3;
            canvas.drawCircle(leftEyeX + pupilOffset, eyeY, eyeSize / 2, paint);
            canvas.drawCircle(rightEyeX + pupilOffset, eyeY, eyeSize / 2, paint);
            
            // 如果玩家死亡，绘制X眼睛
            if (player.isDead) {
                paint.setColor(Color.RED);
                paint.setStrokeWidth(3);
                
                // 左眼X
                canvas.drawLine(leftEyeX - eyeSize, eyeY - eyeSize, leftEyeX + eyeSize, eyeY + eyeSize, paint);
                canvas.drawLine(leftEyeX + eyeSize, eyeY - eyeSize, leftEyeX - eyeSize, eyeY + eyeSize, paint);
                
                // 右眼X
                canvas.drawLine(rightEyeX - eyeSize, eyeY - eyeSize, rightEyeX + eyeSize, eyeY + eyeSize, paint);
                canvas.drawLine(rightEyeX + eyeSize, eyeY - eyeSize, rightEyeX - eyeSize, eyeY + eyeSize, paint);
            }
        }
        
        // 使颜色变暗
        private int darker(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] *= 0.8f; // 降低亮度
            return Color.HSVToColor(hsv);
        }
        
        // 使颜色变亮
        private int brighter(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = Math.min(hsv[2] * 1.2f, 1.0f); // 增加亮度
            return Color.HSVToColor(hsv);
        }
        
        // 获取元素类型的颜色
        private int getColorForElementType(int type) {
            switch (type) {
                case TrickElement.TYPE_NORMAL_PLATFORM:
                    return Color.GRAY;
                case TrickElement.TYPE_FAKE_PLATFORM:
                    return Color.GRAY; // 和普通平台一样，欺骗玩家
                case TrickElement.TYPE_TRAMPOLINE:
                    return Color.YELLOW;
                case TrickElement.TYPE_REVERSE:
                    return Color.MAGENTA;
                case TrickElement.TYPE_GOAL:
                    return Color.GREEN;
                case TrickElement.TYPE_FAKE_GOAL:
                    return Color.GREEN; // 伪装成真目标
                case TrickElement.TYPE_MOVING:
                    return Color.CYAN;
                case TrickElement.TYPE_SLIPPERY:
                    return Color.BLUE;
                default:
                    return Color.GRAY;
            }
        }
        
        // 根据当前光照级别调整颜色亮度
        private int adjustColorByLight(int color) {
            if (!isAmbientLightEnabled) return color;
            
            // 获取原始颜色的RGB分量
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            
            // 计算亮度系数 (0.5-1.5)
            float lightFactor = 0.5f + (currentLightLevel / MAX_LIGHT_LEVEL);
            
            // 调整RGB值
            red = Math.min(255, Math.max(0, (int)(red * lightFactor)));
            green = Math.min(255, Math.max(0, (int)(green * lightFactor)));
            blue = Math.min(255, Math.max(0, (int)(blue * lightFactor)));
            
            return Color.rgb(red, green, blue);
        }
        
        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            // 停止游戏循环，防止内存泄漏
            stopGameLoop();
        }
    }
    
    // 背景图层类 - 用于视差效果
    private class BackgroundLayer {
        private int color;
        private float speed;
        private float offset = 0;
        private List<Star> stars = new ArrayList<>();
        
        public BackgroundLayer(int color, float speed) {
            this.color = color;
            this.speed = speed;
            
            // 创建随机星星
            Random random = new Random();
            for (int i = 0; i < 50; i++) {
                stars.add(new Star(
                    random.nextFloat() * 1000,
                    random.nextFloat() * 500,
                    1 + random.nextFloat() * 3
                ));
            }
        }
        
        public void update(float playerVelocity) {
            // 根据玩家移动更新偏移量
            offset -= playerVelocity * speed * 0.1f;
            
            // 确保偏移量在合理范围内
            if (offset < -1000) offset += 1000;
            if (offset > 1000) offset -= 1000;
        }
        
        public void draw(Canvas canvas, int width, int height) {
            // 绘制背景
            Paint layerPaint = new Paint();
            layerPaint.setColor(color);
            canvas.drawRect(0, 0, width, height * 0.7f, layerPaint);
            
            // 绘制星星
            Paint starPaint = new Paint();
            starPaint.setColor(Color.WHITE);
            
            for (Star star : stars) {
                float x = (star.x + offset * speed) % width;
                if (x < 0) x += width;
                
                starPaint.setAlpha((int)(star.size * 50));
                canvas.drawCircle(x, star.y, star.size, starPaint);
            }
        }
    }
    
    // 星星类 - 用于背景
    private class Star {
        public float x, y, size;
        
        public Star(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停游戏循环
        if (gameView != null) {
            gameView.stopGameLoop();
        }
        
        // 取消注册光线传感器
        if (sensorManager != null) {
            sensorManager.unregisterListener(lightSensorListener);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复游戏循环
        if (gameView != null) {
            gameView.startGameLoop();
        }
        
        // 重新注册光线传感器
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        // 取消注册光线传感器
        if (sensorManager != null) {
            sensorManager.unregisterListener(lightSensorListener);
        }
        
        // 释放音效播放器资源
        releaseMediaPlayers();
    }
    
    /**
     * 释放所有MediaPlayer资源
     */
    private void releaseMediaPlayers() {
        if (mpNani != null) {
            mpNani.release();
            mpNani = null;
        }
        if (mpYeehaw != null) {
            mpYeehaw.release();
            mpYeehaw = null;
        }
        if (mpLaugh != null) {
            mpLaugh.release();
            mpLaugh = null;
        }
        if (mpCollect != null) {
            mpCollect.release();
            mpCollect = null;
        }
        if (mpFail != null) {
            mpFail.release();
            mpFail = null;
        }
        if (mpWin != null) {
            mpWin.release();
            mpWin = null;
        }
    }
}
