package com.yg.amit;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Menu extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;

    private Dialog newPass;
    private TextInputLayout ipPassword, ipOldPassword,ipRepeatPassword;
    private TextInputEditText edtPassword, edtOldPassword,edtRepeatPassword;
    private MaterialButton btnNext, btnCancel;

    private String name, type;
    private TextView tvTitle;
    private Button btnUpcoming, btnClasses, btnTeachers, btnAccount, btnAdmin;

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.student_menu, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        Toolbar toolbar = findViewById(R.id.toolbar3);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("תפריט ");
        toolbar.findViewById(R.id.btnAccount).setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        tvTitle = (TextView) findViewById(R.id.tvTitle);
        btnUpcoming = (MaterialButton) findViewById(R.id.btnUpcoming);
        btnClasses = (MaterialButton) findViewById(R.id.btnClasses);
        btnTeachers = (MaterialButton) findViewById(R.id.btnTeachers);
        btnAccount = (Button) findViewById(R.id.btnAccount);
        btnAdmin = (Button) findViewById(R.id.btnAdmin);
        btnAdmin.setOnClickListener(this);
        btnUpcoming.setOnClickListener(this);
        btnClasses.setOnClickListener(this);
        btnTeachers.setOnClickListener(this);
        btnAccount.setOnClickListener(this);

        name = sharedPreferences.getString(Utils.NAME_KEY, "name");
        type = sharedPreferences.getString(Utils.TYPE_KEY, "student");

        tvTitle.setText("שלום " + name);

        if (type.equals("teacher")) {
            btnTeachers.setVisibility(View.GONE);
        }
        if (type.equals("student")) {
            btnClasses.setVisibility(View.GONE);
            btnTeachers.setVisibility(View.GONE);
        }
        if (name.equals("admin")) {
            btnAdmin.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.btnAccount):
                new MaterialAlertDialogBuilder(this)
                        .setTitle("חשבון")
                        .setMessage("חשבון מחובר: " + name + ".")
                        .setNeutralButton("אוקיי", null)
                        .setNegativeButton("שינוי סיסמה", ((dialog, which) -> {
                            newPass = new Dialog(this);
                            newPass.setContentView(R.layout.new_pass_dialog);

                            ipPassword = (TextInputLayout) newPass.findViewById(R.id.ipPassword);
                            ipOldPassword = (TextInputLayout) newPass.findViewById(R.id.ipOldPassword);
                            ipRepeatPassword = (TextInputLayout) newPass.findViewById(R.id.ipRepeatPassword);

                            edtPassword = (TextInputEditText) newPass.findViewById(R.id.edtPassword);
                            edtOldPassword = (TextInputEditText) newPass.findViewById(R.id.edtOldPassword);
                            edtRepeatPassword = (TextInputEditText) newPass.findViewById(R.id.edtRepeatPassword);

                            btnNext = (MaterialButton) newPass.findViewById(R.id.btnNext);
                            btnCancel = (MaterialButton) newPass.findViewById(R.id.btnCancel);
                            btnNext.setOnClickListener(v12 -> {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                String password = edtPassword.getText().toString().trim();
                                String RePassword = edtRepeatPassword.getText().toString().trim();
                                String oldPassword = edtOldPassword.getText().toString().trim();

                                ipPassword.setError(null); // Clear the error
                                ipOldPassword.setError(null); //
                                ipRepeatPassword.setError(null);//

                                if (password.isEmpty())
                                    ipPassword.setError("הסיסמה ריקה.");

                                if (oldPassword.isEmpty())
                                    ipOldPassword.setError("הסיסמה ריקה.");

                                if(RePassword.isEmpty()){
                                    ipRepeatPassword.setError("הסיסמה ריקה.");
                                }
                                if(!RePassword.equals(password)){
                                    ipPassword.setError("הססמאות החדשות שהזנת אינם תואמות");
                                }

                                if (!password.isEmpty() && !oldPassword.isEmpty() && !RePassword.isEmpty() && RePassword.equals(password)) {
                                    final ProgressDialog pd = ProgressDialog.show(this, "שינוי סיסמה", "משנה סיסמה...", true);
                                    pd.setCancelable(false);
                                    pd.show();

                                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

                                    // Prompt the user to re-provide their sign-in credentials
                                    user.reauthenticate(credential)
                                            .addOnCompleteListener(task -> {
                                                if(task.isSuccessful()) {
                                                    Log.d("TAG", "User re-authenticated.");
                                                    user.updatePassword(password)
                                                            .addOnCompleteListener(task1 -> {
                                                                pd.dismiss();
                                                                if (task1.isSuccessful()) {
                                                                    Log.d("TAG", "User password updated.");
                                                                    newPass.dismiss();
                                                                    Toast.makeText(getApplicationContext(), "הסיסמה שונתה בהצלחה!", Toast.LENGTH_LONG).show();
                                                                } else {
                                                                    // If sign in fails, display a message to the user.
                                                                    Log.w("Login", "signInWithEmail:failure", task1.getException());
                                                                    ipPassword.setError(task1.getException().getMessage());
                                                                }
                                                            });
                                                } else {
                                                    // If sign in fails, display a message to the user.
                                                    Log.w("Login", "signInWithEmail:failure", task.getException());
                                                    ipOldPassword.setError(task.getException().getMessage());
                                                    pd.dismiss();
                                                }
                                            });
                                }
                            });

                            btnCancel.setOnClickListener(v1 -> {
                                newPass.dismiss();
                            });

                            newPass.show();
                            newPass.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                        }))
                        .setPositiveButton("התנתקות", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent i = new Intent(getBaseContext(), Login.class);
                            finish();
                            startActivity(i);
                        })
                        .setIcon(R.drawable.ic_account).show();
                break;
            case (R.id.btnUpcoming):
                startActivity(new Intent(getBaseContext(), MeetingsActivity.class));
                finish();
                break;
            case (R.id.btnClasses):
                startActivity(new Intent(getBaseContext(), ClassesActivity.class));
                finish();
                break;
            case (R.id.btnTeachers):
                startActivity(new Intent(getBaseContext(), TeacherActivity.class));
                finish();
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