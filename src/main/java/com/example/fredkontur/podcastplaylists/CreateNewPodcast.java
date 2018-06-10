/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity retrieves the information from the user for creating a new podcast.
 *              That information is then sent to the REST API as a POST request.
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateNewPodcast extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_podcast);

        // When the user clicks submit, create the string for the request body
        Button submitButton = (Button) findViewById(R.id.submitNewPodcast);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reqBodyStr = "{";
                String reqBodyStrEnd = "}";
                final MediaType JSON = MediaType.parse("application/json");

                final EditText title = (EditText) findViewById(R.id.podcastTitle);
                EditText genre = (EditText) findViewById(R.id.podcastGenre);
                EditText numSeasons = (EditText) findViewById(R.id.podcastNumSeasons);
                EditText numEpisodes = (EditText) findViewById(R.id.podcastNumEpisodes);
                EditText description = (EditText) findViewById(R.id.podcastDescription);
                RadioButton yesStillActive = (RadioButton)  findViewById(R.id.podcastYesStillActive);
                RadioButton noStillActive = (RadioButton) findViewById(R.id.podcastNoStillActive);

                String podcastTitle = title.getText().toString();
                String podcastGenre = genre.getText().toString();
                String podcastNumSeasons = numSeasons.getText().toString();
                String podcastNumEpisodes = numEpisodes.getText().toString();
                String podcastDescription = description.getText().toString();

                // The podcast title is a required field, so alert the user if no title was entered
                // and exit the onClickListener()
                if(TextUtils.isEmpty(podcastTitle)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(CreateNewPodcast.this);
                    dialog.setCancelable(true);
                    dialog.setTitle(getString(R.string.missing_required_field));
                    dialog.setMessage(getString(R.string.podcast_title_required));
                    dialog.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    final AlertDialog alert = dialog.create();
                    alert.show();

                    return;
                }
                // Add the name/value pair for each of the fields. If the user didn't enter a value
                // for a field, then the name/value pair is not created.
                else {
                    reqBodyStr += "\"title\" : \"" + podcastTitle + "\"";
                }

                if(!(TextUtils.isEmpty(podcastGenre))) {
                    reqBodyStr += ", \"genre\" : \"" + podcastGenre + "\"";
                }

                if(!(TextUtils.isEmpty(podcastNumSeasons))) {
                    reqBodyStr += ", \"numSeasons\" : " + podcastNumSeasons;
                }

                if(!(TextUtils.isEmpty(podcastNumEpisodes))) {
                    reqBodyStr += ", \"numEpisodes\" : " + podcastNumEpisodes;
                }

                if(!(TextUtils.isEmpty(podcastDescription))) {
                    reqBodyStr += ", \"description\" : \"" + podcastDescription + "\"";
                }

                if(yesStillActive.isChecked()) {
                    reqBodyStr += ", \"active\" : " + "true";
                }

                if(noStillActive.isChecked()) {
                    reqBodyStr += ", \"active\" : " + "false";
                }

                reqBodyStr += reqBodyStrEnd;

                // Create an OkHttpClient to send the POST request for creating a new podcast
                RequestBody reqBody = RequestBody.create(JSON, reqBodyStr);
                String reqUrl = "https://podcast-playlists.appspot.com/podcast";
                OkHttpClient mOkHttpClient;

                Request playlistReq = new Request.Builder()
                        .url(reqUrl)
                        .post(reqBody)
                        .build();

                mOkHttpClient = new OkHttpClient();
                mOkHttpClient.newCall(playlistReq).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    // When the response is returned, send the user to the SeePodcast activity to
                    // see the newly created podcast.
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
                        final int statusCode = response.code();
                        String r = response.body().string();
                        try {
                            final JSONObject podcast = new JSONObject(r);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // If the podcast was not created correctly, as indicated by
                                    // a status code that is not 201, then display an error message
                                    if (statusCode != 201) {
                                        TextView errorMsg = (TextView) findViewById(R.id.podcastCreateError);
                                        errorMsg.setVisibility(View.VISIBLE);
                                    }
                                    // If the podcast was created correctly, then send the user to
                                    // the SeePodcast activity
                                    else {
                                        try {
                                            String podcastID = podcast.getString("id");
                                            Intent intent = new Intent(CreateNewPodcast.this, SeePodcast.class);
                                            intent.putExtra("podcastID", podcastID);
                                            startActivity(intent);
                                        } catch(JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}
