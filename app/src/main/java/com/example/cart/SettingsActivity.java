package com.example.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton closeBtn, saveBtn;
    private CircleImageView profileImg;
    private TextView chngImgTxtBtn;
    private EditText nameEdt, phnEdt, carNameEdt;
    private String settingCheck = " ";
    private String checker = "";
    private Uri imageUri;
    private String myUrl;

    private StorageReference profileImageStorageRef;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        settingCheck = getIntent().getStringExtra("settingCheck");

        mAuth = FirebaseAuth.getInstance();
        profileImageStorageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(settingCheck);

        closeBtn = findViewById(R.id.settings_close_btn_id);
        saveBtn = findViewById(R.id.settings_save_btn_id);
        profileImg = findViewById(R.id.settings_profile_picture_id);
        chngImgTxtBtn = findViewById(R.id.settings_change_photo_txt);
        nameEdt = findViewById(R.id.settings_name_edt_id);
        phnEdt = findViewById(R.id.settings_phn_edt_id);
        carNameEdt = findViewById(R.id.settings_car_name_edt_id);
        loadingBar = new ProgressDialog(this);



        if(settingCheck.equals("Drivers")){
            carNameEdt.setVisibility(View.VISIBLE);
        }else {
            carNameEdt.setVisibility(View.GONE);
        }

        Toast.makeText(getApplicationContext(),"From "+settingCheck,Toast.LENGTH_LONG).show();

      closeBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              if(settingCheck.equals("Drivers")){
                  Intent driverIntent = new Intent(SettingsActivity.this, DriverMapsActivity.class);
                  driverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                  startActivity(driverIntent);
              }else {
                  Intent customerIntent = new Intent(SettingsActivity.this, CustomerMapsActivity.class);
                  customerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                  startActivity(customerIntent);
              }
          }
      });

      saveBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {

              if(checker.equals("clicked")){

                  validateControllers();

              }else {

                  validateAndSaveOnlyInfos();

              }

          }
      });

      chngImgTxtBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              checker = "clicked";

              CropImage.activity()
                      .setAspectRatio(1,1)
                      .start(SettingsActivity.this);


          }
      });
    }

    @Override
    protected void onStart() {
        super.onStart();

        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists() && snapshot.getChildrenCount() > 0){

                    String name = snapshot.child("name").getValue().toString();
                    nameEdt.setText(name);
                    String phone = snapshot.child("phone").getValue().toString();
                    phnEdt.setText(phone);

                    if(snapshot.hasChild("car")){

                        String car = snapshot.child("car").getValue().toString();
                        carNameEdt.setText(car);

                    }

                    if(snapshot.hasChild("image")){

                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(profileImg);

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data!=null){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            profileImg.setImageURI(imageUri);
        }else {
            if(settingCheck.equals("Drivers")){
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            }else {
                startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
            }

            Toast.makeText(getApplicationContext(),"Error, Try again plz...",Toast.LENGTH_LONG).show();
        }


    }

    private void validateControllers(){
        if(TextUtils.isEmpty(nameEdt.getText().toString())){
            nameEdt.setError("Name is mandatory...");
            nameEdt.requestFocus();
            return;
        }
        else if(TextUtils.isEmpty(phnEdt.getText().toString())){
            phnEdt.setError("Phone number is mandatory...");
            phnEdt.requestFocus();
            return;
        }
        else if(settingCheck.equals("Drivers") && TextUtils.isEmpty(carNameEdt.getText().toString())){
            carNameEdt.setError("Car Name is mandatory...");
            carNameEdt.requestFocus();
            return;
        }
        else if(checker.equals("clicked")){
            uploadProfilePicture();
        }
    }

    private void uploadProfilePicture() {

        loadingBar.setTitle("Uploading your informations with photo...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        if(imageUri != null){

            final StorageReference fileRef = profileImageStorageRef.child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {

                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                        Toast.makeText(getApplicationContext()," Photo Uploaded successfully",Toast.LENGTH_LONG).show();
                        return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        HashMap<String,Object> userMap = new HashMap<>();
                        userMap.put("uid",mAuth.getCurrentUser().getUid());
                        userMap.put("name",nameEdt.getText().toString());
                        userMap.put("phone",phnEdt.getText().toString());
                        userMap.put("image",myUrl);

                        if(settingCheck.equals("Drivers")){
                            userMap.put("car",carNameEdt.getText().toString());
                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    loadingBar.dismiss();
                                    if(settingCheck.equals("Drivers")){
                                        startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                                    }else {
                                        startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
                                    }
                                    Toast.makeText(getApplicationContext(),"Uploaded successfully",Toast.LENGTH_LONG).show();

                                }else {
                                    loadingBar.dismiss();
                                    if(settingCheck.equals("Drivers")){
                                        startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                                    }else {
                                        startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
                                    }
                                    Toast.makeText(getApplicationContext(),"Error: "+task.getException().toString(),Toast.LENGTH_LONG).show();

                                }
                            }
                        });
                    }
                }
            });

        }else {
            Toast.makeText(getApplicationContext(),"Image is not selected!!!",Toast.LENGTH_LONG).show();
        }

    }

    private void validateAndSaveOnlyInfos() {

        if(TextUtils.isEmpty(nameEdt.getText().toString())){
            nameEdt.setError("Name is mandatory...");
            nameEdt.requestFocus();
            return;
        }
        else if(TextUtils.isEmpty(phnEdt.getText().toString())){
            phnEdt.setError("Phone number is mandatory...");
            phnEdt.requestFocus();
            return;
        }
        else if(settingCheck.equals("Drivers") && TextUtils.isEmpty(carNameEdt.getText().toString())){
            carNameEdt.setError("Car Name is mandatory...");
            carNameEdt.requestFocus();
            return;
        } else {

            loadingBar.setTitle("Uploading your informations...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            HashMap<String,Object> userMap = new HashMap<>();
            userMap.put("uid",mAuth.getCurrentUser().getUid());
            userMap.put("name",nameEdt.getText().toString());
            userMap.put("phone",phnEdt.getText().toString());

            if(settingCheck.equals("Drivers")){
                userMap.put("car",carNameEdt.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful()){
                        loadingBar.dismiss();
                        if(settingCheck.equals("Drivers")){
                            startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                        }else {
                            startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
                        }
                        Toast.makeText(getApplicationContext(),"Uploaded successfully",Toast.LENGTH_LONG).show();

                    }else {
                        loadingBar.dismiss();
                        if(settingCheck.equals("Drivers")){
                            startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                        }else {
                            startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
                        }
                        Toast.makeText(getApplicationContext(),"Error: "+task.getException().toString(),Toast.LENGTH_LONG).show();

                    }

                }
            });

        }






    }
}