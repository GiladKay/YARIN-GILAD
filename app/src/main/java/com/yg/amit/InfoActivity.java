package com.yg.amit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import java.util.ArrayList;
import java.util.Calendar;

public class InfoActivity extends AppCompatActivity implements View.OnClickListener {

    DatabaseReference mFirebaseRef2;
    private StorageReference mStorageRef;
    private FirebaseFirestore db;
    private DatabaseReference mFirebaseRef;
    private FirebaseDatabase mFirebaseInstance;
    private SharedPreferences sharedPreferences;

    private String type, name;

    private String sName, className, email;
    private int meetCount;

    private Dialog arrMeeting;  //dialog for arranging a meeting
    private TextView tvDate, tvTime;
    private MaterialButton btnCreate;
    private SwitchMaterial switchCalen; //Switch for calendar save
    private TextView tvSName, tvMeetCount;

    private TextView tvTitle, tvSubTitle, tvNoMeeting;
    private Button btnUpcoming, btnDone, btnFinished, btnNew;
    private ListView lv;

    private ArrayList<Meeting> meetingList;
    private MeetingAdapter meetingAdapter;

    private ArrayList<Meeting> doneList;
    private MeetingAdapter doneAdapter;

    private ArrayList<Meeting> finishedList;
    private MeetingAdapter finishedAdapter;

    private int mode;

    private ProgressDialog pd;

    private Context context;

    private String mDate, mTime;

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
        switchCalen = (SwitchMaterial) arrMeeting.findViewById(R.id.SwitchSave);
        switchCalen.setChecked(sharedPreferences.getBoolean(Utils.SWITCH_STATE, false));

