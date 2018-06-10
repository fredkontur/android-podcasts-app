/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity lists all podcasts by title along with a button to view/edit/delete
 *              the podcast.
 * Sources: In order to create the ListView with event handlers and the ViewHolder pattern, which is
 *          more efficient than typical ArrayAdapters, I followed the guidance on this website:
 *          https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Context;
import android.content.Intent;
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

public class SeeAllPodcasts extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_all_podcasts);

        ListView podcastList = (ListView) findViewById(R.id.podcastList);
        ArrayList<JSONObject> podcasts = new ArrayList<JSONObject>();
        final SeeAllPodcasts.PodcastAdapter podcastAdapter = new SeeAllPodcasts.PodcastAdapter(this, podcasts);
        podcastList.setAdapter(podcastAdapter);

        // Use an OkHttpClient to execute a GET request to get a list of all of the podcasts
        String reqUrl = "https://podcast-playlists.appspot.com/podcasts";
        OkHttpClient mOkHttpClient;

        Request podcastReq = new Request.Builder()
                .url(reqUrl)
                .build();

        mOkHttpClient = new OkHttpClient();
        mOkHttpClient.newCall(podcastReq).enqueue(new Callback() {
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
                            // Iterate through the list of podcasts and add each of them to the
                            // podcastAdapter
                            for(int i = 0; i < j.length(); i++) {
                                try {
                                    JSONObject pod = j.getJSONObject(i);
                                    podcastAdapter.add(pod);
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

    // The PodcastAdapter displays a list with the titles of the podcasts as well as buttons which
    // allow the user to "View/Edit/Delete" the podcast.
    public class PodcastAdapter extends ArrayAdapter<JSONObject> {
        // View lookup cache
        private class ViewHolder {
            TextView pdTitle;
            Button accessPodcast;
        }

        // PodcastAdapter constructor
        private PodcastAdapter(Context context, ArrayList<JSONObject> podcasts) {
            super(context, R.layout.item_podcast, podcasts);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            final JSONObject podcast = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            final SeeAllPodcasts.PodcastAdapter.ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                // If there's no view to re-use, inflate a brand new view for row
                viewHolder = new SeeAllPodcasts.PodcastAdapter.ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_podcast, parent, false);
                viewHolder.pdTitle = (TextView) convertView.findViewById(R.id.pdTitle);
                viewHolder.accessPodcast = (Button) convertView.findViewById(R.id.accessPodcast);
                // Cache the viewHolder object inside the fresh view
                convertView.setTag(viewHolder);
            } else {
                // View is being recycled, retrieve the viewHolder object from tag
                viewHolder = (SeeAllPodcasts.PodcastAdapter.ViewHolder) convertView.getTag();
            }
            // Put the podcast title in the ViewHolder
            try {
                String podcastTitle = podcast.getString("title");
                viewHolder.pdTitle.setText(podcastTitle);
            } catch(JSONException e) {
                e.printStackTrace();
            }
            // Attach the click event handler for the "View/Edit/Delete" button. This handler will
            // send the user to the SeePodcast activity
            viewHolder.accessPodcast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(SeeAllPodcasts.this, SeePodcast.class);
                    try {
                        intent.putExtra("podcastID", podcast.getString("id"));
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

