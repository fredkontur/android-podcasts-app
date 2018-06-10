/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity provides the values of the fields for the chosen podcast and
 *              provides the user with buttons to return to the home page or to edit or delete the
 *              podcast
 * Sources: To create a scrollable layout that is populated with multiple lists, I used the second
 *          answer given in this reference: https://stackoverflow.com/questions/17693578
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.DialogInterface;
import android.content.Intent;
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

public class SeePodcast extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_podcast);

        final String podcastID = getIntent().getStringExtra("podcastID");
        final String reqUrl = "https://podcast-playlists.appspot.com/podcasts/" + podcastID;
        OkHttpClient mOkHttpClient;

        // Set the click event listener for the "HOME PAGE" button to return the user to the
        // MainActivity activity
        Button podcastReturnToHome = (Button) findViewById(R.id.podcastReturnToHome);
        podcastReturnToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SeePodcast.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Create an OkHttpClient to send a GET request for the podcast passed to this activity
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
                    final JSONObject podcast = new JSONObject(r);

                    // Set the click event handler of the "EDIT PODCAST" button to take the user to
                    // the EditPodcast activity
                    Button editPodcast = (Button) findViewById(R.id.editPodcast);
                    editPodcast.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(SeePodcast.this, EditPodcast.class);
                            intent.putExtra("podcast", podcast.toString());
                            intent.putExtra("podcastID", podcastID);
                            startActivity(intent);
                        }
                    });

                    // Set the click event handler of the "DELETE PODCAST" button to make a DELETE
                    // request to the REST API
                    Button deletePodcast = (Button) findViewById(R.id.deletePodcast);
                    deletePodcast.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Send an alert to the user to confirm that he/she wants to delete the
                            // podcast. If not, exit the event handler. If the user confirms the
                            // deletion, then continue with the DELETE request to the REST API
                            AlertDialog.Builder dialog = new AlertDialog.Builder(SeePodcast.this);
                            dialog.setCancelable(true);
                            dialog.setMessage(getString(R.string.confirm_delete));
                            dialog.setPositiveButton(getString(R.string.yes_delete), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Construct a DELETE request for the REST API
                                    OkHttpClient mOkHttpClientDel;

                                    Request podcastReq = new Request.Builder()
                                            .url(reqUrl)
                                            .delete()
                                            .build();

                                    mOkHttpClientDel = new OkHttpClient();
                                    mOkHttpClientDel.newCall(podcastReq).enqueue(new Callback() {
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
                                                    // Construct a message for the user. If the
                                                    // response status code was 204, then inform the
                                                    // user the deletion was successful. Otherwise,
                                                    // inform the user the deletion was not
                                                    // successful.
                                                    String delMsg;
                                                    if (statusCode != 204) {
                                                        delMsg = getString(R.string.del_msg_negative);
                                                    } else {
                                                        delMsg = getString(R.string.del_msg_positive);
                                                    }
                                                    // Send the user to the DeleteResult activity to
                                                    // see the result of the DELETE request
                                                    Intent intent = new Intent(SeePodcast.this, DeleteResult.class);
                                                    intent.putExtra("delMsg", delMsg);
                                                    startActivity(intent);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                            // If the user does not want to delete the podcast, then exit the event
                            // handler
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

                    // Set the click event handler for the "RATE PODCAST" button to take the user to
                    // the RatePodcast activity
                    Button ratePodcastButton = (Button) findViewById(R.id.ratePodcastButton);
                    ratePodcastButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(SeePodcast.this, RatePodcast.class);
                            intent.putExtra("podcastID", podcastID);
                            startActivity(intent);
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int totRatings;
                            int sumRatings = 0;
                            double avgRating;
                            double avgRatingRounded;
                            TextView podTitle = (TextView) findViewById(R.id.podTitle);
                            TextView podGenre = (TextView) findViewById(R.id.podGenre);
                            TextView podNumEpisodes = (TextView) findViewById(R.id.podNumEpisodes);
                            TextView podNumSeasons = (TextView) findViewById(R.id.podNumSeasons);
                            TextView podDescription = (TextView) findViewById(R.id.podDescription);
                            TextView podActive = (TextView) findViewById(R.id.podActive);
                            TextView podTotRatings = (TextView) findViewById(R.id.podTotRatings);
                            TextView podAvgRating = (TextView) findViewById(R.id.podAvgRating);
                            final ListView podPlaylists = (ListView) findViewById(R.id.podPlaylists);
                            ArrayList<String> playlists = new ArrayList<>();
                            final ArrayAdapter<String> playlistAdapter = new ArrayAdapter<>(SeePodcast.this, android.R.layout.simple_list_item_1, playlists);
                            podPlaylists.setAdapter(playlistAdapter);

                            // Display the values of the fields in the podcast
                            try {
                                podTitle.setText(podcast.getString("title"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (!(podcast.isNull("genre"))) {
                                try {
                                    podGenre.setText(podcast.getString("genre"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (!(podcast.isNull("numEpisodes"))) {
                                try {
                                    podNumEpisodes.setText(String.valueOf(podcast.getInt("numEpisodes")));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (!(podcast.isNull("numSeasons"))) {
                                try {
                                    podNumSeasons.setText(String.valueOf(podcast.getInt("numSeasons")));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (!(podcast.isNull("description"))) {
                                try {
                                    podDescription.setText(podcast.getString("description"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (!(podcast.isNull("active"))) {
                                try {
                                    if (podcast.getBoolean("active")) {
                                        podActive.setText(getString(R.string.podcast_yes_still_active));
                                    } else {
                                        podActive.setText(getString(R.string.podcast_no_still_active));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            // Calculate the total number of ratings and the average value of the
                            // ratings (to 2 decimal places) and display those values rather than
                            // a list of the ratings
                            try {
                                JSONArray ratings = podcast.getJSONArray("ratings");
                                totRatings = ratings.length();
                                for (int i = 0; i < totRatings; i++) {
                                    sumRatings += ratings.getInt(i);
                                }
                                if (totRatings > 0) {
                                    avgRating = (double) sumRatings / totRatings;
                                    avgRatingRounded = (double) Math.round(avgRating * 100) / 100;
                                    podTotRatings.setText(String.valueOf(totRatings));
                                    podAvgRating.setText(String.valueOf(avgRatingRounded));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            // Create an OkHttpClient to construct a GET request for the list of all
                            // playlists
                            String reqUrlPlaylists = "https://podcast-playlists.appspot.com/playlists";
                            OkHttpClient mOkHttpClientPlaylists;

                            Request playlistReq = new Request.Builder()
                                    .url(reqUrlPlaylists)
                                    .build();

                            mOkHttpClientPlaylists = new OkHttpClient();
                            mOkHttpClientPlaylists.newCall(playlistReq).enqueue(new Callback() {
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
                                                // Get the playlist names from the list of all
                                                // playlists for the playlists that include the
                                                // chosen podcast
                                                final JSONArray allPlaylists = new JSONArray(r);
                                                String currPlaylistID;
                                                JSONArray p = podcast.getJSONArray("playlists");

                                                // Find out which playlists in the list of all
                                                // playlist have IDs matching those in the playlist
                                                // list for this podcast, and then get the names
                                                // of those playlists for display in the list
                                                for (int i = 0; i < p.length(); i++) {
                                                    currPlaylistID = p.getString(i);
                                                    for (int j = 0; j < allPlaylists.length(); j++) {
                                                        JSONObject playlist = allPlaylists.getJSONObject(j);
                                                        if (currPlaylistID.equals(playlist.getString("id"))) {
                                                            playlistAdapter.add(playlist.getString("name"));
                                                            break;
                                                        }
                                                    }
                                                }
                                                // Use the ListUtils.setDynamicHeight() helper
                                                // function to display the list of playlist names
                                                ListUtils.setDynamicHeight(podPlaylists);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
