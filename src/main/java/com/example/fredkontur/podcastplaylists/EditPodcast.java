/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity allows the user to enter information to edit a podcast. It
 *              then sends the updated information as a PATCH request to the REST API database.
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

public class EditPodcast extends AppCompatActivity {
    String origTitle = "";
    String origGenre = "";
    String origNumSeasons = "";
    String origNumEpisodes = "";
    String origDescription = "";
    String origActive = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_podcast);

        try {
            final JSONObject podcast = new JSONObject(getIntent().getStringExtra("podcast"));
            final EditText title = (EditText) findViewById(R.id.podcastTitle);
            final EditText genre = (EditText) findViewById(R.id.podcastGenre);
            final EditText numSeasons = (EditText) findViewById(R.id.podcastNumSeasons);
            final EditText numEpisodes = (EditText) findViewById(R.id.podcastNumEpisodes);
            final EditText description = (EditText) findViewById(R.id.podcastDescription);
            final RadioButton yesStillActive = (RadioButton) findViewById(R.id.podcastYesStillActive);
            final RadioButton noStillActive = (RadioButton) findViewById(R.id.podcastNoStillActive);
            Button submitButton = (Button) findViewById(R.id.submitNewPodcast);

            // Populate the fields with the values for the already-existing podcast
            title.setText(podcast.getString("title"));
            origTitle = podcast.getString("title");

            if(!(podcast.isNull("genre"))) {
                genre.setText(podcast.getString("genre"));
                origGenre = podcast.getString("genre");
            }

            if(!(podcast.isNull("numSeasons"))) {
                numSeasons.setText(String.valueOf(podcast.getInt("numSeasons")));
                origNumSeasons = String.valueOf(podcast.getInt("numSeasons"));
            }

            if(!(podcast.isNull("numEpisodes"))) {
                numEpisodes.setText(String.valueOf(podcast.getInt("numEpisodes")));
                origNumEpisodes = String.valueOf(podcast.getInt("numEpisodes"));
            }

            if(!(podcast.isNull("description"))) {
                description.setText(podcast.getString("description"));
                origDescription = podcast.getString("description");
            }

            if(!(podcast.isNull("active"))) {
                if(podcast.getBoolean("active")) {
                    yesStillActive.setChecked(true);
                    origActive = "Yes";
                }
                else {
                    noStillActive.setChecked(true);
                    origActive = "No";
                }
            }

            // When the user clicks submit, create the request for changing the podcast and send
            // the PATCH request to the REST API. After the podcast is edited, send the user to
            // the SeePodcast activity to see the updated podcast.
            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String reqBodyStr = "{";
                    String reqBodyStrEnd = "}";
                    final MediaType JSON = MediaType.parse("application/json");
                    int counter = 0;

                    String podcastTitle = title.getText().toString();
                    String podcastGenre = genre.getText().toString();
                    String podcastNumSeasons = numSeasons.getText().toString();
                    String podcastNumEpisodes = numEpisodes.getText().toString();
                    String podcastDescription = description.getText().toString();

                    // When the submit button is clicked, build the request body string using the
                    // key/value pairs for the updated podcast data. Key/value pairs are only
                    // created for the data that the user changed.

                    // The podcast title is a required field, so alert the user if it has been left
                    // blank and exit the onClickListener()
                    if (TextUtils.isEmpty(podcastTitle)) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(EditPodcast.this);
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
                    } else {
                        if(!(origTitle.equals(podcastTitle))) {
                            reqBodyStr += "\"title\" : \"" + podcastTitle + "\"";
                            counter++;
                        }
                    }

                    if (!(TextUtils.isEmpty(podcastGenre))) {
                        if(!(origGenre.equals(podcastGenre))) {
                            if(counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"genre\" : \"" + podcastGenre + "\"";
                            counter++;
                        }
                    }
                    else if(!(TextUtils.isEmpty(origGenre))) {
                        if(counter > 0) {
                            reqBodyStr += ", ";
                        }
                        reqBodyStr += "\"genre\" : null";
                        counter++;
                    }

                    if (!(TextUtils.isEmpty(podcastNumSeasons))) {
                        if(!(origNumSeasons.equals(podcastNumSeasons))) {
                            if(counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"numSeasons\" : " + podcastNumSeasons;
                            counter++;
                        }
                    }
                    else if(!(TextUtils.isEmpty(origNumSeasons))) {
                        if(counter > 0) {
                            reqBodyStr += ", ";
                        }
                        reqBodyStr += "\"numSeasons\" : null";
                        counter++;
                    }

                    if (!(TextUtils.isEmpty(podcastNumEpisodes))) {
                        if(!(origNumEpisodes.equals(podcastNumEpisodes))) {
                            if(counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"numEpisodes\" : " + podcastNumEpisodes;
                            counter++;
                        }
                    }
                    else if(!(TextUtils.isEmpty(origNumEpisodes))) {
                        if(counter > 0) {
                            reqBodyStr += ", ";
                        }
                        reqBodyStr += "\"numEpisodes\" : null";
                        counter++;
                    }

                    if (!(TextUtils.isEmpty(podcastDescription))) {
                        if(!(origDescription.equals(podcastDescription))) {
                            if(counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"description\" : \"" + podcastDescription + "\"";
                            counter++;
                        }
                    }
                    else if(!(TextUtils.isEmpty(origDescription))) {
                        if(counter > 0) {
                            reqBodyStr += ", ";
                        }
                        reqBodyStr += "\"description\" : null";
                        counter++;
                    }

                    if (yesStillActive.isChecked()) {
                        if(origActive.equals("No") || TextUtils.isEmpty(origActive)) {
                            if(counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"active\" : " + "true";
                            counter++;
                        }
                    }
                    else if (noStillActive.isChecked()) {
                        if(origActive.equals("Yes") || TextUtils.isEmpty(origActive)) {
                            if (counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"active\" : " + "false";
                            counter++;
                        }
                    }
                    else {
                        if(!(TextUtils.isEmpty(origActive))) {
                            if(counter > 0) {
                                reqBodyStr += ", ";
                            }
                            reqBodyStr += "\"active\" : null";
                            counter++;
                        }
                    }

                    reqBodyStr += reqBodyStrEnd;

                    // Using the request body string, an OkHttpClient is created to send the PATCH
                    // request to the REST API to update the podcast
                    RequestBody reqBody = RequestBody.create(JSON, reqBodyStr);
                        final String podcastID = getIntent().getStringExtra("podcastID");
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
                                public void run() {
                                    // If the podcast was not successfully edited, as indicated by
                                    // a status code not equal to 204, then display an error
                                    // message.
                                    if (statusCode != 204) {
                                        TextView errorMsg = (TextView) findViewById(R.id.podcastCreateError);
                                        errorMsg.setVisibility(View.VISIBLE);
                                    }
                                    // If the podcast was successfully edited, then send the user to
                                    // the SeePodcast activity to see the updated podcast
                                    else {
                                        Intent intent = new Intent(EditPodcast.this, SeePodcast.class);
                                        intent.putExtra("podcastID", podcastID);
                                        startActivity(intent);
                                    }
                                }
                                });
                        }
                    });
                }
            });
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }
}
