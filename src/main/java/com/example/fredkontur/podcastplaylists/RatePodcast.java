/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity is for a user to rate a podcast.
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RatePodcast extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_podcast);
        Button submitButton = (Button) findViewById(R.id.commentSubmitButton);

        // When the user clicks submit, send the rating to the REST API as a PATCH request and
        // return the user to the SeePodcast activity to see the updated rating for the podcast
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // There is nothing in the REST API dictating the range of values for the rating,
                // but, in this app, the user is given a range from 1-5 to choose from, with 5
                // being the best rating and 1 being the worst.
                final RadioButton one = (RadioButton)  findViewById(R.id.rating1);
                final RadioButton two = (RadioButton)  findViewById(R.id.rating2);
                final RadioButton three = (RadioButton)  findViewById(R.id.rating3);
                final RadioButton four = (RadioButton)  findViewById(R.id.rating4);
                final RadioButton five = (RadioButton)  findViewById(R.id.rating5);
                final String podcastID = getIntent().getStringExtra("podcastID");
                String postRating = "{\"newRating\" : ";
                final MediaType JSON = MediaType.parse("application/json");

                // Determine which rating the user chose. If the user did not choose a rating, then
                // just return to the SeePodcast activity
                if(one.isChecked()) {
                    postRating += "1}";
                }
                else if(two.isChecked()) {
                    postRating += "2}";
                }
                else if(three.isChecked()) {
                    postRating += "3}";
                }
                else if(four.isChecked()) {
                    postRating += "4}";
                }
                else if(five.isChecked()) {
                    postRating += "5}";
                }
                else {
                    Intent intent = new Intent(RatePodcast.this, SeePodcast.class);
                    intent.putExtra("podcastID", podcastID);
                    startActivity(intent);
                }

                // Create the OkHttpClient to sent the PATCH request to the REST API
                RequestBody reqBody = RequestBody.create(JSON, postRating);
                String reqUrl = "https://podcast-playlists.appspot.com/podcasts/" + podcastID;
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
                            public void run(){
                                // If the rating was not successfully created, then notify the user
                                // with an error message
                                if(statusCode != 204) {
                                    TextView errMsg = (TextView) findViewById(R.id.ratingErrorLabel);
                                    errMsg.setVisibility(View.VISIBLE);
                                }
                                // If the rating was successfully created, then send the user to the
                                // SeePodcast activity
                                else {
                                    Intent intent = new Intent(RatePodcast.this, SeePodcast.class);
                                    intent.putExtra("podcastID", podcastID);
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
