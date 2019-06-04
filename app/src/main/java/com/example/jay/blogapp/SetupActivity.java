package com.example.jay.blogapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;


public class SetupActivity extends AppCompatActivity {

    CircleImageView setupImage;
    private Uri mainImageURI = null;

    private  String userID;
    private boolean isChanged = false;

    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgress;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private UploadTask uploadTask = null;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = findViewById(R.id.setup_toolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setup");

        setupImage = findViewById(R.id.setup_image);
        setupBtn = findViewById(R.id.setup_button);
        setupName = findViewById(R.id.setup_name);
        setupProgress = findViewById(R.id.setup_progress);
        setupProgress.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        userID = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("Users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){

                    if (task.getResult().exists()) {

                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");

                        mainImageURI = Uri.parse(image);

                        setupName.setText(name);
                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.default_image);
                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);

//                      Toast.makeText(SetupActivity.this,"Data Exists",Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(SetupActivity.this,"FireStore Retrieve Error: " + task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }

                setupProgress.setVisibility(View.INVISIBLE);
                setupBtn.setEnabled(true);
            }
        });

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String username = setupName.getText().toString();
                if (!TextUtils.isEmpty(username) && mainImageURI != null) {
                setupProgress.setVisibility(View.VISIBLE);

                if(isChanged){
                    final StorageReference imagePath = storageReference.child("profile_images").child(userID + ".jpg");
                    uploadTask = imagePath.putFile(mainImageURI);

                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(SetupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

//                            Toast.makeText(SetupActivity.this,"File Uploaded Successfully",Toast.LENGTH_SHORT).show();
                        }
                    });


                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return imagePath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                storeFirestore(task, username);
//                                Toast.makeText(SetupActivity.this,"Url: "+downloadUri, Toast.LENGTH_SHORT).show();
                            } else {
                                setupProgress.setVisibility(View.INVISIBLE);
                                Toast.makeText(SetupActivity.this, "Image Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


                }
                else{
                    storeFirestore(null, username);
                }
            }

            }
        });



        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(SetupActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){

                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    }
                    else{
                        imagePicker();
                    }

                }
                else{
                    imagePicker();
                }

            }
        });
    }

    private void storeFirestore(@NonNull Task<Uri> task, String username) {

        Uri downloadUri;
        if(task != null) {
            downloadUri = task.getResult();
        }else{
            downloadUri = mainImageURI;
        }
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", username);
        userMap.put("image",downloadUri.toString());
        firebaseFirestore.collection("Users").document(userID).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){

                    Toast.makeText(SetupActivity.this,"User Settings Updated",Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                }else{

                    Toast.makeText(SetupActivity.this,"FireStore Error: " + task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void imagePicker(){
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);
                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
