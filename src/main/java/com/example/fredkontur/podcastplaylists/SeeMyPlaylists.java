/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity lists all playlists owned by the signed-in user by name along with a
 *              button to view/edit/delete the playlists
 * Sources: In order to create the ListView with event handlers and the ViewHolder pattern, which is
 *          more efficient than typical ArrayAdapters, I followed the guidance on this website:
 *          https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class SeeMyPlaylists extends AppCompatActivity {
    public static final String PREF = "StoredAccountInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_my_playlists);
        
        ListView playlistList = (ListView) findViewById(R.id.myPlaylistList);
        ArrayList<JSONObject> playlists = new ArrayList<JSONObject>();
        final SeeMyPlaylists.PlaylistAdapter playlistAdapter = new SeeMyPlaylists.PlaylistAdapter(this, playlists);
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
                    SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
                    final String userID = sharedPreferences.getString("userID", null);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Iterate through the list of playlists. For each of the playlists in
                            // which the owner ID matches the logged-in user's Google account ID,
                            // add the playlist to the playlistAdapter
                            for(int i = 0; i < j.length(); i++) {
                                try {
                                    JSONObject pl = j.getJSONObject(i);
                                    if(userID.equals(pl.getString("ownerID"))) {
                                        playlistAdapter.add(pl);
                                    }
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
    // allow the user to "View/Edit/Delete" the playlist.
    public class PlaylistAdapter extends ArrayAdapter<JSONObject> {
        // View lookup cache
        private class ViewHolder {
            TextView plName;
            Button accessPlaylist;
        }

        private PlaylistAdapter(Context context, ArrayList<JSONObject> playlists) {
            super(context, R.layout.item_playlist, playlists);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            final JSONObject playlist = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            final SeeMyPlaylists.PlaylistAdapter.ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                // If there's no view to re-use, inflate a brand new view for row
                viewHolder = new SeeMyPlaylists.PlaylistAdapter.ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_playlist, parent, false);
                viewHolder.plName = (TextView) convertView.findViewById(R.id.plName);
                viewHolder.accessPlaylist = (Button) convertView.findViewById(R.id.accessPlaylist);
                // Cache the viewHolder object inside the fresh view
                convertView.setTag(viewHolder);
            } else {
                // View is being recycled, retrieve the viewHolder object from tag
                viewHolder = (SeeMyPlaylists.PlaylistAdapter.ViewHolder) convertView.getTag();
            }
            // Put the playlist name in the ViewHolder
            try {
                String playlistName = playlist.getString("name");
                viewHolder.plName.setText(playlistName);
            } catch(JSONException e) {
                e.printStackTrace();
            }
            SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
            viewHolder.accessPlaylist.setText(R.string.view_edit_delete);
            // Attach the click event handler for the "View/Edit/Delete" button. This handler will
            // send the user to the SeePlaylist activity
            viewHolder.accessPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(SeeMyPlaylists.this, SeePlaylist.class);
                    intent.putExtra("canEdit", "yes");
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