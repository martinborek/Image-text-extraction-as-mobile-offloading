package com.temerarious.mccocr13.temerariousocr.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.temerarious.mccocr13.temerariousocr.R;

public class PreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        int numberOfImages = OCRActivity.imageStream.size();

        LinearLayout ll = (LinearLayout) findViewById(R.id.preview_linear_layout);

        for (int i = 0; i < numberOfImages; i++) {
            byte[] blob = OCRActivity.imageStream.get(i);
            ImageView iv = new ImageView(this);
            Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
            iv.setImageBitmap(bitmap);
            ll.addView(iv);
        }

    }
}
