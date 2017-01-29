package com.temerarious.mccocr13.temerariousocr.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.helpers.BenchmarkResults;
import com.temerarious.mccocr13.temerariousocr.helpers.NetworkChangeReceiver;
import com.temerarious.mccocr13.temerariousocr.helpers.OCRInitializer;
import com.temerarious.mccocr13.temerariousocr.tasks.PrepareRemote;
import com.temerarious.mccocr13.temerariousocr.tasks.RunLocalOCR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;


public class OCRActivity extends AppCompatActivity {

    public OCRInitializer ocrInitializer = new OCRInitializer(this, this);

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    public static String token;
    private TextView imgSelectorStatus;
    String[] type = {"Local", "Remote", "Benchmark"};
    String selectedMode = type[0];
    ImageView imgCamera, imgGalery;
    public static Button button_records, logoutFB;
    Uri imageUri;
    public static Spinner spinner;
    public static BenchmarkResults benchmarkResults;
    private NetworkChangeReceiver receiver;
    private int tasksTriggered;
    public ArrayList<String> imageName = new ArrayList<String>();
    public static ArrayList<byte[]> imageStream = new ArrayList<byte[]>();
    public static ArrayAdapter<String> adapter;
    public static boolean imageTakenFromCamera = false;
    public static boolean retakePictureOnResume = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_ocr);


        SharedPreferences sharedPref = getSharedPreferences("sessionData", Context.MODE_PRIVATE);
        token = sharedPref.getString("token", "");

        ocrInitializer.initOCR();

        imgCamera = (ImageView) findViewById(R.id.camera);
        imgGalery = (ImageView) findViewById(R.id.gallery);
        button_records=(Button) findViewById(R.id.button_records);

        imgCamera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                select_from_camera();
            }
        });
        imgGalery.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                select_from_galery();
            }
        });

        imgSelectorStatus = (TextView) findViewById(R.id.img_selector_status);
        imgSelectorStatus.setText(getString(R.string.status_no_image));


        logoutFB=(Button) findViewById(R.id.logoutFB);
        logoutFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnectFromFacebook();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        if (MainActivity.login.equals("basicLogin"))
            logoutFB.setVisibility(View.GONE);
        else
            logoutFB.setVisibility(View.VISIBLE);

        if (MainActivity.networkStatus.equals("On"))
        {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, type);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner = (Spinner) findViewById(R.id.spinner);
            spinner.setAdapter(adapter);
            spinner.setPrompt("Select Type");
            spinner.setSelection(1);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedMode = type[position];
                }


                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        }
        else {
            Toast.makeText(this, R.string.noInternet, Toast.LENGTH_SHORT).show();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, type) {
                @Override
                public boolean isEnabled(int position) {
                    if (position == 1 || position == 2) {
                        // Disable the second and third item from Spinner
                        return false;
                    } else {
                        return true;
                    }
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            spinner.setAdapter(adapter);
            spinner.setPrompt("Select Type");
            spinner.setSelection(0);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedMode = type[position];
                }


                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });


        }
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkChangeReceiver();
        registerReceiver(receiver, filter);
    }

    public void disconnectFromFacebook() {
        LoginManager.getInstance().logOut();
    }

    public void select_from_galery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    public void select_from_camera() {
        String filename = "OCR.jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (imageStream.size() > 0) {
            imageStream.clear();
            imageName.clear();
        }

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                onSelectFromGalleryResult(data);
            }
            else if (requestCode == REQUEST_CAMERA)
                try {
                    onCaptureImageResult(imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private void onCaptureImageResult(Uri uri) throws IOException {
        imageTakenFromCamera = true;
        InputStream is = null;

        try {
            is = this.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 2;
            options.inScreenDensity = DisplayMetrics.DENSITY_LOW;
            Bitmap bm = BitmapFactory.decodeStream(is, null, options);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            imageStream.add(stream.toByteArray());
            imageName.add(System.currentTimeMillis() + ".jpg");

            imgSelectorStatus.setText(getString(R.string.status_img_camera));

            String storagePath = Environment.getExternalStorageDirectory().getPath() + "/G13OCR/images/";
            File myDir = new File(storagePath);
            if (!myDir.exists()) {
                myDir.mkdirs();
            }

            String filename = imageName.get(0);
            File myFile = new File(storagePath, filename);
            myFile.createNewFile();

            FileOutputStream fo;
            fo = new FileOutputStream(myFile);
            fo.write(imageStream.get(0));
            fo.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void onSelectFromGalleryResult(Intent data) {
        imageTakenFromCamera = false;
        try {
            if(data.getData()!=null){
                Bitmap bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                imageStream.add(stream.toByteArray());
                imageName.add(System.currentTimeMillis() + ".jpg");
            } else if(data.getClipData() != null){
                ClipData clipData = data.getClipData();
                for(int i = 0; i < clipData.getItemCount(); i++){
                    Bitmap bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), clipData.getItemAt(i).getUri());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                    imageStream.add(stream.toByteArray());
                    imageName.add(System.currentTimeMillis() + ".jpg");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        imgSelectorStatus.setText(getString(R.string.status_img_gallery_1) + " " + String.valueOf(imageStream.size()) + " " + getString(R.string.status_img_gallery_2));

    }

    public void previewImage(View view) {
        if (imageStream.size() == 0) {
            Toast.makeText(this, getString(R.string.toast_no_images), Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), PreviewActivity.class);
            startActivity(intent);
        }
    }

    public void processImage(View view) {

        if (imageStream.size() == 0) {
            Toast.makeText(this, getString(R.string.toast_no_images), Toast.LENGTH_SHORT).show();
        } else if (selectedMode.equals(type[0])) {
            runLocalMode(false);

        } else if (selectedMode.equals(type[1])) {
            runRemoteMode(false);
        } else if (selectedMode.equals(type[2])) {
            runBenchmarkMode();
        }

    }

    private void runLocalMode(boolean benchmark) {
        RunLocalOCR runLocalOCR = new RunLocalOCR(this, this, benchmark);
        runLocalOCR.execute();
    }

    private void runRemoteMode(boolean benchmark) {
        String images_total = String.valueOf(imageStream.size());
        PrepareRemote prepareRemote = new PrepareRemote(this, this, benchmark);
        prepareRemote.execute(images_total);
    }

    private void runBenchmarkMode() {
        benchmarkResults = new BenchmarkResults();
        benchmarkResults.setNumberOfFiles(imageStream.size());
        tasksTriggered = 2;
        runLocalMode(true);
        runRemoteMode(true);
    }

    public void asyncTaskConcluded() {
        tasksTriggered--;
        if (tasksTriggered == 0) {
            Intent intent = new Intent(this, BenchmarkActivity.class);
            startActivity(intent);
        }
    }

    public void openRecords(View view) {
        Intent intent = new Intent(getApplicationContext(), RecordsActivity.class);
        startActivity(intent);
    }

    public void displayTranslatedText(String result) {
        Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
        intent.putExtra("ocr-result", result);
        startActivity(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item:
                //Toast.makeText(this, "ADD!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void onBackPressed() {
        finish();
    }

    // internet connection state check
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (retakePictureOnResume) {
            retakePictureOnResume = false;
            select_from_camera();
        }
    }

    @Override
    public void onDestroy() {
        try {
            if(receiver != null) {
                unregisterReceiver(receiver);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

}