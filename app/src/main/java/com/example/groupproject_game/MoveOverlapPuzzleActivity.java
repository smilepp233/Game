package com.example.groupproject_game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class MoveOverlapPuzzleActivity extends AppCompatActivity {
    private ImageView draggableObject;
    private ImageView targetObject;
    private ImageView hiddenTarget;
    private ConstraintLayout gameLayout;
    private float dX, dY;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_overlap_puzzle);

        gameLayout = findViewById(R.id.gameLayout);
        draggableObject = findViewById(R.id.draggableObject);
        targetObject = findViewById(R.id.targetObject);
        hiddenTarget = findViewById(R.id.hiddenTarget);
        userManager = new UserManager(this);

        draggableObject.setImageBitmap(createScoopBitmap());
        targetObject.setImageBitmap(createFakeIceCreamBitmap());
        hiddenTarget.setImageBitmap(createRealIceCreamBitmap());

        targetObject.animate().rotation(360f).setDuration(2000).start();

        draggableObject.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        checkOverlap();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private Bitmap createScoopBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        paint.setColor(Color.rgb(255, 182, 193)); // Pink scoop
        canvas.drawCircle(40, 40, 30, paint);
        return bitmap;
    }

    private Bitmap createFakeIceCreamBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        paint.setColor(Color.rgb(139, 69, 19)); // Brown cone
        float[] conePoints = {60, 20, 20, 100, 100, 100};
        canvas.drawLines(conePoints, paint);
        paint.setColor(Color.GRAY); // Fake gray scoop
        canvas.drawCircle(60, 20, 40, paint);
        return bitmap;
    }

    private Bitmap createRealIceCreamBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        paint.setColor(Color.rgb(210, 105, 30)); // Cone brown
        float[] conePoints = {20, 5, 10, 35, 30, 35};
        canvas.drawLines(conePoints, paint);
        paint.setColor(Color.rgb(255, 182, 193)); // Pink scoop
        canvas.drawCircle(20, 5, 15, paint);
        return bitmap;
    }

    private void checkOverlap() {
        if (isViewOverlapping(draggableObject, hiddenTarget)) {
            Toast.makeText(this, "Yum! Real Ice Cream Found!", Toast.LENGTH_SHORT).show();
            onStageComplete();
        } else if (isViewOverlapping(draggableObject, targetObject)) {
            Toast.makeText(this, "Eww! That’s not ice cream!", Toast.LENGTH_SHORT).show();
            draggableObject.animate()
                .x(0f)
                .y(gameLayout.getHeight() - draggableObject.getHeight() - 50)
                .setDuration(500)
                .start();
            targetObject.animate().rotationBy(360f).setDuration(500).start();
        } else {
            Toast.makeText(this, "Scoop the real one!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isViewOverlapping(View firstView, View secondView) {
        int[] firstLocation = new int[2];
        int[] secondLocation = new int[2];

        firstView.getLocationOnScreen(firstLocation);
        secondView.getLocationOnScreen(secondLocation);

        return firstLocation[0] < secondLocation[0] + secondView.getWidth() &&
            firstLocation[0] + firstView.getWidth() > secondLocation[0] &&
            firstLocation[1] < secondLocation[1] + secondView.getHeight() &&
            firstLocation[1] + firstView.getHeight() > secondLocation[1];
    }

    private void onStageComplete() {
        UserManager.User currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            userManager.updateStageProgress(currentUser.username, 3, 100, 30000);
        }
        finish();
    }
}