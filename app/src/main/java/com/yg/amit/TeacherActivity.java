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
import android.widget.ListView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TeacherActivity extends AppCompatActivity {

    private ListView lvT;
    private ArrayList<Teacher> teacherList;


    private StorageReference mStorageRef;

    private ProgressDialog pd;
    private String data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        lvT=(ListView)findViewById(R.id.lvTeachers);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        ProgressDialog newPd = ProgressDialog.show(this, "מורים", "מוריד נתונים...", true);
        pd = newPd;
        pd.setCancelable(false);
        pd.show();

        downloadFile("Teachers.txt");

    }


    private void updateClasses(String file) {
        data = readFromFile(this, file);
        Log.d("TAG", "updateClasses: " + data);

        teacherList = new ArrayList<Teacher>();

        for (int i = 0; i < data.split("&&").length - 1; i++) {
            teacherList.add(new Teacher(data.split("&&")[i].split("==")[0],
                    Integer.parseInt(data.split("&&")[i].split("==")[1])));
        }

        TeacherAdapter tAdapter = new TeacherAdapter(this,  teacherList);

        lvT.setAdapter(tAdapter);

        lvT.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO whatever the admin does with teachers, if at all
            }
        });

        pd.dismiss();
    }

    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file);

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
        startActivity(new Intent(getBaseContext(), Menu.class));
        finish();
    }


}