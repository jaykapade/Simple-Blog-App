package com.example.jay.blogapp;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private FloatingActionButton addPostBtn;
    private String currentUserID;
    private BottomNavigationView mainBottomNav;

    private HomeFragment homeFragment;
    private AccountFragment accountFragment;
    private NotificationFragment notificationFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        mainToolbar = (Toolbar)findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        getSupportActionBar().setTitle("Photo Blog");

        if(mAuth.getCurrentUser()!=null) {

            mainBottomNav = findViewById(R.id.mainBottomNav);

            //Fragments
            homeFragment = new HomeFragment();
            accountFragment = new AccountFragment();
            notificationFragment = new NotificationFragment();

            replaceFragment(homeFragment);


            mainBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                    switch (menuItem.getItemId()) {
                        case R.id.bottom_action_home:
                            replaceFragment(homeFragment);
                            return true;
                        case R.id.bottom_action_account:
                            replaceFragment(accountFragment);
                            return true;
                        case R.id.bottom_action_notify:
                            replaceFragment(notificationFragment);
                            return true;

                        default:
                            return false;
                    }

                }
            });

            addPostBtn = findViewById(R.id.add_post_btn);

            addPostBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent newPostIntent = new Intent(MainActivity.this, NewPostActivity.class);
                    startActivity(newPostIntent);
                }
            });
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser==null){
            sendToLogin();
        }
        else{
            currentUserID = mAuth.getCurrentUser().getUid();
            firebaseFirestore.collection("Users").document(currentUserID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        if(!(task.getResult().exists())){
                            Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                            startActivity(setupIntent);
                            finish();
                        }
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Error: "+task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_logout_btn:
                logOut();
                return true;
            case R.id.action_settings_btn:
                Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                startActivity(setupIntent);
                finish();
            default:
                    return false;
        }
    }

    private void logOut() {
        mAuth.signOut();
        sendToLogin();
    }

    private void sendToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void replaceFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_container,fragment);
        fragmentTransaction.commit();
    }
}
