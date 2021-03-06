package com.yg.amit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

public class MeetingActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;

    private String type, name;

    private Dialog diaEdit;
    private TextView tvTitleEdit;
    private TextView tvTimeEdit, tvDateEdit;
    private Button btnUpdateEdit;

    private String student, teacher, date, time;//data from the meeting file sent to the activity

    private TextView tvTitle, tvSubTitle, tvHelper;
    private TextInputLayout ipInput;
    private TextInputEditText edtInput;
    private Button btnSend;
    private Button btnEdit;
    private Button btnDelete;
    private Button btnAddToCal;

    private ArrayList<Meeting> meetingList;
    private String[] tempArr;

    private DatabaseReference mFirebaseRef;
    private FirebaseDatabase mFirebaseInstance;
    private DatabaseReference mFirebaseRef2;

    private CardView sMashov, tMashov;
    private TextView tvSMashov, tvTMashov;

    private ProgressDialog pd;

    private String meetingFile;
    private int meetingMode, meetCount;
    private String sName, className, pActivity;

    private String data;

    private StorageReference mStorageRef;
    private FirebaseFirestore db;

    private int tHour, tMinute;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        mStorageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();

        context = this;
        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);
        name = sharedPreferences.getString(Utils.NAME_KEY, Utils.NAME_KEY);
        type = sharedPreferences.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);

        Bundle extras = getIntent().getExtras();
        meetingFile = extras.getString(Utils.KEY_FILE_NAME);
        meetingMode = extras.getInt(Utils.KEY_MODE);
        className = extras.getString(Utils.KEY_CLASS_NAME);
        sName = extras.getString(Utils.KEY_STUDENT_NAME);
        meetCount = extras.getInt(Utils.KEY_MEETING_COUNT);
        pActivity = extras.getString(Utils.KEY_PREVIOUS_ACTIVITY);
        tempArr= extras.getStringArray(Utils.KEY_MEETING_ARR);
        meetingList = new ArrayList<>();


        diaEdit = new Dialog(this);
        diaEdit.setContentView(R.layout.edit_meeting_dialog);
        diaEdit.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        tvTitleEdit = (TextView) diaEdit.findViewById(R.id.tvTitle);
        tvTimeEdit = (TextView) diaEdit.findViewById(R.id.tvEditTime);
        tvDateEdit = (TextView) diaEdit.findViewById(R.id.tvEditDate);
        btnUpdateEdit = (Button) diaEdit.findViewById(R.id.btnUpdate);

        btnUpdateEdit.setOnClickListener(view -> {

            new MaterialAlertDialogBuilder(this)
                    .setTitle("עריכת פגישה")
                    .setMessage("האם אתה בטוח שאתה רוצה לשנות את הפגישה לתאריך "+tvDateEdit.getText().toString() + " בשעה "+tvTimeEdit.getText().toString() +"?")
                    .setIcon(R.drawable.error)
                    .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int j) {


                                    String tempDate=tvDateEdit.getText().toString();
                                    String tempTime= tvTimeEdit.getText().toString();
                                    if(meetCount==0&&pActivity.equals(Utils.ORG_INFO)) {
                                        editMethod();
                                    }else{
                                        for (int i = 0; i < meetingList.size(); i++) {
                                            if (meetingList.get(i).getTime().equals("0")) {
                                                downloadMeetingFile(i, tempDate, tempTime);
                                            } else {
                                                String otherDate = meetingList.get(i).getDate();
                                                String otherTime = meetingList.get(i).getTime();
                                                ifDifferentTime(tempDate, tempTime, otherDate, otherTime);

                                                //potential error with more than one meeting(creates/ edits the meeting the second the first checked meeting is not on the same time)
                                            }
                                        }
                                    }
                                }
                            })
                    .setNegativeButton("לא ", null).show();

        });

        tvTimeEdit.setOnClickListener(view -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    MeetingActivity.this,
                    android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                    (timePicker, i, i1) -> {
                        tHour = i;
                        tMinute = i1;
                        String time = i + ":" + i1;
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                        try {
                            Date date = simpleDateFormat.parse(time);
                            tvTimeEdit.setText(simpleDateFormat.format(date));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }, 0, 0, true);
            timePickerDialog.updateTime(tHour, tMinute);
            timePickerDialog.show();

        });

        DatePickerDialog.OnDateSetListener mDateSetListener;

        mDateSetListener = (datePicker, year, month, day) -> {
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

            tvDateEdit.setText(date);
        };

        tvDateEdit.setOnClickListener(view -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    MeetingActivity.this,
                    android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                    mDateSetListener,
                    Integer.parseInt(tvDateEdit.getText().toString().substring(6, 10)), Integer.parseInt(tvDateEdit.getText().toString().substring(3, 5)), Integer.parseInt(tvDateEdit.getText().toString().substring(0, 2)));
            dialog.show();
        });

        tvTitle = findViewById(R.id.tvTitle);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        tvHelper = findViewById(R.id.tvHelper);
        ipInput = findViewById(R.id.ipInput);
        edtInput = findViewById(R.id.edtInput);
        btnSend = findViewById(R.id.btnSend);

        sMashov = findViewById(R.id.sMashov);
        tMashov = findViewById(R.id.tMashov);
        tvSMashov = findViewById(R.id.tvSMashov);
        tvTMashov = findViewById(R.id.tvTMashov);
        btnSend.setOnClickListener(this);
        btnAddToCal = findViewById(R.id.btnAddToCal);
        btnAddToCal.setOnClickListener(this);

        pd = ProgressDialog.show(this, "פגישה", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();
        downloadFile(meetingFile);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btnSend:
                pd = ProgressDialog.show(this, "פגישה", "שולח משוב...", true);
                pd.setCancelable(false);
                pd.show();

                btnAddToCal.setVisibility(View.GONE);
                if (type.equals(Utils.TYPE_TEACHER) || (type.equals(Utils.TYPE_ADMIN) && teacher.equals(name))) {
                    btnEdit.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.GONE);
                }
                String mashov = edtInput.getText().toString().trim();

                if (type.equals(Utils.TYPE_STUDENT)) {
                    String newData = data.split("&&")[0] + "&&" + data.split("&&")[1] + "&&" + data.split("&&")[2] + "&&"
                            + data.split("&&")[3] + "&&" + mashov + "&&" + data.split("&&")[5] + "&&";
                    writeToFile(newData, this, meetingFile);
                    uploadFile(meetingFile, "Meetings/Finished/");
                }
                if (type.equals(Utils.TYPE_TEACHER) || (type.equals(Utils.TYPE_ADMIN) && teacher.equals(name))) {
                    String newData = data.split("&&")[0] + "&&" + data.split("&&")[1] + "&&" + data.split("&&")[2] + "&&"
                            + data.split("&&")[3] + "&&" + data.split("&&")[4] + "&&" + mashov + "&&";
                    writeToFile(newData, this, meetingFile);
                    uploadFile(meetingFile, "Meetings/Done/");
                }
                break;


            case R.id.btnEdit:
                diaEdit.show();
                break;

            case R.id.btnBin:
                new MaterialAlertDialogBuilder(this)
                        .setTitle("מחיקת פגישה")
                        .setMessage("האם אתה בטוח שאתה רוצה למחוק את הפגישה? ")
                        .setIcon(R.drawable.error)
                        .setPositiveButton("כן ", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pd = ProgressDialog.show(MeetingActivity.this, "פגישה", "מוחק פגישה...", true);
                                pd.setCancelable(false);
                                pd.show();

                                mFirebaseInstance = FirebaseDatabase.getInstance();
                                mFirebaseRef = mFirebaseInstance.getReference(className).child(student);

                                mFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        String value = snapshot.getValue(String.class);
                                        mFirebaseRef.setValue((Integer.parseInt(value) - 1) + "");
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                                mFirebaseRef2 = mFirebaseInstance.getReference("מורים").child(teacher);

                                mFirebaseRef2.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        String value = dataSnapshot.getValue(String.class);

                                        mFirebaseRef2.setValue((Integer.parseInt(value) - 1) + "");
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {

                                    }
                                });


                                if (ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                                    DeleteCalendarEntry(ListSelectedCalendars("פגישה עם " + student));

                                } else if (!(ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)) {
                                    RequestStoragePermission1();
                                } else {
                                    RequestStoragePermission2();
                                }
                                String eSubject = "ביטול פגישה - אמי" + "\"" + "ת מודיעין בנים";
                                String eMessage = "הפגישה שהייתה אמורה להתקיים בתאריך: " + date + ", בשעה: " + time + ", עם המורה " + teacher + ", בוטלה.";

                                db.collection("users").whereEqualTo("name", student)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        Log.d(Utils.TAG, document.getId() + " => " + document.getData());

                                                        Utils.sendEmail(context, document.getId(), eSubject, eMessage);
                                                    }
                                                } else {
                                                    Log.w(Utils.TAG, "Error getting documents.", task.getException());
                                                }
                                            }
                                        });

                                StorageReference desertRef = mStorageRef.child("Meetings/Upcoming/" + meetingFile);
                                desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        pd.dismiss();
                                        Toast.makeText(getApplicationContext(), " הפגישה נמחקה ", Toast.LENGTH_LONG).show();

                                        Intent intent = null;
                                        if (pActivity.equals(Utils.ORG_INFO)) {
                                            intent = new Intent(MeetingActivity.this, InfoActivity.class);
                                            intent.putExtra(Utils.KEY_STUDENT_NAME, sName);
                                            intent.putExtra(Utils.KEY_MEETING_COUNT, meetCount - 1);
                                            intent.putExtra(Utils.KEY_CLASS_NAME, className);
                                        }
                                        if (pActivity.equals(Utils.ORG_MEETINGS))
                                            intent = new Intent(MeetingActivity.this, MeetingsActivity.class);

                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("לא ", null).show();

                break;

            case R.id.btnAddToCal:
                Calendar cal = Calendar.getInstance();
                long endTime;
                long startTime;
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tvTimeEdit.getText().toString().split(":")[0]));
                cal.set(Calendar.MINUTE, Integer.parseInt(tvTimeEdit.getText().toString().split(":")[1]));
                cal.set(Calendar.YEAR, Integer.parseInt(tvDateEdit.getText().toString().split("/")[2]));
                cal.set(Calendar.MONTH, Integer.parseInt(tvDateEdit.getText().toString().split("/")[1]) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tvDateEdit.getText().toString().split("/")[0]));

                startTime = cal.getTimeInMillis();
                endTime = startTime + Utils.MEETING_LENGTH * 60 * 1000;

                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra("beginTime", startTime);
                intent.putExtra("rrule", "FREQ=YEARLY");
                intent.putExtra("endTime", endTime);
                if (type.equals(Utils.TYPE_TEACHER) || (type.equals(Utils.TYPE_ADMIN) && teacher.equals(name)))
                    intent.putExtra("title", "פגישה עם " + student);
                if (type.equals(Utils.TYPE_STUDENT))
                    intent.putExtra("title", "פגישה עם " + teacher);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(MeetingActivity.this, "אין לך אפליקציה שיכולה לשמור את התאריך", Toast.LENGTH_LONG).show();
                }

                btnAddToCal.setVisibility(View.GONE);
                break;

        }
    }

    public void updateUI(String file) {
        data = readFromFile(this, file);

        student = data.split("&&")[0];
        teacher = data.split("&&")[1];
        date = data.split("&&")[2];
        time = data.split("&&")[3];


        if(tempArr.length>0) {
            if(tempArr[0].equals("no")){
                mStorageRef.child("Meetings/").child("Upcoming/").listAll()
                        .addOnSuccessListener(listResult -> {
                            for (StorageReference item : listResult.getItems()) {
                                // All the items under listRef.
                                if (item.getName().contains(student)) {
                                    if (!item.getName().contains(teacher)) {
                                        meetingList.add(new Meeting(item.getName().split("&")[0], item.getName().split("&")[1].replace(".txt", ""), "0", "0"));
                                    }
                                }
                            }

                        })
                        .addOnFailureListener(e -> {
                            // Uh-oh, an error occurred!
                            Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                        });

            }else {
                for (int i = 0; i < tempArr.length; i++) {
                    if (!tempArr[i].equals("null"))
                        meetingList.add(new Meeting(tempArr[i].split("&")[0], tempArr[i].split("&")[1].replace(".txt", ""), "0", "0"));
                }
            }
        }

        tvTitle.setText(student + " - " + teacher);
        tvSubTitle.setText(data.split("&&")[2] + " - " + data.split("&&")[3]);

        tvTitleEdit.setText(student + " - " + teacher);
        tvDateEdit.setText(date);
        tvTimeEdit.setText(time);

        if (meetingMode == Utils.MODE_UPCOMING && ( (type.equals(Utils.TYPE_TEACHER) || (type.equals(Utils.TYPE_ADMIN)) ) && teacher.equals(name))) {
            tvHelper.setText("אנא הזן משוב על הפגישה. (אם אינך מעוניין שלח את המשוב ריק).");
            ipInput.setVisibility(View.VISIBLE);
            edtInput.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);

            Toolbar toolbar = findViewById(R.id.toolbar3);
            TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            mTitle.setText(student + " - " + teacher);

            btnEdit = (Button) toolbar.findViewById(R.id.btnEdit);
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(this);

            btnDelete = (Button) toolbar.findViewById(R.id.btnBin);
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(this);

            setSupportActionBar(toolbar);
        }


        if (meetingMode == Utils.MODE_DONE) {
            if (type.equals(Utils.TYPE_STUDENT)) {
                tvHelper.setText("אנא הזן משוב על הפגישה. (אם אינך מעוניין שלח את המשוב ריק).");
                ipInput.setVisibility(View.VISIBLE);
                edtInput.setVisibility(View.VISIBLE);
                btnSend.setVisibility(View.VISIBLE);
            }
            if (type.equals(Utils.TYPE_TEACHER)) {
                tvHelper.setText("הפגישה התקיימה");
            }
            if (type.equals(Utils.TYPE_ADMIN)) {
                if (teacher.equals(name)) {
                    tvHelper.setText("הפגישה התקיימה");
                } else {
                    tvHelper.setVisibility(View.GONE);
                    tMashov.setVisibility(View.VISIBLE);
                    tvTMashov.setVisibility(View.VISIBLE);
                    tvTMashov.setText(data.split("&&")[5]);
                }
            }
        }

        if (meetingMode == Utils.MODE_FINISHED) {
            if (type.equals(Utils.TYPE_TEACHER) || type.equals(Utils.TYPE_STUDENT)) {
                tvHelper.setText("הפגישה התקיימה");
            }
            if (type.equals(Utils.TYPE_ADMIN)) {
                if (teacher.equals(name)) {
                    tvHelper.setText("הפגישה התקיימה");
                } else {
                    tvHelper.setVisibility(View.GONE);
                    sMashov.setVisibility(View.VISIBLE);
                    tvSMashov.setVisibility(View.VISIBLE);
                    tMashov.setVisibility(View.VISIBLE);
                    tvTMashov.setVisibility(View.VISIBLE);
                    tvSMashov.setText(data.split("&&")[4]);
                    tvTMashov.setText(data.split("&&")[5]);
                }
            }
        }

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tvTimeEdit.getText().toString().split(":")[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(tvTimeEdit.getText().toString().split(":")[1]));
        cal.set(Calendar.YEAR, Integer.parseInt(tvDateEdit.getText().toString().split("/")[2]));
        cal.set(Calendar.MONTH, Integer.parseInt(tvDateEdit.getText().toString().split("/")[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tvDateEdit.getText().toString().split("/")[0]));


        long startTime = cal.getTimeInMillis();
        long endTime = startTime + Utils.MEETING_LENGTH * 60 * 1000;

        if (meetingMode == Utils.MODE_UPCOMING) {

            if ((type.equals(Utils.TYPE_TEACHER) && teacher.equals(name)) || (type.equals(Utils.TYPE_ADMIN) && teacher.equals(name))) {
                if (ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    if (!eventExistsOnCalendar("פגישה עם " + student, startTime, endTime)) {
                        btnAddToCal.setVisibility(View.VISIBLE);
                    }
                } else {
                    RequestStoragePermission2();
                }
            }

            if (type.equals(Utils.TYPE_STUDENT)) {
                if (ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    if (!eventExistsOnCalendar("פגישה עם " + teacher, startTime, endTime)) {
                        btnAddToCal.setVisibility(View.VISIBLE);
                    }
                } else {
                    RequestStoragePermission2();
                }
            }
        }

        try {
            pd.dismiss();
        } catch (Exception e) {
            // Uh-oh, an error occurred!
            Log.w("pd", "onFailure: ", e);
            Toast.makeText(getApplicationContext(), "אנא השאר את המסך אנכי", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * method used to download data files from firebase
     *
     * @param file- String containing the name of the file with all the meeting information
     */
    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file);

        String m = "";
        if (meetingMode == Utils.MODE_UPCOMING) m = "Upcoming";
        if (meetingMode == Utils.MODE_DONE) m = "Done";
        if (meetingMode == Utils.MODE_FINISHED) m = "Finished";

        mStorageRef.child("Meetings/" + m + "/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateUI(file);
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

                        StorageReference desertRef = mStorageRef.child("Meetings/Upcoming/" + fileName);

                        if (path.equals("Meetings/Finished/"))
                            desertRef = mStorageRef.child("Meetings/Done/" + fileName);

                        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // File deleted successfully
                                if (type.equals(Utils.TYPE_TEACHER) || (type.equals(Utils.TYPE_ADMIN) && teacher.equals(name))) {
                                    String eSubject = "משוב על שיחה אישית עם מורה - אמי" + "\"" + "ת מודיעין בנים";
                                    String eMessage = "הנך מתבקש לכתוב משוב קצר על הפגישה שהתקיימה בתאריך: " + date + ", בשעה: " + time + ", עם המורה " + teacher + ".";

                                    db.collection("users").whereEqualTo("name", student)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            Log.d(Utils.TAG, document.getId() + " => " + document.getData());

                                                            Utils.sendEmail(context, document.getId(), eSubject, eMessage);
                                                        }
                                                    } else {
                                                        Log.w(Utils.TAG, "Error getting documents.", task.getException());
                                                    }
                                                }
                                            });

                                    pd.dismiss();
                                    tvHelper.setText("המשוב נשלח בהצלחה!");
                                    ipInput.setVisibility(View.GONE);
                                    edtInput.setVisibility(View.GONE);
                                    btnSend.setVisibility(View.GONE);
                                    btnEdit.setVisibility(View.GONE);
                                    btnDelete.setVisibility(View.GONE);
                                    btnAddToCal.setVisibility(View.GONE);
                                } else {
                                    pd.dismiss();
                                    tvHelper.setText("המשוב נשלח בהצלחה!");
                                    ipInput.setVisibility(View.GONE);
                                    edtInput.setVisibility(View.GONE);
                                    btnSend.setVisibility(View.GONE);
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Uh-oh, an error occurred!
                                pd.dismiss();
                                Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w("Upload", "onSuccess: Upload failed", exception);
                        pd.dismiss();
                        Toast.makeText(getApplicationContext(), "אירעה שגיאה", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateFile(String fileName, String path) {
        Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(fileName));
        StorageReference riversRef = mStorageRef.child(path + fileName);

        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload", "onSuccess: Upload succeeded");
                        Toast.makeText(context, "הפגישה עודכנה", Toast.LENGTH_LONG).show();
                        pd.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w("Upload", "onSuccess: Upload failed", exception);
                        Toast.makeText(MeetingActivity.this, "אירעה שגיאה", Toast.LENGTH_LONG).show();
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

    public void RequestStoragePermission1() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CALENDAR)) {

            new AlertDialog.Builder(this)
                    .setTitle("נצרכת הרשאה")
                    .setMessage("ההרשאה הזאת נדרשת על מנת לעדכן את לוח השנה באשר לפגישות הנ\"ל")
                    .setPositiveButton("אוקיי", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MeetingActivity.this, new String[]{Manifest.permission.WRITE_CALENDAR}, Utils.STORAGE_PERMISSION_CODE1);

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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALENDAR}, Utils.STORAGE_PERMISSION_CODE1);
        }
    }

    public void RequestStoragePermission2() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {

            new AlertDialog.Builder(this)
                    .setTitle("נצרכת הרשאה")
                    .setMessage("ההרשאה הזאת נדרשת על מנת לעדכן את לוח השנה באשר לפגישות הנ\"ל")
                    .setPositiveButton("אוקיי", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MeetingActivity.this, new String[]{Manifest.permission.READ_CALENDAR}, Utils.STORAGE_PERMISSION_CODE2);

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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Utils.STORAGE_PERMISSION_CODE2);
        }
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

    public boolean eventExistsOnCalendar(String eventTitle, long startTimeMs, long endTimeMs) {
        if (eventTitle == null || "".equals(eventTitle)) {
            return false;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            RequestStoragePermission2();
        }

        // If no end time, use start + 1 hour or = 1 day. Query is slow if searching a huge time range
        if (endTimeMs <= 0) {
            endTimeMs = startTimeMs + 1000 * 60 * 60; // + 1 hour
        }

        final ContentResolver resolver = getContentResolver();
        final String[] duplicateProjection = {CalendarContract.Events.TITLE}; // Can change to whatever unique param you are searching for
        Cursor cursor =
                CalendarContract.Instances.query(
                        resolver,
                        duplicateProjection,
                        startTimeMs,
                        endTimeMs,
                        '"' + eventTitle + '"');

        if (cursor == null) {
            return false;
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            return false;
        }

        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            if (eventTitle.equals(title)) {
                cursor.close();
                return true;
            }
        }

        cursor.close();
        return false;
    }



    public void editMethod() {
        pd = ProgressDialog.show(context, "עדכון פגישה", "מעדכן פגישה...", true);
        pd.setCancelable(false);
        pd.show();

        time = tvTimeEdit.getText().toString();
        date = tvDateEdit.getText().toString();

        String data = student + "&&" + teacher + "&&" + date + "&&" + time + "&&&&&&";

        String fileName = student + "&" + teacher + ".txt";
        writeToFile(data, getApplicationContext(), fileName);

        tvSubTitle.setText(date + " - " + time);

        diaEdit.dismiss();

        Calendar cal = Calendar.getInstance();
        long endTime;
        long startTime;
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tvTimeEdit.getText().toString().split(":")[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(tvTimeEdit.getText().toString().split(":")[1]));
        cal.set(Calendar.YEAR, Integer.parseInt(tvDateEdit.getText().toString().split("/")[2]));
        cal.set(Calendar.MONTH, Integer.parseInt(tvDateEdit.getText().toString().split("/")[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tvDateEdit.getText().toString().split("/")[0]));

        startTime = cal.getTimeInMillis();
        endTime = startTime + Utils.MEETING_LENGTH * 60 * 1000;

        if (ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            int id = ListSelectedCalendars("פגישה עם " + student);
            UpdateCalendarEntry(id, startTime, endTime);

        } else if (!(ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)) {
            RequestStoragePermission1();
        } else {
            RequestStoragePermission2();
        }

        String eSubject = "שינוי זמן הפגישה - אמי\"ת מודיעין בנים";
        String eMessage = "הפגישה עם המורה " + teacher + ", הועברה לתאריך: " + date + ", בשעה: " + time + ".";

        db.collection("users").whereEqualTo("name", student)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(Utils.TAG, document.getId() + " => " + document.getData());

                            Utils.sendEmail(context, document.getId(), eSubject, eMessage);
                        }
                    } else {
                        Log.w(Utils.TAG, "Error getting documents.", task.getException());
                    }
                });

        updateFile(fileName, "Meetings/Upcoming/");
    }

    private void downloadMeetingFile(int i, String date, String time) {
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

    private void ifDifferentTime(String Date, String time, String otherDate, String otherTime) {
        if (otherDate.equals(Date)) {
            String otherHour = otherTime.split(":")[0];
            String otherMin = otherTime.split(":")[1];

            int totalMINs = Integer.parseInt(time.split(":")[0]) * 60 + Integer.parseInt(time.split(":")[1]);
            int otherTotalMIns = Integer.parseInt(otherHour) * 60 + Integer.parseInt(otherMin);

            if (Math.abs(totalMINs - otherTotalMIns) > Utils.MEETING_LENGTH) {
                editMethod();
            } else {
                Toast.makeText(context, "לתלמיד כבר יש פגישה בזמן הזה, שמתחילה בשעה " + otherTime, Toast.LENGTH_LONG).show();
            }
        } else {
           editMethod();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = null;
        if (pActivity.equals(Utils.ORG_INFO)) {
            intent = new Intent(this, InfoActivity.class);
            intent.putExtra(Utils.KEY_STUDENT_NAME, sName);
            intent.putExtra(Utils.KEY_MEETING_COUNT, meetCount);
            intent.putExtra(Utils.KEY_CLASS_NAME, className);
        }
        if (pActivity.equals(Utils.ORG_MEETINGS))
            intent = new Intent(this, MeetingsActivity.class);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        diaEdit.dismiss();
    }
}