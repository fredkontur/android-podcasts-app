/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This is a follow-up activity to the "CreateNewPlaylist" and "EditPlaylist"
 *              activities. It provides the user with a list of podcasts to choose from for
 *              inclusion in a playlist. After the user has chosen which podcasts to include, the
 *              activity when then make a POST or PATCH request to send the information about the
 *              playlist to the REST API database.
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddPodcasts extends AppCompatActivity {
    private boolean hasPodcasts = false;
    private JSONArray origPodcasts;
    private ArrayList<CheckBox> checkboxList = new ArrayList<>();
    private String playlistID;
    private String idToken;
    private String loggedIn;
    public static final String PREF = "StoredAccountInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_podcasts);

        // Determine if there is already a list of podcasts associated with the playlist
        if(getIntent().hasExtra("podcasts")) {
            hasPodcasts = true;
            try {
                origPodcasts = new JSONArray(getIntent().getStringExtra("podcasts"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // If the request is a PATCH request, then get the playlist ID. If the user is logged in,
        // then get the ID token.
        if(getIntent().getStringExtra("reqType").equals("patch")) {
            playlistID = getIntent().getStringExtra("playlistID");
            SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
            loggedIn = sharedPreferences.getString("loggedIn", null);
            if(loggedIn.equals("yes")) {
                idToken = sharedPreferences.getString("idToken", null);
            }
        }

        // Create a request URL and an OkHttpClient to get the list of podcasts from the REST API
        String reqUrl = "https://podcast-playlists.appspot.com/podcasts";
        OkHttpClient mOkHttpClient;
        Request podcastReq = new Request.Builder()
                .url(reqUrl)
                .build();

        mOkHttpClient = new OkHttpClient();
        mOkHttpClient.newCall(podcastReq).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            // Upon receiving a response for the GET request for the podcasts, put the podcasts in
            // a Checkbox list
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                String r = response.body().string();
                try {
                    final JSONArray jarr = new JSONArray(r);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LinearLayout parent = (LinearLayout) findViewById(R.id.parentLayoutAddPodcasts);
                            // Dynamically create the Checkbox list for the podcasts. I was able to
                            // find out how to do this here:
                            // https://stackoverflow.com/questions/21853537
                            for(int i = 0; i < jarr.length(); i++) {
                                CheckBox checkBox = new CheckBox(AddPodcasts.this);
                                try {
                                    JSONObject pod = jarr.getJSONObject(i);
                                    checkBox.setTag(pod.getString("id"));
                                    checkBox.setText(pod.getString("title"));
                                    parent.addView(checkBox);
                                    // If the playlist already had a list of podcasts, then check
                                    // the Checkbox for all of the podcasts already in the
                                    // playlist's list of podcasts
                                    if(hasPodcasts) {
                                        for (int j = 0; j < origPodcasts.length(); j++) {
                                            String origPodID = origPodcasts.getString(j);
                                            if(origPodID.equals(pod.getString("id"))) {
                                                checkBox.setChecked(true);
                                                break;
                                            }
                                        }
                                    }

                                } catch(JSONException e) {
                                    e.printStackTrace();
                                }
                                // Add the Checkbox to the ArrayList for easy reference later
                                checkboxList.add(checkBox);
                            }
                        }
                    });
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button submitButton = (Button) findViewById(R.id.submitPodcastsButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Build the request body for the POST or PATCH request
                String rbs;
                if(!(getIntent().getStringExtra("reqBodyStr").isEmpty())) {
                    rbs = getIntent().getStringExtra("reqBodyStr");
                    rbs = "{" + rbs + ", \"podcastList\" : [";
                }
                else {
                    rbs = "{\"podcastList\" : [";
                }
                // Android Studio suggested using a StringBuilder in the for loop for efficiency
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(rbs);
                boolean firstAdded = true;
                for(int i = 0; i < checkboxList.size(); i++) {
                    CheckBox currCheckBox = checkboxList.get(i);
                    if(currCheckBox.isChecked()) {
                        // If this is the first podcast added to the list, then there doesn't need
                        // to be a comma before it.
                        if (firstAdded) {
                            stringBuilder.append("\"");
                            stringBuilder.append(currCheckBox.getTag());
                            stringBuilder.append("\"");
                            firstAdded = false;
                        }
                        // If this is not the first podcast added to the list, then there should be
                        // a comma before it.
                        else {
                            stringBuilder.append(", \"");
                            stringBuilder.append(currCheckBox.getTag());
                            stringBuilder.append("\"");
                        }
                    }
                }
                stringBuilder.append("]}");
                String reqBodyStr = stringBuilder.toString();

                // Prepare the POST or PATCH request
                MediaType JSON = MediaType.parse("application/json");
                RequestBody reqBody = RequestBody.create(JSON, reqBodyStr);
                OkHttpClient mOkHttpClient;
                final String reqType = getIntent().getStringExtra("reqType");
                Request playlistReq;

                if(reqType.equals("post")) {
                    String reqUrl = "https://podcast-playlists.appspot.com/playlist";
                    playlistReq = new Request.Builder()
                            .url(reqUrl)
                            .post(reqBody)
                            .build();
                }

                else {
                    String reqUrl = "https://podcast-playlists.appspot.com/playlists/" + playlistID;
                    // If it is a PATCH request and the user is logged in, then pass the ID token
                    // in the header
                    if(loggedIn.equals("yes")) {
                        playlistReq = new Request.Builder()
                                .url(reqUrl)
                                .addHeader("Authorization", idToken)
                                .patch(reqBody)
                                .build();
                    }
                    // If the user is not logged in, then there is no ID token to pass
                    else {
                        playlistReq = new Request.Builder()
                                .url(reqUrl)
                                .patch(reqBody)
                                .build();
                    }
                }

                mOkHttpClient = new OkHttpClient();
                mOkHttpClient.newCall(playlistReq).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    // Upon receiving a response to the POST/PATCH request, send the user to the
                    // "SeePlaylist" activity to see the newly created/edited playlist
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
                        final int statusCode = response.code();
                        // If it was a POST request, then get the playlist ID from the response body
                        if(reqType.equals("post")) {
                            String r = response.body().string();
                            try {
                                JSONObject playlist = new JSONObject(r);
                                playlistID = playlist.getString("id");
                            } catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Check the status code to see if the playlist was successfully
                                // created or edited. If it was not successful, then display an
                                // error message.
                                if (!(statusCode == 201 || statusCode == 204)) {
                                    TextView errorMsg = (TextView) findViewById(R.id.playlistCreateError);
                                    errorMsg.setVisibility(View.VISIBLE);
                                    errorMsg.setText(R.string.playlist_create_error);
                                } else {
                                    Intent intent = new Intent(AddPodcasts.this, SeePlaylist.class);
                                    intent.putExtra("playlistID", playlistID);
                                    intent.putExtra("canEdit", "yes");
                                    startActivity(intent);
                                }
                            }
                        });
                    }
                });
            }
        });

    }
}