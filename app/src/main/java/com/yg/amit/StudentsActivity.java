package com.yg.amit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StudentsActivity extends AppCompatActivity {



    private SharedPreferences sd;

    private ListView lvS;       // listView for students
    private StudentAdapter studentAdapter;
    private ArrayList<Student> studentList;
    private String data;       //String containing data from the chosen class file

    private boolean hasBeenEdited = false; // remembers if an edit to the students has occurred


    private ProgressDialog pd;

    private ContentResolver contentResolver;

    private StorageReference mStorageRef;
    FirebaseFirestore db;

    private SharedPreferences sp;
    private String name;
    private String type;

    private String className;


    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.student_menu, menu);


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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.exit)
            finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Bundle extras = getIntent().getExtras();
        className = extras.getString(Utils.CLASS_NAME_KEY); //fetching the class name from the Intents Extra

        contentResolver = getContentResolver();

        sp = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);
        name = sp.getString(Utils.NAME_KEY, "name");
        type = sp.getString(Utils.TYPE_KEY, Utils.TYPE_STUDENT);

        mStorageRef = FirebaseStorage.getInstance().getReference();



        lvS = (ListView) findViewById(R.id.lvStudents);

        if(!className.equals("Teachers"))
            pd = ProgressDialog.show(this, className, "מוריד נתונים...", true);
        if(className.equals("Teachers"))
            pd = ProgressDialog.show(this, "מורים", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();

        mStorageRef.child("Classes").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        // All the prefixes under listRef.
                        // You may call listAll() recursively on them.
                    }

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        if (item.getName().contains(className))   //locating the file that has the same name as the class we clicked on
                            downloadFile(item.getName());
                    }

                    pd.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    Log.w("getClass", "onFailure: ", e);
                });

        // all the listeners used for the time and date arrangement in a new meeting

        Toolbar toolbar = findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("כיתה " + className);
        if(className.equals("Teachers")) mTitle.setText("מורים");
        setSupportActionBar(toolbar);

    }

    /**
     * method used to download data files from firebase
     *
     * @param file- String containing the name of the file with all the students
     */
    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file);

        mStorageRef.child("Classes/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateClass(file);
                }).addOnFailureListener(exception -> {
            // Handle failed download
            Log.w("Download", "onFailure: Download failed", exception);
        });
    }

    /**
     * Initializes the studentList and fills it with all the Students read from the appropriate file
     * sets an Item listener so that when a student is clicked, the meeting arrangement dialog is shown
     *
     * @param file- name of the class file
     */
    private void updateClass(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateClass: " + data);

        studentList = new ArrayList<Student>();

        for (int i = 0; i < data.split("&&").length - 1; i++) {
            studentList.add(new Student(data.split("&&")[i].split("==")[0],
                    Integer.parseInt(data.split("&&")[i].split("==")[1])));
        }

        studentAdapter = new StudentAdapter(this, studentList, className);
        lvS.setAdapter(studentAdapter);


        lvS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Student student = ((Student) studentAdapter.getItem(i));

                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                intent.putExtra("SName", student.getName());
                intent.putExtra("mCount",student.getMeetingCount());
                intent.putExtra("classname",className);
                startActivity(intent);

                }
            });
    }

    /**
     * reads and outputs the contents of the now local Class file
     *
     * @param context-this
     * @param file-        name of the class file
     * @return a String containing all the data from the file
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





    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(!className.equals("Teachers"))
            startActivity(new Intent(getBaseContext(), ClassesActivity.class));
        else
            startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }
}

