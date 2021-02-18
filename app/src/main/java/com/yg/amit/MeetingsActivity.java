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
    private Dialog editMeet;
    private MaterialButton btnEdit;
    private TextView tvNoMeeting;

    private Dialog sendMashov;
    private EditText ETM;//edit text mashov
    private MaterialButton btnSend;


    private int mode;

    private TextView tvSName, tvDiaTitle;
    private TextView tvDate, tvTime;


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
        context = this;

        mode = Utils.MODE_UPCOMING;

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        name = sharedPreferences.getString(Utils.NAME_KEY, "name");            //getting the users name
        type = sharedPreferences.getString(Utils.TYPE_KEY, "student");         // confirming the user type (student,teacher,admin)


        editMeet = new Dialog(this);                               //initializing Dialog for altering meeting data
        editMeet.setContentView(R.layout.meeting_arrangement_dialog);
        editMeet.setCanceledOnTouchOutside(true);
        editMeet.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        tvSName = (TextView) editMeet.findViewById(R.id.tvStudentName);
        tvDiaTitle = (TextView) editMeet.findViewById(R.id.tvTitle3);
        tvTime = (TextView) editMeet.findViewById(R.id.tvTime2);
        tvDate = (TextView) editMeet.findViewById(R.id.tvDate2);
        btnEdit = (MaterialButton) editMeet.findViewById(R.id.btnCreate);

        sendMashov = new Dialog(this);
        sendMashov.setContentView(R.layout.mashov_dialog);
        ETM = (EditText) sendMashov.findViewById(R.id.ETMashov);
        btnSend = (MaterialButton) sendMashov.findViewById(R.id.btnSend);


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

        if (type.equals("student") || type.equals("teacher")) {
            btnFinished.setVisibility(View.GONE);
        }
        if (type.equals("teacher")) {
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
                        if (item.getName().contains(name) || type.equals("admin"))
                            meetingList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));

                        meetingAdapter =new MeetingAdapter(context, 0, 0, meetingList);
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

        if (!type.equals("teacher")) {
            mStorageRef.child("Meetings/").child("Done/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            if (item.getName().contains(name) || type.equals("admin"))
                                doneList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));


                            doneAdapter = new MeetingAdapter(context, 0, 0, doneList);
                        }

                    })
                    .addOnFailureListener(e -> {
                        // Uh-oh, an error occurred!
                        Log.w("getMeetings", "onFailure: ", e);
                    });
        }


        if (type.equals("admin")) {
            mStorageRef.child("Meetings/").child("Finished/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            finishedList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));

                            finishedAdapter = new MeetingAdapter(context, 0, 0, finishedList);
                        }

                    })
                    .addOnFailureListener(e -> {
                        // Uh-oh, an error occurred!
                        Log.w("getMeetings", "onFailure: ", e);
                    });
        }


        if (type.equals("teacher")) {//if user is of type teacher he may edit the meeting

            DatePickerDialog.OnDateSetListener mDateSetListener;

            mDateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    month = month + 1;
                    Log.d("TAG", "onDateSet: mm/dd/yyy: " + month + "/" + day + "/" + year);

                    String d = "" + day;
                    if (day < 10) {
                        d = "0" + day;
                    }
                    String m = "" + month;
                    if (month < 10) {
                        m = "0" + month;
                    }
                    String date = d + "/" + m + "/" + year;

                    tvDate.setText(date);
                }
            };

            tvDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    DatePickerDialog dialog = new DatePickerDialog(
                            MeetingsActivity.this,
                            android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                            mDateSetListener,
                            Integer.parseInt(tvDate.getText().toString().substring(6, 10)), Integer.parseInt(tvDate.getText().toString().substring(3, 5)), Integer.parseInt(tvDate.getText().toString().substring(0, 2)));

                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.show();
                }
            });

            tvTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            MeetingsActivity.this,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker timePicker, int i, int i1) {
                                    tHour = i;
                                    tMinute = i1;
                                    String time = i + ":" + i1;
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                                    try {
                                        Date date = simpleDateFormat.parse(time);
                                        tvTime.setText(simpleDateFormat.format(date));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 24, 0, true);
                    timePickerDialog.updateTime(tHour, tMinute);
                    timePickerDialog.show();

                }
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
                startActivity(intent);

                pd = ProgressDialog.show(context, "פגישה", "מוריד נתונים...", true);
                pd.setCancelable(false);
                pd.show();

                editMeet.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                if (mode == Utils.MODE_UPCOMING) {
                    if (type.equals("student"))
                        btnEdit.setVisibility(View.GONE);
                    else
                        btnEdit.setVisibility(View.VISIBLE);
                    downloadUpcomingFile(meetingList.get(i).getFileName(), i);
                }

                if (mode == Utils.MODE_DONE) {
                    if (type.equals("student")) {
                        MaterialButton btnDone = editMeet.findViewById(R.id.btnMeetingDone);
                        btnDone.setText("כתוב משוב");
                        editMeet.findViewById(R.id.text8).setVisibility(View.GONE);
                    }
                    downloadDoneFile(doneList.get(i).getFileName(), i);
                }
                if (mode == Utils.MODE_FINISHED) {

                    downloadDoneFile(finishedList.get(i).getFileName(), i);
                }


                //TODO add a button that adds the meeting to the users calendar when pressed

                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String time = tvTime.getText().toString();
                        String Date = tvDate.getText().toString();


                        Meeting meeting = meetingList.get(i);

                        meeting.setDate(Date);
                        meeting.setTime(time);
                        //lv.setAdapter(meetingAdapter);

                        String data = meeting.getStudent() + "&&" + meeting.getTeacher() + "&&" + Date + "&&" + time + "&&";

                        String fileName = meeting.getStudent() + "&" + meeting.getTeacher() + ".txt";
                        writeToFile(data, getApplicationContext(), fileName);//update the meeting counter
                        uploadFile(fileName, "Meetings/Upcoming/");//

                        Calendar cal = Calendar.getInstance();
                        long endTime;
                        long startTime;
                        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tvTime.getText().toString().split(":")[0]));
                        cal.set(Calendar.MINUTE, Integer.parseInt(tvTime.getText().toString().split(":")[1]));
                        cal.set(Calendar.YEAR, Integer.parseInt(tvDate.getText().toString().split("/")[2]));
                        cal.set(Calendar.MONTH, Integer.parseInt(tvDate.getText().toString().split("/")[1]) - 1);
                        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tvDate.getText().toString().split("/")[0]));


                        startTime = cal.getTimeInMillis();
                        endTime = startTime + 30 * 60 * 1000;

                        if (ContextCompat.checkSelfPermission(MeetingsActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                            int id = ListSelectedCalendars("פגישה עם " + meeting.getStudent());
                            UpdateCalendarEntry(id, startTime, endTime);

                            editMeet.dismiss();
                        } else {
                            RequestStoragePermission();

                            editMeet.dismiss();
                        }

                        Toast.makeText(getApplicationContext(), "time: " + time + " Date: " + Date, Toast.LENGTH_LONG).show();


                    }
                });


                MaterialButton done = (MaterialButton) editMeet.findViewById(R.id.btnMeetingDone);
                if (type.equals("teacher"))
                    done.setVisibility(View.VISIBLE);

                done.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendMashov.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                        sendMashov.show();

                        btnSend.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String mashov = ETM.getText().toString();

                                mashov.replace("&&", " ");

                                if (type.equals("teacher")) {
                                    String fileName = meetingList.get(i).getStudent() + "&" + meetingList.get(i).getTeacher() + ".txt";

                                    StorageReference desertRef = mStorageRef.child("Meetings/Upcoming/" + fileName);

                                    desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // File deleted successfully
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Uh-oh, an error occurred!
                                        }
                                    });

                                    String fileText = meetingList.get(i).getStudent() + "&&" + meetingList.get(i).getTeacher() + "&&" + meetingList.get(i).getDate() + "&&" + meetingList.get(i).getTime() + "&&" + mashov + "&&";
                                    writeToFile(fileText, getApplicationContext(), fileName);//update the meeting counter
                                    uploadFile(fileName, "Meetings/Done/");//

                                    String eSubject = "משוב על שיחה אישית עם מורה - אמ" + "\"" + "ית מודיעין בנים";
                                    String eMessage = "הנך מתבקש לכתוב משוב קצר על הפגישה שהתקיימה בתאריך: " + meetingList.get(i).getDate() + ", בשעה: " + meetingList.get(i).getTime() + ", עם המורה " + meetingList.get(i).getTeacher() + ".";


                                    db.collection("users").whereEqualTo("name", meetingList.get(i).getStudent())
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            Log.d(Utils.TAG, document.getId() + " => " + document.getData());


                                                            sendEmail(document.getId(), eSubject, eMessage);
                                                        }
                                                    } else {
                                                        Log.w(Utils.TAG, "Error getting documents.", task.getException());
                                                    }
                                                }
                                            });

                                    Toast.makeText(getApplicationContext(), "המשוב על הפגישה עם " + meetingList.get(i).getStudent() + " נשלח בהצלחה", Toast.LENGTH_LONG).show();
                                    meetingList.remove(i);

                                }

                                if (type.equals("student")) {
                                    String fileName = doneList.get(i).getStudent() + "&" + doneList.get(i).getTeacher() + ".txt";

                                    StorageReference desertRef = mStorageRef.child("Meetings/Done/" + fileName);

                                    desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // File deleted successfully
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Uh-oh, an error occurred!
                                        }
                                    });

                                    String fileText = doneList.get(i).getStudent() + "&&" + doneList.get(i).getTeacher() + "&&" + doneList.get(i).getDate() + "&&" + doneList.get(i).getTime() + "&&" + mashov + "&&" + doneList.get(i).getMashov() + "&&";
                                    writeToFile(fileText, getApplicationContext(), fileName);//update the meeting counter
                                    uploadFile(fileName, "Meetings/Finished/");//

                                    Toast.makeText(getApplicationContext(), "המשוב על הפגישה עם " + doneList.get(i).getTeacher() + " נשלח בהצלחה", Toast.LENGTH_LONG).show();
                                    doneList.remove(i);
                                }
                                //  lv.setAdapter(meetingAdapter);
                                sendMashov.hide();
                                editMeet.hide();
                            }
                        });


                    }
                });


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

    /**
     * method used to download data files from firebase
     *
     * @param file- String containing the name of the file with all the meeting information
     */
    private void downloadUpcomingFile(String file, int i) {
        File localFile = new File(getFilesDir() + "/" + file);

        mStorageRef.child("Meetings/Upcoming/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateUpcomingMeeting(file, i);
                }).addOnFailureListener(exception -> {
            // Handle failed download
            Log.w("Download", "onFailure: Download failed", exception);
        });

    }

    /**
     * Initializes the meetingList and fills it with all the meetings read from the appropriate file
     *
     * @param file- name of the meeting file
     */
    private void updateUpcomingMeeting(String file, int i) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateMeeting: " + data);

        Meeting meeting = new Meeting(data.split("&&")[0], data.split("&&")[1], data.split("&&")[2], data.split("&&")[3]);//show time, date , and name of student


        meetingList.set(i, meeting);

        tvSName.setText(meetingList.get(i).getStudent());
        tvDate.setText(meetingList.get(i).getDate());
        tvTime.setVisibility(View.VISIBLE);
        tvTime.setText(meetingList.get(i).getTime());
        tvDiaTitle.setText((" ערוך פגישה"));

        TextView num1 = (TextView) editMeet.findViewById(R.id.text3);
        num1.setVisibility(View.GONE);
        TextView num2 = (TextView) editMeet.findViewById(R.id.tvMeetings);
        num2.setVisibility(View.GONE);

        editMeet.findViewById(R.id.text8).setVisibility(View.GONE);
        editMeet.findViewById(R.id.text10).setVisibility(View.GONE);
        editMeet.findViewById(R.id.SV1).setVisibility(View.GONE);
        editMeet.findViewById(R.id.SV2).setVisibility(View.GONE);


        btnEdit.setText("שמור שינויים");

        if (!type.equals("teacher")) {
            btnEdit.setVisibility(View.GONE);
            tvDiaTitle.setText(" פגישה");
            editMeet.getWindow().setLayout(600, 900);
        }
        if (type.equals("student")) {
            btnEdit.setVisibility(View.GONE);
            tvSName.setText(meetingList.get(i).getTeacher());
        }

        pd.dismiss();
        editMeet.show();
        // meetingAdapter = new MeetingAdapter(this, 0, 0, meetingList);
        // lv.setAdapter(meetingAdapter);
    }


    /**
     * reads and outputs the contents of the now local meeting file
     *
     * @param context-this
     * @param file-        name of the meeting file
     * @return String containing all the data from the file
     */
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

    private void writeToFile(String data, Context context, String file) {


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file, MODE_PRIVATE)); // APPEND OR PRIVATE
            outputStreamWriter.append(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void uploadFile(String fileName, String path) {
        Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(fileName));
        StorageReference riversRef = mStorageRef.child(path + fileName);

        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload", "onSuccess: Upload succeeded");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w("Upload", "onSuccess: Upload failed", exception);
                    }
                });
    }

    private void downloadDoneFile(String file, int i) {
        File localFile = new File(getFilesDir() + "/" + file);

        if (mode == Utils.MODE_DONE)
            mStorageRef.child("Meetings/Done/" + file).getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Successfully downloaded data to local file
                        Log.d("Download", "onSuccess: Download succeeded");
                        updateDoneMeeting(file, i);
                    }).addOnFailureListener(exception -> {
                // Handle failed download
                Log.w("Download", "onFailure: Download failed", exception);
            });

        if (mode == Utils.MODE_FINISHED)
            mStorageRef.child("Meetings/Finished/" + file).getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Successfully downloaded data to local file
                        Log.d("Download", "onSuccess: Download succeeded");
                        updateDoneMeeting(file, i);
                    }).addOnFailureListener(exception -> {
                // Handle failed download
                Log.w("Download", "onFailure: Download failed", exception);
            });

    }

    private void updateDoneMeeting(String file, int i) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateMeeting: " + data);


        if (mode == Utils.MODE_DONE) {
            Meeting meeting = new Meeting(data.split("&&")[0], data.split("&&")[1], data.split("&&")[2], data.split("&&")[3], data.split("&&")[4]);//show time, date , and name of student

            doneList.set(i, meeting);

            tvSName.setText(doneList.get(i).getStudent());
            tvDate.setText(doneList.get(i).getDate());
            tvTime.setVisibility(View.VISIBLE);
            tvTime.setText(doneList.get(i).getTime());


            tvDiaTitle.setText((" הוסף משוב"));

            editMeet.findViewById(R.id.text3).setVisibility(View.GONE);
            editMeet.findViewById(R.id.tvMeetings).setVisibility(View.GONE);


            editMeet.findViewById(R.id.text8).setVisibility(View.VISIBLE);
            editMeet.findViewById(R.id.text10).setVisibility(View.GONE);

            btnEdit.setVisibility(View.GONE);
            if (type.equals("admin")) {
                tvDiaTitle.setText(" משוב מורה");
                TextView tvMashov = editMeet.findViewById(R.id.disTMashov);
                editMeet.findViewById(R.id.SV1).setVisibility(View.VISIBLE);
                tvMashov.setText(doneList.get(i).getMashov());
            }
            editMeet.findViewById(R.id.SV2).setVisibility(View.GONE);
            if (type.equals("student")) {
                tvSName.setText(doneList.get(i).getTeacher());
                MaterialButton btndone = editMeet.findViewById(R.id.btnMeetingDone);
                btndone.setVisibility(View.VISIBLE);
            }
        }


        if (mode == Utils.MODE_FINISHED) {
            Meeting meeting = new Meeting(data.split("&&")[0], data.split("&&")[1], data.split("&&")[2], data.split("&&")[3], data.split("&&")[4], data.split("&&")[5]);//show time, date , and name of student

            finishedList.set(i, meeting);

            tvSName.setText(finishedList.get(i).getStudent());
            tvDate.setText(finishedList.get(i).getDate());
            tvTime.setVisibility(View.GONE);


            tvDiaTitle.setText((" משוב"));

            TextView num1 = (TextView) editMeet.findViewById(R.id.text3);
            num1.setVisibility(View.GONE);
            TextView num2 = (TextView) editMeet.findViewById(R.id.tvMeetings);
            num2.setVisibility(View.GONE);

            btnEdit.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);

            editMeet.findViewById(R.id.text8).setVisibility(View.VISIBLE);
            editMeet.findViewById(R.id.text10).setVisibility(View.VISIBLE);

            TextView tvMashov = editMeet.findViewById(R.id.disTMashov);
            editMeet.findViewById(R.id.SV2).setVisibility(View.VISIBLE);
            tvMashov.setText(finishedList.get(i).getMashov());

            TextView tvSMashov = editMeet.findViewById(R.id.disSMashov);
            editMeet.findViewById(R.id.SV1).setVisibility(View.VISIBLE);
            tvSMashov.setText(finishedList.get(i).getSmashov());

        }
        pd.dismiss();
        editMeet.show();
        // meetingAdapter = new MeetingAdapter(this, 0, 0, meetingList);
        // lv.setAdapter(meetingAdapter);
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

    /**
     * sends emails
     *
     * @param address = email address of recipient
     * @param subject = title of email
     * @param message = contents of email
     */
    public void sendEmail(String address, String subject, String message) {
        javaMailAPI javaMailAPI = new javaMailAPI(this, address, subject, message);
        javaMailAPI.execute();

        Log.d(Utils.TAG, "email sent");
    }

    public void RequestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CALENDAR)) {

            new AlertDialog.Builder(this)
                    .setTitle("נצרכת הרשאה")
                    .setMessage("ההרשאה הזאת נדרשת על מנת לעדכן את לוח השנה באשר לפגישות הנ\"ל")
                    .setPositiveButton("אוקיי", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MeetingsActivity.this, new String[]{Manifest.permission.WRITE_CALENDAR}, Utils.STORAGE_PERMISSION_CODE);

                        }
                    })
                    .setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALENDAR}, Utils.STORAGE_PERMISSION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Utils.STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "בקשת הרשאה אושרה", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "בקשת הרשאה נדחתה", Toast.LENGTH_LONG).show();
            }
        }
    }


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