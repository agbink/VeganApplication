package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vegan.Manage.ManageMainActivity;

public class AddProductActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private static final int GALLERY_REQUEST_1 = 1;
    private static final int GALLERY_REQUEST_2 = 2;
    private EditText AddPid, AddPBrand, AddPname, AddPPrice, AddStock,AddSalesCount;

    private int strAddPid, strAddCategoryId, strAddPPrice, strAddStock, strAddSalesCount;

    private String strAddPimg, strAddPDetailimg, strAddPname, strAddPBrand;

    private ImageButton AddPimg, AddPDetailimg;

    private Uri imageUri1, imageUri2;
    private Button btnMGProductAdd;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("Product");
        storageReference = FirebaseStorage.getInstance().getReference();


        AddPid = (EditText) findViewById(R.id.AddPid);
        AddPimg = (ImageButton) findViewById(R.id.AddPImg);
        AddPDetailimg = (ImageButton) findViewById(R.id.AddPDetailimg);
        AddPBrand = (EditText) findViewById(R.id.AddPBrand);
        AddPname = (EditText) findViewById(R.id.AddPname);
        AddPPrice = (EditText) findViewById(R.id.AddPPrice);
        AddStock = (EditText) findViewById(R.id.AddStock);
        AddSalesCount=(EditText)findViewById(R.id.AddSalesCount);
        btnMGProductAdd = (Button) findViewById(R.id.btnMGProductAdd);

        dialog = new Dialog(AddProductActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        AddPimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST_1);
            }
        });

        AddPDetailimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST_2);
            }
        });
        Spinner spinner = findViewById(R.id.category_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,

                R.array.category_select_item,
                android.R.layout.simple_spinner_dropdown_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        btnMGProductAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strAddPid = Integer.parseInt(AddPid.getText().toString());
                strAddPBrand = AddPBrand.getText().toString();
                strAddPname = AddPname.getText().toString();
                strAddPPrice = Integer.parseInt(AddPPrice.getText().toString());
                strAddStock = Integer.parseInt(AddStock.getText().toString());
                strAddSalesCount=Integer.parseInt(AddSalesCount.getText().toString());
                String selectedCategory = spinner.getSelectedItem().toString();


                if (selectedCategory.equals("101-푸드")) {
                    strAddCategoryId = 101;
                } else if (selectedCategory.equals("102-패션")) {
                    strAddCategoryId = 102;
                } else if (selectedCategory.equals("103-뷰티")) {
                    strAddCategoryId = 103;
                } else {
                    strAddCategoryId = 102;
                }

                dialog.show();

                TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
                confirmTextView.setText("상품을 추가하시겠습니까?");

                Button btnleft1 = dialog.findViewById(R.id.btn_left);
                btnleft1.setText("취소");
                btnleft1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                Button btnright1 = dialog.findViewById(R.id.btn_right);
                btnright1.setText("확인");
                btnright1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 이미지 업로드 후 데이터베이스에 저장
                        dialog.dismiss();
                        uploadImagesAndSaveData();

                    }
                });

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_1 && resultCode == RESULT_OK) {
            imageUri1 = data.getData();
            AddPimg.setImageURI(imageUri1);
        } else if (requestCode == GALLERY_REQUEST_2 && resultCode == RESULT_OK) {
            imageUri2 = data.getData();
            AddPDetailimg.setImageURI(imageUri2);
        }
    }

    // 이미지 업로드 및 데이터베이스 저장 처리
    private void uploadImagesAndSaveData() {
        if (imageUri1 != null && imageUri2 != null) {
            StorageReference filePath1 = storageReference.child("ProductImages").child(imageUri1.getLastPathSegment());
            StorageReference filePath2 = storageReference.child("ProductImages").child(imageUri2.getLastPathSegment());

            // 첫 번째 이미지를 Firebase Storage에 업로드
            filePath1.putFile(imageUri1).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task1) {
                    if (task1.isSuccessful()) {
                        // 첫 번째 이미지의 다운로드 URL을 가져옵니다.
                        filePath1.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri1) {
                                String imageUrl1 = uri1.toString();

                                // 두 번째 이미지를 Firebase Storage에 업로드
                                filePath2.putFile(imageUri2).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task2) {
                                        if (task2.isSuccessful()) {
                                            // 두 번째 이미지의 다운로드 URL을 가져옵니다.
                                            filePath2.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri2) {
                                                    String imageUrl2 = uri2.toString();

                                                    // 나머지 데이터를 Firebase Realtime Database에 저장
                                                    DatabaseReference productRef = databaseReference.child(String.valueOf(strAddPid));
                                                    productRef.child("pimg").setValue(imageUrl1);
                                                    productRef.child("pdetailimg").setValue(imageUrl2);
                                                    productRef.child("pid").setValue(strAddPid);
                                                    productRef.child("category").setValue(strAddCategoryId);
                                                    productRef.child("pbname").setValue(strAddPBrand);
                                                    productRef.child("pname").setValue(strAddPname);
                                                    productRef.child("pprice").setValue(strAddPPrice);
                                                    productRef.child("stock").setValue(strAddStock);
                                                    productRef.child("timestamp").setValue(System.currentTimeMillis());
                                                    Toast.makeText(AddProductActivity.this, "상품이 추가되었습니다.", Toast.LENGTH_SHORT).show();

                                                    Intent intent = new Intent(AddProductActivity.this, ManageMainActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            });
                                        } else {
                                            // 두 번째 이미지 업로드 실패 처리
                                            Toast.makeText(AddProductActivity.this, "두 번째 이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        // 첫 번째 이미지 업로드 실패 처리
                        Toast.makeText(AddProductActivity.this, "첫 번째 이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // 이미지가 선택되지 않았을 경우에 대한 처리
            DatabaseReference productRef = databaseReference.child(String.valueOf(strAddPid));
            productRef.child("pimg").setValue("");
            productRef.child("pdetailimg").setValue("");
            productRef.child("pid").setValue(strAddPid);
            productRef.child("category").setValue(strAddCategoryId);
            productRef.child("pbname").setValue(strAddPBrand);
            productRef.child("pname").setValue(strAddPname);
            productRef.child("pprice").setValue(strAddPPrice);
            productRef.child("stock").setValue(strAddStock);
            productRef.child("salescount").setValue(strAddSalesCount);
            productRef.child("timestamp").setValue(System.currentTimeMillis()); // Add timestamp

            Toast.makeText(AddProductActivity.this, "상품이 추가되었습니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AddProductActivity.this, ManageMainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
