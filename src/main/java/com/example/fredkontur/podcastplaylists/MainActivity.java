/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This is the main page for an app that allows a user to create playlists of podcasts.
 *              With this app, a user can enter the information for podcasts and then build
 *              a playlist object featuring chosen podcasts. The playlist object can be open for
 *              editing by anyone, or the user can log in through Google and link the playlist to
 *              his or her user account so that only the signed-in user can edit or delete the
 *              playlist.
 * Sources: I followed the guidance at https://developers.google.com/identity/sign-in/android/start
 *          to integrate Google Sign-In into my app
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Space;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // RC_SIGN_IN is a dummy number to make sure that this activity was the one that called the
    // Google Sign-In Intent
    private static final int RC_SIGN_IN = 5432;
    public static final String PREF = "StoredAccountInfo";
    public static final String userID = "userID";
    public static final String idToken = "idToken";
    public static final String loggedIn = "loggedIn";
    public static final String signInTime = "signInTime";
    private GoogleSignInClient mGoogleSignInClient;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The default value of the "loggedIn" Shared Preferences variable should be "no"
        sharedPreferences = getSharedPreferences(PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(loggedIn, "no");
        editor.apply();

        // Set up OnClickListener() for the "See All Playlists" button
        Button seeAllPlaylistsButton = (Button) findViewById(R.id.seeAllPlaylists);
        seeAllPlaylistsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SeeAllPlaylists.class);
                startActivity(intent);
            }
        });

        // Set up OnClickListener() for the "See All Podcasts" button
        Button seeAllPodcastsButton = (Button) findViewById(R.id.seeAllPodcasts);
        seeAllPodcastsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SeeAllPodcasts.class);
                startActivity(intent);
            }
        });

        // Set up OnClickListener() for the "Create New Podcast" button
        Button createNewPodcastButton = (Button) findViewById(R.id.createNewPodcast);
        createNewPodcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateNewPodcast.class);
                startActivity(intent);
            }
        });

        // Set up OnClickListener() for the "Create New Playlist" button
        Button createNewPlaylistButton = (Button) findViewById(R.id.createNewPlaylist);
        createNewPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateNewPlaylist.class);
                startActivity(intent);
            }
        });

        // Set up OnClickListener() for the "See My Playlist" button. Also, define the variable for
        // the space to create a regular-sized spacing between the buttons if the "See My Playlist"
        // button becomes visible
        final Space seeMyPlaylistsSpace = (Space) findViewById(R.id.seeMyPlaylistsSpace);
        final Button seeMyPlaylistsButton = (Button) findViewById(R.id.seeMyPlaylists);
        seeMyPlaylistsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SeeMyPlaylists.class);
                startActivity(intent);
            }
        });


        // Configure sign-in to request the user's ID and basic profile, which are included in
        // DEFAULT_SIGN_IN. Also request an ID Token to send to the backend server.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Create a Google Sign-In button and a (regular) sign-out button
        final SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        final Button signOutButton = (Button) findViewById(R.id.sign_out_button);
        // Set the dimensions of the sign-in button.
        signInButton.setSize(SignInButton.SIZE_WIDE);

        // Set up the OnClickListener() for the Google sign-in button
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
                // Since the ID Token only lasts for ~1 hour, record the time when the user signed
                // in to determine if the ID Token needs to be refreshed
                long currTime = System.currentTimeMillis();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(signInTime, String.valueOf(currTime));
                editor.apply();

            }
        });

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleSignInClient.signOut();
                signOutButton.setVisibility(View.GONE);
                signInButton.setVisibility(View.VISIBLE);
                seeMyPlaylistsButton.setVisibility(View.GONE);
                seeMyPlaylistsSpace.setVisibility(View.GONE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(loggedIn, "no");
                editor.apply();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    // Once sign-in has taken place, this method will pass the account information to the updateUI()
    // method
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            updateUI(account);
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);
            updateUI(null);
        }
    }

    @Override
    protected void onStart() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null) {
            updateUI(null);
        }
        else {
            // Use silentSignIn() to update idToken if necessary
            mGoogleSignInClient.silentSignIn()
                    .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                        @Override
                        public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                            handleSignInResult(task);
                        }
                    });
        }
        super.onStart();
    }

    // This method updates the UI based on the results of the sign-in process
    protected void updateUI(GoogleSignInAccount account) {
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        Button signOutButton = (Button) findViewById(R.id.sign_out_button);
        Button seeMyPlaylistsButton = (Button) findViewById(R.id.seeMyPlaylists);
        Space seeMyPlaylistsSpace = (Space) findViewById(R.id.seeMyPlaylistsSpace);

        // If the user is signed in, then account is non-null. In this case, make the sign-out
        // button visible, the sign-in button invisible, and the "See My Playlists" button visible.
        // Also, store the userID, idToken, and loggedIn = "yes" in the shared preferences.
        if (account != null) {
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
            seeMyPlaylistsButton.setVisibility(View.VISIBLE);
            seeMyPlaylistsSpace.setVisibility(View.VISIBLE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(userID, account.getId());
            editor.putString(idToken, account.getIdToken());
            editor.putString(loggedIn, "yes");
            editor.apply();
        }
        // If the user is not signed in, then account is null. In this case, make the sign-in
        // button visible, the sign-out button invisible, and the "See My Playlists" button
        // invisible. Also, store loggedIn = "no" in the shared preferences.
        else {
            signOutButton.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
            seeMyPlaylistsButton.setVisibility(View.GONE);
            seeMyPlaylistsSpace.setVisibility(View.GONE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(loggedIn, "no");
            editor.apply();
        }
    }
}
