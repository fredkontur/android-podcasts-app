/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 26, 2017
 * Description: This activity is for a user to enter an endorsement for a playlist.
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EnterEndorsement extends AppCompatActivity {
    public static final String PREF = "StoredAccountInfo";
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_endorsement);

        Button submitButton = (Button) findViewById(R.id.endorsementSubmitButton);

        // When the user clicks submit, send the endorsement as a PATCH request to the REST API and
        // then send the user to the SeePlaylist activity to see the playlist with the new
        // endorsement
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText newEndorsement = (EditText)  findViewById(R.id.newEndorsement);
                String ne = newEndorsement.getText().toString();
                final String playlistID = getIntent().getStringExtra("playlistID");
                String postNewEndorsement = "{\"newEndorsement\" : \"" + ne + "\"}";
                final MediaType JSON = MediaType.parse("application/json");

                // If the endorsement is blank, return the user to the SeePlaylist activity
                if(ne.isEmpty()) {
                    Intent intent = new Intent(EnterEndorsement.this, SeePlaylist.class);
                    intent.putExtra("canEdit", getIntent().getStringExtra("canEdit"));
                    intent.putExtra("playlistID", playlistID);
                    startActivity(intent);
                }
                else {
                    // Create the OkHttpClient to send the PATCH request to the REST API to add the new
                    // endorsement
                    RequestBody reqBody = RequestBody.create(JSON, postNewEndorsement);
                    newEndorsement.setText("");
                    String reqUrl = "https://podcast-playlists.appspot.com/playlists/" + playlistID;
                    OkHttpClient mOkHttpClient;

                    Request playlistReq = new Request.Builder()
                            .url(reqUrl)
                            .patch(reqBody)
                            .build();

                    mOkHttpClient = new OkHttpClient();
                    mOkHttpClient.newCall(playlistReq).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response)
                                throws IOException {
                            final int statusCode = response.code();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // If there was a problem creating the endorsement as indicated by
                                    // the status code not equaling 204, then notify the user with a
                                    // message
                                    if (statusCode != 204) {
                                        newEndorsement.setText(getString(R.string.endorsement_problem));
                                    }
                                    // If the endorsement was successfully created, then send the user
                                    // to the SeePlaylist activity to see the playlist with the new
                                    // endorsement
                                    else {
                                        sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
                                        // Indicate that this user has already endorsed the playlist to
                                        // prevent multiple endorsements of the same playlist by the
                                        // same user.
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString(playlistID, "endorsed");
                                        editor.apply();
                                        Intent intent = new Intent(EnterEndorsement.this, SeePlaylist.class);
                                        intent.putExtra("canEdit", getIntent().getStringExtra("canEdit"));
                                        intent.putExtra("playlistID", playlistID);
                                        startActivity(intent);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}
