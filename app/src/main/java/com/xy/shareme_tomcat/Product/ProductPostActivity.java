package com.xy.shareme_tomcat.Product;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.xy.shareme_tomcat.R;
import com.xy.shareme_tomcat.adapter.ImageQueueAdapter;
import com.xy.shareme_tomcat.data.ImageObject;
import com.xy.shareme_tomcat.network_helper.ImageUploadTask;
import com.xy.shareme_tomcat.network_helper.MyOkHttp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.xy.shareme_tomcat.data.DataHelper.KEY_CONDITION;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_NOTE;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PHOTO1;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PHOTO2;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PHOTO3;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PHOTO4;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PHOTO5;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PRICE;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PRODUCT;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PRODUCT_ID;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_PS;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_SELLER;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_STATUS;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_TITLE;
import static com.xy.shareme_tomcat.data.DataHelper.KEY_TYPE;
import static com.xy.shareme_tomcat.adapter.ImageQueueAdapter.REQUEST_ALBUM;
import static com.xy.shareme_tomcat.adapter.ImageQueueAdapter.REQUEST_CROP;

public class ProductPostActivity extends AppCompatActivity implements View.OnClickListener {
    private Context context;

    private RecyclerView recyclerView;
    private ArrayList<ImageObject> images = new ArrayList<>();
    private ImageQueueAdapter queueAdapter;
    private int itemIndex, itemAmount;

    private EditText edtTitle, edtStatus, edtPrice, edtPS;
    private CheckBox chkAI, chkFN, chkFT, chkIB, chkBM, chkIM, chkAF, chkCD, chkCC, chkDM, chkGN;

    private String title, condition, note, price, ps;
    private StringBuffer sbDep;

