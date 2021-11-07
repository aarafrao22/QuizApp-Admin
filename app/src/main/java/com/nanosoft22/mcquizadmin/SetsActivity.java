package com.nanosoft22.mcquizadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nanosoft22.mcquizadmin.R;

import java.util.Objects;

public class SetsActivity extends AppCompatActivity {
    private GridView gridView;
    private Dialog loadingDialogue;
    private GridAdapter adapter;
    private String categoryName;
    private DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadingDialogue = new Dialog(this);
        loadingDialogue.setContentView(R.layout.loading);
        loadingDialogue.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialogue.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialogue.setCancelable(false);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        categoryName = getIntent().getStringExtra("title");
        getSupportActionBar().setTitle(categoryName);


        gridView = findViewById(R.id.grid_view);
        myRef = FirebaseDatabase.getInstance().getReference();
        adapter = new GridAdapter(getIntent().getIntExtra("sets", 0), getIntent().getStringExtra("title"), new GridAdapter.GridListener() {
            @Override
            public void addSet() {
                loadingDialogue.show();

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                database.getReference().child("Categories").child(getIntent().getStringExtra("key")).child("sets").setValue(getIntent().getIntExtra("sets", 0)+1).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            adapter.sets++;
                            adapter.notifyDataSetChanged();
                            loadingDialogue.dismiss();
                        }else {

                        }
                    }
                });
            }

            @Override
            public void onLongClick(final int setNo) {

                new AlertDialog.Builder(SetsActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Set"+setNo)
                        .setMessage("Are You Sure, You want to delete this Set?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadingDialogue.show();
                                myRef.
                                        child("SETS").child(categoryName)
                                        .child("questions").orderByChild("setNo")
                                        .equalTo(setNo).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                                            String id = dataSnapshot1.getKey();
                                            myRef.child("SETS").child(categoryName)
                                                    .child("questions").child(id).removeValue();
                                        }
                                        loadingDialogue.dismiss();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Toast.makeText(SetsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                        adapter.sets --;
                                        loadingDialogue.dismiss();
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("cancel",null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();


            }
        });
        gridView.setAdapter(adapter);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()== android.R.id.home)
        {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
