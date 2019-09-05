package com.example.commonvoicesentencecollector.net;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.net.ssl.SSLSocketFactory;

public class KintoClient {

  public static void retrieveUnreviewedSentences(final String lang, final String user, final String auth, final RetrievedSentencesListener onFinishedListener) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        URL url = null;
        try {
          url = new URL("https://kinto.mozvoice.org/v1/buckets/App/collections/Sentences_Meta_" + lang + "/records?has_Sentences_Meta_UserVote_" + user + "=false&has_approved=false");
          HttpURLConnection connection = null;
          try {
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Authorization", auth.trim());
            Log.d("NET", "Starting fetching sentences");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
              final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
              StringBuilder sb = new StringBuilder();
              String line;
              while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
              }
              reader.close();
              Log.d("NET", "Fetched sentences");
              try {
                final JSONArray _sentences = new JSONObject(sb.toString()).getJSONArray("data");
                final ArrayList<JSONObject> sentences = new ArrayList<>();
                for (int i = 0; i < _sentences.length(); i++) {
                  sentences.add(_sentences.getJSONObject(i));
                }
                Collections.sort(sentences, new Comparator<JSONObject>() {
                  @Override
                  public int compare(JSONObject t0, JSONObject t1) {
                    try {
                      int votes0 = t0.getJSONArray("valid").length() + t0.getJSONArray("invalid").length();
                      int votes1 = t1.getJSONArray("valid").length() + t1.getJSONArray("invalid").length();
                      if (votes0 < votes1) {
                        return -1;
                      } else if (votes0 > votes1) {
                        return 1;
                      }
                      int timeStamp0 = t0.getInt("createdAt");
                      int timeStamp1 = t1.getInt("createdAt");
                      if (timeStamp0 < timeStamp1) {
                        return -1;
                      } else if (timeStamp0 > timeStamp1) {
                        return 1;
                      } else {
                        return 0;
                      }

                    } catch (JSONException e) {
                      e.printStackTrace();
                      return 0;
                    }
                  }
                });

                onFinishedListener.run(sentences);
              } catch (JSONException e) {
                e.printStackTrace();
              }
            }

          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            if (connection != null) {
              connection.disconnect();
            }
          }
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  // We test for that here because some things may have changed since fetching the sentences initially
  public static boolean testIfApprovalIsRequiredForSentence(final String id, final String lang, final boolean vote) {
    URL url;
    HttpURLConnection connection = null;
    try {
      url = new URL("https://kinto.mozvoice.org/v1/buckets/App/collections/Sentences_Meta_" + lang + "/records/" + id);
      connection = (HttpURLConnection) url.openConnection();
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
          sb.append("\n");
        }
        reader.close();
        try {
          final JSONObject sentence = new JSONObject(sb.toString()).getJSONObject("data");
          if (sentence.has("approved")) return false; // I'm sorry but your vote doesn't matter anymore
          if (vote && sentence.getJSONArray("valid").length() > 0) return true; // Your vote brings over APPROVAL_MIN_VALID_VOTES
          if (!vote && sentence.getJSONArray("invalid").length() > 0) return true; // There is no way previous could be valid anymore
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      connection.disconnect();
    }
    return false;
  }

  public static void updateSentence(final String id, final String username, final String auth, final String lang, final Boolean accepted) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String payload =
              "[{\"op\":\"add\",\"path\":\"/data/" + (accepted ? "" : "in") + "valid/0\",\"value\":\"" + username + "\"}," +
              "{\"op\":\"add\",\"path\":\"/data/Sentences_Meta_UserVote_" + username + "\",\"value\":" + accepted.toString() + "}," +
              "{\"op\":\"add\",\"path\":\"/data/Sentences_Meta_UserVoteDate_" + username + "\",\"value\":" + System.currentTimeMillis() + "}" +
              (testIfApprovalIsRequiredForSentence(id, lang, accepted) ?
              ",{\"op\":\"add\",\"path\":\"/data/approved\",\"value\":" + accepted.toString() + "}," +
              "{\"op\":\"add\",\"path\":\"/data/approvalDate\",\"value\":" + System.currentTimeMillis() + "}": "") +
              "]\r\n";
          Socket socket = SSLSocketFactory.getDefault().createSocket("kinto.mozvoice.org", 443);
          final PrintWriter writer = new PrintWriter(socket.getOutputStream());
          writer.print("PATCH /v1/buckets/App/collections/Sentences_Meta_" + lang + "/records/" + id);
          writer.print(" HTTP/1.1\r\nUser-Agent: SentenceCollectorAndroidCustom\r\n");
          writer.print("Host: kinto.mozvoice.org\r\nAuthorization: " + auth + "\r\n");
          writer.print("Content-Type: application/json-patch+json\r\n");
          writer.print("Content-Length: " + payload.length() + "\r\n");
          writer.print("Connection: keep-alive\r\nAccept: */*\r\n\r\n");
          writer.print(payload);
          writer.flush();
          final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          String responseFirstLine = br.readLine();
          if (responseFirstLine == null || !responseFirstLine.startsWith("HTTP/1.1 20")) {
            Log.e("NET", responseFirstLine == null ? "null": responseFirstLine);
            while ((responseFirstLine = br.readLine()) != null) {
              Log.e("NET", responseFirstLine);
            }
            throw new RuntimeException("htjgsken");
          }
          br.close();
        } catch (IOException e) {
          throw new RuntimeException("jgafhn");
        }
      }
    }).start();
  }

  public interface RetrievedSentencesListener {
    void run(ArrayList<JSONObject> sentences);
  }
}
