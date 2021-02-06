package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class ClassesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView lvClass;               //ListView for Classes
    private ArrayList<Class> classList;     // Array list for the listView
    private String data;                    // String containing data from the classes file

    private StorageReference mStorageRef;

    private ProgressDialog pd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Toolbar toolbar=findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("רשימת כיתות");
        setSupportActionBar(toolbar);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        lvClass = (ListView) findViewById(R.id.lvClasses);

        ProgressDialog newPd = ProgressDialog.show(this, "כיתות", "מוריד נתונים...", true);
        pd = newPd;
        pd.setCancelable(false);
        pd.show();

        classList =new ArrayList<>();
        mStorageRef.child("Classes/").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        // All the prefixes under listRef.
                        // You may call listAll() recursively on them.
                    }

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (!item.getName().contains("Teachers")) { // if the meeting as connected to the user (contains his name) or if the user is an admin
                            classList.add(new Class(item.getName()));
                        }

                    }

                    if (listResult.getItems().isEmpty())
                        Toast.makeText(this, "אין כיתות", Toast.LENGTH_LONG).show();

                    ClassAdapter cAdapter = new ClassAdapter(this, 0, 0, classList);

                    lvClass.setAdapter(cAdapter);

                    lvClass.setOnItemClickListener(this);

                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getMeetings", "onFailure: ", e);
                });
    }




    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String classname = classList.get(i).getClassName(); //inserts into classname the class name that was clicked in the listView

        Intent intent = new Intent(getBaseContext(), StudentsActivity.class);
        intent.putExtra(Utils.CLASS_NAME_KEY, classname);// place the class name in an Extra that will be sent to StudentsActivity
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