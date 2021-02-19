package com.yg.amit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MeetingActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;

    private String type;

    private TextView tvTitle, tvSubTitle, tvHelper;
    private TextInputLayout ipInput;
    private TextInputEditText edtInput;
    private Button btnSend;

    private CardView sMashov, tMashov;
    private TextView tvSMashov, tvTMashov;

    private ProgressDialog pd;

    private String meetingFile;
    private int meetingMode;

    private String data;

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        mStorageRef = FirebaseStorage.getInstance().getReference();

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        type = sharedPreferences.getString(Utils.TYPE_KEY, "type");

        tvTitle = findViewById(R.id.tvTitle);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        tvHelper = findViewById(R.id.tvHelper);
        ipInput = findViewById(R.id.ipInput);
        edtInput = findViewById(R.id.edtInput);
        btnSend = findViewById(R.id.btnSend);
        sMashov = findViewById(R.id.sMashov);
        tMashov = findViewById(R.id.tMashov);
        tvSMashov = findViewById(R.id.tvSMashov);
        tvTMashov = findViewById(R.id.tvTMashov);
        btnSend.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();
        meetingFile = extras.getString("Meeting");
        meetingMode = extras.getInt("Mode");

        pd = ProgressDialog.show(this, "פגישה", "מוריד נתונים...", true);
        pd.setCancelable(false);
        pd.show();
        downloadFile(meetingFile);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSend) {
            pd = ProgressDialog.show(this, "פגישה", "שולח משוב...", true);
            pd.setCancelable(false);
            pd.show();

            String mashov = edtInput.getText().toString().trim();

            if(type.equals("student")) {
                String newData = data.split("&&")[0] + "&&" + data.split("&&")[1] + "&&" + data.split("&&")[2] + "&&"
                        + data.split("&&")[3] + "&&" + mashov + "&&" + data.split("&&")[5] + "&&";
                writeToFile(newData, this, meetingFile);
                uploadFile(meetingFile, "Meetings/Finished/");
            }
            if(type.equals("teacher")) {
                String newData = data.split("&&")[0] + "&&" + data.split("&&")[1] + "&&" + data.split("&&")[2] + "&&"
                        + data.split("&&")[3] + "&&" + data.split("&&")[4] + "&&" + mashov + "&&";
                writeToFile(newData, this, meetingFile);
                uploadFile(meetingFile, "Meetings/Done/");
            }
        }
    }

    public void updateUI(String file) {
        data = readFromFile(this, file);

        tvTitle.setText(data.split("&&")[0] + " - " + data.split("&&")[1]);
        tvSubTitle.setText(data.split("&&")[2] + " - " + data.split("&&")[3]);

        if (meetingMode == Utils.MODE_UPCOMING) {
            if (type.equals("teacher")) {
                tvHelper.setText("אנא הזן משוב על הפגישה. (אם אינך מעוניין שלח את המשוב ריק).");
                ipInput.setVisibility(View.VISIBLE);
                edtInput.setVisibility(View.VISIBLE);
                btnSend.setVisibility(View.VISIBLE);
            }
        }
        if (meetingMode == Utils.MODE_DONE) {
            if (type.equals("student")) {
                tvHelper.setText("אנא הזן משוב על הפגישה. (אם אינך מעוניין שלח את המשוב ריק).");
                ipInput.setVisibility(View.VISIBLE);
                edtInput.setVisibility(View.VISIBLE);
                btnSend.setVisibility(View.VISIBLE);
            }
            if (type.equals("teacher")) {
                tvHelper.setText("הפגישה התקיימה");
            }
            if (type.equals("admin")) {
                tvHelper.setVisibility(View.GONE);
                tMashov.setVisibility(View.VISIBLE);
                tvTMashov.setVisibility(View.VISIBLE);
                tvTMashov.setText(data.split("&&")[5]);
            }
        }
        if (meetingMode == Utils.MODE_FINISHED) {
            if (type.equals("teacher") || type.equals("student")) {
                tvHelper.setText("הפגישה התקיימה");
            }
            if (type.equals("admin")) {
                tvHelper.setVisibility(View.GONE);
                sMashov.setVisibility(View.VISIBLE);
                tvSMashov.setVisibility(View.VISIBLE);
                tMashov.setVisibility(View.VISIBLE);
                tvTMashov.setVisibility(View.VISIBLE);
                tvSMashov.setText(data.split("&&")[4]);
                tvTMashov.setText(data.split("&&")[5]);
            }
        }

        pd.dismiss();
    }

    /**
     * method used to download data files from firebase
     *
     * @param file- String containing the name of the file with all the meeting information
     */
    private void downloadFile(String file) {
        File localFile = new File(getFilesDir() + "/" + file);

        String m = "";
        if (meetingMode == Utils.MODE_UPCOMING) m = "Upcoming";
        if (meetingMode == Utils.MODE_DONE) m = "Done";
        if (meetingMode == Utils.MODE_FINISHED) m = "Finished";

        mStorageRef.child("Meetings/" + m + "/" + file).getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // Successfully downloaded data to local file
                    Log.d("Download", "onSuccess: Download succeeded");
                    updateUI(file);
                })
                .addOnFailureListener(exception -> {
                    // Handle failed download
                    Log.w("Download", "onFailure: Download failed", exception);
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(),"אירעה שגיאה", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * reads and outputs the contents of the now local meeting file
     *
     * @param context-this
     * @param file-        name of the meeting file
     * @return String containing all the data from the file
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

    private void writeToFile(String data, Context context, String file) {


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file, MODE_PRIVATE)); // APPEND OR PRIVATE
            outputStreamWriter.append(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void uploadFile(String fileName, String path) {
        Uri file = Uri.fromFile(getBaseContext().getFileStreamPath(fileName));
        StorageReference riversRef = mStorageRef.child(path + fileName);

        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload", "onSuccess: Upload succeeded");

                        StorageReference desertRef = mStorageRef.child("Meetings/Upcoming/" + fileName);

                        if(path.equals("Meetings/Finished/")) desertRef = mStorageRef.child("Meetings/Done/" + fileName);

                        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // File deleted successfully
                                // TODO send email if type.equals("teacher")
                                pd.dismiss();
                                tvHelper.setText("המשוב נשלח בהצלחה!");
                                ipInput.setVisibility(View.GONE);
                                edtInput.setVisibility(View.GONE);
                                btnSend.setVisibility(View.GONE);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Uh-oh, an error occurred!
                                pd.dismiss();
                                Toast.makeText(getApplicationContext(),"אירעה שגיאה", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w("Upload", "onSuccess: Upload failed", exception);
                        pd.dismiss();
                        Toast.makeText(getApplicationContext(),"אירעה שגיאה", Toast.LENGTH_LONG).show();
                    }
                });
    }
}