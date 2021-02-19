package com.yg.amit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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
    private String data;
    private MaterialButton btnEdit;
    private TextView tvNoMeeting;



    private int mode;

    private StorageReference mStorageRef;
    private FirebaseFirestore db;

    private ProgressDialog pd;

    private int tHour, tMinute;


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
                if(mode==Utils.MODE_UPCOMING)
                    meetingAdapter.getFilter().filter(s);
                if(mode==Utils.MODE_DONE)
                    doneAdapter.getFilter().filter(s);
                if(mode==Utils.MODE_FINISHED)
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


        context=this;

        mode = Utils.MODE_UPCOMING;

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        name = sharedPreferences.getString(Utils.NAME_KEY, "name");            //getting the users name
        type = sharedPreferences.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);         // confirming the user type (student,teacher,admin)






        mStorageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();

        meetingList = new ArrayList<>();
        doneList = new ArrayList<>();
        finishedList = new ArrayList<>();


        btnUpcoming = (Button) findViewById(R.id.btnUpcoming);
        btnUpcoming.setOnClickListener(this);
        btnDone = (Button) findViewById(R.id.btnDone);
        btnDone.setOnClickListener(this);
        btnFinished = (Button) findViewById(R.id.btnFinished);
        btnFinished.setOnClickListener(this);

        if (type.equals(Utils.TYPE_STUDENT) || type.equals(Utils.TYPE_TEACHER)) {
            btnFinished.setVisibility(View.GONE);
        }
        if (type.equals(Utils.TYPE_TEACHER)) {
            btnDone.setVisibility(View.GONE);
            btnUpcoming.setVisibility(View.GONE);
        }

        lv = (ListView) findViewById(R.id.lv);
        tvNoMeeting = (TextView) findViewById(R.id.tvNoMeet);
        tvNoMeeting.setVisibility(View.GONE);

        pd = ProgressDialog.show(this, "פגישות", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();


        mStorageRef.child("Meetings/").child("Upcoming/").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        // All the prefixes under listRef.
                        // You may call listAll() recursively on them.
                    }

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (item.getName().contains(name) || type.equals(Utils.TYPE_ADMIN))
                            meetingList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));

                        meetingAdapter =new MeetingAdapter(context, meetingList);
                        lv.setAdapter(meetingAdapter);
                    }

                    if (meetingList.isEmpty()) {
                        tvNoMeeting.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "אין פגישות קרובות!", Toast.LENGTH_LONG).show();
                    }


                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getMeetings", "onFailure: ", e);
                });

        if (!type.equals(Utils.TYPE_TEACHER)) {
            mStorageRef.child("Meetings/").child("Done/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            if (item.getName().contains(name) || type.equals(Utils.TYPE_ADMIN))
                                doneList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));


                            doneAdapter = new MeetingAdapter(context, doneList);
                        }

                    })
                    .addOnFailureListener(e -> {
                        // Uh-oh, an error occurred!
                        Log.w("getMeetings", "onFailure: ", e);
                    });
        }


        if (type.equals(Utils.TYPE_ADMIN)) {
            mStorageRef.child("Meetings/").child("Finished/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            finishedList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));

                            finishedAdapter = new MeetingAdapter(context, finishedList);
                        }

                    })
                    .addOnFailureListener(e -> {
                        // Uh-oh, an error occurred!
                        Log.w("getMeetings", "onFailure: ", e);
                    });
        }
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), MeetingActivity.class);
                intent.putExtra("Mode", mode);
                if (mode == Utils.MODE_UPCOMING) intent.putExtra("Meeting", meetingList.get(i).getFileName());
                if (mode == Utils.MODE_DONE) intent.putExtra("Meeting", doneList.get(i).getFileName());
                if (mode == Utils.MODE_FINISHED) intent.putExtra("Meeting", finishedList.get(i).getFileName());
                intent.putExtra("position", i);
                startActivity(intent);

                pd = ProgressDialog.show(context, "פגישה", "מוריד נתונים...", true);
                pd.setCancelable(false);
                pd.show();



                //TODO add a button that adds the meeting to the users calendar when pressed




            }
        });


        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                int event_id = ListSelectedCalendars("פגישה עם " + meetingList.get(i).getStudent());
                DeleteCalendarEntry(event_id);
                return false;
            }
        });
    }

    public int ListSelectedCalendars(String eventtitle) {


        Uri eventUri;
        if (android.os.Build.VERSION.SDK_INT <= 7) {
            // the old way

            eventUri = Uri.parse("content://calendar/events");
        } else {
            // the new way

            eventUri = Uri.parse("content://com.android.calendar/events");
        }

        int result = 0;
        String projection[] = {"_id", "title"};
        Cursor cursor = getContentResolver().query(eventUri, null, null, null,
                null);

        if (cursor.moveToFirst()) {

            String calName;
            String calID;

            int nameCol = cursor.getColumnIndex(projection[1]);
            int idCol = cursor.getColumnIndex(projection[0]);
            do {
                calName = cursor.getString(nameCol);
                calID = cursor.getString(idCol);

                if (calName != null && calName.contains(eventtitle)) {
                    result = Integer.parseInt(calID);
                }

            } while (cursor.moveToNext());
            cursor.close();
        }

        return result;

    }

    @SuppressLint("InlinedApi")
    private int UpdateCalendarEntry(int entryID, long start, long end) {
        int iNumRowsUpdated = 0;

        Uri eventUri;
        if (android.os.Build.VERSION.SDK_INT <= 7) {
            // the old way

            eventUri = Uri.parse("content://calendar/events");
        } else {
            // the new way

            eventUri = Uri.parse("content://com.android.calendar/events");
        }

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, start);
        values.put(CalendarContract.Events.DTEND, end);

        Uri updateUri = ContentUris.withAppendedId(eventUri, entryID);
        iNumRowsUpdated = getContentResolver().update(updateUri, values, null,
                null);

        return iNumRowsUpdated;
    }

    public int DeleteCalendarEntry(int entryID) {
        int iNumRowsDeleted = 0;

        Uri eventUri = ContentUris
                .withAppendedId(getCalendarUriBase(), entryID);
        iNumRowsDeleted = getContentResolver().delete(eventUri, null, null);

        return iNumRowsDeleted;
    }

    private Uri getCalendarUriBase() {
        Uri eventUri;
        if (android.os.Build.VERSION.SDK_INT <= 7) {
            // the old way

            eventUri = Uri.parse("content://calendar/events");
        } else {
            // the new way

            eventUri = Uri.parse("content://com.android.calendar/events");
        }

        return eventUri;
    }




