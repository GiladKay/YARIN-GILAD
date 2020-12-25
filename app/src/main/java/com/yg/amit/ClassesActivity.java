package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

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

    public static final String CLASS_NAME_KEY = "class";// key for the Intent Extra that contains the class name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        mStorageRef = FirebaseStorage.getInstance().getReference();

        lvClass = (ListView) findViewById(R.id.lvClasses);

        ProgressDialog newPd = ProgressDialog.show(this, "כיתות", "מוריד נתונים...", true);
        pd = newPd;
        pd.setCancelable(false);
        pd.show();

        downloadFile("Classes.txt");
    }

    /**
     * method used to download data files from firebase
     * @param file- String containing the name of the file with all the classes
     */
    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file); //locate the file using the file String for the path

        mStorageRef.child("Classes/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateClasses(file);
                }).addOnFailureListener(exception -> {
            // Handle failed download
            Log.w("Download", "onFailure: Download failed", exception);
        });
    }

    /**
     * Initializes the classList and fills it with all the classes read from Classes.txt
     * @param file-"Classes.txt"
     */
    private void updateClasses(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateClasses: " + data);

        classList = new ArrayList<Class>();

        for (int i = 0; i < data.split("&&").length - 1; i++) {
            classList.add(new Class(data.split("&&")[i]));
        }

        ClassAdapter cAdapter = new ClassAdapter(this, 0, 0, classList);

        lvClass.setAdapter(cAdapter);

        lvClass.setOnItemClickListener(this);

        pd.dismiss();
    }

    /**
     * reads and outputs the contents of the now local "Classes.txt" file
     * @param context-this
     * @param file- "Classes.txt"
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
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String classname = classList.get(i).getClassName(); //inserts into classname the class name that was clicked in the listView

        Intent intent = new Intent(getBaseContext(), StudentsActivity.class);
        intent.putExtra(CLASS_NAME_KEY, classname);// place the class name in an Extra that will be sent to StudentsActivity
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }
}