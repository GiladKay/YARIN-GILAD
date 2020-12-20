package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class StudentsActivity extends AppCompatActivity {

    private Dialog arrMeeting;  //dialog for arranging a meeting
    private TextView tvDate, tvTime;
    private MaterialButton btnCreate;

    private ListView lvS;       // listView for students
    private StudentAdapter studentAdapter;
    private ArrayList<Student> studentList;
    private String data;

    private TextView tvTitle;   // Title of the activity
    private TextView tvSName, tvMeetCount;

    private ProgressDialog pd;

    private StorageReference mStorageRef;

    private int tHour, tMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Bundle extras = getIntent().getExtras();
        String className = extras.getString(ClassesActivity.CLASS_NAME_KEY);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        arrMeeting = new Dialog(this);
        arrMeeting.setContentView(R.layout.meeting_arrangement_dialog);

        tvMeetCount = (TextView) arrMeeting.findViewById(R.id.tvMeetings);
        tvSName = (TextView) arrMeeting.findViewById(R.id.tvStudentName);

        tvTime = (TextView) arrMeeting.findViewById(R.id.tvTime2);
        tvDate = (TextView) arrMeeting.findViewById(R.id.tvDate2);

        DatePickerDialog.OnDateSetListener mDateSetListener;

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                Log.d("TAG", "onDateSet: mm/dd/yyy: " + month + "/" + day + "/" + year);

                String date = day + "/" + month + "/" + year;
                tvDate.setText(date);
            }
        };

        tvDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        StudentsActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        mDateSetListener,
                        year, month, day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        tvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        StudentsActivity.this,
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

        btnCreate = (MaterialButton) arrMeeting.findViewById(R.id.btnCreate);

        lvS = (ListView) findViewById(R.id.lvStudents);
        tvTitle = (TextView) findViewById(R.id.tvClassTitle);

        tvTitle.setText(className);

        ProgressDialog newPd = ProgressDialog.show(this, className, "מוריד נתונים...", true);
        pd = newPd;
        pd.setCancelable(false);
        pd.show();

        mStorageRef.child("Classes").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        // All the prefixes under listRef.
                        // You may call listAll() recursively on them.
                    }

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (item.getName().contains(className))
                            downloadFile(item.getName());
                    }

                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getClass", "onFailure: ", e);
                });
    }

    private void updateClass(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateClass: " + data);

        studentList = new ArrayList<Student>();

        for (int i = 0; i < data.split("&&").length - 1; i++) {
            studentList.add(new Student(data.split("&&")[i].split("==")[0],
                    Integer.parseInt(data.split("&&")[i].split("==")[1])));
        }

        StudentAdapter studentAdapter = new StudentAdapter(this, 0, 0, studentList);
        lvS.setAdapter(studentAdapter);

        lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                tvSName.setText(studentList.get(i).getName() + " ");
                tvMeetCount.setText(studentList.get(i).getMeetingCount() + "/2 ");

                btnCreate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String time = tvTime.getText().toString();
                        String Date = tvDate.getText().toString();
                        if (!time.isEmpty() && !Date.isEmpty()) {
                            //TODO create a meeting in a file with the date and time and sent email
                            //TODO add one to the students meeting count(update in file) and update list accordingly- lvS.setAdapter(adapter);
                            Toast.makeText(getApplicationContext(), "time: " + time + "Date: " + Date, Toast.LENGTH_LONG).show();
                            arrMeeting.hide();
                        } else {
                            Toast.makeText(getApplicationContext(), "יש למלא את כל השדות " + Date, Toast.LENGTH_LONG).show();
                        }
                    }
                });

                arrMeeting.show();

            }
        });
    }

    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file);

        mStorageRef.child("Classes/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateClass(file);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }
}