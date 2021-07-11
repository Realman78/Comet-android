package com.example.comet;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;

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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    String TAG = "Heoking shit scoob";
    ImageView imgView;
    TextView codeTV;
    TextView getFilesTV;
    TextView leftTV;
    TextView timeRemTV;
    int i;
    int counter = 0;
    CountDownTimer timer;

    ArrayList<Uri> mArrayUri;
    boolean isLast = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.loadimage);
        imgView = findViewById(R.id.targetimage);
        codeTV = findViewById(R.id.codeTV);
        getFilesTV = findViewById(R.id.getFilesTV);
        leftTV = findViewById(R.id.leftTv);
        timeRemTV = findViewById(R.id.timeRemainingTV);

        codeTV.setText("");
        HashMap config = new HashMap();
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
    /*public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "test", null);
        return Uri.parse(path);
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == 1 && resultCode == RESULT_OK
                    && null != data) {
                mArrayUri = new ArrayList<>();
                getFilesTV.setVisibility(View.INVISIBLE);
                leftTV.setVisibility(View.INVISIBLE);
                timeRemTV.setVisibility(View.INVISIBLE);
                imgView.setImageURI(null);
                codeTV.setText("");
                if (timer != null)
                    timer.cancel();
                if (data.getClipData() != null) {
                    codeTV.setText("Loading...");
                    int cout = data.getClipData().getItemCount();
                    for (int i = 0; i < cout; i++) {
                        // adding imageuri in array
                        Uri imageurl = data.getClipData().getItemAt(i).getUri();
                        mArrayUri.add(imageurl);
                        Log.i(TAG, "onActivityResult: " + imageurl);
                    }

                } else {
                    Uri imageurl = data.getData();
                    mArrayUri.add(imageurl);

                }
                int min = 100000;
                int max = 999999;
                final int code = new Random().nextInt((max - min) + 1) + min;
                counter = 0;
                for (final Uri tmpUri : mArrayUri){
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
                                    request(resultData, mArrayUri.size()-1, tmpUri);
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

    public void request(Map<String, String> body, final int left, final Uri showImageUri){
        String url = "http://192.168.1.101:3000/";
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject jsonObject;
        //Ayo why no update github?

        jsonObject = new JSONObject(body);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (counter++ == left || left == 0){
                                codeTV.setText(response.getString("code"));
                                getFilesTV.setVisibility(View.VISIBLE);
                                imgView.setImageURI(showImageUri);
                                timeRemTV.setVisibility(View.VISIBLE);
                                if (left >= 1){
                                    leftTV.setText(String.format("+%d more", left));
                                    leftTV.setVisibility(View.VISIBLE);
                                }


                                timer = new CountDownTimer(300*1000, 1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                        int secondsUntilFinished = (int) millisUntilFinished/1000;
                                        int minutes = secondsUntilFinished/60;
                                        int seconds = secondsUntilFinished%60;
                                        timeRemTV.setText(String.format("Time remaining: %02d:%02d", minutes, seconds));
                                    }

                                    @Override
                                    public void onFinish() {
                                        getFilesTV.setVisibility(View.INVISIBLE);
                                        leftTV.setVisibility(View.INVISIBLE);
                                        imgView.setImageURI(null);
                                        timeRemTV.setVisibility(View.INVISIBLE);
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