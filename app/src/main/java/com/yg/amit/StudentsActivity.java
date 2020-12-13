package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class StudentsActivity extends AppCompatActivity implements View.OnClickListener {

    private Dialog arrMeeting;  //dialog for arranging a meeting
    private ListView lvS;       // listView for students
    private TextView tvTitle;   // Title of the activity
    private ArrayList<Student> studentList;
    private TextInputLayout ipDate, ipTime;
    private TextInputEditText edtDate, edtTime;
    private MaterialButton btnCreate;
    private TextView tvMeetCount;
    private TextView tvSName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();

        Bundle extras = getIntent().getExtras();
        String className= extras.getString(ClassesActivity.CLASS_NAME_KEY);

        arrMeeting= new Dialog(this);
        arrMeeting.setContentView(R.layout.meeting_arrangement_dialog);

        ipDate=(TextInputLayout)arrMeeting.findViewById(R.id.ipDate);
        ipTime=(TextInputLayout)arrMeeting.findViewById(R.id.ipTime);

        edtDate=(TextInputEditText)arrMeeting.findViewById(R.id.edtDate);
        edtTime=(TextInputEditText)arrMeeting.findViewById(R.id.edtTime);

        tvMeetCount=(TextView)arrMeeting.findViewById(R.id.tvMeetings);
        tvSName=(TextView)arrMeeting.findViewById(R.id.tvStudentName);

        btnCreate=(MaterialButton)arrMeeting.findViewById(R.id.btnCreate);

        lvS=(ListView)findViewById(R.id.lvStudents);
        tvTitle=(TextView)findViewById(R.id.tvClassTitle);

        tvTitle.setText(className);

        studentList=new ArrayList<Student>();

        //TODO create students from file that has the same name as String className
        studentList.add(new Student("bob",1));
        studentList.add(new Student("jon",0));
        studentList.add(new Student("lennon",0));
        studentList.add(new Student("yarin",2));

        StudentAdapter adapter=new StudentAdapter(this,studentList);
        lvS.setAdapter(adapter);

        lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO make the edit text force the user to enter date with dd/mm/yy format and time with mm/hh format
                //TODO make sure fields are not left empty
                tvSName.setText(studentList.get(i).getName()+" ");
                tvMeetCount.setText(studentList.get(i).getMeetingCount()+"/2 ");
                edtDate.setText("");
                edtTime.setText("");

                btnCreate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                            String date = edtDate.getText().toString();
                            String time = edtTime.getText().toString();
                            //TODO create a meeting in a file with the date and time and sent email
                            //TODO add one to the students meeting count(update in file) and update list accordingly- lvS.setAdapter(adapter);
                            arrMeeting.hide();

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