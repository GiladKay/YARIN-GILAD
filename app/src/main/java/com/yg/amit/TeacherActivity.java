package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

public class TeacherActivity extends AppCompatActivity {

    private ListView lvT;
    private ArrayList<Teacher> teacherList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();

        lvT=(ListView)findViewById(R.id.lvTeachers);

        teacherList=new ArrayList<Teacher>();

        teacherList.add(new Teacher("Chaim",1));
        teacherList.add(new Teacher("Simon",6));
        teacherList.add(new Teacher("flemming",3));
        teacherList.add(new Teacher("larry",4));

        TeacherAdapter teacherAdapter=new TeacherAdapter(this,teacherList);
        lvT.setAdapter(teacherAdapter);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }
}