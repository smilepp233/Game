package com.example.groupproject_game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class ScoreboardActivity extends AppCompatActivity {
    private ListView scoreboardListView;
    private UserManager userManager;
    private List<UserManager.ScoreEntry> topUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        // Initialize UserManager
        userManager = new UserManager(this);

        // Find ListView
        scoreboardListView = findViewById(R.id.scoreboardListView);

        // Retrieve top users from the scoreboard
        topUsers = userManager.getScoreboard();

        // Set up adapter
        ScoreboardAdapter adapter = new ScoreboardAdapter(topUsers);
        scoreboardListView.setAdapter(adapter);
    }

    // Custom adapter for scoreboard
    private class ScoreboardAdapter extends BaseAdapter {
        private List<UserManager.ScoreEntry> users;

        public ScoreboardAdapter(List<UserManager.ScoreEntry> users) {
            this.users = users;
        }

        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public UserManager.ScoreEntry getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Inflate the layout for each scoreboard item if necessary
            if (convertView == null) {
                convertView = LayoutInflater.from(ScoreboardActivity.this)
                    .inflate(R.layout.scoreboard_item, parent, false);
            }

            // Get UI components from the item layout
            TextView rankTextView = convertView.findViewById(R.id.textViewRank);
            TextView usernameTextView = convertView.findViewById(R.id.textViewUsername);
            TextView scoreTextView = convertView.findViewById(R.id.textViewScore);

            // Get current ScoreEntry
            UserManager.ScoreEntry user = getItem(position);

            // Set the values
            rankTextView.setText(String.valueOf(position + 1));
            usernameTextView.setText(user.username);
            scoreTextView.setText(String.valueOf(user.score));

            return convertView;
        }
    }
}
