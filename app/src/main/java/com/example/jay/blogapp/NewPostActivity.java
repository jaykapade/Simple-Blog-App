package com.example.jay.blogapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import id.zelory.compressor.Compressor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NewPostActivity extends AppCompatActivity {

    private static final int MAX_LENGTH = 100;
    private Toolbar newPostToolbar;
    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;
    private ProgressBar newPostProgress;

    private Uri postImageUri = null;
    private UploadTask uploadTask = null;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private String currentUserID;

    private Bitmap compressedImageFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        newPostToolbar = findViewById(R.id.new_post_toolbar);
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        newPostImage = findViewById(R.id.new_post_image);
        newPostDesc = findViewById(R.id.new_post_desc);
        newPostBtn = findViewById(R.id.post_btn);
        newPostProgress = findViewById(R.id.new_post_progress);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        currentUserID = firebaseAuth.getCurrentUser().getUid();


        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512,512)
                        .setAspectRatio(1, 1)
                        .start(NewPostActivity.this);

            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String desc = newPostDesc.getText().toString();
                if(!TextUtils.isEmpty(desc)&& postImageUri !=null){
                    newPostProgress.setVisibility(View.VISIBLE);

                    final String randomName = UUID.randomUUID().toString();
                    final StorageReference filePath = storageReference.child("post_images").child(randomName + ".jpg");
                    uploadTask = filePath.putFile(postImageUri);

                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(NewPostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(NewPostActivity.this,"File Uploaded Successfully",Toast.LENGTH_SHORT).show();
                        }
                    });

                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return filePath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull final Task<Uri> task) {
                            final String downloadUri = task.getResult().toString();
                            if (task.isSuccessful()) {

                                File newImageFile = new File(postImageUri.getPath());

                                try {
                                    compressedImageFile = new Compressor(NewPostActivity.this)
                                            .setMaxHeight(100)
                                            .setMaxWidth(100)
                                            .setQuality(2)
                                            .compressToBitmap(newImageFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] thumbData = baos.toByteArray();

                                final StorageReference thumbpath = storageReference.child("post_images/thumbs").child(randomName+".jpg");
                                thumbpath.putBytes(thumbData).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                        if(task.isSuccessful()){
                                            thumbpath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {

                                                    String downloadThumbUri = uri.toString();

                                                    Map<String, Object> postMap = new HashMap<>();
                                                    postMap.put("image_url", downloadUri);
                                                    postMap.put("thumb_url",downloadThumbUri);
                                                    postMap.put("desc", desc);
                                                    postMap.put("user_id", currentUserID);
                                                    postMap.put("timestamp", FieldValue.serverTimestamp());

                                                    firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentReference> task) {

                                                            if(task.isSuccessful()){
                                                                Toast.makeText(NewPostActivity.this, "Post was Added Successfully",Toast.LENGTH_LONG).show();
                                                                Intent mainIntent = new Intent(NewPostActivity.this, MainActivity.class);
                                                                startActivity(mainIntent);
                                                                finish();

                                                            }else{
                                                                Toast.makeText(NewPostActivity.this, task.getException().getMessage(),Toast.LENGTH_LONG).show();
                                                            }
                                                            newPostProgress.setVisibility(View.INVISIBLE);
                                                        }
                                                    });

                                                }
                                            });
                                        }
                                    }
                                });

                            } else {
                                newPostProgress.setVisibility(View.INVISIBLE);
                                Toast.makeText(NewPostActivity.this, "Image Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });





                }

            }
        });


    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                    postImageUri = result.getUri();
                    newPostImage.setImageURI(postImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}
