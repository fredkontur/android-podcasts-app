/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 26, 2017
 * Description: This activity provides the values of the fields for the chosen playlist and
 *              provides the user with buttons to return to the home page or, if applicable, to
 *              edit or delete the playlist
 * Sources: To create a scrollable layout that is populated with multiple lists, I used the second
 *          answer given in this reference: https://stackoverflow.com/questions/17693578
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
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


public class SeePlaylist extends AppCompatActivity {
    public static final String PREF = "StoredAccountInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_playlist);

        // Set the click event listener for the "HOME PAGE" button to return the user to the
        // MainActivity activity
        Button playlistReturnToHome = (Button) findViewById(R.id.playlistReturnToHome);
        playlistReturnToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SeePlaylist.this, MainActivity.class);
                startActivity(intent);
            }
        });


        final SharedPreferences sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
        final String loggedIn = sharedPreferences.getString("loggedIn", null);
        final String playlistID = getIntent().getStringExtra("playlistID");
        final String reqUrl = "https://podcast-playlists.appspot.com/playlists/" + playlistID;
        OkHttpClient mOkHttpClient;

        // Create an OkHttpClient to send a GET request for the playlist passed to this activity
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
                            final JSONObject playlist = new JSONObject(r);
                            TextView plName = (TextView) findViewById(R.id.playlistName);
                            TextView plOwner = (TextView) findViewById(R.id.playlistOwner);
                            TextView plDesc = (TextView) findViewById(R.id.playlistDescription);
                            final ListView podcastList = (ListView) findViewById(R.id.playlistPodcasts);
                            ListView endorsementList = (ListView) findViewById(R.id.playlistEndorsements);
                            ListView commentList = (ListView) findViewById(R.id.playlistComments);
                            Button addComment = (Button) findViewById(R.id.addComment);
                            Button addEndorsement = (Button) findViewById(R.id.addEndorsement);
                            Button editPlaylist = (Button) findViewById(R.id.editPlaylist);
                            Button deletePlaylist = (Button) findViewById(R.id.deletePlaylist);
                            ArrayList<String> podcasts = new ArrayList<>();
                            ArrayList<String> endorsements = new ArrayList<>();
                            ArrayList<String> comments = new ArrayList<>();
                            final ArrayAdapter<String> podcastAdapter = new ArrayAdapter<>(SeePlaylist.this, android.R.layout.simple_list_item_1, podcasts);
                            ArrayAdapter<String> endorsementAdapter = new ArrayAdapter<>(SeePlaylist.this, android.R.layout.simple_list_item_1, endorsements);
                            final ArrayAdapter<String> commentAdapter = new ArrayAdapter<>(SeePlaylist.this, android.R.layout.simple_list_item_1, comments);
                            podcastList.setAdapter(podcastAdapter);
                            endorsementList.setAdapter(endorsementAdapter);
                            commentList.setAdapter(commentAdapter);

                            // By default, set the visibility of the "ADD ENDORSEMENT" button to be
                            // GONE.
                            addEndorsement.setVisibility(View.GONE);

                            // If the user can edit the playlist, then make the "EDIT PLAYLIST" and
                            // "DELETE PLAYLIST" buttons visible
                            if(getIntent().getStringExtra("canEdit").equals("yes")) {
                                editPlaylist.setVisibility(View.VISIBLE);
                                deletePlaylist.setVisibility(View.VISIBLE);

                                // Set the click event handler of the "EDIT PLAYLIST" button to take
                                // the user to the EditPlaylist activity
                                editPlaylist.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(SeePlaylist.this, EditPlaylist.class);
                                        intent.putExtra("playlist", playlist.toString());
                                        intent.putExtra("playlistID", playlistID);
                                        startActivity(intent);
                                    }
                                });

                                // Set the click event handler of the "DELETE PLAYLIST" to make a
                                // DELETE request to the REST API
                                deletePlaylist.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Send an alert to the user to confirm that he/she wants to
                                        // delete the playlist. If not, exit the event handler. If
                                        // the user confirms the deletion, then continue with the
                                        // DELETE request to the REST API
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(SeePlaylist.this);
                                        dialog.setCancelable(true);
                                        dialog.setMessage(getString(R.string.confirm_delete_playlist));
                                        dialog.setPositiveButton(getString(R.string.yes_delete), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // Construct a DELETE request for the REST API
                                                OkHttpClient mOkHttpClientDel;
                                                Request playlistDelReq;

                                                // If the user is logged in, then send the ID token
                                                // with the DELETE request
                                                if(loggedIn.equals("yes")) {
                                                    String idToken = sharedPreferences.getString("idToken", null);
                                                    playlistDelReq = new Request.Builder()
                                                            .url(reqUrl)
                                                            .addHeader("Authorization", idToken)
                                                            .delete()
                                                            .build();
                                                }
                                                // If the user is not logged in, then do not send
                                                // and ID token with the DELETE request
                                                else {
                                                    playlistDelReq = new Request.Builder()
                                                            .url(reqUrl)
                                                            .delete()
                                                            .build();
                                                }

                                                mOkHttpClientDel = new OkHttpClient();
                                                mOkHttpClientDel.newCall(playlistDelReq).enqueue(new Callback() {
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
                                                                // Construct a message for the user.
                                                                // If the response status code was
                                                                // 204, then inform the user the
                                                                // deletion was successful.
                                                                // Otherwise, inform the user
                                                                // the deletion was not successful.
                                                                String delMsg;
                                                                if (statusCode != 204) {
                                                                    delMsg = getString(R.string.del_msg_negative_playlist);
                                                                } else {
                                                                    delMsg = getString(R.string.del_msg_positive_playlist);
                                                                }
                                                                // Send the user to the DeleteResult
                                                                // activity to see the result of
                                                                // the DELETE request
                                                                Intent intent = new Intent(SeePlaylist.this, DeleteResult.class);
                                                                intent.putExtra("delMsg", delMsg);
                                                                startActivity(intent);
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                        // If the user does not want to delete the playlist, then
                                        // exit the event handler
                                        dialog.setNegativeButton(getString(R.string.no_delete), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });
                                        final AlertDialog alert = dialog.create();
                                        alert.show();
                                    }
                                });
                            }

                            // Fill in the information in the playlist fields
                            plName.setText(playlist.getString("name"));

                            String owner = playlist.getString("ownerName");
                            if((owner.equals("null"))) {
                                plOwner.setText("");
                            }
                            else {
                                plOwner.setText(owner);
                            }

                            String desc = playlist.getString("description");
                            if((desc.equals("null"))) {
                                plDesc.setText("");
                            }
                            else {
                                plDesc.setText(desc);
                            }

                            // Create the endorsements and comments lists using the
                            // ListUtils.setDynamicHeight() helper function
                            JSONArray e = playlist.getJSONArray("endorsements");
                            for (int i = 0; i < e.length(); i++) {
                                endorsementAdapter.add(e.getString(i));
                            }
                            ListUtils.setDynamicHeight(endorsementList);

                            JSONArray c = playlist.getJSONArray("comments");
                            for (int i = 0; i < c.length(); i++) {
                                commentAdapter.add(c.getString(i));
                            }
                            ListUtils.setDynamicHeight(commentList);

                            // Create an OkHttpClient to construct a GET request for the list of all
                            // podcasts
                            String reqUrlPlaylists = "https://podcast-playlists.appspot.com/podcasts";
                            OkHttpClient mOkHttpClientPodcasts;

                            Request playlistReq = new Request.Builder()
                                    .url(reqUrlPlaylists)
                                    .build();

                            mOkHttpClientPodcasts = new OkHttpClient();
                            mOkHttpClientPodcasts.newCall(playlistReq).enqueue(new Callback() {
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
                                                // Get the podcast names from the list of all
                                                // podcasts for the podcasts in the playlist
                                                final JSONArray allPodcasts = new JSONArray(r);
                                                String currPodcastID;
                                                JSONArray p = playlist.getJSONArray("podcastList");

                                                // Find out which podcasts in the list of all
                                                // podcasts have IDs matching those in the podcast
                                                // list for this playlist, and then get the names
                                                // of those podcasts for display in the list
                                                for (int i = 0; i < p.length(); i++) {
                                                    currPodcastID = p.getString(i);
                                                    for (int j = 0; j < allPodcasts.length(); j++) {
                                                        JSONObject podcast = allPodcasts.getJSONObject(j);
                                                        if (currPodcastID.equals(podcast.getString("id"))) {
                                                            podcastAdapter.add(podcast.getString("title"));
                                                            break;
                                                        }
                                                    }
                                                }
                                                // Use the ListUtils.setDynamicHeight() helper
                                                // function to display the list of podcast names
                                                ListUtils.setDynamicHeight(podcastList);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            });

                            // Only logged in users can add an endorsement. If the user is logged
                            // in, and has not previously added an endorsement for this playlist,
                            // then display the "ADD ENDORSEMENT" button. Checking the playlistID
                            // key in the sharedPreferences determines if this playlist was already
                            // endorsed by the user. It will have a value of "endorsed" if it has
                            // been endorsed, but the key will not exits if it has not been
                            // endorsed, so simply checking to see if the key exists is sufficient.
                            if(loggedIn.equals("yes")) {
                                if(!(sharedPreferences.contains(playlistID))) {
                                    addEndorsement.setVisibility(View.VISIBLE);
                                    // Set the click event listener to send the user to the
                                    // EnterEndorsement activity
                                    addEndorsement.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent(SeePlaylist.this, EnterEndorsement.class);
                                            intent.putExtra("playlistID", playlistID);
                                            intent.putExtra("canEdit", getIntent().getStringExtra("canEdit"));
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }

                            // Set the ADD COMMENT button click event listener to send the user to
                            // the EnterComment activity
                            addComment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(SeePlaylist.this, EnterComment.class);
                                    intent.putExtra("playlistID", playlistID);
                                    intent.putExtra("canEdit", getIntent().getStringExtra("canEdit"));
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

    // The ListUtils class was constructed based on guidance from the second answer here:
    // https://stackoverflow.com/questions/17693578. It dynamically sets the height of the list
    // with its setDynamicHeight() method based on how many entries are in the list.
    public static class ListUtils {
        private static void setDynamicHeight(ListView mListView) {
            ListAdapter mListAdapter = mListView.getAdapter();
            if (mListAdapter == null) {
                // when adapter is null
                return;
            }
            int height = 0;
            int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                View listItem = mListAdapter.getView(i, null, mListView);
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                height += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mListView.getLayoutParams();
            params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
            mListView.setLayoutParams(params);
            mListView.requestLayout();
        }
    }
}