/*
    public void DeleteEvent(int your_event_id){

        Uri eventsUri = Uri.parse("content://com.android.calendar/events");
        Cursor cur = getContentResolver().query(eventsUri, null, null, null, null);

        while (cur.moveToNext()){

            Uri eventUri = ContentUris.withAppendedId(eventsUri, your_event_id);
             getContentResolver().delete(eventUri, null, null);
        }
    }*/






    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnUpcoming:
                lv.setAdapter(meetingAdapter);
                tvNoMeeting.setText("אין פגישות קרובות");
                if (meetingList.isEmpty()) {
                    Toast.makeText(this, "אין פגישות קרובות!", Toast.LENGTH_LONG).show();
                    tvNoMeeting.setVisibility(View.VISIBLE);
                } else
                    tvNoMeeting.setVisibility(View.GONE);
                mode = Utils.MODE_UPCOMING;
                break;
            case R.id.btnDone:
                lv.setAdapter(doneAdapter);
                tvNoMeeting.setText("ברגע שהמורה יסיים לכתוב משוב, תוכל לכתוב משוב על הפגישה שלך עם המורה");
                if (doneList.isEmpty()) {
                    Toast.makeText(this, "אין פגישות שצריכות משוב", Toast.LENGTH_LONG).show();
                    tvNoMeeting.setVisibility(View.VISIBLE);
                } else
                    tvNoMeeting.setVisibility(View.GONE);
                mode = Utils.MODE_DONE;
                break;
            case R.id.btnFinished:
                lv.setAdapter(finishedAdapter);
                if (finishedList.isEmpty()) {
                    Toast.makeText(this, "אין פגישות עם משוב מורה-תלמיד", Toast.LENGTH_LONG).show();
                    tvNoMeeting.setVisibility(View.VISIBLE);
                } else
                    tvNoMeeting.setVisibility(View.GONE);
                mode = Utils.MODE_FINISHED;
                break;

            case R.id.exit:
                finish();
                break;

        }
    }
}