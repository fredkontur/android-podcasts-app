/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 26, 2017
 * Description: This activity retrieves the information from the user for creating a new playlist.
 *              After getting the basic information, the activity sends the user to the AddPodcasts
 *              activity to choose podcasts for the playlist.
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreateNewPlaylist extends AppCompatActivity {
    public static final String PREF = "StoredAccountInfo";
    private boolean result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_playlist);

        SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
        final String loggedIn = sharedPreferences.getString("loggedIn", null);
        final String userID = sharedPreferences.getString("userID", null);
        final TextView accessLabel = (TextView) findViewById(R.id.createPlaylistAccessLabel);
        final TextView ownerLabel = (TextView) findViewById(R.id.createPlaylistOwnerLabel);
        final EditText name = (EditText) findViewById(R.id.createPlaylistName);
        final EditText description = (EditText) findViewById(R.id.createPlaylistDescription);
        final EditText owner = (EditText) findViewById(R.id.createPlaylistOwner);
        final RadioButton onlyMe = (RadioButton) findViewById(R.id.createPlaylistOnlyMe);
        final RadioButton anyone = (RadioButton) findViewById(R.id.createPlaylistAnyone);

        // If the user has not logged in, then they should not see the options for who can edit the
        // playlist. By default, all playlists created by users who are not logged in can be edited
        // by anyone.
        if (!(loggedIn.equals("yes"))) {
            accessLabel.setVisibility(View.GONE);
            onlyMe.setVisibility(View.GONE);
            anyone.setVisibility(View.GONE);
        }

        // If the user is logged in, then they should see the radio button option to choose whether
        // the playlist can only be edited by the user or whether it can be edited by anyone. If
        // the playlist can only be edited by the user, then the user must type in the displayed
        // owner name for the playlist. Regardless of the onwer name chosen, the playlist will be
        // linked to the user's Google account ID.
        else {
            onlyMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (onlyMe.isChecked()) {
                        ownerLabel.setVisibility(View.VISIBLE);
                        owner.setVisibility(View.VISIBLE);
                    }
                    if (anyone.isChecked()) {
                        ownerLabel.setVisibility(View.GONE);
                        owner.setVisibility(View.GONE);
                    }
                }
            });
            anyone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (onlyMe.isChecked()) {
                        ownerLabel.setVisibility(View.VISIBLE);
                        owner.setVisibility(View.VISIBLE);
                    }
                    if (anyone.isChecked()) {
                        ownerLabel.setVisibility(View.GONE);
                        owner.setVisibility(View.GONE);
                    }
                }
            });
        }

        // Use an OkHttpClient to execute a GET request to get a list of all of the
        // playlists to check the playlist names against the one chosen
        String reqUrl = "https://podcast-playlists.appspot.com/playlists";
        OkHttpClient mOkHttpClient;

        Request playlistReq = new Request.Builder()
                .url(reqUrl)
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
                // When the user clicks Submit, a request body string is created from the user's choices.
                // This string is sent to the AddPodcast activity for further additions, and so it is not
                // in complete JSON format. The enclosing brackets "{}" are not included with the string
                Button submitButton = (Button) findViewById(R.id.submitAndEnterPodcasts);
                submitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Use an OkHttpClient to execute a GET request to get a list of all of the
                        // playlists to check the playlist names
                        String reqUrl = "https://podcast-playlists.appspot.com/playlists";
                        OkHttpClient mOkHttpClient;

                        Request playlistReq = new Request.Builder()
                                .url(reqUrl)
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
                                final String r = response.body().string();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String nameStr = name.getText().toString();
                                        String descriptionStr = description.getText().toString();
                                        String reqBodyStr = "";
                                        try {
                                            JSONArray playlists = new JSONArray(r);
                                            // A name is required for the playlist, so alert the user is the name field is
                                            // empty and exit the onClickListener()
                                            if (TextUtils.isEmpty(nameStr)) {
                                                AlertDialog.Builder dialog = new AlertDialog.Builder(CreateNewPlaylist.this);
                                                dialog.setCancelable(true);
                                                dialog.setTitle(getString(R.string.missing_required_field));
                                                dialog.setMessage(getString(R.string.playlist_name_required));
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
                                            // Check to see if the playlist name chosen is already
                                            // being used by another playlist. If it is, prompt the
                                            // user to choose another name.
                                            else if (isNameTaken(nameStr, playlists)) {
                                                AlertDialog.Builder dialog = new AlertDialog.Builder(CreateNewPlaylist.this);
                                                dialog.setCancelable(true);
                                                dialog.setMessage(getString(R.string.name_already_taken));
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
                                            // Add each of the user-entered fields to the request body string. If the user did
                                            // not enter anything into a field, then no name/value pair is created for that
                                            // field.
                                            else {
                                                reqBodyStr = "\"name\" : \"" + nameStr + "\"";
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if (!(TextUtils.isEmpty(descriptionStr))) {
                                            reqBodyStr += ", \"description\" : \"" + descriptionStr + "\"";
                                        }

                                        if (loggedIn.equals("yes")) {
                                            if (onlyMe.isChecked()) {
                                                String ownerStr = owner.getText().toString();
                                                if (!(TextUtils.isEmpty(ownerStr))) {
                                                    reqBodyStr += ", \"ownerName\" : \"" + ownerStr + "\"";
                                                }
                                                reqBodyStr += ", \"ownerID\" : \"" + userID + "\"";

                                            }
                                        }
                                        // Send the user to the "AddPodcasts" activity
                                        Intent intent = new Intent(CreateNewPlaylist.this, AddPodcasts.class);
                                        intent.putExtra("reqBodyStr", reqBodyStr);
                                        intent.putExtra("reqType", "post");
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    // This method checks to see if a playlist name corresponds to one that is already being used
    protected boolean isNameTaken(String name, JSONArray playlists) {
        for(int i  = 0; i < playlists.length(); i++) {
            try {
                JSONObject pl = playlists.getJSONObject(i);
                if (name.equals(pl.getString("name"))) {
                    return true;
                }
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}