package com.yg.amit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class MeetingsActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;

    private Context context;
    private String name, type;

    private ArrayList<Meeting> meetingList;
    private MeetingAdapter meetingAdapter;

    private ArrayList<Meeting> doneList;
    private MeetingAdapter doneAdapter;

    private ArrayList<Meeting> finishedList;
    private MeetingAdapter finishedAdapter;

    private Button btnUpcoming;
    private Button btnDone;
    private Button btnFinished;

    private ListView lv;

    private TextView tvNoMeeting;

    private int mode;

    private StorageReference mStorageRef;

    private ProgressDialog pd;

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        MenuItem menuItem = menu.findItem(R.id.search);
        menuItem.setVisible(true);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setQueryHint("חפש תלמיד");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (mode == Utils.MODE_UPCOMING)
                    meetingAdapter.getFilter().filter(s);
                if (mode == Utils.MODE_DONE)
                    doneAdapter.getFilter().filter(s);
                if (mode == Utils.MODE_FINISHED)
                    finishedAdapter.getFilter().filter(s);
                return false;
            }


        });
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Toolbar toolbar = findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("פגישות");
        setSupportActionBar(toolbar);

        context = this;

        mode = Utils.MODE_UPCOMING;

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        name = sharedPreferences.getString(Utils.NAME_KEY, "name");            //getting the users name
        type = sharedPreferences.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);         // confirming the user type (student,teacher,admin)

        mStorageRef = FirebaseStorage.getInstance().getReference();

        meetingList = new ArrayList<>();
        doneList = new ArrayList<>();
        finishedList = new ArrayList<>();

        btnUpcoming = (Button) findViewById(R.id.btnUpcoming);
        btnUpcoming.setOnClickListener(this);
        btnDone = (Button) findViewById(R.id.btnDone);
        btnDone.setOnClickListener(this);
        btnFinished = (Button) findViewById(R.id.btnFinished);
        btnFinished.setOnClickListener(this);

        lv = (ListView) findViewById(R.id.lv);
        tvNoMeeting = (TextView) findViewById(R.id.tvNoMeet);
        tvNoMeeting.setVisibility(View.GONE);

        updateLists();
    }

    public void updateLists() {
        pd = ProgressDialog.show(this, "פגישות", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();

        mStorageRef.child("Meetings/").child("Upcoming/").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (item.getName().contains(name) || type.equals(Utils.TYPE_ADMIN))
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
                                    if (item.getName().contains(name) || type.equals(Utils.TYPE_ADMIN))
                                        doneList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));
                                }
                                doneAdapter = new MeetingAdapter(context, doneList);

                                mStorageRef.child("Meetings/").child("Finished/").listAll()
                                        .addOnSuccessListener(listResult2 -> {
                                            for (StorageReference item : listResult2.getItems()) {
                                                // All the items under listRef.
                                                if (item.getName().contains(name) || type.equals(Utils.TYPE_ADMIN))
                                                    finishedList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));
                                            }
                                            finishedAdapter = new MeetingAdapter(context, finishedList);

                                            lv.setOnItemClickListener((adapterView, view, i, l) -> {
                                                pd = ProgressDialog.show(this, "פגישה", "פותח פגישה...", true);
                                                pd.setCancelable(false);
                                                pd.show();

                                                Intent intent = new Intent(getApplicationContext(), MeetingActivity.class);
                                                intent.putExtra(Utils.KEY_MODE, mode);
                                                String student = "";
                                                if (mode == Utils.MODE_UPCOMING) {
                                                    intent.putExtra(Utils.KEY_FILE_NAME, meetingList.get(i).getFileName());
                                                    student = meetingList.get(i).getStudent();
                                                }
                                                if (mode == Utils.MODE_DONE) {
                                                    intent.putExtra(Utils.KEY_FILE_NAME, doneList.get(i).getFileName());
                                                    student = doneList.get(i).getStudent();
                                                }
                                                if (mode == Utils.MODE_FINISHED) {
                                                    intent.putExtra(Utils.KEY_FILE_NAME, finishedList.get(i).getFileName());
                                                    student = finishedList.get(i).getStudent();
                                                }
                                                if (type.equals(Utils.TYPE_TEACHER)) {
                                                    DatabaseReference reference;
                                                    reference = FirebaseDatabase.getInstance().getReference();
                                                    reference.orderByChild(student).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                                                String keys = childSnapshot.getKey();
                                                                intent.putExtra(Utils.KEY_CLASS_NAME, keys);
                                                            }

                                                            intent.putExtra(Utils.KEY_PREVIOUS_ACTIVITY,Utils.ORG_MEETINGS);

                                                            pd.dismiss();
                                                            startActivity(intent);
                                                            finish();
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                } else {
                                                    intent.putExtra(Utils.KEY_CLASS_NAME, "class");
                                                    intent.putExtra(Utils.KEY_PREVIOUS_ACTIVITY, Utils.ORG_MEETINGS);
                                                    pd.dismiss();
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            });

                                            try {
                                                pd.dismiss();
                                            } catch (Exception e) {
                                                // Uh-oh, an error occurred!
                                                Log.w("pd", "onFailure: ", e);
                                                Toast.makeText(getApplicationContext(), "אנא השאר את המסך אנכי", Toast.LENGTH_LONG).show();
                                                finish();
                                            }
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
    }


}