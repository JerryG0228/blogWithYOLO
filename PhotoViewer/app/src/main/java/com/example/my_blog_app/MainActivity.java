package com.example.my_blog_app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Intent;
import android.provider.MediaStore;


public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    TextView textView;
    String site_url = "http://10.0.2.2:8000"; // local
    //String site_url = "https://jerryzoo.pythonanywhere.com/"; // pythonanywhere
    JSONObject post_json;
    String imageUrl = null;
    Bitmap bmImg = null;
    CloadImage taskDownload;
    View btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> showInputDialog());
    }

    private static final int REQUEST_IMAGE_PICK = 100;
    private TextView tvImagePath;
    private Uri selectedImageUri;
    private String createdDate;
    private String publishedDate;


    private void showInputDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_data, null);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextText = dialogView.findViewById(R.id.editTextText);
        Button btnSelectCreatedDate = dialogView.findViewById(R.id.btnSelectCreatedDate);
        Button btnSelectPublishedDate = dialogView.findViewById(R.id.btnSelectPublishedDate);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        TextView tvCreatedDate = dialogView.findViewById(R.id.tvCreatedDate);
        TextView tvPublishedDate = dialogView.findViewById(R.id.tvPublishedDate);
        tvImagePath = dialogView.findViewById(R.id.tvImagePath);

        // Created Date Picker
        btnSelectCreatedDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);

                        // Time Picker after date selection
                        new TimePickerDialog(MainActivity.this,
                                (view1, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    createdDate = String.format("%04d-%02d-%02dT%02d:%02d:00Z",
                                            year, month + 1, dayOfMonth, hourOfDay, minute);
                                    tvCreatedDate.setText("Created Date: " + createdDate);
                                },
                                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Published Date Picker
        btnSelectPublishedDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);

                        // Time Picker after date selection
                        new TimePickerDialog(MainActivity.this,
                                (view1, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    publishedDate = String.format("%04d-%02d-%02dT%02d:%02d:00Z",
                                            year, month + 1, dayOfMonth, hourOfDay, minute);
                                    tvPublishedDate.setText("Published Date: " + publishedDate);
                                },
                                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Image Picker
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        new AlertDialog.
                Builder(this)
                .setTitle("Enter Post Data")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) ->
                {
                    String title = editTextTitle.getText().toString();
                    String text = editTextText.getText().toString();

                    // PutPost 실행
                    new PutPost()
                            .execute(title, text, createdDate, publishedDate, selectedImageUri != null ? selectedImageUri.toString() : "");
                    Toast.makeText(getApplicationContext(), "New Post Created", Toast.LENGTH_LONG).show();
                }).setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss()).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (tvImagePath != null) {  // tvImagePath가 null인지 확인
                tvImagePath.setText("Image Selected: " + selectedImageUri.toString());
            }

            // 모달 창 표시
            ImageInfoDialogFragment dialog = ImageInfoDialogFragment.newInstance(selectedImageUri);
            dialog.show(getSupportFragmentManager(), "ImageInfoDialog");
        }
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "동기화", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);
                        imageUrl = post_json.getString("image");
                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            bitmapList.add(imageBitmap);
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private class PutPost extends AsyncTask<String, Void, Void> {
        private static final String BOUNDARY = "*****"; // 원하는 boundary 문자열
        private static final String LINE_END = "\r\n";
        private static final String TWO_HYPHENS = "--";

        @Override
        protected Void doInBackground(String... params) {
            try {
                String apiUrl = "http://10.0.2.2:8000/api_root/Post/"; // local
                //String apiUrl = "https://jerryzoo.pythonanywhere.com/api_root/Post/"; // pythonanywhere
                String token = "3d078969cebfd1a2924906f96ccb0fc4e81974e8";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();

                // Author
                os.write((TWO_HYPHENS + BOUNDARY + LINE_END).getBytes());
                os.write(("Content-Disposition: form-data; name=\"author\"" + LINE_END).getBytes());
                os.write(LINE_END.getBytes());
                os.write("1".getBytes("UTF-8"));
                os.write(LINE_END.getBytes());

                // Title
                os.write((TWO_HYPHENS + BOUNDARY + LINE_END).getBytes());
                os.write(("Content-Disposition: form-data; name=\"title\"" + LINE_END).getBytes());
                os.write(LINE_END.getBytes());
                os.write(params[0].getBytes("UTF-8"));
                os.write(LINE_END.getBytes());

                // Text
                os.write((TWO_HYPHENS + BOUNDARY + LINE_END).getBytes());
                os.write(("Content-Disposition: form-data; name=\"text\"" + LINE_END).getBytes());
                os.write(LINE_END.getBytes());
                os.write(params[1].getBytes("UTF-8"));
                os.write(LINE_END.getBytes());

                // Created Date
                os.write((TWO_HYPHENS + BOUNDARY + LINE_END).getBytes());
                os.write(("Content-Disposition: form-data; name=\"created_date\"" + LINE_END).getBytes());
                os.write(LINE_END.getBytes());
                os.write(params[2].getBytes("UTF-8"));
                os.write(LINE_END.getBytes());

                // Published Date
                os.write((TWO_HYPHENS + BOUNDARY + LINE_END).getBytes());
                os.write(("Content-Disposition: form-data; name=\"published_date\"" + LINE_END).getBytes());
                os.write(LINE_END.getBytes());
                os.write(params[3].getBytes("UTF-8"));
                os.write(LINE_END.getBytes());

                // Image
                if (params[4] != null && !params[4].isEmpty()) {
                    os.write((TWO_HYPHENS + BOUNDARY + LINE_END).getBytes());
                    os.write(("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"" + LINE_END).getBytes());
                    os.write(("Content-Type: image/jpeg" + LINE_END).getBytes());
                    os.write(LINE_END.getBytes());

                    // Load the image from the URI
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(Uri.parse(params[4]));
                        if (imageStream == null) {
                            Log.e("PutPost", "Image InputStream is null");
                            return null; // 이미지 스트림이 null이면 종료
                        }

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = imageStream.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        Log.e("PutPost", "Error reading image: " + e.getMessage());
                        return null; // 에러가 발생하면 종료
                    } finally {
                        if (imageStream != null) {
                            imageStream.close();
                        }
                    }

                    os.write(LINE_END.getBytes());
                }

                os.write((TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END).getBytes()); // End of multipart/form-data
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Handle success
                    InputStream responseStream = conn.getInputStream();
                    // Read the response if needed
                    BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.d("PostSuccess", "Response: " + response.toString());
                } else {
                    // Log or handle failure
                    InputStream errorStream = conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();
                    Log.e("PostError", "Error response: " + errorResponse.toString());
                }
            } catch (IOException e) {
                Log.e("PutPost", "IOException: " + e.getMessage());
            } catch (Exception e) {
                Log.e("PutPost", "Unexpected error: " + e.getMessage());
            }
            return null;
        }
    }
}