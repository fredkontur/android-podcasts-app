/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 26, 2017
 * Description: This activity is for a user to enter a comment on a playlist.
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Intent;
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

public class EnterComment extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_comment);

        Button submitButton = (Button) findViewById(R.id.commentSubmitButton);

        // When the user clicks submit, send the comment as a PATCH request to the REST API and
        // then send the user to the SeePlaylist activity to see the playlist with the new comment
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText newComment = (EditText)  findViewById(R.id.newComment);
                String nc = newComment.getText().toString();
                final String playlistID = getIntent().getStringExtra("playlistID");
                String postNewComment = "{\"newComment\" : \"" + nc + "\"}";
                final MediaType JSON = MediaType.parse("application/json");

                // If the comment is blank, return the user to the SeePlaylist activity
                if(nc.isEmpty()) {
                    Intent intent = new Intent(EnterComment.this, SeePlaylist.class);
                    intent.putExtra("canEdit", getIntent().getStringExtra("canEdit"));
                    intent.putExtra("playlistID", playlistID);
                    startActivity(intent);
                }
                else {
                    // Create the OkHttpClient to send the PATCH request to the REST API to add the new
                    // comment
                    RequestBody reqBody = RequestBody.create(JSON, postNewComment);
                    newComment.setText("");
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
                                    // If there was a problem creating the comment as indicated by the
                                    // status code not equaling 204, then notify the user with a
                                    // message
                                    if (statusCode != 204) {
                                        newComment.setText(getString(R.string.comment_problem));
                                    }
                                    // If the comment was successfully created, then send the user
                                    // to the SeePlaylist activity to see the playlist with the new
                                    // comment
                                    else {
                                        Intent intent = new Intent(EnterComment.this, SeePlaylist.class);
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
