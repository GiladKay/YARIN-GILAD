package com.yg.amit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
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
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class StudentsActivity extends AppCompatActivity {

    private Dialog arrMeeting;  //dialog for arranging a meeting
    private TextView tvDate, tvTime;
    private MaterialButton btnCreate;
    private Switch switchCalen; //Switch for calendar save

    private  SharedPreferences sd;

    public static final String SWITCH_STATE="switchState";

    private ListView lvS;       // listView for students
    private StudentAdapter studentAdapter;
    private ArrayList<Student> studentList;
    private String data;       //String containing data from the chosen class file

    private boolean hasBeenEdited=false; // remembers if an edit to the students has occurred

    private TextView tvSName, tvMeetCount;

    private ProgressDialog pd;

    private StorageReference mStorageRef;

    private SharedPreferences sp;
    private String name;
    private String type;

    private String className;

    private int tHour, tMinute;





    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.student_menu,menu);


        MenuItem menuItem=menu.findItem(R.id.search);
        menuItem.setVisible(true);
        SearchView searchView =(SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setQueryHint("חפש תלמיד");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                studentAdapter.getFilter().filter(s);
                return false;
            }


        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()== R.id.exit)
                finish();
        return true;
    }

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

        sd=getSharedPreferences(Menu.AMIT_SP,MODE_PRIVATE);
        switchCalen=(Switch)arrMeeting.findViewById(R.id.SwitchSave);
        switchCalen.setChecked(sd.getBoolean(SWITCH_STATE,false));

        lvS = (ListView) findViewById(R.id.lvStudents);


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

                String d=""+day;
                if(day<10){
                    d="0"+day;
                }
                String m=""+month;
                if(month<10){
                    m="0"+month;
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


        Toolbar toolbar=findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("כיתה "+className);
        setSupportActionBar(toolbar);

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

        studentAdapter = new StudentAdapter(this,  studentList);
        lvS.setAdapter(studentAdapter);




        lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Student student=((Student) studentAdapter.getItem(i));

                switchCalen.setVisibility(View.VISIBLE);

                if(student.getMeetingCount()<2&&type.equals("teacher") ) {

                    tvSName.setText(student.getName() + " ");
                    tvMeetCount.setText(student.getMeetingCount() + "/2 ");

                    btnCreate.setOnClickListener(new View.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onClick(View view) {

                            String time = tvTime.getText().toString();
                            String Date = tvDate.getText().toString();
                            if (!time.isEmpty() && !Date.isEmpty()) {
                                //TODO send an email to the student

                                student.incMeetingCount();

                                hasBeenEdited = true;
                                lvS.setAdapter(studentAdapter);

                                createMeeting(student.getName(),time,Date);


                                switchCalen.setVisibility(View.GONE);

                                if(switchCalen.isChecked()){
                                    Calendar cal = Calendar.getInstance();
                                    long endTime;
                                    long startTime=0;
                                    cal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(tvTime.getText().toString().split(":")[0]));
                                    cal.set(Calendar.MINUTE,Integer.parseInt(tvTime.getText().toString().split(":")[1]));


                                    cal.set(Calendar.YEAR,Integer.parseInt(tvDate.getText().toString().split("/")[2]));
                                    cal.set(Calendar.MONTH,Integer.parseInt(tvDate.getText().toString().split("/")[1])-1);
                                    cal.set(Calendar.DAY_OF_MONTH,Integer.parseInt(tvDate.getText().toString().split("/")[0]));

                                    endTime=cal.getTimeInMillis()+30*60*1000;
                                    Date d = cal.getTime();

                                    startTime=d.getTime();
                                    startTime=cal.getTimeInMillis();

                                    Intent intent = new Intent(Intent.ACTION_EDIT);
                                    intent.setType("vnd.android.cursor.item/event");
                                    intent.putExtra("beginTime",startTime);
                                    intent.putExtra("rrule", "FREQ=YEARLY");
                                    intent.putExtra("endTime", endTime);
                                    intent.putExtra("title", "פגישה עם "+student.getName());
                                    if(intent.resolveActivity(getPackageManager())!=null){
                                        startActivity(intent);
                                    }else{
                                        Toast.makeText(StudentsActivity.this,"אין לך אפליקציה שיכולה לשמור את התאריך",Toast.LENGTH_LONG).show();
                                    }
                                }
                                Toast.makeText(getApplicationContext(), "פגישה עם "+student.getName()+" בתאריך: "+Date+ " בשעה: "+time, Toast.LENGTH_LONG).show();
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
        writeToFile(studentName+"&&"+name+"&&"+Date+"&&"+time+"&&",this,studentName+"&"+name+".txt");
        uploadFile(studentName+"&"+name+".txt","Meetings/Upcoming/");
     }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(hasBeenEdited){//if a meetings was booked
            //update the number of meetings a student has in file
            updateMeetingCount();
        }

        SharedPreferences.Editor editor=sd.edit();
        editor.putBoolean(SWITCH_STATE,switchCalen.isChecked());
        editor.commit();
    }

    public void updateMeetingCount(){

        //TODO fix the method so the file is uploaded only after the lambda is finished


        for(int i=0;i<studentList.size();i++){
            Student student=studentList.get(i);
            student.setMeetingCount(0);


            mStorageRef.child("Meetings/").child("Upcoming/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            if (item.getName().contains(student.getName())) {
                                student.incMeetingCount();
                            }
                        }

                    });
            mStorageRef.child("Meetings/").child("Done/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            if (item.getName().contains(student.getName())) {
                                student.incMeetingCount();
                            }
                        }

                    });
            mStorageRef.child("Meetings/").child("Finished/").listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            // All the prefixes under listRef.
                            // You may call listAll() recursively on them.
                        }

                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
                            if (item.getName().contains(student.getName())) {
                                student.incMeetingCount();
                            }
                        }

                    });

        }

                String data="";

                for(Student student:studentList){
                    data+=student.getName()+"=="+student.getMeetingCount()+"&&" ;
                }


                className+=".txt";
                writeToFile(data,getBaseContext(),className);//update the meeting counter
                uploadFile(className,"Classes/");//

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), ClassesActivity.class));
        finish();
    }

}

