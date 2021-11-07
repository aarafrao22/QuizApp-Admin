package com.nanosoft22.mcquizadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<CategoryModel> list;
    private CategoryAdapter adapter;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private Dialog loadingDialogue,categoryDialog;
    private EditText categoryname;
    private Button addbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Toolbar toolbar = findViewById(R.id.toolbar);


        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Categories");

        loadingDialogue = new Dialog(this);
        loadingDialogue.setContentView(R.layout.loading);
        loadingDialogue.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialogue.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialogue.setCancelable(false);

        setCategoryDialog();
        recyclerView = findViewById(R.id.rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();
        adapter = new CategoryAdapter(list, new CategoryAdapter.DeleteListener() {
            @Override
            public void onDelete(final String key, final int position) {
                new AlertDialog.Builder(CategoryActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Category")
                        .setMessage("Are You Sure, You want to delete?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        loadingDialogue.show();
                                myRef.child("Categories").child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){

                                            myRef.child("SETS").child(list.get(position).getName()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()){
                                                        list.remove(position);
                                                        adapter.notifyDataSetChanged();
                                                    }else{

                                                        Toast.makeText(CategoryActivity.this,"failed to Delete", Toast.LENGTH_SHORT).show();
                                                    }
                                                    loadingDialogue.dismiss();
                                                }
                                            });

                                        }else {
                                            Toast.makeText(CategoryActivity.this,"failed to Delete", Toast.LENGTH_SHORT).show();
                                            loadingDialogue.dismiss();
                                        }
                                    }
                                });
                                    }
                                })
                        .setNegativeButton("cancel",null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            }
        });
        recyclerView.setAdapter(adapter);

        loadingDialogue.show();
        myRef.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    list.add(new CategoryModel(dataSnapshot1.child("name").getValue().toString(),
                            Integer.parseInt(dataSnapshot1.child("sets").getValue().toString()),
                            dataSnapshot1.getKey()
                    ));
                    }
                adapter.notifyDataSetChanged();
                loadingDialogue.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CategoryActivity.this,databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialogue.dismiss();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId()== R.id.add) {
            categoryDialog.show();
        }
        if (item.getItemId() == R.id.logout){
            new AlertDialog.Builder(CategoryActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                    .setTitle("Logout")
                    .setMessage("Are You Sure, You want to Logout?")
                    .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadingDialogue.show();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(CategoryActivity.this,MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("cancel",null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }
    private void setCategoryDialog(){
        categoryDialog = new Dialog(this);
        categoryDialog.setContentView(R.layout.add_category_dialog);
        categoryDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_box));
        categoryDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        categoryDialog.setCancelable(true);

        addbtn = categoryDialog.findViewById(R.id.add);
        categoryname = categoryDialog.findViewById(R.id.category_name);

        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (categoryname.getText().toString().isEmpty() ){
                    categoryname.setError("Required!");
                    return;
                }
                for(CategoryModel model : list){

                    if (categoryname.getText().toString().equals(model.getName())){
                        categoryname.setError("Already Exist");
                        return;
                    }
                }
                categoryDialog.dismiss();
                uploadCategoryName();
            }
        });
    }
        private void uploadCategoryName(){
            loadingDialogue.show();
            Map<String,Object>  map  = new HashMap<>();
            map.put("name",categoryname.getText().toString());
            map.put("sets",0);

            FirebaseDatabase  database = FirebaseDatabase.getInstance();

            String id = UUID.randomUUID().toString();

            database.getReference().child("Categories").child("category"+(list.size()+1)).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                            list.add(new CategoryModel(categoryname.getText().toString(),0,"category"+(list.size()+1)));
                            adapter.notifyDataSetChanged();
                            categoryname.setText(null);
                    }else{
                        Toast.makeText(CategoryActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }   loadingDialogue.dismiss();
                }
            });
        }
}
