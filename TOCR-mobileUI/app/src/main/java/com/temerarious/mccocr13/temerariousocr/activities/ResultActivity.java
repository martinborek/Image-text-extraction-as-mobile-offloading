package com.temerarious.mccocr13.temerariousocr.activities;


import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.helpers.OCRInitializer;
import com.temerarious.mccocr13.temerariousocr.helpers.SaveResultHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import static android.view.View.*;

public class ResultActivity extends AppCompatActivity {

    Button btnSave;
    public SaveResultHelper saveResultHelper = new SaveResultHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        LinearLayout ll = (LinearLayout) findViewById(R.id.result_linear_layout);

        final String ocrResult = getIntent().getStringExtra("ocr-result");

        TextView resultView = new TextView(this);
        resultView.setText(ocrResult);
        ll.addView(resultView);

        btnSave = (Button) findViewById(R.id.button_save);
        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveResultHelper.saveToText(ocrResult);
            }
        });

    }

    public void retakePicture(View view) {
        if(OCRActivity.imageTakenFromCamera) {
            OCRActivity.retakePictureOnResume = true;
            finish();
        } else {
            Toast.makeText(this, getString(R.string.not_from_camera), Toast.LENGTH_SHORT).show();
        }
    }

}


