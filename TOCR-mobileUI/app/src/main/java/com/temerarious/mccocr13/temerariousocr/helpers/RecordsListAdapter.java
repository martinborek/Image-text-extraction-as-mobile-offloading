package com.temerarious.mccocr13.temerariousocr.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by fabiano.brito on 30/11/2016.
 */

public class RecordsListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final ArrayList<String> mTextsList;
    private final ArrayList<ArrayList<String>> mThumbsCollection;
    private final String mServerIp;
    private String credentials = Credentials.basic(OCRActivity.token, "");

    public RecordsListAdapter(Activity context, ArrayList<String> textsList, ArrayList<ArrayList<String>> thumbsCollection, String serverIp) {
        super(context, R.layout.records_list_item, textsList);

        this.context = context;
        this.mTextsList = textsList;
        this.mThumbsCollection = thumbsCollection;
        this.mServerIp = serverIp;
    }

    public View getView(int i, View view, ViewGroup parent) {

        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.records_list_item, null,true);

        ImageView ocrThumb = (ImageView) rowView.findViewById(R.id.record_thumb);
        TextView ocrText = (TextView) rowView.findViewById(R.id.record_text);

        Bitmap bmp = null;

        if (mThumbsCollection.get(i).get(0).equals("")) {
            bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.not_available);
        } else {

            try {
                String imageUrl = "https://" + mServerIp + "/image/" + mThumbsCollection.get(i).get(0);

                OkHttpClient client = new OkHttpClient()
                        .setSslSocketFactory(SecureSocket.getSSLContext(context).getSocketFactory())
                        .setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });

                Request request = new Request.Builder()
                        .url(imageUrl)
                        .header("Authorization", credentials)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.code() != 200) {
                    throw new IOException("Unauthorized");
                }
                bmp = BitmapFactory.decodeStream(response.body().byteStream());


            } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
        }

        ocrThumb.setImageBitmap(bmp);

        String displayedText = mTextsList.get(i);
        int stringLimit = 30;
        if (displayedText.length() > stringLimit) {
            displayedText = displayedText.substring(0, stringLimit) + "...";
        }
        ocrText.setText(displayedText);

        return rowView;

    };
}