package com.example.commonvoicesentencecollector.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.commonvoicesentencecollector.InitActivity;
import com.example.commonvoicesentencecollector.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    final EditText usernameEditText = findViewById(R.id.username);
    final EditText passwordEditText = findViewById(R.id.password);
    final Button loginButton = findViewById(R.id.login);
    final ProgressBar loadingProgressBar = findViewById(R.id.loading);
    final SharedPreferences preferences = getApplicationContext().getSharedPreferences(getString(R.string.prefsFileName), MODE_PRIVATE);
    if (preferences.contains("auth")) finish();

    final TextWatcher enableLoginWatcher = new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void afterTextChanged(Editable editable) {
        if (usernameEditText.length() != 0 && passwordEditText.length() != 0) {
          loginButton.setEnabled(true);
        } else {
          loginButton.setEnabled(false);
        }
      }
    };
    usernameEditText.addTextChangedListener(enableLoginWatcher);
    passwordEditText.addTextChangedListener(enableLoginWatcher);


    loginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        usernameEditText.setEnabled(false);
        passwordEditText.setEnabled(false);
        loginButton.setEnabled(false);
        new Thread(new Runnable() {
          @Override
          public void run() {
            HttpURLConnection connection = null;
            try{
              URL url = new URL("https://kinto.mozvoice.org/v1/buckets/App/collections/User/records/" + usernameEditText.getText());
              try {
                connection = (HttpURLConnection) url.openConnection();
                final String user_pass_encoded = Base64.encodeToString((usernameEditText.getText() + ":" + passwordEditText.getText()).getBytes("UTF-8"), Base64.DEFAULT);
                final String auth = "Basic " + user_pass_encoded;
                connection.addRequestProperty("Authorization", auth);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                  final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                  StringBuilder sb = new StringBuilder();
                  String line;
                  while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                  }
                  reader.close();
                  final SharedPreferences.Editor prefs = preferences.edit();
                  try {
                    final JSONObject data = new JSONObject(sb.toString()).getJSONObject("data");
                    prefs.putString("username", data.getString("id"));
                    StringBuilder langs = new StringBuilder();
                    final JSONArray langsArray = data.getJSONArray("languages");
                    for (int i = 0; i < langsArray.length(); i++) {
                      if (i != 0) langs.append(","); // only append for nonfirst, so we don't start with ,
                      langs.append(langsArray.getString(i));
                    }
                    prefs.putString("languages", langs.toString());
                  } catch (JSONException e) {
                    LoginActivity.this.runOnUiThread(new Runnable() {
                      @Override public void run() { Toast.makeText(LoginActivity.this, "Received malformed data from server!", Toast.LENGTH_SHORT).show(); }
                    });
                  }
                  prefs.putString("auth", auth).apply();
                  LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                      Toast.makeText(LoginActivity.this, "Logged in.", Toast.LENGTH_SHORT).show();
                      finish();
                      startActivity(new Intent(LoginActivity.this, InitActivity.class)); }
                  });
                } else {
                  LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      loadingProgressBar.setVisibility(View.INVISIBLE);
                      usernameEditText.setEnabled(true);
                      passwordEditText.setEnabled(true);
                      loginButton.setEnabled(true);
                    }
                  });
                }
              } catch (IOException e) {
                LoginActivity.this.runOnUiThread(new Runnable() {
                  @Override public void run() { Toast.makeText(LoginActivity.this, "Error trying to connect to the server.", Toast.LENGTH_SHORT).show(); }
                });
                e.printStackTrace();
              }
            } catch(MalformedURLException e) {
              LoginActivity.this.runOnUiThread(new Runnable() {
                @Override public void run() { Toast.makeText(LoginActivity.this, "THIS SHOULD NEVER HAPPEN (1)", Toast.LENGTH_SHORT).show(); }
              });
            } finally {
              if (connection != null) {
                connection.disconnect();
              }
            }
          }
        }).start();
      }
    });
  }

}
