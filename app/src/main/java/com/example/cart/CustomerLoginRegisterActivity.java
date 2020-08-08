package com.example.cart;

import android.app.ProgressDialog;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginRegisterActivity extends AppCompatActivity {

    private TextView swapLogRegTxt, notregTxtBtn;
    private TextInputEditText emailEdt, passEdt;
    private Button loginBtn, registerBtn;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference CustomerDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_register);

        mAuth = FirebaseAuth.getInstance();

        swapLogRegTxt = findViewById(R.id.customerlogregTxt);
        notregTxtBtn = findViewById(R.id.customernotregisteredTxtId);
        emailEdt = findViewById(R.id.customeremailedt);
        passEdt = findViewById(R.id.customerpasswordedt);
        loginBtn = findViewById(R.id.customerloginBtnId);
        registerBtn = findViewById(R.id.customerregisterBtnId);
        loadingBar = new ProgressDialog(this);

        swapLogRegTxt.setText("Customer Login");
        loginBtn.setVisibility(View.VISIBLE);
        notregTxtBtn.setVisibility(View.VISIBLE);
        registerBtn.setVisibility(View.GONE);


        notregTxtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                swapLogRegTxt.setText("Customer Register");
                loginBtn.setVisibility(View.GONE);
                notregTxtBtn.setVisibility(View.GONE);
                registerBtn.setVisibility(View.VISIBLE);


            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customerLogin();

                emailEdt.setText("");
                passEdt.setText("");

                loadingBar.setTitle("Log in process");
                loadingBar.setMessage("Wait, while you are logging in...");
                loadingBar.setCanceledOnTouchOutside(false);

            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customerRegister();

                emailEdt.setText("");
                passEdt.setText("");

                loadingBar.setTitle("Register process");
                loadingBar.setMessage("Wait, while we are creating credentials...");
                loadingBar.setCanceledOnTouchOutside(false);

            }
        });

    }

    private void customerRegister() {

        String email = emailEdt.getText().toString();
        String password = passEdt.getText().toString();

        if(TextUtils.isEmpty(email)){
            emailEdt.setError("email is mandatory...");
            emailEdt.requestFocus();
            return;
        }
        else if(TextUtils.isEmpty(password)){
            passEdt.setError("password is mandatory...");
            passEdt.requestFocus();
            return;
        }
        else if(!email.contains("@")){

            emailEdt.setError("email must be valid i.e. example@example.com ...");
            emailEdt.requestFocus();
            return;

        }
        else {
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){

                                String onlineCustomerId = mAuth.getCurrentUser().getUid();
                                CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users")
                                        .child("Customers").child(onlineCustomerId);
                                CustomerDatabaseRef.setValue(true);
                                loadingBar.dismiss();
                                Toast.makeText(getApplicationContext(),"You are Registered successfully...",Toast.LENGTH_LONG).show();
                                Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this,CustomerMapsActivity.class);
                                customerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(customerIntent);
                                finish();
                            }else {
                                loadingBar.dismiss();
                                Toast.makeText(getApplicationContext(),"Error :"+task.getException().toString(),Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void customerLogin() {

        String email = emailEdt.getText().toString();
        String password = passEdt.getText().toString();

        if(TextUtils.isEmpty(email)){
            emailEdt.setError("email is mandatory...");
            emailEdt.requestFocus();
            return;
        }
        else if(TextUtils.isEmpty(password)){
            passEdt.setError("password is mandatory...");
            passEdt.requestFocus();
            return;
        }
        else if(!email.contains("@")){

            emailEdt.setError("email must be valid i.e. example@example.com ...");
            emailEdt.requestFocus();
            return;

        }
        else {
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){
                                loadingBar.dismiss();
                                Toast.makeText(getApplicationContext(),"You are logged in successfully...",Toast.LENGTH_LONG).show();
                                Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this,CustomerMapsActivity.class);
                                customerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(customerIntent);
                                finish();
                            }else {
                                loadingBar.dismiss();
                                Toast.makeText(getApplicationContext(),"Error :"+task.getException().toString(),Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }
}