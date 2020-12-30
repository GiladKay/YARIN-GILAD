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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
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

public class MeetingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private String name, type;

    private ArrayList<Meeting> meetingList;
    private ListView lv;
    private MeetingAdapter meetingAdapter;
    private String data;
    private Dialog editMeet;
    private MaterialButton btnEdit;

    private Dialog sendMashov;
    private EditText ETM;//edit text mashov
    private MaterialButton btnSend;


    private TextView tvTitle;   // Title of the activity

    private TextView tvSName, tvMeetCount,tvDiaTitle;
    private TextView tvDate, tvTime;

    private boolean hasBeenEdited;

    private StorageReference mStorageRef;

    private ProgressDialog pd;

    private int tHour, tMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        hasBeenEdited=false;

        editMeet = new Dialog(this);                               //initializing Dialog for altering meeting data
        editMeet.setContentView(R.layout.meeting_arrangement_dialog);
        editMeet.setCanceledOnTouchOutside(true);
        tvMeetCount = (TextView) editMeet.findViewById(R.id.tvMeetings);
        tvSName = (TextView) editMeet.findViewById(R.id.tvStudentName);
        tvDiaTitle = (TextView) editMeet.findViewById(R.id.tvTitle3);
        tvTime = (TextView) editMeet.findViewById(R.id.tvTime2);
        tvDate = (TextView) editMeet.findViewById(R.id.tvDate2);
        btnEdit = (MaterialButton) editMeet.findViewById(R.id.btnCreate);

        sendMashov=new Dialog(this);
        sendMashov.setContentView(R.layout.mashov_dialog);
        ETM=(EditText)sendMashov.findViewById(R.id.ETMashov);
        btnSend=(MaterialButton)sendMashov.findViewById(R.id.btnSend);


        sharedPreferences = getSharedPreferences(Menu.AMIT_SP, MODE_PRIVATE);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        meetingList = new ArrayList<Meeting>();

        lv = (ListView) findViewById(R.id.lv);

        name = sharedPreferences.getString(Menu.NAME_KEY, "name");            //getting the users name
        type = sharedPreferences.getString(Menu.TYPE_KEY, "student");         // confirming the user type (student,teacher,admin)

        pd = ProgressDialog.show(this, "פגישות", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();

        mStorageRef.child("Meetings/Upcoming").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        // All the prefixes under listRef.
                        // You may call listAll() recursively on them.
                    }

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (item.getName().contains(name) || type.equals("admin")) { // if the meeting as connected to the user (contains his name) or if the user is an admin
                            downloadFile(item.getName());
                        }

                    }

                    if (listResult.getItems().isEmpty())
                        Toast.makeText(this, "אין פגישות קרובות!", Toast.LENGTH_LONG).show();

                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getMeetings", "onFailure: ", e);
                });

        if (type.equals("teacher")) {//if user is of type teacher he may edit the meeting

            DatePickerDialog.OnDateSetListener mDateSetListener;

            mDateSetListener=new DatePickerDialog.OnDateSetListener() {
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

            tvDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    DatePickerDialog dialog = new DatePickerDialog(
                            MeetingsActivity.this,
                            android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                            mDateSetListener,
                            Integer.parseInt(tvDate.getText().toString().substring(6,10)),Integer.parseInt(tvDate.getText().toString().substring(3,5)), Integer.parseInt(tvDate.getText().toString().substring(0,2)));

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



                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        tvSName.setText(meetingList.get(i).getStudent());
                        tvDate.setText(meetingList.get(i).getDate());
                        tvTime.setText(meetingList.get(i).getTime());
                        tvDiaTitle.setText((" ערוך פגישה"));

                        TextView num1 = (TextView) editMeet.findViewById(R.id.text3);
                        num1.setText("");
                        TextView num2 = (TextView) editMeet.findViewById(R.id.tvMeetings);
                        num2.setText("");

                        btnEdit.setText("שמור שינויים");
                        btnEdit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                String time = tvTime.getText().toString();
                                String Date = tvDate.getText().toString();


                                Meeting meeting=meetingList.get(i);

                                meeting.setDate(Date);
                                meeting.setTime(time);
                                lv.setAdapter(meetingAdapter);

                                String data=meeting.getStudent()+"&&"+meeting.getTeacher()+"&&"+Date+"&&"+time+"&&";

                                String fileName=meeting.getTeacher()+" - "+meeting.getStudent()+".txt";
                                writeToFile(data,getApplicationContext(),fileName);//update the meeting counter
                                uploadFile(fileName,"Meetings/Upcoming/");//

                                Toast.makeText(getApplicationContext(), "time: " + time + " Date: " + Date, Toast.LENGTH_LONG).show();
                                    editMeet.hide();

                            }
                        });


                        MaterialButton done = (MaterialButton) editMeet.findViewById(R.id.btnMeetingDone);
                        done.setVisibility(View.VISIBLE);
                        done.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                sendMashov.getWindow().setLayout(700,900);
                                sendMashov.show();

                                btnSend.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        String mashov=ETM.getText().toString();

                                        String fileName=meetingList.get(i).getTeacher()+" - "+meetingList.get(i).getStudent()+".txt";

                                        StorageReference desertRef = mStorageRef.child("Meetings/Upcoming/"+fileName);

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

                                        writeToFile(mashov,getApplicationContext(),fileName);//update the meeting counter
                                        uploadFile(fileName,"Meetings/Done/");//

                                        Toast.makeText(getApplicationContext(),"המשוב על הפגישה עם "+meetingList.get(i).getStudent()+" נשלח בהצלחה",Toast.LENGTH_LONG).show();
                                        meetingList.remove(i);
                                        lv.setAdapter(meetingAdapter);
                                        sendMashov.hide();
                                        editMeet.hide();
                                    }
                                });

                                //TODO remove meeting from teacher and students feed, add the mashov to the meeting's file, and move it to a location where we will store all the meetings that were done
                                //TODO add one to the meeting count of the teacher

                            }
                        });


                        editMeet.show();
                    }
                });
            }
        }


    /**
     * method used to download data files from firebase
     * @param file- String containing the name of the file with all the meeting information
     */
    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file);

        mStorageRef.child("Meetings/Upcoming/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateMeeting(file);
                }).addOnFailureListener(exception -> {
            // Handle failed download
            Log.w("Download", "onFailure: Download failed", exception);
        });

    }

    /**
     * Initializes the meetingList and fills it with all the meetings read from the appropriate file
     * @param file- name of the meeting file
     */
    private void updateMeeting(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateMeeting: " + data);

        Meeting meeting = new Meeting(data.split("&&")[0],data.split("&&")[1], data.split("&&")[2], data.split("&&")[3]);//show time, date , and name of student

        meetingList.add(meeting);

        meetingAdapter = new MeetingAdapter(this, 0, 0, meetingList);
        lv.setAdapter(meetingAdapter);
    }


    /**
     * reads and outputs the contents of the now local meeting file
     * @param context-this
     * @param file- name of the meeting file
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



    private void writeToFile(String data,Context context, String file) {


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file,MODE_PRIVATE)); // APPEND OR PRIVATE
            outputStreamWriter.append(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void uploadFile(String fileName,String path) {
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



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }
}