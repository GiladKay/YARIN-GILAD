package com.yg.amit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class UpcomingMeetings extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private String name, type;

    private ArrayList<Meeting> meetingList;
    private ListView lv;
    private MeetingAdapter meetingAdapter;
    private String data;

    private StorageReference mStorageRef;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_meetings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();

        sharedPreferences = getSharedPreferences(Menu.AMIT_SP, MODE_PRIVATE);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        lv = (ListView) findViewById(R.id.lv);

        name = sharedPreferences.getString(Menu.NAME_KEY, "name");
        type = sharedPreferences.getString(Menu.TYPE_KEY, "student");

        ProgressDialog newPd = ProgressDialog.show(this, "פגישות קרובות", "מוריד נתונים...", true);
        pd = newPd;
        pd.setCancelable(false);
        pd.show();

        mStorageRef.child("Meetings").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        // All the prefixes under listRef.
                        // You may call listAll() recursively on them.
                    }

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if(item.getName().contains(name)) {
                            downloadFile(item.getName());
                        }
                        else if (type.equals("admin")) {
                            downloadFile(item.getName());
                        }
                    }
                    if (listResult.getItems().isEmpty())
                        Toast.makeText(this, "אין פגישות קרובות!", Toast.LENGTH_LONG).show();
                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getMeetings", "onFailure: ", e);
                });
    }

    private void updateMeeting(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateMeeting: "+data);

        meetingList = new ArrayList<Meeting>();

        Meeting meeting = new Meeting("name", "1.1.1", "00:00");

        if(type.equals("student"))
            meeting = new Meeting(data.split("&&")[1], data.split("&&")[2], data.split("&&")[3]);
        if(type.equals("teacher"))
            meeting = new Meeting(data.split("&&")[0], data.split("&&")[2], data.split("&&")[3]);
        if(type.equals("admin"))
            meeting = new Meeting(data.split("&&")[0]+" - "+data.split("&&")[1], data.split("&&")[2], data.split("&&")[3]);

        meetingList.add(meeting);

        meetingAdapter = new MeetingAdapter(this, 0, 0, meetingList);
        lv.setAdapter(meetingAdapter);
    }

    private void downloadFile(String file) {
        File localFile = new File(getFilesDir()+"/"+file);

        mStorageRef.child("Meetings/"+file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateMeeting(file);
                }).addOnFailureListener(exception -> {
                    // Handle failed download
                    Log.w("Download", "onFailure: Download failed", exception);
                });
    }

    private String readFromFile(Context context, String file) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(file);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString).append("\n");
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}