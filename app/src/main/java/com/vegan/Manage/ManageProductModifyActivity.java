package com.vegan.Manage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vegan.ItemsDomain;
import com.vegan.ProductListActivity;
import com.vegan.R;

public class ManageProductModifyActivity extends AppCompatActivity {
    private EditText ModifyPid, ModifyCategory, ModifyPimg, ModifyPDetailimg, ModifyPbname, ModifyPname, ModifyPprice, ModifyStock, ModifySalesCount;


    private int strModifyPid, strModifyCategory, strModifyPprice, strModifyStock, strModifyPSalesCount;

    private String strModifyPimg, strModifyPDetailimg, strModifyPbname, strModifyPname;

    private Button MGRemoveProduct, MGModifiyProduct;

    Dialog dialog;

    Dialog dialog2;

    private ItemsDomain item = null;

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_product_modify);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        ModifyPid = (EditText) findViewById(R.id.ModifyPid);
        ModifyCategory = (EditText) findViewById(R.id.ModifyCategory);
        ModifyPimg = (EditText) findViewById(R.id.ModifyPimg);
        ModifyPDetailimg = (EditText) findViewById(R.id.ModifyPDetailimg);
        ModifyPbname = (EditText) findViewById(R.id.ModifyPbname);
        ModifyPname = (EditText) findViewById(R.id.ModifyPname);
        ModifyPprice = (EditText) findViewById(R.id.ModifyPprice);
        ModifyStock = (EditText) findViewById(R.id.ModifyStock);
        ModifySalesCount = (EditText) findViewById(R.id.ModifySalesCount);
        MGRemoveProduct = (Button) findViewById(R.id.MGRemoveProduct);
        MGModifiyProduct = (Button) findViewById(R.id.MGProductModify);

        final Object object = getIntent().getSerializableExtra("ManageProductModify");

        if (object instanceof ItemsDomain) {
            item = (ItemsDomain) object;
        }

        database = FirebaseDatabase.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Product");

        firebaseAuth = FirebaseAuth.getInstance();

        dialog = new Dialog(ManageProductModifyActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        dialog2 = new Dialog(ManageProductModifyActivity.this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm2);

        ModifyPid.setText(String.valueOf(item.getPid()));
        ModifyCategory.setText(String.valueOf(item.getCategory()));
        ModifyPimg.setText(String.valueOf(item.getPimg()));
        ModifyPDetailimg.setText(String.valueOf(item.getPdetailimg()));
        ModifyPbname.setText(String.valueOf(item.getPbname()));
        ModifyPname.setText(String.valueOf(item.getPname()));
        ModifyPprice.setText(String.valueOf(item.getPprice()));
        ModifyStock.setText(String.valueOf(item.getStock()));
        ModifySalesCount.setText(String.valueOf(item.getSalescount()));


        MGRemoveProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();

                TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
                confirmTextView.setText("상품을 삭제하시겠습니까?\n삭제 후에는 작업을 되돌릴 수 없습니다.");

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
                        databaseReference.child(String.valueOf(item.getPid())).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                Log.d("ManageProductModify", "상품 삭제 완료");
                                Intent intent = new Intent(ManageProductModifyActivity.this, ProductListActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });
            }
        });

        MGModifiyProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog2.show();

                TextView confirmTextView = dialog2.findViewById(R.id.confirmTextView);
                confirmTextView.setText("상품 정보를 수정하시겠습니까?\n수정 후에는 작업을 되돌릴 수 없습니다.");

                Button btnleft2 = dialog2.findViewById(R.id.btn_left);
                btnleft2.setText("취소");
                btnleft2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog2.dismiss();
                    }
                });

                Button btnright2 = dialog2.findViewById(R.id.btn_right);
                btnright2.setText("확인");
                btnright2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog2.dismiss();

                        try {
                            strModifyPid = Integer.parseInt(ModifyPid.getText().toString().trim());
                            strModifyCategory = Integer.parseInt(ModifyCategory.getText().toString().trim());
                            strModifyPimg = ModifyPimg.getText().toString().trim();
                            strModifyPDetailimg = ModifyPDetailimg.getText().toString().trim();
                            strModifyPbname = ModifyPbname.getText().toString().trim();
                            strModifyPname = ModifyPname.getText().toString().trim();
                            strModifyPprice = Integer.parseInt(ModifyPprice.getText().toString().trim());
                            strModifyStock = Integer.parseInt(ModifyStock.getText().toString().trim());
                            strModifyPSalesCount = Integer.parseInt(ModifySalesCount.getText().toString().trim());

                            // Firebase에 값 업데이트
                            databaseReference.child(String.valueOf(item.getPid())).child("pid").setValue(strModifyPid);
                            databaseReference.child(String.valueOf(item.getPid())).child("category").setValue(strModifyCategory);
                            databaseReference.child(String.valueOf(item.getPid())).child("pdetailimg").setValue(strModifyPDetailimg);
                            databaseReference.child(String.valueOf(item.getPid())).child("pimg").setValue(strModifyPimg);
                            databaseReference.child(String.valueOf(item.getPid())).child("pbname").setValue(strModifyPbname);
                            databaseReference.child(String.valueOf(item.getPid())).child("pname").setValue(strModifyPname);
                            databaseReference.child(String.valueOf(item.getPid())).child("pprice").setValue(strModifyPprice);
                            databaseReference.child(String.valueOf(item.getPid())).child("stock").setValue(strModifyStock);
                            databaseReference.child(String.valueOf(item.getPid())).child("salescount").setValue(strModifyPSalesCount);

                            Log.d("ManageProductModify", "상품 수정 완료: " + strModifyPSalesCount);

                            databaseReference.child(String.valueOf(item.getPid())).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    ItemsDomain updatedProduct = dataSnapshot.getValue(ItemsDomain.class);
                                    Intent intent = new Intent(ManageProductModifyActivity.this, ManageProductModifyActivity.class);
                                    intent.putExtra("ManageProductModify", updatedProduct);
                                    startActivity(intent);
                                    Toast.makeText(ManageProductModifyActivity.this, "상품 수정이 되었습니다.", Toast.LENGTH_SHORT).show();
                                    finish(); // 현재 액티비티를 종료
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    // 에러 처리
                                    Log.e("ManageProductModify", "상품 수정 실패", databaseError.toException());
                                }
                            });
                        } catch (NumberFormatException e) {
                            Toast.makeText(ManageProductModifyActivity.this, "입력한 값이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}