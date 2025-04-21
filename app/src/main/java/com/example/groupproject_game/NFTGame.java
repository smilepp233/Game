package com.example.groupproject_game;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class NFTGame extends AppCompatActivity {
    private static final String TAG = "NFCRockPaperScissors";
    
    // NFC相关
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    
    // 游戏相关
    private TextView resultTextView;
    private TextView yourChoiceTextView;
    private TextView computerChoiceTextView;
    private ImageView yourChoiceImageView;
    private ImageView computerChoiceImageView;
    private Button randomButton;
    private int playerWins = 0;
    private int computerWins = 0;
    private TextView winCountTextView;
    
    // 猜拳选项
    private static final String ROCK = "石頭";
    private static final String PAPER = "布";
    private static final String SCISSORS = "剪刀";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_game);
        
        // 初始化视图组件
        resultTextView = findViewById(R.id.resultTextView);
        yourChoiceTextView = findViewById(R.id.yourChoiceTextView);
        computerChoiceTextView = findViewById(R.id.computerChoiceTextView);
        yourChoiceImageView = findViewById(R.id.yourChoiceImageView);
        computerChoiceImageView = findViewById(R.id.computerChoiceImageView);
        randomButton = findViewById(R.id.randomButton);
        winCountTextView = findViewById(R.id.winCountTextView);
        
        // 初始化NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        // 检查设备是否支持NFC
        if (nfcAdapter == null) {
            Toast.makeText(this, "此設備不支持NFC", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // 检查NFC是否开启
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "請開啟NFC功能", Toast.LENGTH_LONG).show();
        }
        
        // 设置NFC意图
        pendingIntent = PendingIntent.getActivity(
            this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            
        // 设置意图过滤器
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("MimeType错误", e);
        }
        intentFiltersArray = new IntentFilter[] { ndef };
        
        // 设置随机按钮点击事件
        randomButton.setOnClickListener(v -> playRandomGame());
        
        // 显示NFC使用说明
        showNfcInstructions();
        
        // 更新胜利计数
        updateWinCount();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 启用NFC前台调度系统
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // 禁用NFC前台调度系统
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        // 处理NFC扫描结果
        String nfcResult = processNfcIntent(intent);
        if (nfcResult != null) {
            playGame(nfcResult);
        }
    }
    
    /**
     * 处理NFC标签中的数据
     */
    private String processNfcIntent(Intent intent) {
        // 检查是否是NFC标签
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    if (rawMessages != null) {
                        NdefMessage[] messages = new NdefMessage[rawMessages.length];
                        for (int i = 0; i < rawMessages.length; i++) {
                            messages[i] = (NdefMessage) rawMessages[i];
                        }
                        
                        // 尝试从NDEF消息中解析出文本
                        for (NdefMessage message : messages) {
                            NdefRecord[] records = message.getRecords();
                            for (NdefRecord record : records) {
                                String result = parseNdefRecord(record);
                                if (result != null) {
                                    return result;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 解析NDEF记录，提取文本
     */
    private String parseNdefRecord(NdefRecord record) {
        try {
            byte[] payload = record.getPayload();
            if (payload != null && payload.length > 0) {
                // 检查记录类型
                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && 
                    java.util.Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    
                    // 第一个字节包含状态位和语言代码的长度
                    int languageCodeLength = payload[0] & 0x3F; // 6 bits
                    
                    // 获取文本内容
                    return new String(payload, languageCodeLength + 1, 
                           payload.length - languageCodeLength - 1, "UTF-8");
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "不支持的编码", e);
        }
        return null;
    }
    
    /**
     * 处理游戏逻辑
     */
    private void playGame(String playerChoice) {
        // 标准化选择（去除空格，转换为标准格式）
        String normalizedChoice = normalizeChoice(playerChoice);
        
        // 设置玩家的选择
        yourChoiceTextView.setText("您的选择: " + normalizedChoice);
        
        // 设置玩家的图像
        setChoiceImage(yourChoiceImageView, normalizedChoice);
        
        // 电脑随机选择
        String[] choices = {ROCK, PAPER, SCISSORS};
        String computerChoice = choices[new Random().nextInt(choices.length)];
        
        // 设置电脑的选择
        computerChoiceTextView.setText("电脑的选择: " + computerChoice);
        
        // 设置电脑的图像
        setChoiceImage(computerChoiceImageView, computerChoice);
        
        // 确定胜负
        String result = determineWinner(normalizedChoice, computerChoice);
        resultTextView.setText(result);
        
        // 更新胜利计数
        updateWinCount();
    }
    
    /**
     * 随机游戏（按钮触发）
     */
    private void playRandomGame() {
        // 电脑随机选择
        String[] choices = {ROCK, PAPER, SCISSORS};
        String playerChoice = choices[new Random().nextInt(choices.length)];
        
        // 播放游戏
        playGame(playerChoice);
    }
    
    /**
     * 标准化用户选择
     */
    private String normalizeChoice(String choice) {
        if (choice == null) return ROCK;
        
        choice = choice.trim().toLowerCase();
        
        if (choice.contains("石") || choice.contains("rock")) {
            return ROCK;
        } else if (choice.contains("布") || choice.contains("paper")) {
            return PAPER;
        } else if (choice.contains("剪") || choice.contains("scissors")) {
            return SCISSORS;
        }
        
        // 默认返回石头
        return ROCK;
    }
    
    /**
     * 设置选择对应的图像
     */
    private void setChoiceImage(ImageView imageView, String choice) {
        if (ROCK.equals(choice)) {
            imageView.setImageResource(R.drawable.rock);
        } else if (PAPER.equals(choice)) {
            imageView.setImageResource(R.drawable.paper);
        } else if (SCISSORS.equals(choice)) {
            imageView.setImageResource(R.drawable.scissors);
        }
        
        // 确保图像可见
        imageView.setVisibility(View.VISIBLE);
    }
    
    /**
     * 判断胜负
     */
    private String determineWinner(String playerChoice, String computerChoice) {
        if (playerChoice.equals(computerChoice)) {
            return "平局";
        } else if ((ROCK.equals(playerChoice) && SCISSORS.equals(computerChoice)) ||
                  (SCISSORS.equals(playerChoice) && PAPER.equals(computerChoice)) ||
                  (PAPER.equals(playerChoice) && ROCK.equals(computerChoice))) {
            playerWins++;
            return "您赢了！";
        } else {
            computerWins++;
            return "电脑赢了";
        }
    }
    
    /**
     * 更新胜利计数
     */
    private void updateWinCount() {
        winCountTextView.setText("连胜次数: " + playerWins + " | 电脑胜: " + computerWins);
    }
    
    /**
     * 显示NFC使用教程
     */
    private void showNfcInstructions() {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            return;
        }
        
        // 显示一次性教程提示
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("如何準備NFC標籤")
            .setMessage("1. 準備三個NFC標籤\n\n" +
                        "2. 使用NFC Tools等應用寫入純文本數據：\n" +
                        "   - 第一個標籤寫入「剪刀」或「scissors」\n" +
                        "   - 第二個標籤寫入「石頭」或「rock」\n" +
                        "   - 第三個標籤寫入「布」或「paper」\n\n" +
                        "3. 標記標籤背面，但不要透露內容\n\n" +
                        "4. 遊戲時將手機靠近標籤即可")
            .setPositiveButton("知道了", null)
            .show();
    }
}