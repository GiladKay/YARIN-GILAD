package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ClassesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    ListView lvClass; //ListView for Classes
    ArrayList<String> classes;// array list full of the names of the classes for the listview
    public static final String CLASS_NAME_KEY="class";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();


        lvClass=(ListView)findViewById(R.id.lvClasses);

        //TODO fill the array list with classes from a file
        classes=new ArrayList<String>();
        classes.add("z1");
        classes.add("z2");
        classes.add("z3");
        classes.add("z4");
        //TODO fill the array list with classes from a file

        ArrayAdapter<String> cAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,classes);



        lvClass.setAdapter(cAdapter);

        lvClass.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String classname =classes.get(i);

        Intent intent=new Intent(getBaseContext(), StudentsActivity.class);
        intent.putExtra(CLASS_NAME_KEY,classname);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();

    }
}