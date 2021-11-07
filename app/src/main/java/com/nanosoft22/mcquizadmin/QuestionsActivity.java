package com.nanosoft22.mcquizadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class QuestionsActivity extends AppCompatActivity {

    private Button addBtn, excelBtn;
    private RecyclerView recyclerView;
    private QuestionsAdapter adapter;

    private DatabaseReference myRef;
    private Dialog loadingDialogue;
    public static List<QuestionModel> list;
    public static final int CELL_COUNT = 6;
    private int set;
    private String categoryName;
    private TextView loadingText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        Toolbar toolbar = findViewById(R.id.toolbar);

        myRef = FirebaseDatabase.getInstance().getReference();

        loadingDialogue = new Dialog(this);
        loadingDialogue.setContentView(R.layout.loading);
        loadingDialogue.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialogue.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialogue.setCancelable(false);
        loadingText = loadingDialogue.findViewById(R.id.textview);


        setSupportActionBar(toolbar);

        categoryName = getIntent().getStringExtra("category");
        set = getIntent().getIntExtra("setNo",1);
        getSupportActionBar().setTitle(categoryName+"/set "+set);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addBtn = findViewById(R.id.addBtn);
        excelBtn = findViewById(R.id.excelBtn);
        recyclerView = findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);

        list =  new ArrayList<>();
        adapter = new QuestionsAdapter(list, categoryName, new QuestionsAdapter.DeleteListener() {
            @Override
            public void onLongClick(final int position, final String id) {
                new AlertDialog.Builder(QuestionsActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Question")
                        .setMessage("Are You Sure, You want to delete this question?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadingDialogue.show();
                                myRef.child("SETS").child(categoryName).child("questions").child(id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            list.remove(position);
                                            adapter.notifyItemRemoved(position);
                                            loadingDialogue.dismiss();

                                        }else {
                                            Toast.makeText(QuestionsActivity.this,"failed to Delete", Toast.LENGTH_SHORT).show();
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

        getData(categoryName,set);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addQuestionIntent  = new Intent(QuestionsActivity.this,AddQuestionActivity.class);
                addQuestionIntent.putExtra("categoryName",categoryName);
                addQuestionIntent.putExtra("setNo",set);
                startActivity(addQuestionIntent);
            }
        });

        excelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(QuestionsActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        selectFile();
                }else {
                    ActivityCompat.requestPermissions(QuestionsActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},101);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode ==  101){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectFile();
            }else {
                Toast.makeText(this, "You must Grant Permission", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void selectFile(){
            Intent intent =  new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent,"Select File"),102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode ==  102){
            if (resultCode == RESULT_OK){

                String filePath = data.getData().getPath();
                if (filePath.endsWith(".xlsx")){
                    readFile(data.getData());
                }else {
                    Toast.makeText(this, "Please Choose an Excel File!", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
    private void getData(String categoryName, final int set){
        loadingDialogue.show();
        myRef.
        child("SETS").child(categoryName)
                .child("questions").orderByChild("setNo")
                .equalTo(set).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    String id = dataSnapshot1.getKey();
                    String question = dataSnapshot1.child("question").getValue().toString();
                    String a = dataSnapshot1.child("optionA").getValue().toString();
                    String b = dataSnapshot1.child("optionB").getValue().toString();
                    String c = dataSnapshot1.child("optionC").getValue().toString();
                    String d = dataSnapshot1.child("optionD").getValue().toString();
                    String answer = dataSnapshot1.child("correctAns").getValue().toString();

                    list.add(new QuestionModel(id,question,a,b,c,d,answer,set));
                }
                loadingDialogue.dismiss();
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(QuestionsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                loadingDialogue.dismiss();
                finish();
            }
        });

    }

    private void readFile(final Uri fileuri){

        loadingText.setText("Scanning Questions...");
        loadingDialogue.show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                    final HashMap<String,Object> parentMap = new HashMap<>();
                    final List<QuestionModel> tempList = new ArrayList<>();

                    try {
                        InputStream inputStream = getContentResolver().openInputStream(fileuri);
                        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                        XSSFSheet sheet = workbook.getSheetAt(0);
                        FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

                        int rowCount = sheet.getPhysicalNumberOfRows();
                        if (rowCount > 0){

                            for (int r = 0 ; r < rowCount; r++){
                                Row row = sheet.getRow(r);

                                if (row.getPhysicalNumberOfCells() == CELL_COUNT){
                                    String question = getCellData(row,0,formulaEvaluator);
                                    String a = getCellData(row,1,formulaEvaluator);
                                    String b = getCellData(row,2,formulaEvaluator);
                                    String c = getCellData(row,3,formulaEvaluator);
                                    String d = getCellData(row,4,formulaEvaluator);
                                    String correctAns = getCellData(row,5,formulaEvaluator);

                                    if (correctAns.equals(a) || correctAns.equals(b) || correctAns.equals(c) || correctAns.equals(d) ){

                                        HashMap<String,Object> questionMap = new HashMap<>();
                                        questionMap.put("question",question);
                                        questionMap.put("optionA",a);
                                        questionMap.put("optionB",b);
                                        questionMap.put("optionC",c);
                                        questionMap.put("optionD",d);
                                        questionMap.put("correctAns",correctAns);
                                        questionMap.put("setNo",set);

                                        String id = UUID.randomUUID().toString();

                                        parentMap.put(id,questionMap);

                                        tempList.add(new QuestionModel(id,question,a,b,c,d,correctAns,set));

                                    }else {
                                        final int finalR1 = r;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                loadingText.setText("Loading...");
                                                loadingDialogue.dismiss();
                                                Toast.makeText(QuestionsActivity.this, "Row no. "+(finalR1 +1)+"has no correct option", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        return;
                                    }

                                }else {
                                    final int finalR = r;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadingText.setText("Loading...");
                                            loadingDialogue.dismiss();
                                            Toast.makeText(QuestionsActivity.this, "Row no. "+(finalR +1)+"has incorrect data", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    return;
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingText.setText("Uploading...");
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("SETS").child(categoryName)
                                            .child("questions").updateChildren(parentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                list.addAll(tempList);
                                                adapter.notifyDataSetChanged();

                                            }else {
                                                loadingText.setText("Loading...");
                                                Toast.makeText(QuestionsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            }
                                            loadingDialogue.dismiss();
                                }
                            });


                                }
                            });

                        }else {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingText.setText("Loading...");
                                    loadingDialogue.dismiss();
                                    Toast.makeText(QuestionsActivity.this, "File is empty", Toast.LENGTH_SHORT).show();
                                }
                            });

                            return;
                        }

                    }catch (final FileNotFoundException e){
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("Loading...");
                                Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                loadingDialogue.dismiss();
                            }
                        });

                    } catch (final IOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("Loading...");
                                loadingDialogue.dismiss();
                                Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.notifyDataSetChanged();
    }

    private String getCellData(Row row, int cellPosition, FormulaEvaluator formulaEvaluator){

        String value = "";
        Cell cell = row.getCell(cellPosition);

        switch(cell.getCellType()){

            case Cell.CELL_TYPE_BOOLEAN:
                return value+cell.getBooleanCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                return value+cell.getNumericCellValue();

            case Cell.CELL_TYPE_STRING:
                return value+cell.getStringCellValue();

            default:
                return value;


        }

    }
}
