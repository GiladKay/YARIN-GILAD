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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
import java.util.Calendar;
import java.util.Date;

public class MeetingActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;

    private String type;

    private Dialog diaEdit;
    private TextView tvTitleEdit;
    private TextView tvTimeEdit, tvDateEdit;
    private Button btnUpdateEdit;

    private String student,teacher, date , time;


    private TextView tvTitle, tvSubTitle, tvHelper;
    private TextInputLayout ipInput;
    private TextInputEditText edtInput;
    private Button btnSend;
    private Button btnEdit;

    private CardView sMashov, tMashov;
    private TextView tvSMashov, tvTMashov;

    private ProgressDialog pd;

    private String meetingFile;
    private int meetingMode;
    private int pos;

    private String data;

    private StorageReference mStorageRef;
    private FirebaseFirestore db;

    private int tHour, tMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        mStorageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        type = sharedPreferences.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);


        Bundle extras = getIntent().getExtras();
        meetingFile = extras.getString("Meeting");
        meetingMode = extras.getInt("Mode");
        pos= extras.getInt("position");

        diaEdit = new Dialog(this);
        diaEdit.setContentView(R.layout.edit_meeting_dialog);
        diaEdit.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        tvTitleEdit = (TextView) diaEdit.findViewById(R.id.tvTitle);
        tvTimeEdit = (TextView) diaEdit.findViewById(R.id.tvEditTime);
        tvDateEdit = (TextView) diaEdit.findViewById(R.id.tvEditDate);
        btnUpdateEdit = (Button) diaEdit.findViewById(R.id.btnUpdate);
        btnUpdateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String time = tvTimeEdit.getText().toString();
                String Date = tvDateEdit.getText().toString();


                //lv.setAdapter(meetingAdapter);

                String data =  student+ "&&" + teacher + "&&" + Date + "&&" + time + "&&";

                String fileName = student + "&" + teacher + ".txt";
                writeToFile(data, getApplicationContext(), fileName);//update the meeting counter
                updateFile(fileName, "Meetings/Upcoming/");//

                tvSubTitle.setText(Date + " - "+ time);

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
                endTime = startTime + 30 * 60 * 1000;

                if (ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    int id = ListSelectedCalendars("פגישה עם " + student);
                    UpdateCalendarEntry(id, startTime, endTime);

                } else if(!(ContextCompat.checkSelfPermission(MeetingActivity.this, Manifest.permission.WRITE_CALENDAR)== PackageManager.PERMISSION_GRANTED )){
                    RequestStoragePermission1();
                } else{
                    RequestStoragePermission2();
                }

                Toast.makeText(getApplicationContext(), "time: " + time + " Date: " + Date, Toast.LENGTH_LONG).show();


            }
        });


        tvTimeEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        MeetingActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int i, int i1) {
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
                            }
                        }, 24, 0, true);
                timePickerDialog.updateTime(tHour, tMinute);
                timePickerDialog.show();

            }
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

                tvDateEdit.setText(date);
            }
        };

        tvDateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatePickerDialog dialog = new DatePickerDialog(
                        MeetingActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        mDateSetListener,
                        Integer.parseInt(tvDateEdit.getText().toString().substring(6, 10)), Integer.parseInt(tvDateEdit.getText().toString().substring(3, 5)), Integer.parseInt(tvDateEdit.getText().toString().substring(0, 2)));

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });



        tvTitle = findViewById(R.id.tvTitle);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        tvHelper = findViewById(R.id.tvHelper);
        ipInput = findViewById(R.id.ipInput);
        edtInput = findViewById(R.id.edtInput);
        btnSend = findViewById(R.id.btnSend);
        btnEdit = findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(this);
        sMashov = findViewById(R.id.sMashov);
        tMashov = findViewById(R.id.tMashov);
        tvSMashov = findViewById(R.id.tvSMashov);
        tvTMashov = findViewById(R.id.tvTMashov);
        btnSend.setOnClickListener(this);




        pd = ProgressDialog.show(this, "פגישה", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();
        downloadFile(meetingFile);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSend) {
            pd = ProgressDialog.show(this, "פגישה", "שולח משוב...", true);
            pd.setCancelable(false);
            pd.show();

            String mashov = edtInput.getText().toString().trim();

            if(type.equals(Utils.TYPE_STUDENT)) {
                String newData = data.split("&&")[0] + "&&" + data.split("&&")[1] + "&&" + data.split("&&")[2] + "&&"
                        + data.split("&&")[3] + "&&" + mashov + "&&" + data.split("&&")[5] + "&&";
                writeToFile(newData, this, meetingFile);
                uploadFile(meetingFile, "Meetings/Finished/");
            }
            if(type.equals(Utils.TYPE_TEACHER)) {
                String newData = data.split("&&")[0] + "&&" + data.split("&&")[1] + "&&" + data.split("&&")[2] + "&&"
                        + data.split("&&")[3] + "&&" + data.split("&&")[4] + "&&" + mashov + "&&";
                writeToFile(newData, this, meetingFile);
                uploadFile(meetingFile, "Meetings/Done/");
            }

            String eSubject = "משוב על שיחה אישית עם מורה - אמ" + "\"" + "ית מודיעין בנים";
            String eMessage = "הנך מתבקש לכתוב משוב קצר על הפגישה שהתקיימה בתאריך: " + date + ", בשעה: " + time + ", עם המורה " + teacher + ".";


            db.collection("users").whereEqualTo("name", student)
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
        }

        if(v.getId() == R.id.btnEdit){
            diaEdit.show();
        }

    }

    public void updateUI(String file) {
        data = readFromFile(this, file);

        student=data.split("&&")[0];
        teacher=data.split("&&")[1];
        date=data.split("&&")[2];
        time=data.split("&&")[3];

        tvTitle.setText(teacher + " - " + student);
        tvSubTitle.setText(data.split("&&")[2] + " - " + data.split("&&")[3]);

        tvTitleEdit.setText(teacher + " - " + student);
        tvDateEdit.setText(date);
        tvTimeEdit.setText(time);

        if (meetingMode == Utils.MODE_UPCOMING && type.equals(Utils.TYPE_TEACHER)) {

                tvHelper.setText("אנא הזן משוב על הפגישה. (אם אינך מעוניין שלח את המשוב ריק).");
                ipInput.setVisibility(View.VISIBLE);
                edtInput.setVisibility(View.VISIBLE);
                btnSend.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.VISIBLE);
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
                tvHelper.setVisibility(View.GONE);
                tMashov.setVisibility(View.VISIBLE);
                tvTMashov.setVisibility(View.VISIBLE);
                tvTMashov.setText(data.split("&&")[5]);
            }
        }

        if (meetingMode == Utils.MODE_FINISHED) {
            if (type.equals(Utils.TYPE_TEACHER) || type.equals(Utils.TYPE_STUDENT)) {
                tvHelper.setText("הפגישה התקיימה");
            }
            if (type.equals(Utils.TYPE_ADMIN)) {
                tvHelper.setVisibility(View.GONE);
                sMashov.setVisibility(View.VISIBLE);
                tvSMashov.setVisibility(View.VISIBLE);
                tMashov.setVisibility(View.VISIBLE);
                tvTMashov.setVisibility(View.VISIBLE);
                tvSMashov.setText(data.split("&&")[4]);
                tvTMashov.setText(data.split("&&")[5]);
            }
        }

        pd.dismiss();
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
                    Toast.makeText(getApplicationContext(),"אירעה שגיאה", Toast.LENGTH_LONG).show();
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

                        if(path.equals("Meetings/Finished/")) desertRef = mStorageRef.child("Meetings/Done/" + fileName);

                        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // File deleted successfully
                                // TODO send email if type.equals("teacher")
                                pd.dismiss();
                                tvHelper.setText("המשוב נשלח בהצלחה!");
                                ipInput.setVisibility(View.GONE);
                                edtInput.setVisibility(View.GONE);
                                btnSend.setVisibility(View.GONE);
                                btnEdit.setVisibility(View.GONE);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Uh-oh, an error occurred!
                                pd.dismiss();
                                Toast.makeText(getApplicationContext(),"אירעה שגיאה", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(getApplicationContext(),"אירעה שגיאה", Toast.LENGTH_LONG).show();
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
    public void onBackPressed() {
        Intent intent = new Intent(this,MeetingsActivity.class);
        intent.putExtra("pos",pos);
        startActivity(intent);
        finish();
    }
}