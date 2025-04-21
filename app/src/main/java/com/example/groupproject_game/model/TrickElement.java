package com.example.groupproject_game.model;

// TrickElement 类 - 代表游戏中的陷阱元素
public class TrickElement {
    public static final int TYPE_NORMAL_PLATFORM = 0;    // 普通平台
    public static final int TYPE_FAKE_PLATFORM = 1;      // 假平台（会消失）
    public static final int TYPE_TRAMPOLINE = 2;         // 弹簧（弹起玩家）
    public static final int TYPE_REVERSE = 3;            // 反向控制
    public static final int TYPE_INVISIBLE = 4;          // 隐形平台
    public static final int TYPE_MOVING = 5;             // 移动平台
    public static final int TYPE_SLIPPERY = 6;           // 滑动平台
    public static final int TYPE_GOAL = 7;               // 目标

    public static final int TYPE_FAKE_GOAL = 8;          // 假目标（会消失）
    public int type;                // 元素类型
    public float x, y;              // 位置坐标
    public float width, height;     // 尺寸
    public boolean isVisible;       // 是否可见
    public boolean isActive;        // 是否激活
    public String message;          // 显示的消息（如提示或嘲讽）
    
    // 特殊属性
    public boolean isTrap;          // 是否为陷阱
    public float moveSpeed;         // 移动速度
    public float moveDistance;      // 移动距离
    public float currentOffset;     // 当前偏移量
    
    public TrickElement(int type, float x, float y, float width, float height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isVisible = true;
        this.isActive = true;
        this.isTrap = false;
        this.message = "";
        
        // 根据类型初始化特殊属性
        switch(type) {
            case TYPE_FAKE_PLATFORM:
                this.isTrap = true;
                break;
            case TYPE_INVISIBLE:
                this.isVisible = false;
                break;
            case TYPE_MOVING:
                this.moveSpeed = 2.0f;
                this.moveDistance = 200.0f;
                this.currentOffset = 0;
                break;
        }
    }
    
    // 更新元素状态（用于动画和行为）
    public void update() {
        if (type == TYPE_MOVING && isActive) {
            // 移动平台逻辑
            currentOffset += moveSpeed;
            if (Math.abs(currentOffset) > moveDistance) {
                moveSpeed = -moveSpeed; // 改变方向
            }
        }
    }
    
    // 添加嘲讽消息
    public void setMessage(String message) {
        this.message = message;
    }
}