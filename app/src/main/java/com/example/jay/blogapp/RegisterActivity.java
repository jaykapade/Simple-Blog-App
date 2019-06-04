package com.example.jay.blogapp;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText regEmailText;
    private EditText regPassText;
    private EditText regConfirmPassText;
    private Button regBtn;
    private Button regLoginBtn;
    private ProgressBar regProgress;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        regEmailText = (EditText)findViewById(R.id.reg_email);
        regPassText = (EditText)findViewById(R.id.reg_pass);
        regConfirmPassText = (EditText)findViewById(R.id.reg_confirm_pass);
        regBtn =(Button)findViewById(R.id.reg_btn);
        regLoginBtn=(Button)findViewById(R.id.reg_login_btn);
        regProgress=(ProgressBar)findViewById(R.id.reg_progress);

        regLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent loginIntent = new Intent(RegisterActivity.this,LoginActivity.class);
//                startActivity(loginIntent);
                finish();
            }
        });

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = regEmailText.getText().toString();
                String pass = regPassText.getText().toString();
                String confirm_pass = regConfirmPassText.getText().toString();

            if(!TextUtils.isEmpty(email)&&!TextUtils.isEmpty(pass)&&!TextUtils.isEmpty(confirm_pass)){

                if(pass.equals(confirm_pass)){
                    regProgress.setVisibility(View.VISIBLE);
                    mAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(RegisterActivity.this,"Registered Successfully",Toast.LENGTH_SHORT).show();
                                Intent setupIntent = new Intent(RegisterActivity.this, SetupActivity.class);
                                startActivity(setupIntent);
                                finish();

                            }
                            else{
                                Toast.makeText(RegisterActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                            }
                            regProgress.setVisibility(View.INVISIBLE);
                        }
                    });

                }
                else{
                    Toast.makeText(RegisterActivity.this,"Passwords Don't Match",Toast.LENGTH_SHORT).show();
                }
            }

            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentuser = mAuth.getCurrentUser();
        if(currentuser!= null){
            sendToMain();
        }
    }

    private void sendToMain() {
        Intent mainIntent = new Intent(RegisterActivity.this,MainActivity.class);
        startActivity(mainIntent);
        finish();

    }
}
