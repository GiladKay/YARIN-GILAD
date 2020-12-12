package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class StudentsActivity extends AppCompatActivity {

    ListView lvS;//list view for students
    TextView tvTitle; //title of the activity
    ArrayList<Student> studentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();

        Bundle extras = getIntent().getExtras();
        String className= extras.getString(ClassesActivity.CLASS_NAME_KEY);

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
                //TODO creates a dialog where you set up a meeting with a student and see stats about him
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), ClassesActivity.class));
        finish();
    }
}