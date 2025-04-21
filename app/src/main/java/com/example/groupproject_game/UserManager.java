package com.example.groupproject_game;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class UserManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "game.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_SCORE = "score"; // For scoreboard/progress

    // Inner class representing a user.
    public static class User {
        public int id;
        public String username;
        public String password;
        public int totalScore;

        public User(int id, String username, String password, int totalScore) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.totalScore = totalScore;
        }
    }
    public static class ScoreEntry {
        public String username;
        public int score;

        public ScoreEntry(String username, int score) {
            this.username = username;
            this.score = score;
        }
    }

    /**
     * Retrieves the scoreboard as a list of ScoreEntry objects, sorted by score in descending order.
     */
    public List<ScoreEntry> getScoreboard() {
        List<ScoreEntry> scoreboard = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Query the users table ordering by score descending.
        Cursor cursor = db.query(TABLE_USERS,
            new String[]{COLUMN_USERNAME, COLUMN_SCORE},
            null, null, null, null,
            COLUMN_SCORE + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE));
                scoreboard.add(new ScoreEntry(username, score));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return scoreboard;
    }

    // Inner class for registration results.
    public static class RegistrationResult {
        public boolean success;
        public String message;

        public RegistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    // Inner class for login results.
    public static class LoginResult {
        public boolean success;
        public String message;

        public LoginResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    // For simplicity, we store the current logged-in user here.
    private static User currentUser = null;

    public UserManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create the users table with a score column.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_USERNAME + " TEXT UNIQUE, "
            + COLUMN_PASSWORD + " TEXT, "
            + COLUMN_SCORE + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_USERS_TABLE);
    }

    // Simple upgrade by dropping and recreating the table.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Register a new user.
    public RegistrationResult registerUser(String username, String password) {
        if (userExists(username)) {
            return new RegistrationResult(false, "Username already exists");
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password); // In production, store a hashed password.
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result == -1 ? new RegistrationResult(false, "Registration failed")
            : new RegistrationResult(true, "Registration successful");
    }

    // Check if a user exists.
    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
            new String[] { COLUMN_ID },
            COLUMN_USERNAME + "=?",
            new String[] { username },
            null, null, null);
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return exists;
    }

    // Login user and return a LoginResult.
    public LoginResult loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
            new String[] { COLUMN_ID, COLUMN_SCORE },
            COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
            new String[] { username, password },
            null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            int score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE));
            currentUser = new User(id, username, password, score);
            cursor.close();
            db.close();
            return new LoginResult(true, "Login successful");
        } else {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
            return new LoginResult(false, "Invalid credentials");
        }
    }

    // Retrieve the current logged-in user.
    public User getCurrentUser() {
        return currentUser;
    }

    // Update stage progress (e.g., update score).
    public void updateStageProgress(String username, int stage, int score, int time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCORE, score); // Update score field
        int rowsAffected = db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        db.close();

        // Optionally update the in-memory currentUser
        if (currentUser != null && currentUser.username.equals(username)) {
            currentUser.totalScore = score;
        }

        // Debug log (or Toast) to see if the update occurred
        // Log.d("UserManager", "Rows updated: " + rowsAffected);
    }

}
