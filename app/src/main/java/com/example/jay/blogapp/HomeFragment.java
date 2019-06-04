package com.example.jay.blogapp;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private RecyclerView blogListView;
    private List<BlogPost> blogList;
    private FirebaseFirestore firebaseFirestore;
    private BlogRecyclerAdapter blogRecyclerAdapter;
    private FirebaseAuth firebaseAuth;

    private DocumentSnapshot lastVisible;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        blogList = new ArrayList<>();
        blogListView = view.findViewById(R.id.blog_list_view);

        firebaseAuth = FirebaseAuth.getInstance();

        blogRecyclerAdapter = new BlogRecyclerAdapter(blogList);
        blogListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        blogListView.setAdapter(blogRecyclerAdapter);

        if(firebaseAuth.getCurrentUser() != null) {

            firebaseFirestore = FirebaseFirestore.getInstance();

            blogListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    Boolean reachedBottom = !(recyclerView.canScrollVertically(1));

                    if(reachedBottom){

                        String desc = lastVisible.getString("desc");
                        Toast.makeText(container.getContext(), "Reached: " + desc, Toast.LENGTH_SHORT).show();

                        loadMorePosts();
                    }
                }
            });

            Query firstQuery = firebaseFirestore.collection("Posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(3);

            firstQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                    lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() -1);

                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            BlogPost blogPost = doc.getDocument().toObject(BlogPost.class);
                            blogList.add(blogPost);

                            blogRecyclerAdapter.notifyDataSetChanged();

                        }
                    }
                }
            });

        }


        return view;
    }

    public void loadMorePosts(){

        Query nextQuery = firebaseFirestore.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(3);

        nextQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                if(queryDocumentSnapshots.isEmpty()) {

                    lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            BlogPost blogPost = doc.getDocument().toObject(BlogPost.class);
                            blogList.add(blogPost);

                            blogRecyclerAdapter.notifyDataSetChanged();

                        }
                    }
                }
            }
        });



    }

}
