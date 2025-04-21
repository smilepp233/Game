package com.example.groupproject_game;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private FloatingActionButton btnToggleMusic;
    private boolean isMusicPlaying = false;
    private MusicService musicService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            updateMusicButton();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupAnimations();
        setupClickListeners();
        displayWelcomeMessage();
        startMusicService();
    }
    
    private void initViews() {
        welcomeTextView = findViewById(R.id.textViewWelcome);
        btnToggleMusic = findViewById(R.id.btnToggleMusic);
    }
    
    private void setupAnimations() {
        View puzzleCard = findViewById(R.id.puzzleCard);
        puzzleCard.setTranslationY(1000f);
        puzzleCard.animate()
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(200)
            .start();
    }
    
    private void setupClickListeners() {
        // 找到所有按钮
        MaterialButton btnTapCoordinate = findViewById(R.id.btnTapCoordinatePuzzle);
        MaterialButton btnTapMoveTimer = findViewById(R.id.btnTapMoveTimerPuzzle);
        MaterialButton btnMoveOverlap = findViewById(R.id.btnMoveOverlapPuzzle);
        MaterialButton btnShakeMove = findViewById(R.id.btnShakeMoveCoordinatePuzzle);
        MaterialButton btnColorSwitch = findViewById(R.id.btnColorSwitchMadness);
        MaterialButton btnReactionSequence = findViewById(R.id.btnReactionSequenceChallenge);
        MaterialButton btnMathSpeed = findViewById(R.id.btnMathSpeedChallenge);
        MaterialButton btnColorMemory = findViewById(R.id.btnColorMemoryChallenge);
        MaterialButton btnScoreboard = findViewById(R.id.btnScoreboard);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        MaterialButton btnTricksterPath = findViewById(R.id.btnTricksterPath);
        MaterialButton btnVRGame = findViewById(R.id.btnVRGame);
        FloatingActionButton btnLogin = findViewById(R.id.btnLogin);
        
        // 设置点击监听器
        btnTapCoordinate.setOnClickListener(v -> startGame(TapCoordinatePuzzleActivity.class));
        btnTapMoveTimer.setOnClickListener(v -> startGame(TapMoveTimerPuzzleActivity.class));
        btnMoveOverlap.setOnClickListener(v -> startGame(MoveOverlapPuzzleActivity.class));
        btnShakeMove.setOnClickListener(v -> startGame(ShakeMoveCoordinatePuzzleActivity.class));
        btnColorSwitch.setOnClickListener(v -> startGame(ColorSwitchMadnessActivity.class));
        btnTricksterPath.setOnClickListener(v -> startGame(TricksterPathActivity.class));
        btnReactionSequence.setOnClickListener(v -> startGame(ReactionSequenceChallengeActivity.class));
        btnMathSpeed.setOnClickListener(v -> startGame(MathSpeedChallengeActivity.class));
        btnColorMemory.setOnClickListener(v -> startGame(ColorMemoryChallengeActivity.class));
        btnScoreboard.setOnClickListener(v -> startGame(ScoreboardActivity.class));
        btnRegister.setOnClickListener(v -> startGame(RegisterActivity.class));
        btnLogin.setOnClickListener(v -> startGame(LoginActivity.class));
        btnVRGame.setOnClickListener(v -> startGame(NFTGame.class));
        
        // 设置音乐控制按钮点击监听器
        btnToggleMusic.setOnClickListener(v -> toggleMusic());
    }
    
    private void startGame(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }
    
    private void displayWelcomeMessage() {
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = "Guest";
        }
        welcomeTextView.setText("Welcome, " + username + "!");
    }
    
    private void startMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }
    
    private void toggleMusic() {
        if (isBound && musicService != null) {
            Intent intent = new Intent(this, MusicService.class);
            intent.setAction("TOGGLE");
            startService(intent);
            isMusicPlaying = !isMusicPlaying;
            updateMusicButton();
        }
    }
    
    private void updateMusicButton() {
        if (isBound && musicService != null) {
            isMusicPlaying = musicService.isPlaying();
            btnToggleMusic.setImageResource(isMusicPlaying ? 
                android.R.drawable.ic_media_pause : 
                android.R.drawable.ic_media_play);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (isBound && musicService != null) {
            updateMusicButton();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }
}