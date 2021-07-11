package com.example.comet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {
    String TAG = "Heoking shit scoob";
    ImageView imgView;
    TextView codeTV;
    TextView getFilesTV;
    TextView leftTV;

    ArrayList<Uri> mArrayUri;
    boolean isLast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.loadimage);
        imgView = findViewById(R.id.targetimage);
        codeTV = findViewById(R.id.codeTV);
        getFilesTV = findViewById(R.id.getFilesTV);
        leftTV = findViewById(R.id.leftTv);

        codeTV.setText("");
        Map config = new HashMap();
        config.put("cloud_name", "dzmz24nr0");
        config.put("secure", true);
        config.put("api_key", "411549117332673");
        config.put("api_secret", "crgNRrcVJ7v6PA76-8HlbEzx5vE");


        MediaManager.init(this, config);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), 1);
            }
        });
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "test", null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == 1 && resultCode == RESULT_OK
                    && null != data) {
                mArrayUri = new ArrayList<>();
                if (data.getClipData() != null) {
                    int cout = data.getClipData().getItemCount();
                    for (int i = 0; i < cout; i++) {
                        // adding imageuri in array
                        Uri imageurl = data.getClipData().getItemAt(i).getUri();
                        mArrayUri.add(imageurl);
                        Log.i(TAG, "onActivityResult: " + imageurl);
                    }
                    imgView.setImageURI( data.getClipData().getItemAt(0).getUri());

                } else {
                    Uri imageurl = data.getData();
                    mArrayUri.add(imageurl);

                }
                int min = 100000;
                int max = 999999;
                final int code = new Random().nextInt((max - min) + 1) + min;
                isLast = false;
                for (int i = 0; i < mArrayUri.size(); i++){
                    if (i ==  mArrayUri.size()-1) {
                        isLast = true;
                    }
                    Uri tmpUri = mArrayUri.get(i);
                    MediaManager.get().upload(tmpUri)
                            .unsigned("julve1gi")
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {

                                }

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {

                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    resultData.put("code", code + "");
                                    //Log.i(TAG, "onSuccess: " + resultData.get("public_id") + "; url = " + resultData.get("url"));
                                    request(resultData, isLast,  mArrayUri.size()-1);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {

                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {

                                }
                            })
                            .dispatch();
                }

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void request(Map<String, String> body, final boolean last, final int left){
        String url = "http://192.168.1.101:3000/";
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject jsonObject = null;
        jsonObject = new JSONObject(body);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (last){
                                getFilesTV.setVisibility(View.VISIBLE);
                                if (left > 0){
                                    leftTV.setText("+" +  left + " more");
                                    leftTV.setVisibility(View.VISIBLE);
                                }

                                codeTV.setText(response.getString("code"));
                                CountDownTimer timer = new CountDownTimer(360*1000, 5*1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {

                                    }

                                    @Override
                                    public void onFinish() {
                                        getFilesTV.setVisibility(View.INVISIBLE);
                                        leftTV.setVisibility(View.INVISIBLE);
                                        imgView.setImageURI(null);
                                        codeTV.setText("");
                                    }
                                };
                                timer.start();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.i(TAG, "error: " + error);

                    }
                });


        queue.add(jsonObjectRequest);
        queue.start();
    }
}