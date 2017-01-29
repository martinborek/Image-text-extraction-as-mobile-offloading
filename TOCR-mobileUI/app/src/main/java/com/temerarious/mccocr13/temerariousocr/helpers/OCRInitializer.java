package com.temerarious.mccocr13.temerariousocr.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by fabiano.brito on 30/11/2016.
 */

public class OCRInitializer {

    public OCRActivity activity = null;
    private Context context;

    public OCRInitializer(OCRActivity mActivity, Context mContext) {
        activity = mActivity;
        context = mContext;
    }

    private TessBaseAPI mTess;
    private String datapath = "";

    public void initOCR() {
        //initialize Tesseract API
        String language = "eng";
        datapath = activity.getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);
    }

    public String runOCR(byte[] blob) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();
        return result;
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = activity.getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
