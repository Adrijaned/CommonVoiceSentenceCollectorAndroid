package com.example.commonvoicesentencecollector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.commonvoicesentencecollector.net.KintoClient;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReviewActivity extends AppCompatActivity {

  private ArrayList<String> strings;
  private ArrayList<String> stringsBac; // for undo functionality
  private ArrayList<String> stringIds;
  private ArrayList<Boolean> stringAccepted;
  private int placeholdersPlaced;
  private ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_review);
    final SwipeFlingAdapterView cards = findViewById(R.id.swipeFlingAdapterView);
    final String lang = getIntent().getStringExtra("language");
    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    strings = new ArrayList<>();
    stringIds = new ArrayList<>();
    stringAccepted = new ArrayList<>();
    stringsBac = new ArrayList<>();
    placeholdersPlaced = 0;
    final SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.prefsFileName), MODE_PRIVATE);
    adapter = new ArrayAdapter<>(this, R.layout.item, R.id.helloText, strings);

    KintoClient.retrieveUnreviewedSentences(lang, prefs.getString("username", "INVALID"), prefs.getString("auth", "000"), new KintoClient.RetrievedSentencesListener() {
      @Override
      public void run(ArrayList<JSONObject> sentences) {
        try {
          for (int i = 0; i < sentences.size(); i++) {
            final JSONObject sentence = sentences.get(i);
            final String sentenceString = sentence.getString("sentence");
            strings.add(sentenceString);
            stringsBac.add(sentenceString);
            stringIds.add(sentence.getString("id"));
            ReviewActivity.this.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                adapter.notifyDataSetChanged();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
              }
            });
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });

    cards.setAdapter(adapter);
    final SwipeFlingAdapterView.onFlingListener tmp = new SwipeFlingAdapterView.onFlingListener() {
      @Override
      public void removeFirstObjectInAdapter() {
        strings.remove(0);
        adapter.notifyDataSetChanged();
      }

      @Override
      public void onLeftCardExit(Object o) {
        if (placeholdersPlaced > 0) {
          placeholdersPlaced--;
          return;
        }
        stringAccepted.add(false);
      }

      @Override
      public void onRightCardExit(Object o) {
        if (placeholdersPlaced > 0) {
          placeholdersPlaced--;
          return;
        }
        stringAccepted.add(true);
      }

      @Override
      public void onAdapterAboutToEmpty(int i) {
        strings.add("placeholder; please reject");
        adapter.notifyDataSetChanged();
        placeholdersPlaced++;
        Log.d("LIST", "Placeholder inserted");
      }

      @Override
      public void onScroll(float v) {

      }
    };
    cards.setFlingListener(tmp);

    findViewById(R.id.undoButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (stringAccepted.size() != 0) {
          strings.add(0, stringsBac.get(stringAccepted.size() - 1));
          strings.add(0, "whatever");
          stringAccepted.remove(stringAccepted.size() - 1);
          adapter.notifyDataSetChanged();
          placeholdersPlaced++;
          cards.getTopCardListener().selectLeft();
          Log.d("LIST", "rolled back, head now at " + strings.get(1));
        }
      }
    });
    findViewById(R.id.acceptButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        cards.getTopCardListener().selectRight();
      }
    });
    findViewById(R.id.rejectButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        cards.getTopCardListener().selectLeft();
      }
    });
    findViewById(R.id.submitButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          new AlertDialog.Builder(ReviewActivity.this).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int _i) {
              dialogInterface.dismiss();
              for (int i = 0; i < stringAccepted.size() && i < stringIds.size(); i++) {
                KintoClient.updateSentence(stringIds.get(i), prefs.getString("username", "INVALID"), prefs.getString("auth", "000"), lang, stringAccepted.get(i));
              }
              ReviewActivity.this.finish();
              startActivity(new Intent(ReviewActivity.this, InitActivity.class));
            }
          }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              dialogInterface.dismiss();
            }
          }).setMessage("Do you really want to submit " + stringAccepted.size() + " sentences?")
              .create()
              .show();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
