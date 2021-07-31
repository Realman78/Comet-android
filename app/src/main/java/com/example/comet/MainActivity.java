package com.example.comet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
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
import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.utils.ObjectUtils;

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
    View mainLayout;
    Button button;
    int counter = 0;
    CountDownTimer timer;
    Cloudinary cloudinary;
    int min = 100000;
    int max = 999999;
    Uri lastPic;

    ArrayList<Uri> mArrayUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.loadimage);
        imgView = findViewById(R.id.targetimage);
        codeTV = findViewById(R.id.codeTV);
        getFilesTV = findViewById(R.id.getFilesTV);
        leftTV = findViewById(R.id.leftTv);
        timeRemTV = findViewById(R.id.timeRemainingTV);
        mainLayout = findViewById(R.id.mainLayout);

        codeTV.setText("");
        HashMap config = new HashMap();
        config.put("cloud_name", "dzmz24nr0");
        config.put("secure", true);
        config.put("api_key", "411549117332673");
        config.put("api_secret", "crgNRrcVJ7v6PA76-8HlbEzx5vE");
        cloudinary = new Cloudinary(config);

        MediaManager.init(this, config);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 31){
                    Toast.makeText(MainActivity.this, "It wont work on your version", Toast.LENGTH_SHORT).show();

                }else{
                    Intent intent = new Intent();
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent,"Select Media"), 1);
                }

            }
        });
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        if (Build.VERSION.SDK_INT >= 29){
            Toast.makeText(getApplicationContext(), "Recorded videos from gallery wont work", Toast.LENGTH_LONG).show();
        }else{
            button.performClick();
        }
        
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == 1 && resultCode == RESULT_OK && null != data) {
                mArrayUri = new ArrayList<>();
                cleanHome();
                codeTV.setText("Loading...");
                if (timer != null) timer.cancel();

                if (data.getClipData() != null) {
                    int cout = data.getClipData().getItemCount();
                    for (int i = 0; i < cout; i++) {
                        Uri imageurl = data.getClipData().getItemAt(i).getUri();
                        mArrayUri.add(imageurl);
                    }
                } else {
                    Uri imageurl = data.getData();
                    mArrayUri.add(imageurl);
                }

                final int code = new Random().nextInt((max - min) + 1) + min;
                counter = 0;
                for (final Uri tmpUri : mArrayUri){
                    MediaManager.get().upload(tmpUri)
                            .options(ObjectUtils.asMap("resource_type", "auto"))
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
                                    lastPic = tmpUri;
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
                cleanHome();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG)
                    .show();
            cleanHome();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void request(Map<String, String> body, final int left, final Uri showImageUri){
        String url = "http://192.168.1.126:3000/";
        RequestQueue queue = Volley.newRequestQueue(this);

        final JSONObject jsonObject;
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
                                imgView.setVisibility(View.VISIBLE);
                                timeRemTV.setVisibility(View.VISIBLE);
                                if (left >= 1){
                                    leftTV.setText(String.format("+%d more", left));
                                    leftTV.setVisibility(View.VISIBLE);
                                }
                                
                                //If no picture provided, use video thumbnail in imageView
                                if (imgView.getDrawable() == null){
                                    Bitmap bmp;
                                    MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
                                    mMMR.setDataSource(getApplicationContext(), lastPic);
                                    bmp = mMMR.getFrameAtTime();
                                    imgView.setImageBitmap(bmp);
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
                                        cleanHome();
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
                        Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG)
                                .show();
                        Log.i(TAG, "error: " + error);
                        cleanHome();

                    }
                });
        queue.add(jsonObjectRequest);
    }

    private void cleanHome(){
        getFilesTV.setVisibility(View.INVISIBLE);
        leftTV.setVisibility(View.INVISIBLE);
        imgView.setImageURI(null);
        imgView.setVisibility(View.INVISIBLE);
        timeRemTV.setVisibility(View.INVISIBLE);
        codeTV.setText("");
    }
}