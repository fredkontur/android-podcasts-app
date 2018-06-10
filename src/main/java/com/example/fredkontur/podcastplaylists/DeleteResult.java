/**************************************************************************************************
 * Author: Frederick Kontur
 * Date Written: November 24, 2017
 * Last Edited: November 24, 2017
 * Description: This activity diplays the result of an attempt to delete a playlist or podcast, and
 *              provides a button for the user to return to the home page
 **************************************************************************************************/
package com.example.fredkontur.podcastplaylists;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DeleteResult extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_podcast_result);

        // Retrive the delete message from getIntent() indicating whether the deletion was
        // successful or not
        String msg = getIntent().getStringExtra("delMsg");
        TextView deleteMsg = (TextView) findViewById(R.id.deleteMessage);
        deleteMsg.setText(msg);

        // Set up the onClickListener() for the home page button
        Button homePageButton = (Button) findViewById(R.id.homePageButton);
        homePageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeleteResult.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
}
