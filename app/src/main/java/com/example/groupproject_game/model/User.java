package com.example.groupproject_game.model;

public class User {
    private int id;
    private String username;
    private String password;
    private int totalScore; // or any other user progress field

    public User() {
        // Default constructor
    }

    public User(int id, String username, String password, int totalScore) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.totalScore = totalScore;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    @Override
    public String toString() {
        return "User{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", totalScore=" + totalScore +
            '}';
    }
}
