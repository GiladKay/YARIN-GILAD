package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StudentsActivity extends AppCompatActivity implements View.OnClickListener {

    private Dialog arrMeeting;  //dialog for arranging a meeting
    private TextView tvDate, tvTime;
    private Button btnDate, btnTime;
    private MaterialButton btnCreate;

    private ListView lvS;       // listView for students
    private TextView tvTitle;   // Title of the activity
    private ArrayList<Student> studentList;

    private TextView tvSName, tvMeetCount;

    int tHour, tMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Bundle extras = getIntent().getExtras();
        String className = extras.getString(ClassesActivity.CLASS_NAME_KEY);

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

        studentList = new ArrayList<Student>();

        //TODO create students from file that has the same name as String className
        studentList.add(new Student("bob", 1));
        studentList.add(new Student("jon", 0));
        studentList.add(new Student("lennon", 0));
        studentList.add(new Student("yarin", 2));

        StudentAdapter adapter = new StudentAdapter(this, 0, 0, studentList);
        lvS.setAdapter(adapter);

        lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO make the edit text force the user to enter date with dd/mm/yy format and time with mm/hh format
                //TODO make sure fields are not left empty
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


    @Override
    public void onClick(View view) {

    }
}