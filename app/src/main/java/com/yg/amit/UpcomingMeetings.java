package com.yg.amit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

public class UpcomingMeetings extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;

    private String name, type;
    private TextView tvMeeting1;

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_meetings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();

        sharedPreferences = getSharedPreferences("AMIT", MODE_PRIVATE);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        tvMeeting1 = (TextView) findViewById(R.id.tvMeeting1);

        name = sharedPreferences.getString("name", "name");
        type = sharedPreferences.getString("type", "student");

        mStorageRef.child("Meetings").listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            if(item.getName().contains(name)) {
                                tvMeeting1.setText(item.getName());
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Uh-oh, an error occurred!
                    }
                });
    }

    @Override
    public void onClick(View v) {

    }
}