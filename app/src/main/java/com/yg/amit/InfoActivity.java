package com.yg.amit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class InfoActivity extends AppCompatActivity implements View.OnClickListener {

    private StorageReference mStorageRef;

    private SharedPreferences sharedPreferences;

    private String type, name;

    private String sName;

    private TextView tvTitle, tvNoMeeting;
    private Button btnUpcoming, btnDone, btnFinished, btnNew;
    private ListView lv;
    private String data;

    private ArrayList<Meeting> meetingList;
    private MeetingAdapter meetingAdapter;

    private ArrayList<Meeting> doneList;
    private MeetingAdapter doneAdapter;

    private ArrayList<Meeting> finishedList;
    private MeetingAdapter finishedAdapter;

    private int mode;

    private ProgressDialog pd;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        mStorageRef = FirebaseStorage.getInstance().getReference();

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        type = sharedPreferences.getString(Utils.TYPE_KEY, "type");
        name = sharedPreferences.getString(Utils.NAME_KEY, "name");

        context = this;

        tvTitle = findViewById(R.id.tvTitle);
        tvNoMeeting = findViewById(R.id.tvNoMeet);
        lv = findViewById(R.id.lv);

        btnUpcoming = (Button) findViewById(R.id.btnUpcoming);
        btnUpcoming.setOnClickListener(this);
        btnDone = (Button) findViewById(R.id.btnDone);
        btnDone.setOnClickListener(this);
        btnFinished = (Button) findViewById(R.id.btnFinished);
        btnFinished.setOnClickListener(this);
        btnNew = findViewById(R.id.btnNew);
        btnNew.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();
        sName = extras.getString("SName");

        tvTitle.setText(sName);

        tvNoMeeting.setVisibility(View.GONE);

        mode = Utils.MODE_UPCOMING;

        meetingList = new ArrayList<>();
        doneList = new ArrayList<>();
        finishedList = new ArrayList<>();

        pd = ProgressDialog.show(this, "פגישות", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();

        mStorageRef.child("Meetings/").child("Upcoming/").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (item.getName().contains(sName))
                            meetingList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));
                    }
                    meetingAdapter = new MeetingAdapter(context, meetingList);
                    lv.setAdapter(meetingAdapter);

                    if (meetingList.isEmpty()) {
                        tvNoMeeting.setVisibility(View.VISIBLE);
                    }

                    mStorageRef.child("Meetings/").child("Done/").listAll()
                            .addOnSuccessListener(listResult1 -> {
                                for (StorageReference item : listResult1.getItems()) {
                                    // All the items under listRef.
                                    if (item.getName().contains(sName))
                                        doneList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));
                                }
                                doneAdapter = new MeetingAdapter(context, doneList);

                                mStorageRef.child("Meetings/").child("Finished/").listAll()
                                        .addOnSuccessListener(listResult2 -> {
                                            for (StorageReference item : listResult2.getItems()) {
                                                // All the items under listRef.
                                                if (item.getName().contains(sName))
                                                    finishedList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));
                                            }
                                            finishedAdapter = new MeetingAdapter(context, finishedList);

                                            if (type.equals("teacher")) {
                                                if (meetingList.size() + doneList.size() + finishedList.size() < 2) {
                                                    Boolean b = true;
                                                    for (int i = 0; i < meetingList.size(); i++) {
                                                        if (meetingList.get(i).getTeacher().equals(name))
                                                            b = false;
                                                    }
                                                    for (int i = 0; i < doneList.size(); i++) {
                                                        if (doneList.get(i).getTeacher().equals(name))
                                                            b = false;
                                                    }
                                                    for (int i = 0; i < finishedList.size(); i++) {
                                                        if (finishedList.get(i).getTeacher().equals(name))
                                                            b = false;
                                                    }
                                                    if (b) {
                                                        btnNew.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            }

                                            lv.setOnItemClickListener((adapterView, view, i, l) -> {
                                                Intent intent = new Intent(getApplicationContext(), MeetingActivity.class);
                                                intent.putExtra("Mode", mode);
                                                if (mode == Utils.MODE_UPCOMING)
                                                    intent.putExtra("Meeting", meetingList.get(i).getFileName());
                                                if (mode == Utils.MODE_DONE)
                                                    intent.putExtra("Meeting", doneList.get(i).getFileName());
                                                if (mode == Utils.MODE_FINISHED)
                                                    intent.putExtra("Meeting", finishedList.get(i).getFileName());
                                                startActivity(intent);
                                            });

                                            pd.dismiss();
                                        })
                                        .addOnFailureListener(e -> {
                                            // Uh-oh, an error occurred!
                                            Log.w("getMeetings", "onFailure: ", e);
                                            Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                // Uh-oh, an error occurred!
                                Log.w("getMeetings", "onFailure: ", e);
                                Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getMeetings", "onFailure: ", e);
                    Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnUpcoming) {
            lv.setAdapter(meetingAdapter);
            tvNoMeeting.setText("אין פגישות קרובות");
            if (meetingList.isEmpty()) {
                tvNoMeeting.setVisibility(View.VISIBLE);
            } else
                tvNoMeeting.setVisibility(View.GONE);
            mode = Utils.MODE_UPCOMING;
        }
        if (v.getId() == R.id.btnDone) {
            lv.setAdapter(doneAdapter);
            tvNoMeeting.setText("אין פגישות שנעשו וללא משוב תלמיד");
            if (doneList.isEmpty()) {
                tvNoMeeting.setVisibility(View.VISIBLE);
            } else
                tvNoMeeting.setVisibility(View.GONE);
            mode = Utils.MODE_DONE;
        }
        if (v.getId() == R.id.btnFinished) {
            lv.setAdapter(finishedAdapter);
            tvNoMeeting.setText("אין פגישות שנעשו");
            if (finishedList.isEmpty()) {
                tvNoMeeting.setVisibility(View.VISIBLE);
            } else
                tvNoMeeting.setVisibility(View.GONE);
            mode = Utils.MODE_FINISHED;
        }
        if (v.getId() == R.id.btnNew) {
            // TODO new meeting
        }
    }
}