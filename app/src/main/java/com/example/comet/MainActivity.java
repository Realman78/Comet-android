package com.example.comet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String TAG = "Heoking shit scoob";
    ImageView imgView;
    TextView codeTV;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.loadimage);
        imgView = findViewById(R.id.targetimage);
        codeTV = findViewById(R.id.codeTV);
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
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 0);
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
    protected void onActivityResult(int requestCode, final int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: ayo wtf");
        if (resultCode == RESULT_OK){
            Uri targetUri = data != null ? data.getData() : null;

            if (targetUri == null) return;
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                //imgView.setImageBitmap();
                imgView.setImageURI(getImageUri(this, bitmap));

                String requestId = MediaManager.get().upload(getImageUri(this, bitmap))
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
                                Log.i(TAG, "onSuccess: " + resultData.get("public_id") + "; url = " + resultData.get("url"));
                                request(resultData);
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {

                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {

                            }
                        })
                        .dispatch();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                Log.i("error", "error: wtf");
            }

        }
    }

    public void request(Map<String, String> body){
        String url = "http://192.168.1.101:3000/";
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject jsonObject = null;
        jsonObject = new JSONObject(body);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            codeTV.setText(response.getString("code"));
                            CountDownTimer timer = new CountDownTimer(360*1000, 5*1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    Log.i("Tick", "heok");
                                }

                                @Override
                                public void onFinish() {
                                    imgView.setImageURI(null);
                                    codeTV.setText("");
                                }
                            };
                            timer.start();
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