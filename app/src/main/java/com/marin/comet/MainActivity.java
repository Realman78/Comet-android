package com.marin.comet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
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
    private EditText codeET;
    private DownloadManager manager;

    private int counter = 0;
    private boolean uploading = false;
    private CountDownTimer timer;
    static final int MIN = 100000;
    static final int MAX = 999999;
    static final int REQUEST_CODE = 1;
    static final int MAX_SIZE = 50000000;
    static final String URL = "http://192.168.1.5:3000/";
    String code = "";
    private Uri lastPic;
    private ArrayList<Uri> mArrayUri;
    private Uri largestUri = null;
    private long maxSize = 0;
    private long reference;
    private long batch_size = 0;


    @SuppressLint("ClickableViewAccessibility")
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
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Button getFilesButton = findViewById(R.id.getFilesButton);

        mArrayUri = new ArrayList<>();
        codeTV.setText("");

        //Status bar color change
        Window window = MainActivity.this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.rgb(44, 52, 60));

        if (!isConnectedToInternet())
            Toast.makeText(getApplicationContext(), "Please connect to the internet otherwise you won't be able to use the app", Toast.LENGTH_LONG).show();

        HashMap<String, String> config = new HashMap<>();
        config.put("cloud_name", "");
        config.put("secure", "true");
        config.put("api_key", "");
        config.put("api_secret", "");
        new Cloudinary(config);

        MediaManager.init(this, config);

        //Popup section
        getFilesButton.setOnClickListener(v ->{
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_window, null);

            int width = LinearLayout.LayoutParams.WRAP_CONTENT;
            int height = LinearLayout.LayoutParams.WRAP_CONTENT;
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

            popupWindow.showAtLocation(imgView, Gravity.CENTER, 0, 0);
            codeET = popupView.findViewById(R.id.codeET);

            codeET.requestFocus();
            showKeyboard();
            toggleBackground(View.INVISIBLE);
            codeET.setOnKeyListener((view, i, keyEvent) -> {
                if (codeET.getText().length() == 6){
                    popupWindow.dismiss();
                    requestCrater(codeET.getText().toString());
                }
                return false;
            });
            popupWindow.setOnDismissListener(() -> {
                closeKeyboard();
                toggleBackground(View.VISIBLE);
            });
            // dismiss the popup window when touched
            popupView.setOnTouchListener((v1, event) -> {
                popupWindow.dismiss();
                return true;
            });
        });
        button.setOnClickListener(v -> {
            if (uploading){
                Toast.makeText(getApplicationContext(), "Please wait until the previous file uploads", Toast.LENGTH_SHORT).show();
                return;
            }
            openChooser();
        });
        //Link copy section
        codeTV.setOnClickListener(v -> {
            if (codeTV.getVisibility() == View.VISIBLE && !codeTV.getText().toString().contains("Loading") && !codeTV.getText().toString().contains("Uploading")){
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("link", URL+code);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Link copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        /*
        Open gallery ASA app opened disabled because of the "get files" feature
        else
           openChooser();
        */
        button.setEnabled(true);
    }

    private void requestCrater(String inputted){
        String checkUrl = URL + inputted + "/get";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(checkUrl, response -> {
            for (int i = 0; i < response.length(); i++){
                try {
                    manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = Uri.parse(response.getJSONObject(i).get("url").toString());
                    DownloadManager.Request request = new DownloadManager.Request(uri)
                            .setTitle(response.getJSONObject(i).get("fileName").toString())
                            .setDescription("Upcomet download")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true);
                    reference = manager.enqueue(request);
                    Log.i(TAG, "onResponse: ref" + reference);
                } catch (Exception e) {
                    String errorMessage = isConnectedToInternet() ? "Something went wrong" : "Something went wrong, check your internet connection";
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG)
                            .show();
                    Log.i(TAG, "error: " + e);
                    cleanHome();
                }
            }
            Log.i(TAG, "onResponse: CKECK: " + response.toString());
        }, error -> {
            String errorMessage = isConnectedToInternet() ? "No files with the code " + inputted : "Something went wrong, check your internet connection";
            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG)
                    .show();
            Log.i(TAG, "error: " + error);
            cleanHome();
        });


// Access the RequestQueue through your singleton class.
       queue.add(jsonArrayRequest);
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == reference){
                Toast.makeText(getApplicationContext(), "Download completed!", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private void toggleBackground(int visibilty){
        getFilesTV.setVisibility(visibilty);
        codeTV.setVisibility(visibilty);
        imgView.setVisibility(visibilty);
        leftTV.setVisibility(visibilty);
        getFilesTV.setVisibility(visibilty);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }

    private boolean isConnectedToInternet(){
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public void showKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        codeET.clearFocus();
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
        if (uri.getScheme().equals("content"))
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
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
                        batch_size+=size;
                        mArrayUri.add(imageUri);
                    }
                } else {
                    Uri imageUri = data.getData();
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
                    batch_size += size;
                    mArrayUri.add(imageUri);
                }
                if (batch_size > MAX_SIZE){
                    Toast.makeText(getApplicationContext(), "Files too large, max size 50MB", Toast.LENGTH_SHORT).show();
                    cleanHome();
                    return;
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
                (Request.Method.POST, URL, jsonObject, response -> {
                    try {
                        if (counter++ == left || left == 0){
                            uploading = false;
                            code = response.getString("code");
                            codeTV.setText("upcomet.herokuapp.com\nCode: "+ code);
                            getFilesTV.setVisibility(View.VISIBLE);
                            imgView.setImageURI(showImageUri);
                            Toast.makeText(getApplicationContext(), "Press the link to copy", Toast.LENGTH_SHORT).show();

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
                                @SuppressLint("DefaultLocale")
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
                }, error -> {
                    String errorMessage = isConnectedToInternet() ? "Something went wrong" : "Something went wrong, check your internet connection";
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG)
                            .show();
                    Log.i(TAG, "error: " + error);
                    cleanHome();
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
        batch_size = 0;
    }
}