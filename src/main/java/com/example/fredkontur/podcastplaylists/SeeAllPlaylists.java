/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 26, 2017
 * Description: This activity lists all playlists by name along with a button to either view or
 *              view/edit/delete the playlist. The button displayed depends on whether a user has
 *              access to edit or delete a playlist.
 * Sources: In order to create the ListView with event handlers and the ViewHolder pattern, which is
 *          more efficient than typical Array Adapters, I followed the guidance on this website:
 *          https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SeeAllPlaylists extends AppCompatActivity {
    public static final String PREF = "StoredAccountInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_all_playlists);

        ListView playlistList = (ListView) findViewById(R.id.playlistList);
        ArrayList<JSONObject> playlists = new ArrayList<JSONObject>();
        final PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, playlists);
        playlistList.setAdapter(playlistAdapter);

        // Use an OkHttpClient to execute a GET request to get a list of all of the playlists
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
                String r = response.body().string();
                try {
                    final JSONArray j = new JSONArray(r);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Iterate through the list of playlists and add each of them to the
                            // playlistAdapter
                            for(int i = 0; i < j.length(); i++) {
                                try {
                                    JSONObject pl = j.getJSONObject(i);
                                    playlistAdapter.add(pl);
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

    // The PlaylistAdapter displays a list with the names of the playlists as well as buttons which
    // allow the user to either "View" the playlist or "View/Edit/Delete" the playlist, depending on
    // what access the user has to edit and delete the playlist
    public class PlaylistAdapter extends ArrayAdapter<JSONObject> {
        // View lookup cache
        private class ViewHolder {
            TextView plName;
            Button accessPlaylist;
        }

        // PlaylistAdapter constructor
        private PlaylistAdapter(Context context, ArrayList<JSONObject> playlists) {
            super(context, R.layout.item_playlist, playlists);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            final JSONObject playlist = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            final ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                // If there's no view to re-use, inflate a brand new view for row
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_playlist, parent, false);
                viewHolder.plName = (TextView) convertView.findViewById(R.id.plName);
                viewHolder.accessPlaylist = (Button) convertView.findViewById(R.id.accessPlaylist);
                // Cache the viewHolder object inside the fresh view
                convertView.setTag(viewHolder);
            } else {
                // View is being recycled, retrieve the viewHolder object from tag
                viewHolder = (ViewHolder) convertView.getTag();
            }
            // Put the playlist name in the ViewHolder
            try {
                String playlistName = playlist.getString("name");
                viewHolder.plName.setText(playlistName);
            } catch(JSONException e) {
                e.printStackTrace();
            }
            // Put the button in the ViewHolder. The conditionals below determine whether the button
            // should say "View/Edit/Delete" or "View" based on the user's access and the owner
            // setting of the playlist
            SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
            String loggedIn = sharedPreferences.getString("loggedIn", null);
            if(loggedIn.equals("yes")) {
                String userID = sharedPreferences.getString("userID", null);
                // If the user is logged in and his/her Google ID matches the owner ID of the
                // playlist, then have the "View/Edit/Delete" button appear
                if(!(playlist.isNull("ownerID"))) {
                    try {
                        if (userID.equals(playlist.getString("ownerID"))) {
                            viewHolder.accessPlaylist.setText(R.string.view_edit_delete);
                        }
                        else {
                            viewHolder.accessPlaylist.setText(R.string.view);
                        }
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
                // If the user is logged in and the playlist does not have an owner ID, then have
                // the "View/Edit/Delete" button appear
                else {
                    viewHolder.accessPlaylist.setText(R.string.view_edit_delete);
                }
            }
            // If the user is not logged in that the playlist does not have an owner ID, then have
            // the "View/Edit/Delete" button appear
            else if(playlist.isNull("ownerID")) {
                        viewHolder.accessPlaylist.setText(R.string.view_edit_delete);
            }
            else {
                viewHolder.accessPlaylist.setText(R.string.view);
            }
            // Attach the click event handler. This button will take the user to the SeePlaylist
            // activity. The handler also passes on whether the playlist can be edited as well as
            // the playlistID.
            viewHolder.accessPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(SeeAllPlaylists.this, SeePlaylist.class);
                    if(viewHolder.accessPlaylist.getText().toString().equals(getString(R.string.view_edit_delete))) {
                        intent.putExtra("canEdit", "yes");
                    }
                    else {
                        intent.putExtra("canEdit", "no");
                    }
                    try {
                        intent.putExtra("playlistID", playlist.getString("id"));
                        startActivity(intent);
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            // Return the completed view to render on screen
            return convertView;
        }
    }
}
