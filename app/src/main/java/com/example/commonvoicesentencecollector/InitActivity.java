package com.example.commonvoicesentencecollector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.commonvoicesentencecollector.ui.login.LoginActivity;

public class InitActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_init);
    final SharedPreferences preferences = getApplicationContext().getSharedPreferences(getString(R.string.prefsFileName), MODE_PRIVATE);
    final Button loginButton = findViewById(R.id.login);
    final Button logoutButton = findViewById(R.id.logout);
    final Button langButton = findViewById(R.id.language);
    final Button reviewButton = findViewById(R.id.review);

    if (preferences.contains("auth")) {
      loginButton.setText("Logged in");
      loginButton.setEnabled(false);
      logoutButton.setText("Log out");
      logoutButton.setEnabled(true);
      logoutButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          preferences.edit().remove("auth").remove("languages").remove("username").apply();
          finish();
          startActivity(getIntent());
        }
      });
      langButton.setText(preferences.getString("languages", "No languages").split(",")[0]);
      langButton.setEnabled(true);
      reviewButton.setEnabled(true);
      reviewButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          final Intent intent = new Intent(InitActivity.this, ReviewActivity.class);
          intent.putExtra("language", langButton.getText());
          startActivity(intent);
        }
      });
    } else {
      loginButton.setText("Log in");
      loginButton.setEnabled(true);
      loginButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          finish();
          startActivity(new Intent(InitActivity.this, LoginActivity.class));
        }
      });
      logoutButton.setText("Not logged in");
      logoutButton.setEnabled(false);
      langButton.setText("No languages");
      langButton.setEnabled(false);
      reviewButton.setEnabled(false);
    }
  }
}
