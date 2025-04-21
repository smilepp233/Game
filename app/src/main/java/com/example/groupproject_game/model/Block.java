package com.example.groupproject_game.model;

import android.graphics.Color;

public class Block {
    public int id;          // Unique ID for the block
    public int row;         // Current row in the grid
    public int col;         // Current column in the grid
    public int width;       // Width in grid cells
    public int height;      // Height in grid cells
    public boolean movableHorizontally;
    public boolean movableVertically;
    public boolean isGoalBlock;
    public int color;       // Block color

    // 预定义颜色
    public static final int COLOR_RED = Color.rgb(231, 76, 60);     // 红色
    public static final int COLOR_BLUE = Color.rgb(52, 152, 219);   // 蓝色
    public static final int COLOR_GREEN = Color.rgb(46, 204, 113);  // 绿色
    public static final int COLOR_PURPLE = Color.rgb(155, 89, 182); // 紫色
    public static final int COLOR_ORANGE = Color.rgb(230, 126, 34); // 橙色
    public static final int COLOR_YELLOW = Color.rgb(241, 196, 15); // 黄色
    public static final int COLOR_TEAL = Color.rgb(26, 188, 156);   // 青色
    
    // 颜色数组，用于随机选择
    public static final int[] COLORS = {
        COLOR_RED, COLOR_BLUE, COLOR_GREEN, COLOR_PURPLE, 
        COLOR_ORANGE, COLOR_YELLOW, COLOR_TEAL
    };

    // Temporary positions (in pixels) used during dragging.
    // If not dragging, these remain negative.
    public float tempX = -1;
    public float tempY = -1;

    // Constructor with a default color.
    public Block(int id, int row, int col, int width, int height,
        boolean movableHorizontally, boolean movableVertically) {
        this.id = id;
        this.row = row;
        this.col = col;
        this.width = width;
        this.height = height;
        this.movableHorizontally = movableHorizontally;
        this.movableVertically = movableVertically;
        this.isGoalBlock = false;
        this.color = COLORS[id % COLORS.length]; // 使用ID取模分配颜色
    }

    // Overloaded constructor allowing a specific color.
    public Block(int id, int row, int col, int width, int height,
        boolean movableHorizontally, boolean movableVertically, int color) {
        this(id, row, col, width, height, movableHorizontally, movableVertically);
        this.color = color;
    }
    
    // 设置为目标方块，并使用红色
    public void setAsGoalBlock() {
        this.isGoalBlock = true;
        this.color = COLOR_RED;
    }
}
