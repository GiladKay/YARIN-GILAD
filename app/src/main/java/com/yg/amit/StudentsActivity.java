package com.yg.amit;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class StudentsActivity extends AppCompatActivity {

    FirebaseFirestore db;
    private ListView lvS;       // listView for students
    private StudentAdapter studentAdapter;
    private ArrayList<Student> studentList;
    private String data;       //String containing data from the chosen class file
    private boolean hasBeenEdited = false; // remembers if an edit to the students has occurred
    private ProgressDialog pd;
    private ContentResolver contentResolver;
    private StorageReference mStorageRef;
    private SharedPreferences sp;
    private String name;
    private String type;

    private String className;
    private boolean isTeachers;

    private DatabaseReference mFirebaseRef;
    private FirebaseDatabase mFirebaseInstance;

    private Context context;

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);


        MenuItem menuItem = menu.findItem(R.id.search);
        menuItem.setVisible(true);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        isTeachers = false;
        contentResolver = getContentResolver();

        sp = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);
        name = sp.getString(Utils.NAME_KEY, "name");
        type = sp.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);
        className = sp.getString(Utils.CLASS_NAME_KEY, "class"); // fetching the class name from the Intents Extra

        mStorageRef = FirebaseStorage.getInstance().getReference();

        context = this;


        lvS = (ListView) findViewById(R.id.lvStudents);

        if (!className.equals("Teachers"))
            pd = ProgressDialog.show(this, className, "מוריד נתונים...", true);
        if (className.equals("Teachers")) {
            pd = ProgressDialog.show(this, "מורים", "מוריד נתונים...", true);
            isTeachers = true;
        }
        pd.setCancelable(false);
        pd.show();


        mFirebaseInstance = FirebaseDatabase.getInstance();
        if (isTeachers)
            mFirebaseRef = mFirebaseInstance.getReference("מורים");
        else
            mFirebaseRef = mFirebaseInstance.getReference(className);


        studentList = new ArrayList<>();

        mFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    studentList.add(new Student(snapshot.getKey(), Integer.parseInt(snapshot.getValue().toString())));
                    Log.d(Utils.TAG, snapshot.getValue().toString());
                }
                if (isTeachers)
                    studentAdapter = new StudentAdapter(context, studentList, "מורים");
                else
                    studentAdapter = new StudentAdapter(context, studentList, className);

                lvS.setAdapter(studentAdapter);

                lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        Student student = ((Student) studentAdapter.getItem(i));

                        Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                        intent.putExtra("SName", student.getName());
                        intent.putExtra("mCount", student.getMeetingCount());
                        intent.putExtra("classname", className);
                        startActivity(intent);
                        finish();
                    }
                });
                pd.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(StudentsActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("כיתה " + className);
        if (className.equals("Teachers")) mTitle.setText("מורים");
        setSupportActionBar(toolbar);
    }


}

