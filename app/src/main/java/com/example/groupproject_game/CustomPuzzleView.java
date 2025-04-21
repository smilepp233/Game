package com.example.groupproject_game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.groupproject_game.model.Block;
import java.util.List;

public class CustomPuzzleView extends View {
    private int numRows;
    private int numCols;
    private float cellSize;    // Calculated in onSizeChanged()

    private List<Block> blocks;  // All puzzle blocks
    private int[][] board;       // Board: -1 = empty; or block ID

    private Paint blockPaint;
    private Paint gridPaint;
    private Paint targetPaint;
    private Paint textPaint;

    private Block selectedBlock = null;
    // Offsets for dragging (sample code variables)
    private float dX, dY;
    private float touchStartX, touchStartY;

    // Target position for the goal block
    private int targetRow = -1;
    private int targetCol = -1;

    // For tracking moves
    private OnMoveListener onMoveListener;
    private OnPuzzleSolvedListener onPuzzleSolvedListener;
    private int movesCount = 0;
    private boolean puzzleSolved = false;


    public interface OnMoveListener {
        void onMoveMade(int movesCount);
    }

    public interface OnPuzzleSolvedListener {
        void onPuzzleSolved();
    }

    public CustomPuzzleView(Context context, List<Block> blocks, int[][] board, int numRows, int numCols) {
        super(context);
        this.blocks = blocks;
        this.board = board;
        this.numRows = numRows;
        this.numCols = numCols;

        // Ensure the view is focusable and clickable
        setFocusable(true);
        setClickable(true);

        // Paint for the blocks
        blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Paint for the grid lines
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.BLACK);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3);

        // Paint for target position
        targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setColor(Color.rgb(255, 0, 0));
        targetPaint.setAlpha(40);

        // Paint for text
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.onMoveListener = listener;
    }

    public void setOnPuzzleSolvedListener(OnPuzzleSolvedListener listener) {
        this.onPuzzleSolvedListener = listener;
    }

    public void setTargetPosition(int row, int col) {
        this.targetRow = row;
        this.targetCol = col;
        // 重置拼图解决状态，因为目标位置改变了
        resetPuzzleSolvedState();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Determine the cell size based on the smaller dimension
        cellSize = Math.min((float) w / numCols, (float) h / numRows);
        // Adjust text size
        textPaint.setTextSize(cellSize * 0.4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制白色背景，使网格更加清晰
        canvas.drawColor(Color.rgb(240, 240, 240));

        // 绘制目标位置 - 使用更明显的颜色
        if (targetRow >= 0 && targetCol >= 0) {
            float left = targetCol * cellSize;
            float top = targetRow * cellSize;
            float right = left + 2 * cellSize; // 假设目标是2x2
            float bottom = top + 2 * cellSize;
            
            // 使用更明显的红色，透明度提高
            targetPaint.setColor(Color.rgb(255, 0, 0));
            targetPaint.setAlpha(80); // 增加透明度，使更加明显
            canvas.drawRect(left, top, right, bottom, targetPaint);
            
            // 添加边框更清晰地标记目标位置
            Paint targetBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            targetBorderPaint.setColor(Color.RED);
            targetBorderPaint.setStyle(Paint.Style.STROKE);
            targetBorderPaint.setStrokeWidth(5);
            canvas.drawRect(left, top, right, bottom, targetBorderPaint);
        }

        // 绘制网格 - 使用更加明显的线条
        drawGrid(canvas);

        // 绘制每个方块，添加阴影使其看起来更加立体
        for (Block block : blocks) {
            // 如果这个方块正在被拖动，使用其临时位置
            float left, top;
            if (block == selectedBlock && block.tempX >= 0 && block.tempY >= 0) {
                left = block.tempX;
                top = block.tempY;
            } else {
                left = block.col * cellSize;
                top = block.row * cellSize;
            }
            float right = left + block.width * cellSize;
            float bottom = top + block.height * cellSize;

            // 设置方块颜色
            blockPaint.setColor(block.color);
            
            // 添加阴影
            blockPaint.setShadowLayer(8, 4, 4, Color.DKGRAY);
            
            // 绘制圆角矩形
            RectF rect = new RectF(left + 4, top + 4, right - 4, bottom - 4);
            canvas.drawRoundRect(rect, 15, 15, blockPaint);
            
            // 移除阴影，以免影响文本绘制
            blockPaint.setShadowLayer(0, 0, 0, 0);

            // 绘制方块ID/文本 - 使用更大的字体
            float centerX = left + (block.width * cellSize / 2);
            float centerY = top + (block.height * cellSize / 2);
            
            // 更改文本大小和颜色，增强可读性
            textPaint.setTextSize(cellSize * 0.5f);
            textPaint.setColor(Color.WHITE);
            textPaint.setFakeBoldText(true); // 使文本加粗
            
            // 添加文本阴影
            textPaint.setShadowLayer(2, 1, 1, Color.BLACK);
            
            canvas.drawText(String.valueOf(block.id), centerX, centerY + 10, textPaint);
            textPaint.setShadowLayer(0, 0, 0, 0); // 移除阴影
        }
    }

    private void drawGrid(Canvas canvas) {
        // 使用更明显的网格线
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStrokeWidth(2);
        
        // 绘制水平线
        for (int r = 0; r <= numRows; r++) {
            float y = r * cellSize;
            canvas.drawLine(0, y, numCols * cellSize, y, gridPaint);
        }
        // 绘制垂直线
        for (int c = 0; c <= numCols; c++) {
            float x = c * cellSize;
            canvas.drawLine(x, 0, x, numRows * cellSize, gridPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (puzzleSolved) {
            Log.d("PuzzleView", "拼图已完成，忽略触摸事件");
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Select block at touch position
                selectedBlock = getBlockAtPosition(x, y);
                if (selectedBlock != null) {
                    // Compute the offset between block's top-left (in pixels) and the raw touch coordinates
                    float blockLeft = selectedBlock.col * cellSize;
                    float blockTop = selectedBlock.row * cellSize;
                    dX = blockLeft - event.getRawX();
                    dY = blockTop - event.getRawY();
                    // Initialize temporary position for dragging
                    selectedBlock.tempX = blockLeft;
                    selectedBlock.tempY = blockTop;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (selectedBlock != null) {
                    // Update temporary position using the sample drag code logic
                    selectedBlock.tempX = event.getRawX() + dX;
                    selectedBlock.tempY = event.getRawY() + dY;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (selectedBlock != null) {
                    // Snap block to the nearest grid cell
                    int newCol = Math.round(selectedBlock.tempX / cellSize);
                    int newRow = Math.round(selectedBlock.tempY / cellSize);
                    
                    // 是否移动了方块
                    boolean blockMoved = false;
                    
                    if (canMoveBlock(selectedBlock, newRow - selectedBlock.row, newCol - selectedBlock.col)) {
                        removeBlockFromBoard(selectedBlock);
                        selectedBlock.row = newRow;
                        selectedBlock.col = newCol;
                        placeBlockOnBoard(selectedBlock);
                        movesCount++;
                        blockMoved = true;
                        if (onMoveListener != null) {
                            onMoveListener.onMoveMade(movesCount);
                        }
                    }
                    // Reset temporary dragging values
                    selectedBlock.tempX = -1;
                    selectedBlock.tempY = -1;
                    
                    // 检查是否完成拼图
                    if (blockMoved) {
                        Log.d("PuzzleView", "方块已移动，检查是否完成");
                        checkWinCondition();
                    }
                    
                    selectedBlock = null;
                    invalidate();
                }
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Returns the block at the given pixel position, or null if none.
     */
    private Block getBlockAtPosition(float x, float y) {
        int col = (int) (x / cellSize);
        int row = (int) (y / cellSize);
        if (row < 0 || row >= numRows || col < 0 || col >= numCols) return null;
        int blockId = board[row][col];
        if (blockId == -1) return null;
        for (Block block : blocks) {
            if (block.id == blockId) {
                return block;
            }
        }
        return null;
    }

    /**
     * Checks if a block can move by (dRow, dCol) without colliding.
     */
    private boolean canMoveBlock(Block block, int dRow, int dCol) {
        int newRow = block.row + dRow;
        int newCol = block.col + dCol;
        if (newRow < 0 || newRow + block.height > numRows) return false;
        if (newCol < 0 || newCol + block.width > numCols) return false;
        removeBlockFromBoard(block);
        for (int r = newRow; r < newRow + block.height; r++) {
            for (int c = newCol; c < newCol + block.width; c++) {
                if (board[r][c] != -1) {
                    placeBlockOnBoard(block);
                    return false;
                }
            }
        }
        placeBlockOnBoard(block);
        return true;
    }

    private void removeBlockFromBoard(Block block) {
        for (int r = block.row; r < block.row + block.height; r++) {
            for (int c = block.col; c < block.col + block.width; c++) {
                board[r][c] = -1;
            }
        }
    }

    private void placeBlockOnBoard(Block block) {
        for (int r = block.row; r < block.row + block.height; r++) {
            for (int c = block.col; c < block.col + block.width; c++) {
                board[r][c] = block.id;
            }
        }
    }

    private void checkWinCondition() {
        if (targetRow >= 0 && targetCol >= 0) {
            // 寻找目标方块
            Block goalBlock = null;
            for (Block block : blocks) {
                if (block.isGoalBlock) {
                    goalBlock = block;
                    break;
                }
            }
            
            // 如果找到目标方块，检查它是否在目标位置
            if (goalBlock != null) {
                // 记录检查情况
                Log.d("PuzzleView", "检查目标方块位置: (" + goalBlock.row + "," + goalBlock.col + 
                                   ") 目标位置: (" + targetRow + "," + targetCol + ")");
                
                if (goalBlock.row == targetRow && goalBlock.col == targetCol) {
                    // 如果已经解决，避免重复触发
                    if (!puzzleSolved) {
                        Log.d("PuzzleView", "拼图完成！触发完成回调");
                        puzzleSolved = true;
                        invalidate(); // 确保UI更新
                        
                        // 震动提示完成
                        try {
                            android.os.Vibrator vibrator = (android.os.Vibrator) getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
                            if (vibrator != null && vibrator.hasVibrator()) {
                                vibrator.vibrate(300);
                            }
                        } catch (Exception e) {
                            Log.e("PuzzleView", "振动器错误: " + e.getMessage());
                        }
                        
                        // 直接调用回调，不使用Handler延迟
                        if (onPuzzleSolvedListener != null) {
                            Log.d("PuzzleView", "立即调用onPuzzleSolved监听器");
                            onPuzzleSolvedListener.onPuzzleSolved();
                        } else {
                            Log.e("PuzzleView", "错误：onPuzzleSolvedListener为null");
                        }
                    }
                }
            } else {
                Log.e("PuzzleView", "错误：未找到目标方块");
            }
        } else {
            Log.e("PuzzleView", "错误：目标位置未设置 targetRow=" + targetRow + ", targetCol=" + targetCol);
        }
    }

    /**
     * 检查拼图是否已经完成
     * @return 如果目标方块已经到达目标位置，返回true
     */
    public boolean isPuzzleSolved() {
        if (targetRow < 0 || targetCol < 0) {
            Log.d("PuzzleView", "isPuzzleSolved: 目标位置未设置");
            return false;
        }
        
        // 如果拼图已经标记为完成，返回true
        if (puzzleSolved) {
            return true;
        }
        
        // 检查目标位置是否被目标方块占据
        for (Block block : blocks) {
            if (block.isGoalBlock && 
                block.row == targetRow && 
                block.col == targetCol) {
                Log.d("PuzzleView", "isPuzzleSolved: 拼图已解决！");
                // 标记为已解决
                puzzleSolved = true;
                return true;
            }
        }
        
        // 输出当前目标方块位置和目标位置，方便调试
        for (Block block : blocks) {
            if (block.isGoalBlock) {
                Log.d("PuzzleView", "isPuzzleSolved: 目标方块位置("+block.row+","+block.col+"), 目标位置("+targetRow+","+targetCol+")");
                break;
            }
        }
        
        return false;
    }

    /**
     * 重置拼图的解决状态，当开始新关卡或重置时调用
     */
    public void resetPuzzleSolvedState() {
        puzzleSolved = false;
        Log.d("PuzzleView", "拼图状态已重置");
    }
}
