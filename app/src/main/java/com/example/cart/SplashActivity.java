package com.example.cart;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    Thread.sleep(7000);

                    }catch (Exception e){

                    e.printStackTrace();

                }
                finally {

                    Intent intent = new Intent(SplashActivity.this,WelcomeActivity.class);
                    startActivity(intent);

                }

            }
        });
        thread.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}