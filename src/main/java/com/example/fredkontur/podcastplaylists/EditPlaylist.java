/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 26, 2017
 * Description: This activity allows the user to enter information for editing a playlist. It has
 *              fields for the non-podcast information of a playlist, and then it passes on the
 *              updates to the AddPodcasts activity, which then sends the PATCH request to the
 *              REST API database.
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

public class EditPlaylist extends AppCompatActivity {
    public static final String PREF = "StoredAccountInfo";
    String origName = "";
    String origDescription = "";
    String origOwner = "";
    String origAccess = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_playlist);

        final SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
        final String loggedIn = sharedPreferences.getString("loggedIn", null);
        final TextView accessLabel = (TextView) findViewById(R.id.editPlaylistAccessLabel);
        final EditText name = (EditText) findViewById(R.id.editPlaylistName);
        final EditText description = (EditText) findViewById(R.id.editPlaylistDescription);
        final EditText owner = (EditText) findViewById(R.id.editPlaylistOwner);
        final RadioButton onlyMe = (RadioButton) findViewById(R.id.editPlaylistOnlyMe);
        final RadioButton anyone = (RadioButton) findViewById(R.id.editPlaylistAnyone);
        final TextView ownerLabel = (TextView) findViewById(R.id.editPlaylistOwnerLabel);
        final Button submitButton = (Button) findViewById(R.id.editPlaylistSubmit);

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
                final String r = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final JSONArray playlists = new JSONArray(r);
                            // Retrieve the chosen playlist from getIntent() and then retrieve the
                            // podcastList from the playlist
                            final JSONObject playlist = new JSONObject(getIntent().getStringExtra("playlist"));
                            final JSONArray podcastList = playlist.getJSONArray("podcastList");

                            // If the user is not logged in, then do not display the options to change the
                            // setting on who can edit the playlist. When the user is not logged in and
                            // he/she is editing a playlist, then the default setting of "Anyone" can edit
                            // applies.
                            if (loggedIn.equals("no") || playlist.isNull("ownerID")) {
                                accessLabel.setVisibility(View.GONE);
                                onlyMe.setVisibility(View.GONE);
                                anyone.setVisibility(View.GONE);
                            }

                            // If the user is logged in and selects that "Only Me" can edit the playlist, then
                            // make the owner name field visible.
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
                                // If the user is logged in but selects that "Anyone" can edit the playlist, then
                                // hide the owner name field since it does not apply.
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

                            // Populate all of the fields with the already-existing values they have for the
                            // playlist
                            name.setText(playlist.getString("name"));
                            origName = playlist.getString("name");

                            if (!(playlist.isNull("description"))) {
                                description.setText(playlist.getString("description"));
                                origDescription = playlist.getString("description");
                            }

                            if (!(playlist.isNull("ownerID"))) {
                                onlyMe.setChecked(true);
                                owner.setText(playlist.getString("ownerName"));
                                origOwner = playlist.getString("ownerName");
                                origAccess = "Only Me";
                            } else {
                                anyone.setChecked(true);
                                origAccess = "Anyone";
                            }

                            // When the submit button is clicked, checked for any values that have been
                            // changed. For the changed values, add them to a request body string that will
                            // be sent to the AddPodcasts activity. Since this string will be added to in t
                            // AddPodcasts, it is notproper JSON, since it is missing the enclosing brackets
                            // "{}".
                            submitButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String reqBodyStr = "";
                                    int counter = 0;

                                    String playlistName = name.getText().toString();
                                    String playlistDescription = description.getText().toString();
                                    String playlistOwner = owner.getText().toString();

                                    // The name field is mandatory for playlists. If it was left empty,
                                    // alert the user and exit the onClickListener()
                                    if (TextUtils.isEmpty(playlistName)) {
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(EditPlaylist.this);
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
                                    // Check to see if the playlist name has already been taken by another
                                    // playlist. If it has, then open an alert.
                                    else if (isNameTaken(playlistName, playlists) && !(origName.equals(playlistName))) {
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(EditPlaylist.this);
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
                                    } else {
                                        if (!(origName.equals(playlistName))) {
                                            reqBodyStr += "\"name\" : \"" + playlistName + "\"";
                                            counter++;
                                        }
                                    }

                                    if (!(TextUtils.isEmpty(playlistDescription))) {
                                        if (!(origDescription.equals(playlistDescription))) {
                                            if (counter > 0) {
                                                reqBodyStr += ", ";
                                            }
                                            reqBodyStr += "\"description\" : \"" + playlistDescription + "\"";
                                            counter++;
                                        }
                                    } else if (!(TextUtils.isEmpty(origDescription))) {
                                        if (counter > 0) {
                                            reqBodyStr += ", ";
                                        }
                                        reqBodyStr += "\"description\" : null";
                                        counter++;
                                    }

                                    if (!(TextUtils.isEmpty(playlistOwner))) {
                                        if (!(origOwner.equals(playlistOwner))) {
                                            if (counter > 0) {
                                                reqBodyStr += ", ";
                                            }
                                            reqBodyStr += "\"ownerName\" : \"" + playlistOwner + "\"";
                                            counter++;
                                        }
                                    } else if (!(TextUtils.isEmpty(origOwner))) {
                                        if (counter > 0) {
                                            reqBodyStr += ", ";
                                        }
                                        reqBodyStr += "\"ownerName\" : null";
                                        counter++;
                                    }

                                    if (onlyMe.isChecked()) {
                                        if (origAccess.equals("Anyone")) {
                                            if (counter > 0) {
                                                reqBodyStr += ", ";
                                            }
                                            reqBodyStr += "\"ownerID\" : \"" + sharedPreferences.getString("userID", null) + "\"";
                                            counter++;
                                        }
                                    } else {
                                        if (origAccess.equals("Only Me")) {
                                            if (counter > 0) {
                                                reqBodyStr += ", ";
                                            }
                                            reqBodyStr += "\"ownerName\" : null, \"ownerID\" : null";
                                            counter++;
                                        }
                                    }

                                    String playlistID = getIntent().getStringExtra("playlistID");

                                    // Send the request body string, podcast list, reqType = "POST", and the
                                    // playlistID to the AddPodcasts activity
                                    Intent intent = new Intent(EditPlaylist.this, AddPodcasts.class);
                                    intent.putExtra("reqBodyStr", reqBodyStr);
                                    intent.putExtra("podcasts", podcastList.toString());
                                    intent.putExtra("reqType", "patch");
                                    intent.putExtra("playlistID", playlistID);
                                    startActivity(intent);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    // This method checks to see if a playlist name corresponds to one that is already being used
    protected boolean isNameTaken(String name, JSONArray playlists) {
        for (int i = 0; i < playlists.length(); i++) {
            try {
                JSONObject pl = playlists.getJSONObject(i);
                if (name.equals(pl.getString("name"))) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
