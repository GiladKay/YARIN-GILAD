package com.yg.amit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.FirebaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ClassesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private SharedPreferences sp;

    private ListView lvClass;               //ListView for Classes
    //private ArrayList<Class> classList;     // Array list for the listView
    private String data;// String containing data from the classes file

    private Context context;
    private List<Class>  classList;
    private StorageReference mStorageRef;

    private DatabaseReference mFirebaseRef;
    private FirebaseDatabase mFirebaseInstance;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        context=this;

        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseRef = mFirebaseInstance.getReference("כיתות");


        sp = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

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




        lvClass.setOnItemClickListener(this);
        classList = new ArrayList<Class>();
        ClassAdapter cAdapter = new ClassAdapter(this, 0,0, classList);

        lvClass.setAdapter(cAdapter);

        mFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot:dataSnapshot.getChildren())
                {
                    classList.add(new Class(snapshot.getValue().toString()+".txt"));
                    Log.d(Utils.TAG, snapshot.getValue().toString());
                }

                ClassAdapter cAdapter = new ClassAdapter(context, 0,0, classList);

                lvClass.setAdapter(cAdapter);

                lvClass.setOnItemClickListener((AdapterView.OnItemClickListener) context);
                pd.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ClassesActivity.this,databaseError.toString(),Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String classname = classList.get(i).getClassName(); //inserts into classname the class name that was clicked in the listView

        sp.edit().putString(Utils.CLASS_NAME_KEY, classname).commit();
        startActivity(new Intent(getBaseContext(), StudentsActivity.class));
    }




}