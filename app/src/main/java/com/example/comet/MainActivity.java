package com.example.comet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;

import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import org.json.JSONObject;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Heoking shit scoob";
    private ImageView imgView;
    private TextView codeTV;
    private TextView getFilesTV;
    private TextView leftTV;
    private TextView timeRemTV;

    private int counter = 0;
    private boolean uploading = false;
    private CountDownTimer timer;
    static final int MIN = 100000;
    static final int MAX = 999999;
    static final int REQUEST_CODE = 1;
    static final String URL = "https://fcomet.herokuapp.com/";
    String code = "";
    private Uri lastPic;
    private ArrayList<Uri> mArrayUri;
    private Uri largestUri = null;
    private long maxSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.loadImage);
        button.setEnabled(false);
        imgView = findViewById(R.id.targetImage);
        codeTV = findViewById(R.id.codeTV);
        getFilesTV = findViewById(R.id.getFilesTV);
        leftTV = findViewById(R.id.leftTv);
        timeRemTV = findViewById(R.id.timeRemainingTV);

        mArrayUri = new ArrayList<>();
        codeTV.setText("");

        HashMap<String, String> config = new HashMap<>();
        config.put("cloud_name", "dzmz24nr0");
        config.put("secure", "true");
        config.put("api_key", "411549117332673");
        config.put("api_secret", "crgNRrcVJ7v6PA76-8HlbEzx5vE");
        new Cloudinary(config);

        MediaManager.init(this, config);
        button.setOnClickListener(v -> {
            if (uploading){
                Toast.makeText(getApplicationContext(), "Please wait until the previous file uploads", Toast.LENGTH_SHORT).show();
                return;
            }
            openChooser();
        });
        codeTV.setOnLongClickListener(v -> {
            if (codeTV.getVisibility() == View.VISIBLE && !codeTV.getText().toString().contains("Loading") && !codeTV.getText().toString().contains("Uploading")){
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("link", URL+code);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Link copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        else
            openChooser();
        button.setEnabled(true);
    }

    private boolean isConnectedToInternet(){
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void openChooser() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] extraMimeTypes = {"image/*", "video/*", "application/*", "text/*", "audio/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && null != data) {
                mArrayUri.clear();
                cleanHome();
                uploading = true;
                codeTV.setText(getString(R.string.loading_string));
                if (timer != null) timer.cancel();

                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        long size;

                        if (imageUri.getScheme().equals("file")){
                            size = getFileSize(imageUri);
                        }else{
                            size = getVideoSize(imageUri);
                            if (size == 0){
                                size = getImageSize(imageUri);
                            }
                        }
                        if (size == 0){
                            size = getAudioSize(getApplicationContext(), imageUri);
                        }
                        if (size > maxSize){
                            maxSize = size;
                            largestUri = imageUri;
                        }
                        mArrayUri.add(imageUri);
                    }
                } else {
                    Uri imageUri = data.getData();
                    mArrayUri.add(imageUri);
                }

                final int code = new Random().nextInt((MAX - MIN) + 1) + MIN;
                counter = 0;
                HashMap<String, String> options = new HashMap<>();
                options.put("resource_type", "auto");
                for (final Uri tmpUri : mArrayUri){
                    MediaManager.get().upload(tmpUri)
                            .options(options)
                            .unsigned("julve1gi")
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {

                                }

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {
                                    if (tmpUri == largestUri || maxSize == 0) {
                                        Long percentage = (long) ((float) bytes / totalBytes * 100);
                                        codeTV.setText(String.format("Uploading...  %s%%", percentage));
                                    }
                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    resultData.put("code", Integer.toString(code));
                                    resultData.put("fileName", getFileName(tmpUri));
                                    lastPic = tmpUri;
                                    request(resultData, mArrayUri.size()-1, tmpUri);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    String errorMessage = isConnectedToInternet() ? "Something went wrong" : "Something went wrong, check your internet connection";
                                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG)
                                            .show();
                                    cleanHome();
                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {

                                }
                            })
                            .dispatch();
                }

            } else {
                Toast.makeText(this, "You haven't picked an Image/Video",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = isConnectedToInternet() ? "Something went wrong" : "Something went wrong, check your internet connection";
            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG)
                    .show();
            cleanHome();
        }
    }

    public void request(Map<String, String> body, final int left, final Uri showImageUri){
        RequestQueue queue = Volley.newRequestQueue(this);

        final JSONObject jsonObject = new JSONObject(body);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, URL, jsonObject, new Response.Listener<JSONObject>() {

                    @SuppressLint({"DefaultLocale", "SetTextI18n"})
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (counter++ == left || left == 0){
                                uploading = false;
                                code = response.getString("code");
                                codeTV.setText("fcomet.herokuapp.com\n\nCode: "+ code);
                                getFilesTV.setVisibility(View.VISIBLE);
                                imgView.setImageURI(showImageUri);
                                Toast.makeText(getApplicationContext(), "Long press the link to copy", Toast.LENGTH_SHORT).show();

                                imgView.setVisibility(View.VISIBLE);
                                timeRemTV.setVisibility(View.VISIBLE);
                                if (left >= 1){
                                    leftTV.setText(String.format("+%d more", left));
                                    leftTV.setVisibility(View.VISIBLE);
                                }
                                
                                //If no picture provided, use video thumbnail in imageView
                                if (imgView.getDrawable() == null){
                                    try{
                                        Bitmap bmp;
                                        MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
                                        mMMR.setDataSource(getApplicationContext(), lastPic);
                                        bmp = mMMR.getFrameAtTime();
                                        imgView.setImageBitmap(bmp);
                                        if (bmp == null){
                                            imgView.setBackgroundResource(R.drawable.file);
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        imgView.setBackgroundResource(R.drawable.file);
                                    }
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

                            } catch (Exception e) {
                                e.printStackTrace();
                                imgView.setBackgroundResource(R.drawable.file);
                            }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = isConnectedToInternet() ? "Something went wrong" : "Something went wrong, check your internet connection";
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG)
                                .show();
                        Log.i(TAG, "error: " + error);
                        cleanHome();
                    }
                });
        queue.add(jsonObjectRequest);
    }

    private long getVideoSize(Uri uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try{
            retriever.setDataSource(getApplicationContext(), uri);
            if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null){
                long bitrate = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
                long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                return (bitrate / 8 * duration / 1000);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return 0;
    }
    private long getImageSize(Uri uri) {
        try{
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageInByte = stream.toByteArray();
            return imageInByte.length;
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    private long getFileSize(Uri uri){
        try{
            return new File(String.valueOf(uri)).length();
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }
    private long getAudioSize(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Audio.Media.SIZE };
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            cursor.moveToFirst();
            return Long.parseLong(cursor.getString(column_index));
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    private void cleanHome(){
        getFilesTV.setVisibility(View.INVISIBLE);
        leftTV.setVisibility(View.INVISIBLE);
        imgView.setImageURI(null);
        imgView.setBackgroundResource(0);
        imgView.setVisibility(View.INVISIBLE);
        timeRemTV.setVisibility(View.INVISIBLE);
        codeTV.setText("");
        uploading = false;
        maxSize = 0;
    }
}