    private ImageUploadTask imageTask;
    private TextView txtUploadHint;
    private Dialog dlgUpload;
    private MyOkHttp conn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_post);
        context = this;

        //設置Toolbar相關元件
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ImageView btnPost = (ImageView) toolbar.findViewById(R.id.btnPost);
        btnPost.setOnClickListener(this);

        //添加一張空白圖到佇列
        Bitmap bitmap = null;
        images.add(new ImageObject(bitmap)); //此為空白圖

        recyclerView = (RecyclerView) findViewById(R.id.recy_books);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        queueAdapter = new ImageQueueAdapter(context, images, this);
        recyclerView.setAdapter(queueAdapter); //顯示recyclerView
        images = null;

        //商品資訊元件
        edtTitle = (EditText) findViewById(R.id.edtTitle);
        edtStatus = (EditText) findViewById(R.id.edtStatus);
        edtPrice = (EditText) findViewById(R.id.edtPrice);
        edtPS = (EditText) findViewById(R.id.edtPS);
        chkAI = (CheckBox) findViewById(R.id.chkAI);
        chkFN = (CheckBox) findViewById(R.id.chkFN);
        chkFT = (CheckBox) findViewById(R.id.chkFT);
        chkIB = (CheckBox) findViewById(R.id.chkIB);
        chkBM = (CheckBox) findViewById(R.id.chkBM);
        chkIM = (CheckBox) findViewById(R.id.chkIM);
        chkAF = (CheckBox) findViewById(R.id.chkAF);
        chkCD = (CheckBox) findViewById(R.id.chkCD);
        chkCC = (CheckBox) findViewById(R.id.chkCC);
        chkDM = (CheckBox) findViewById(R.id.chkDM);
        chkGN = (CheckBox) findViewById(R.id.chkGN);

        Spinner spnNote = (Spinner) findViewById(R.id.spnNote);
        spnNote.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                note = String.valueOf(i); //0為請選擇
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //上傳進度的Dialog
        prepareDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //詢問是否開啟權限
        switch(requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得權限，進行檔案存取
                    queueAdapter.pickImageDialog();
                }else {
                    //使用者拒絕權限，停用檔案存取功能，並顯示訊息
                    Toast.makeText(context, "未給予權限，無法選擇圖片", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //挑選完圖片後執行此方法，將圖片放上cardView
        if (resultCode != RESULT_OK)
            return;

        Uri selectedImageUri = data.getData();
        switch (requestCode) {
            case REQUEST_ALBUM:
                if (!queueAdapter.createImageFile())
                    return;
                if (selectedImageUri != null)
                    queueAdapter.cropImage(selectedImageUri);
                break;
            case REQUEST_CROP:
                final int position = queueAdapter.getPressedPosition();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageObject image = queueAdapter.getItem(position);
                        boolean isEmptyCard = image.getBitmap() == null; //退出選圖畫面後，若為null代表剛剛沒選圖
                        boolean bitmapIsNull = true;
                        do {
                            bitmapIsNull = queueAdapter.mImageFileToBitmap(image);
                        } while (bitmapIsNull);

                        image.setEntity(true);
                        queueAdapter.setItem(position, image);

                        if (isEmptyCard && queueAdapter.getItemCount() < 5) { //isEmptyCard意義不明
                            Bitmap bitmap = null;
                            queueAdapter.addItem(new ImageObject(bitmap, false)); //再新增一張空白圖
                        }
                    }
                }).start();
                recyclerView.scrollToPosition(position);

                break;
        }
    }

    private boolean isInfoValid() {
        StringBuffer sbErrMsg = new StringBuffer();
        title = edtTitle.getText().toString();
        price = edtPrice.getText().toString();
        condition = edtStatus.getText().toString();
        ps = edtPS.getText().toString();
        sbDep = new StringBuffer();

        //書名、價格
        if (title.equals("")) sbErrMsg.append("書名\n");
        if (price.equals(""))
            sbErrMsg.append("價格\n"); //EditText已限制只能輸入>=0的整數，就算複製其他文字過來也能過濾掉
        else
            price = String.valueOf(Integer.parseInt(price)); //避免有人開頭輸入一堆0

        //科系
        if (chkGN.isChecked()) sbDep.append("00#");
        if (chkAI.isChecked()) sbDep.append("01#");
        if (chkFN.isChecked()) sbDep.append("02#");
        if (chkFT.isChecked()) sbDep.append("03#");
        if (chkIB.isChecked()) sbDep.append("04#");
        if (chkBM.isChecked()) sbDep.append("05#");
        if (chkIM.isChecked()) sbDep.append("06#");
        if (chkAF.isChecked()) sbDep.append("07#");
        if (chkCD.isChecked()) sbDep.append("A#");
        if (chkCC.isChecked()) sbDep.append("B#");
        if (chkDM.isChecked()) sbDep.append("C#");
        if (sbDep.length() == 0)
            sbErrMsg.append("分類\n");

        //書況、筆記
        if (condition.equals("")) sbErrMsg.append("書況\n");
        if (note.equals("0") || note.equals("")) sbErrMsg.append("筆記提供方式\n");


        if (sbErrMsg.length() != 0) {
            sbErrMsg.insert(0, "以下資料未填寫：\n");
            AlertDialog.Builder msgbox = new AlertDialog.Builder(context);
            msgbox.setTitle("刊登商品")
                    .setPositiveButton("確定", null)
                    .setMessage(sbErrMsg.substring(0, sbErrMsg.length() - 1))
                    .show();
            return false;
        }else
            return true;
    }

    private void prepareDialog() {
        dlgUpload = new Dialog(context);
        dlgUpload.setContentView(R.layout.dlg_uploading);
        dlgUpload.setCancelable(false);
        txtUploadHint = (TextView) dlgUpload.findViewById(R.id.txtHint);

        LinearLayout layUpload = (LinearLayout) dlgUpload.findViewById(R.id.layUpload);
        layUpload.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder msgbox = new AlertDialog.Builder(context);
                msgbox.setTitle("刊登商品")
                        .setMessage("確定取消上傳嗎？")
                        .setNegativeButton("否", null)
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    imageTask = null;
                                    conn.cancel();
                                    dlgUpload.dismiss();
                                    Toast.makeText(context, "上傳已取消", Toast.LENGTH_SHORT).show();
                                }catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).show();
                return true;
            }
        });
    }

    private void initTrdWaitPhoto(boolean restart) {
        Thread trdWaitPhoto = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                hdrWaitPhoto.sendMessage(hdrWaitPhoto.obtainMessage());
            }
        });
        if (restart)
            trdWaitPhoto.start();
    }

    private Handler hdrWaitPhoto = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (imageTask == null)
                return;

            String fileName = imageTask.getPhotoName();
            if (fileName == null) {
                initTrdWaitPhoto(true); //還沒收到檔名，繼續監聽
            }else {
                initTrdWaitPhoto(false);
                //寫入檔名
                ImageObject image = queueAdapter.getItem(itemIndex);
                image.setFileName(fileName);
                queueAdapter.setItem(itemIndex, image);

                //上傳下一張
                itemIndex++;
                if (itemIndex >= itemAmount) { //圖片都上傳完
                    postProduct();
                    return;
                }
                final ImageObject newImage = queueAdapter.getItem(itemIndex);
                new Thread(new Runnable() {
                    public void run() {
                        imageTask = new ImageUploadTask(context, getString(R.string.link_upload_image));
                        imageTask.uploadFile(newImage.getBitmap());
                    }
                }).start();
                initTrdWaitPhoto(true);
                txtUploadHint.setText(getString(R.string.hint_upload_photo, String.valueOf(itemIndex + 1), String.valueOf(itemAmount)));
            }
        }
    };

    private void postProduct() {
        conn = new MyOkHttp(ProductPostActivity.this, new MyOkHttp.TaskListener() {
            @Override
            public void onFinished(String result) {
                if (result == null) {
                    Toast.makeText(context, "連線失敗", Toast.LENGTH_SHORT).show();
                    dlgUpload.dismiss();
                    return;
                }
                try {
                    Toast.makeText(context, "刊登成功", Toast.LENGTH_SHORT).show();
                    JSONObject resObj = new JSONObject(result);
                    if (resObj.getBoolean(KEY_STATUS)) {
                        dlgUpload.dismiss();
                        //上傳成功
                        JSONObject obj = resObj.getJSONObject(KEY_PRODUCT);
                        Intent it = new Intent(context, ProductDetailActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString(KEY_PRODUCT_ID, obj.getString(KEY_PRODUCT_ID));
                        bundle.putString(KEY_TITLE, obj.getString(KEY_TITLE));
                        it.putExtras(bundle);
                        startActivity(it);
                        finish();
                    }else
                        Toast.makeText(context, "伺服器發生例外", Toast.LENGTH_SHORT).show();
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        //開始連線
        try {
            //修正圖檔名稱
            String[] fileName = new String[5];
            for (int i=0; i<itemAmount; i++)
                fileName[i] = queueAdapter.getItem(i).getFileName();
            for (int i=0; i<5; i++) {
                if (fileName[i] == null)
                    fileName[i] = "";
            }

            JSONObject reqObj = new JSONObject();
            reqObj.put(KEY_TITLE, title);
            reqObj.put(KEY_PRICE, price);
            reqObj.put(KEY_TYPE, sbDep.toString());
            reqObj.put(KEY_CONDITION, condition);
            reqObj.put(KEY_NOTE, note);
            reqObj.put(KEY_PS, ps);
            reqObj.put(KEY_SELLER, "10346011");
            reqObj.put(KEY_PHOTO1, fileName[0]);
            reqObj.put(KEY_PHOTO2, fileName[1]);
            reqObj.put(KEY_PHOTO3, fileName[2]);
            reqObj.put(KEY_PHOTO4, fileName[3]);
            reqObj.put(KEY_PHOTO5, fileName[4]);
            conn.execute(getString(R.string.link_post_product), reqObj.toString());
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnPost:
                if (!isInfoValid())
                    return;

                //初始化指標、確認圖片總數量
                itemIndex = 0;
                itemAmount = queueAdapter.getEntityAmount();
                if (itemAmount == 0) {
                    Toast.makeText(context, "未選擇圖片", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (int i=itemIndex; i<itemAmount; i++) {
                    final ImageObject newImage = queueAdapter.getItem(i);
                    if (newImage.getBitmap() != null) {
                        //開始上傳
                        new Thread(new Runnable() {
                            public void run() {
                                imageTask = new ImageUploadTask(context, getString(R.string.link_upload_image));
                                imageTask.uploadFile(newImage.getBitmap());
                            }
                        }).start();
                        initTrdWaitPhoto(true); //監聽正在上傳的圖片檔名

                        txtUploadHint.setText(getString(R.string.hint_upload_photo, String.valueOf(itemIndex + 1), String.valueOf(itemAmount)));
                        dlgUpload.show();
                        break;
                    }

                    if (itemIndex == itemAmount - 1)
                        Toast.makeText(context, "未選擇圖片", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        cancelConnection();
        System.gc();
        super.onDestroy();
    }

    private void cancelConnection() {
        try {
            conn.cancel();
        }catch (NullPointerException e) {}
    }
}