        type = sharedPreferences.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);
        name = sharedPreferences.getString(Utils.NAME_KEY, "name");

        context = this;


        tvTitle = findViewById(R.id.tvTitle);
        tvSubTitle = findViewById(R.id.tvSubTitle);
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
        sName = extras.getString(Utils.KEY_STUDENT_NAME);
        className = extras.getString(Utils.KEY_CLASS_NAME);
        meetCount = extras.getInt(Utils.KEY_MEETING_COUNT);


        Toolbar toolbar = findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText(sName);
        setSupportActionBar(toolbar);

        tvTitle.setText(sName);
        tvSubTitle.setText("מספר פגישות: " + meetCount);

        tvNoMeeting.setVisibility(View.GONE);

        mode = Utils.MODE_UPCOMING;

        meetingList = new ArrayList<>();
        doneList = new ArrayList<>();
        finishedList = new ArrayList<>();

        email = "";
        db.collection("users").whereEqualTo("name", sName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(Utils.TAG, document.getId() + " => " + document.getData());

                                email = document.getId();
                            }
                        } else {
                            Log.w(Utils.TAG, "Error getting documents.", task.getException());
                        }
                    }
                });

        updateLists();

        clickListeners();
    }

    private void clickListeners() {
        Calendar cal = Calendar.getInstance();  //get the date of the current day
        int tYear = cal.get(Calendar.YEAR);
        int tMonth = cal.get(Calendar.MONTH);
        int tDay = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, day) -> {
            month = month + 1;
            Log.d("TAG", "onDateSet: dd/mm/yyy: " + day + "/" + month + "/" + year);

            String d = "" + day;
            if (day < 10) {
                d = "0" + day;
            }
            String m = "" + month;
            if (month < 10) {
                m = "0" + month;
            }
            mDate = d + "/" + m + "/" + year;
            tvDate.setText(mDate);
        };

        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute) -> {
            Log.d("TAG", "createMeeting: " + hourOfDay + ":" + minute);

            String h = "" + hourOfDay;
            if (hourOfDay < 10) {
                h = "0" + hourOfDay;
            }
            String m = "" + minute;
            if (minute < 10) {
                m = "0" + minute;
            }
            mTime = h + ":" + m;
            tvTime.setText(mTime);
        };

        tvDate.setOnClickListener(view -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                    android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                    onDateSetListener,
                    tYear, tMonth, tDay);
            datePickerDialog.show();
        });

        tvTime.setOnClickListener(view -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                    onTimeSetListener,
                    0, 0, true);
            timePickerDialog.show();
        });

        switchCalen.setVisibility(View.VISIBLE);

        tvSName.setText(sName);
        tvMeetCount.setText(meetCount + "/2");

        btnCreate.setOnClickListener(view -> {

            String time = tvTime.getText().toString();
            String Date = tvDate.getText().toString();

            if (!time.equals("בחירת שעה") && !Date.equals("בחירת תאריך")) {

                new MaterialAlertDialogBuilder(this)
                        .setTitle("יצירת פגישה")
                        .setMessage("האם אתה בטוח שאתה רוצה ליצור פגישה בתאריך "+Date + " בשעה "+time+"?")
                        .setIcon(R.drawable.error)
                        .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int j) {
                                if (meetCount == 0) {

                                    pd = ProgressDialog.show(context, "יצירת פגישה", "יוצר את הפגישה...", true);
                                    pd.setCancelable(false);
                                    pd.show();

                                    writeToFile(sName + "&&" + name + "&&" + mDate + "&&" + mTime + "&&&&&&", context, sName + "&" + name + ".txt");
                                    uploadMeeting(sName + "&" + name + ".txt", "Meetings/Upcoming/");
                                    if(email.contains("@"))
                                        sendNewMail();

                                } else {

                                    for (int i = 0; i < meetingList.size(); i++) {
                                        if (meetingList.get(i).getTime().equals("0")) {
                                            downloadFile(i, Date, time);
                                        } else {
                                            String otherDate = meetingList.get(i).getDate();
                                            String otherTime = meetingList.get(i).getTime();
                                            ifDifferentTime(Date, time, otherDate, otherTime);
                                        }
                                    }
                                }
                            }
                        })
                        .setNegativeButton("לא ", null).show();

            } else {
                Toast.makeText(getApplicationContext(), "יש למלא את כל השדות", Toast.LENGTH_LONG).show();
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
            arrMeeting.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    public void updateLists() {
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

                                            if (!className.equals("Teachers")) {
                                                if (meetingList.size() + doneList.size() + finishedList.size() < 2) {
                                                    boolean b = true;
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
                                                    intent.putExtra(Utils.KEY_MODE, mode);
                                                    if (mode == Utils.MODE_UPCOMING)
                                                        intent.putExtra(Utils.KEY_FILE_NAME, meetingList.get(i).getFileName());
                                                    if (mode == Utils.MODE_DONE)
                                                        intent.putExtra(Utils.KEY_FILE_NAME, doneList.get(i).getFileName());
                                                    if (mode == Utils.MODE_FINISHED)
                                                        intent.putExtra(Utils.KEY_FILE_NAME, finishedList.get(i).getFileName());

                                                    String[] intentArr = new String[meetingList.size()];
                                                    for(int j=0;j<meetingList.size();j++){
                                                        if(j!=i)
                                                            intentArr[j]=meetingList.get(j).getFileName();
                                                        else
                                                            intentArr[j]="null";
                                                    }
                                                    intent.putExtra(Utils.KEY_MEETING_ARR,intentArr);
                                                    intent.putExtra(Utils.KEY_STUDENT_NAME, sName);
                                                    intent.putExtra(Utils.KEY_CLASS_NAME, className);
                                                    intent.putExtra(Utils.KEY_MEETING_COUNT, meetCount);
                                                    intent.putExtra(Utils.KEY_PREVIOUS_ACTIVITY, Utils.ORG_INFO);


                                                    startActivity(intent);
                                                    finish();
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

    public void updateMeetingCount() {
        meetCount++;
        tvSubTitle.setText("מספר פגישות: " + meetCount);


        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseRef = mFirebaseInstance.getReference(className).child(sName);

        mFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);

                mFirebaseRef.setValue((Integer.parseInt(value) + 1) + "");

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(Utils.TAG, "Failed to read value.", error.toException());
            }
        });

        mFirebaseRef2 = mFirebaseInstance.getReference("מורים").child(name);

        mFirebaseRef2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);

                mFirebaseRef2.setValue((Integer.parseInt(value) + 1) + "")
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                btnNew.setVisibility(View.GONE);
                                Toast.makeText(context, "הפגישה נוצרה בהצלחה!", Toast.LENGTH_LONG).show();
                                arrMeeting.dismiss();
                                tvNoMeeting.setVisibility(View.GONE);
                                meetingList.add(new Meeting(sName, name, mDate, mTime));
                                btnUpcoming.performClick();
                                pd.dismiss();
                                if (switchCalen.isChecked())
                                    createEvent();
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    public void createEvent() {
        Calendar cal = Calendar.getInstance();
        long endTime;
        long startTime;

        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(mTime.split(":")[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(mTime.split(":")[1]));
        cal.set(Calendar.YEAR, Integer.parseInt(mDate.split("/")[2]));
        cal.set(Calendar.MONTH, Integer.parseInt(mDate.split("/")[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mDate.split("/")[0]));

        startTime = cal.getTimeInMillis();
        endTime = startTime + Utils.MEETING_LENGTH * 60 * 1000;
        String title = "פגישה עם " + sName;

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("beginTime", startTime);
        intent.putExtra("endTime", endTime);
        intent.putExtra("title", title);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(InfoActivity.this, "אין לך אפליקציה שיכולה לשמור את התאריך", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * uploads the meeting file to fire base, and calls the methods that add events to calendar and
     * those that increase the meeting count of users by one in realtime database
     *
     * @param fileName
     * @param path     location of upload in firebase
     */
    private void uploadMeeting(String fileName, String path) {
        Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(fileName));
        StorageReference riversRef = mStorageRef.child(path + fileName);

        riversRef.putFile(file)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("Upload", "onSuccess: Upload succeeded - " + fileName);
                    updateMeetingCount();
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w("Upload", "onSuccess: Upload failed", exception);
                        Toast.makeText(context, "אירעה שגיאה", Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void sendNewMail() {
        String eSubject = " שיחה אישית עם מורה - אמי" + "\"" + "ת מודיעין בנים";
        String eMessage = "נקבעה לך שיחה אישית עם המורה " + name + ", בתאריך: " + mDate + ", בשעה: " + mTime + ".\n כל הפרטים נמצאים באפליקציית אמי\"ת.";

        Utils.sendEmail(context, email, eSubject, eMessage);
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

    private void downloadFile(int i, String date, String time) {
        String file = meetingList.get(i).getFileName();
        File localFile = new File(getFilesDir() + "/" + file);


        mStorageRef.child("Meetings/Upcoming/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    String data = readFromFile(context, file);
                    meetingList.set(i, new Meeting(data.split("&&")[0], data.split("&&")[1], data.split("&&")[2], data.split("&&")[3]));
                    ifDifferentTime(date, time, meetingList.get(i).getDate(), meetingList.get(i).getTime());
                })
                .addOnFailureListener(exception -> {
                    // Handle failed download
                    Log.w("Download", "onFailure: Download failed", exception);
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                    finish();
                });
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

    private void ifDifferentTime(String Date, String time, String otherDate, String otherTime) {
        if (otherDate.equals(Date)) {
            String otherHour = otherTime.split(":")[0];
            String otherMin = otherTime.split(":")[1];

            int totalMINs = Integer.parseInt(time.split(":")[0]) * 60 + Integer.parseInt(time.split(":")[1]);
            int otherTotalMIns = Integer.parseInt(otherHour) * 60 + Integer.parseInt(otherMin);

            if (Math.abs(totalMINs - otherTotalMIns) > Utils.MEETING_LENGTH) {
                pd = ProgressDialog.show(context, "יצירת פגישה", "יוצר את הפגישה...", true);
                pd.setCancelable(false);
                pd.show();

                writeToFile(sName + "&&" + name + "&&" + mDate + "&&" + mTime + "&&&&&&", context, sName + "&" + name + ".txt");
                uploadMeeting(sName + "&" + name + ".txt", "Meetings/Upcoming/");
                if(email.contains("@"))
                    sendNewMail();
            } else {
                Toast.makeText(context, "לתלמיד כבר יש פגישה בזמן הזה, שמתחילה בשעה " + otherTime, Toast.LENGTH_LONG).show();
            }
        } else {
            pd = ProgressDialog.show(context, "יצירת פגישה", "יוצר את הפגישה...", true);
            pd.setCancelable(false);
            pd.show();

            writeToFile(sName + "&&" + name + "&&" + mDate + "&&" + mTime + "&&&&&&", context, sName + "&" + name + ".txt");
            uploadMeeting(sName + "&" + name + ".txt", "Meetings/Upcoming/");
            if(email.contains("@"))
                sendNewMail();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(arrMeeting.isShowing())
            arrMeeting.dismiss();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Utils.SWITCH_STATE, switchCalen.isChecked());
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(this, StudentsActivity.class));
        finish();
    }
}