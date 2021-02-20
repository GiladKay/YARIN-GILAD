package com.yg.amit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InfoActivity extends AppCompatActivity implements View.OnClickListener {

    private StorageReference mStorageRef;
    private FirebaseFirestore db;

    private SharedPreferences sharedPreferences;

    private String type, name;

    private String sName, className;
    private int meetCount;

    private Dialog arrMeeting;  //dialog for arranging a meeting
    private TextView tvDate, tvTime;
    private MaterialButton btnCreate;
    private Switch switchCalen; //Switch for calendar save
    private TextView tvSName, tvMeetCount;

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


    private int tHour, tMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        mStorageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);


        arrMeeting = new Dialog(this);                               //Initializing meeting arrangement dialog
        arrMeeting.setContentView(R.layout.meeting_arrangement_dialog);

        tvMeetCount = (TextView) arrMeeting.findViewById(R.id.tvMeetings);
        tvSName = (TextView) arrMeeting.findViewById(R.id.tvStudentName);
        tvTime = (TextView) arrMeeting.findViewById(R.id.tvTime2);
        tvDate = (TextView) arrMeeting.findViewById(R.id.tvDate2);
        btnCreate = (MaterialButton) arrMeeting.findViewById(R.id.btnCreate);

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);
        switchCalen = (Switch) arrMeeting.findViewById(R.id.SwitchSave);
        switchCalen.setChecked(sharedPreferences.getBoolean(Utils.SWITCH_STATE, false));

        type = sharedPreferences.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);
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
        className = extras.getString("classname");
        meetCount = extras.getInt("mCount");

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

                                            if (type.equals(Utils.TYPE_TEACHER)) {
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

        tvDate.setOnClickListener(new View.OnClickListener() {         //Dialog for the user to choose a date for the meeting
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance(); //get the date of the current day
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        InfoActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        mDateSetListener,
                        year, month, day);

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        tvTime.setOnClickListener(new View.OnClickListener() {         //Dialog for the user to choose a time for the meeting
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        InfoActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hour, int min) {
                                tHour = hour;
                                tMinute = min;
                                String time = hour + ":" + min;
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


        switchCalen.setVisibility(View.VISIBLE);


            tvSName.setText(sName + " ");
            tvMeetCount.setText( meetCount+ "/2 ");

            btnCreate.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onClick(View view) {

                    String time = tvTime.getText().toString();
                    String Date = tvDate.getText().toString();

                    if (!time.isEmpty() && !Date.isEmpty()) {


                        if (!switchCalen.isChecked())
                            createMeeting(sName, time, Date);


                        if (switchCalen.isChecked()) {
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
                            String title = "פגישה עם " + sName;


                            Intent intent = new Intent(Intent.ACTION_EDIT);
                            intent.setType("vnd.android.cursor.item/event");
                            intent.putExtra("beginTime", startTime);
                            intent.putExtra("rrule", "FREQ=YEARLY");
                            intent.putExtra("endTime", endTime);
                            intent.putExtra("title", title);

                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                                createMeeting(sName, time, Date);
                            } else {
                                Toast.makeText(InfoActivity.this, "אין לך אפליקציה שיכולה לשמור את התאריך", Toast.LENGTH_LONG).show();
                                createMeeting(sName, time, Date);
                            }

                        }

                        String eSubject = " שיחה אישית עם מורה - אמ" + "\"" + "ית מודיעין בנים";
                        String eMessage = "נקבעה לך שיחה אישית עם המורה " + name + ", בתאריך: " + Date + ", בשעה: " + time + ".\n כל הפרטים נמצאים באפליקציית אמ\"ית.";

                        db.collection("users").whereEqualTo("name", sName)
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

                        Toast.makeText(getApplicationContext(), "פגישה עם " + sName + " בתאריך: " + Date + " בשעה: " + time, Toast.LENGTH_LONG).show();
                        arrMeeting.hide();
                        btnNew.setVisibility(View.GONE);
                        tvNoMeeting.setVisibility(View.GONE);
                        meetingList.add(new Meeting(sName, name, Date,time));
                        btnUpcoming.callOnClick();
                        lv.setAdapter(meetingAdapter);
                        switchCalen.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getApplicationContext(), "יש למלא את כל השדות ", Toast.LENGTH_LONG).show();
                    }

                }
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

            arrMeeting.show();
        }
    }

    private void createMeeting(String studentName, String time, String Date) {
        writeToFile(studentName + "&&" + name + "&&" + Date + "&&" + time + "&&", this, studentName + "&" + name + ".txt");
        uploadFile(studentName + "&" + name + ".txt", "Meetings/Upcoming/");
        updateMeetingCount();
    }


    public void updateMeetingCount() {
        AtomicInteger n = new AtomicInteger();
        mStorageRef.child("Classes/").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference classRef : listResult.getItems()) {
                        // All the items under listRef.
                        if (classRef.getName().contains("Teachers") || classRef.getName().contains(className)) {
                            File localFile = new File(getFilesDir() + "/" + classRef.getName());
                            AtomicReference<String> classTxt = new AtomicReference<>("");

                            mStorageRef.child("Classes/" + classRef.getName()).getFile(localFile)
                                    .addOnSuccessListener(taskSnapshot -> {
                                        // Successfully downloaded data to local file
                                        Log.d("Download", "onSuccess: Download succeeded - " + classRef.getName());

                                        String txt = readFromFile(getApplicationContext(), classRef.getName());
                                        for (String s : txt.split("&&")) {
                                            if (s.contains("==")) {
                                                AtomicInteger c = new AtomicInteger();
                                                mStorageRef.child("Meetings/").listAll()
                                                        .addOnSuccessListener(listResult1 -> {
                                                            for (StorageReference prefix : listResult1.getPrefixes()) {
                                                                // All the prefixes under listRef.
                                                                // You may call listAll() recursively on them.
                                                                prefix.listAll().addOnSuccessListener(listResult2 -> {
                                                                    for (StorageReference item : listResult2.getItems()) {
                                                                        // All the items under listRef.
                                                                        if (item.getName().contains(s.split("==")[0])) {
                                                                            c.getAndIncrement();
                                                                        }
                                                                    }
                                                                    if (prefix.getName().equals(listResult1.getPrefixes().get(2).getName())) {
                                                                        classTxt.set(classTxt + s.split("==")[0] + "==" + c.get() + "&&");
                                                                        if (txt.split("&&").length == classTxt.get().split("&&").length + 1) {
                                                                            Log.d("TAG", "updateMeetingCount: " + classTxt.get());
                                                                            writeToFile(classTxt.get(), getApplicationContext(), classRef.getName());
                                                                            Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(classRef.getName()));
                                                                            mStorageRef.child(classRef.getPath()).putFile(file)
                                                                                    .addOnSuccessListener(taskSnapshot1 -> {
                                                                                        n.getAndIncrement();
                                                                                        if (n.get() == 2) {
                                                                                            Log.d(Utils.TAG, "updateMeetingCount: ENDED!");
                                                                                            // END OF THE METHOD
                                                                                            // WHATEVER YOU WANT

                                                                                        }
                                                                                    });
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        });
                                            }
                                        }
                                    }).addOnFailureListener(exception -> {
                                // Handle failed download
                                Log.w("Download", "onFailure: Download failed", exception);
                            });
                        }
                    }
                });
    }

    private void uploadFile(String fileName, String path) {
        Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(fileName));
        StorageReference riversRef = mStorageRef.child(path + fileName);

        riversRef.putFile(file)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("Upload", "onSuccess: Upload succeeded - " + fileName);
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w("Upload", "onSuccess: Upload failed", exception);
                    }
                });
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


    /**
     * reads and outputs the contents of the now local Class file
     *
     * @param context-this
     * @param file-        name of the class file
     * @return a String containing all the data from the file
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
    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Utils.SWITCH_STATE, switchCalen.isChecked());
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();


    }
}