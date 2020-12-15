package com.yg.amit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class Menu extends AppCompatActivity implements View.OnClickListener {

    public static final String AMIT_SP = "AMIT";
    public static final String NAME_KEY = "name";
    public static final String TYPE_KEY = "type";

    private SharedPreferences sharedPreferences;

    private String name, type;
    private TextView tvTitle;
    private Button btnUpcoming, btnClasses, btnTeachers, btnAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false
        getSupportActionBar().hide();

        sharedPreferences = getSharedPreferences(AMIT_SP, MODE_PRIVATE);

        tvTitle = (TextView) findViewById(R.id.tvTitle);
        btnUpcoming = (MaterialButton) findViewById(R.id.btnUpcoming);
        btnClasses = (MaterialButton) findViewById(R.id.btnClasses);
        btnTeachers = (MaterialButton) findViewById(R.id.btnTeachers);
        btnAccount = (Button) findViewById(R.id.btnAccount);
        btnUpcoming.setOnClickListener(this);
        btnClasses.setOnClickListener(this);
        btnTeachers.setOnClickListener(this);
        btnAccount.setOnClickListener(this);

        name = sharedPreferences.getString(NAME_KEY, "name");
        type = sharedPreferences.getString(TYPE_KEY, "student");

        tvTitle.setText("שלום " + name);

        if (type.equals("teacher")) {
            btnTeachers.setVisibility(View.GONE);
        }
        if (type.equals("student")) {
            btnClasses.setVisibility(View.GONE);
            btnTeachers.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.btnAccount):
                // TODO add change password option
                new MaterialAlertDialogBuilder(this)
                        .setTitle("חשבון")
                        .setMessage("חשבון מחובר: " + name + ".")
                        .setNeutralButton("אוקיי", null)
                        .setPositiveButton("התנתקות", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent i = new Intent(getBaseContext(), Login.class);
                            finish();
                            startActivity(i);
                        })
                        .setIcon(R.drawable.account).show();
                break;
            case (R.id.btnUpcoming):
                startActivity(new Intent(getBaseContext(), UpcomingMeetings.class));
                break;
            case (R.id.btnClasses):
                startActivity(new Intent(getBaseContext(), ClassesActivity.class));
                break;
            case (R.id.btnTeachers):
                startActivity(new Intent(getBaseContext(), TeacherActivity.class));
                break;

        }

    }

    @Override
    public void onBackPressed() {
        // Exit App
        new MaterialAlertDialogBuilder(this)
                .setTitle("יציאה")
                .setMessage("האם לצאת מהאפליקציה?")
                .setNegativeButton("לא", null)
                .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(R.drawable.error).show();
    }
}