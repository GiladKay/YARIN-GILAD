package com.yg.amit;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private Dialog login;
    private TextInputLayout ipEmail, ipPassword;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnNext, btnCancel, btnForget;

    private Dialog forget;
    private TextInputLayout ipEmail1;
    private TextInputEditText edtEmail1;
    private MaterialButton btnNext1, btnCancel1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set orientation to false

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            new Handler().postDelayed(() -> {
                Intent i = new Intent(getBaseContext(), Menu.class);
                finish();
                startActivity(i);
            }, 2000);
        } else {
            login = new Dialog(this);
            login.setContentView(R.layout.login_dialog);
            login.setCancelable(false);

            ipEmail = (TextInputLayout) login.findViewById(R.id.ipEmail);
            ipPassword = (TextInputLayout) login.findViewById(R.id.ipPassword);

            edtEmail = (TextInputEditText) login.findViewById(R.id.edtEmail);
            edtPassword = (TextInputEditText) login.findViewById(R.id.edtPassword);

            btnNext = (MaterialButton) login.findViewById(R.id.btnNext);
            btnCancel = (MaterialButton) login.findViewById(R.id.btnCancel);
            btnForget = (MaterialButton) login.findViewById(R.id.btnForget);
            btnNext.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
            btnForget.setOnClickListener(this);

            new Handler().postDelayed(() -> {
                login.show();
                login.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }, 2000);
        }
    }

    private void updateUI(FirebaseUser currentUser) {
        db.collection("users")
                .document(currentUser.getEmail())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                editor = sharedPreferences.edit();
                                editor.putString(Utils.TYPE_KEY, (String) document.get(Utils.TYPE_KEY));
                                editor.putString(Utils.NAME_KEY, (String) document.get(Utils.NAME_KEY));
                                editor.commit();

                                Intent i = new Intent(getBaseContext(), Menu.class);
                                finish();
                                startActivity(i);
                            } else {
                                Log.d(Utils.TAG, "No such document");
                            }
                        } else {
                            Log.d(Utils.TAG, "get failed with ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnNext) {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            ipEmail.setError(null); // Clear the error
            ipPassword.setError(null); // Clear the error

            if (email.isEmpty())
                ipEmail.setError("המייל ריק.");

            if (password.isEmpty())
                ipPassword.setError("הסיסמה ריקה.");

            if (!email.isEmpty() && !password.isEmpty()) {
                final ProgressDialog pd = ProgressDialog.show(this, "התחברות", "מתחבר...", true);
                pd.setCancelable(false);
                pd.show();

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                pd.dismiss();
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d("Login", "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                    login.dismiss();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w("Login", "signInWithEmail:failure", task.getException());
                                    if (task.getException().getMessage().contains("email") ||
                                            task.getException().getMessage().contains("Email"))
                                        ipEmail.setError(task.getException().getMessage());
                                    else if (task.getException().getMessage().contains("password") ||
                                            task.getException().getMessage().contains("Password"))
                                        ipPassword.setError(task.getException().getMessage());
                                    else if (task.getException().getMessage().contains("user") ||
                                            task.getException().getMessage().contains("User"))
                                        ipEmail.setError(task.getException().getMessage());
                                }
                            }
                        });
            }
        }
        if (v.getId() == R.id.btnCancel) {
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
        if (v.getId() == R.id.btnForget) {
            forget = new Dialog(this);
            forget.setContentView(R.layout.forget_dialog);

            ipEmail1 = (TextInputLayout) forget.findViewById(R.id.ipEmail1);

            edtEmail1 = (TextInputEditText) forget.findViewById(R.id.edtEmail1);

            btnNext1 = (MaterialButton) forget.findViewById(R.id.btnNext1);
            btnCancel1 = (MaterialButton) forget.findViewById(R.id.btnCancel1);
            btnNext1.setOnClickListener(v12 -> {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String emailAddress = edtEmail1.getText().toString().trim();

                if (emailAddress.isEmpty())
                    ipEmail1.setError("המייל ריק.");
                else {
                    final ProgressDialog pd = ProgressDialog.show(this, "שחזור סיסמה", "שולח מייל...", true);
                    pd.setCancelable(false);
                    pd.show();

                    auth.sendPasswordResetEmail(emailAddress)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    pd.dismiss();
                                    if (task.isSuccessful()) {
                                        Log.d("TAG", "Email sent.");
                                        forget.dismiss();
                                        Toast.makeText(getApplicationContext(), "נשלח מייל לשחזור סיסמה", Toast.LENGTH_LONG).show();
                                    } else {
                                        Log.w("Login", "signInWithEmail:failure", task.getException());
                                        ipEmail1.setError(task.getException().getMessage());
                                    }
                                }
                            });
                }
            });
            btnCancel1.setOnClickListener(v1 -> forget.dismiss());

            forget.show();
            forget.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }
}