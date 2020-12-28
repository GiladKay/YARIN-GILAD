package com.yg.amit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
    private String data;       //String containing data from the chosen class file

    private boolean hasBeenEdited=false; // remembers if an edit to the students has occurred

    private TextView tvTitle;   // Title of the activity
    private TextView tvSName, tvMeetCount;

    private ProgressDialog pd;

    private StorageReference mStorageRef;

    private SharedPreferences sp;
    private String name;
    private String type;

    private String className;

    private int tHour, tMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Bundle extras = getIntent().getExtras();
        className = extras.getString(ClassesActivity.CLASS_NAME_KEY); //fetching the class name from the Intents Extra

        sp=getSharedPreferences(Menu.AMIT_SP,MODE_PRIVATE);
        name=sp.getString(Menu.NAME_KEY,"name");
        type=sp.getString(Menu.TYPE_KEY,"student");

        mStorageRef = FirebaseStorage.getInstance().getReference();

        arrMeeting = new Dialog(this);                               //Initializing meeting arrangement dialog
        arrMeeting.setContentView(R.layout.meeting_arrangement_dialog);

        tvMeetCount = (TextView) arrMeeting.findViewById(R.id.tvMeetings);
        tvSName = (TextView) arrMeeting.findViewById(R.id.tvStudentName);
        tvTime = (TextView) arrMeeting.findViewById(R.id.tvTime2);
        tvDate = (TextView) arrMeeting.findViewById(R.id.tvDate2);
        btnCreate = (MaterialButton) arrMeeting.findViewById(R.id.btnCreate);

        lvS = (ListView) findViewById(R.id.lvStudents);
        tvTitle = (TextView) findViewById(R.id.tvClassTitle);

        tvTitle.setText(className);

        pd = ProgressDialog.show(this, className, "מוריד נתונים...", true);
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
                        if (item.getName().contains(className))   //locating the file that has the same name as the class we clicked on
                            downloadFile(item.getName());
                    }

                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getClass", "onFailure: ", e);
                });

        // all the listeners used for the time and date arrangement in a new meeting
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

        tvDate.setOnClickListener(new View.OnClickListener() {         //Dialog for the user to choose a date for the meeting
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance(); //get the date of the current day
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

        tvTime.setOnClickListener(new View.OnClickListener() {         //Dialog for the user to choose a time for the meeting
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        StudentsActivity.this,
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

    }

    /**
     * method used to download data files from firebase
     * @param file- String containing the name of the file with all the students
     */
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

    /**
     * Initializes the studentList and fills it with all the Students read from the appropriate file
     * sets an Item listener so that when a student is clicked, the meeting arrangement dialog is shown
     * @param file- name of the class file
     */
    private void updateClass(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateClass: " + data);

        studentList = new ArrayList<Student>();

        for (int i = 0; i < data.split("&&").length - 1; i++) {
            studentList.add(new Student(data.split("&&")[i].split("==")[0],
                    Integer.parseInt(data.split("&&")[i].split("==")[1])));
        }

        studentAdapter = new StudentAdapter(this, 0, 0, studentList);
        lvS.setAdapter(studentAdapter);


        lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if(studentList.get(i).getMeetingCount()<2&&type.equals("teacher") ) {

                    tvSName.setText(studentList.get(i).getName() + " ");
                    tvMeetCount.setText(studentList.get(i).getMeetingCount() + "/2 ");

                    btnCreate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            String time = tvTime.getText().toString();
                            String Date = tvDate.getText().toString();
                            if (!time.isEmpty() && !Date.isEmpty()) {
                                //TODO send an email to the student

                                studentList.get(i).incMeetingCount();
                                hasBeenEdited = true;
                                lvS.setAdapter(studentAdapter);

                                createMeeting(studentList.get(i).getName(),time,Date);

                                Toast.makeText(getApplicationContext(), "time: " + time + "Date: " + Date, Toast.LENGTH_LONG).show();
                                arrMeeting.hide();
                            } else {
                                Toast.makeText(getApplicationContext(), "יש למלא את כל השדות " + Date, Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    arrMeeting.show();

                }
            }
        });
    }

    /**
     * reads and outputs the contents of the now local Class file
     * @param context-this
     * @param file- name of the class file
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



    private void uploadFile(String fileName,String path) {
        Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(fileName));
        StorageReference riversRef = mStorageRef.child(path+ fileName);

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

    private void writeToFile(String data,Context context, String file) {


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file,MODE_PRIVATE)); // APPEND OR PRIVATE
            outputStreamWriter.append(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

     private void createMeeting(String studentName,String time,String Date){
        writeToFile(studentName+"&&"+name+"&&"+Date+"&&"+time+"&&",this,name+" - "+studentName+".txt");
        uploadFile(name+ " - "+studentName+".txt","Meetings/Upcoming/");
     }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(hasBeenEdited){//if a meetings was booked

            String data="";
            for(Student student:studentList){
                data+=student.getName()+"=="+student.getMeetingCount()+"&&" ;
            }
            className+=".txt";
            writeToFile(data,this,className);//update the meeting counter
            uploadFile(className,"Classes/");//


        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), ClassesActivity.class));
        finish();
    }
}