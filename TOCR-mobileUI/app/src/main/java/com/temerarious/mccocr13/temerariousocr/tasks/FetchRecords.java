package com.temerarious.mccocr13.temerariousocr.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.activities.MainActivity;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;
import com.temerarious.mccocr13.temerariousocr.helpers.SecureSocket;
import com.temerarious.mccocr13.temerariousocr.activities.RecordsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by fabiano.brito on 30/11/2016.
 */

public class FetchRecords extends AsyncTask<String,Void,String> {

    public RecordsActivity source = null;
    private Context context;
    private ProgressDialog loading;

    public FetchRecords(RecordsActivity fl, Context ctx) {
        source = fl;
        context = ctx;
    }

    @Override
    protected String doInBackground(String... params) {

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
        String server_ip = SP.getString("server_ip", context.getResources().getString(R.string.server_default_ip));
        String numberOfRecords = SP.getString("records_amount", context.getResources().getString(R.string.records_pref_default));

        String credentials = Credentials.basic(OCRActivity.token, "");

        Log.v("LOG-DEBUG", numberOfRecords);
        String urlParameter = numberOfRecords.equals("All") ? "" : "?amount=" + numberOfRecords;
        String recordsUrl = "https://" + server_ip + "/records/" + urlParameter;

        try {

            OkHttpClient client = new OkHttpClient()
                    .setSslSocketFactory(SecureSocket.getSSLContext(context).getSocketFactory())
                    .setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });

            Request request = new Request.Builder()
                    .url(recordsUrl)
                    .header("Authorization", credentials)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.code() != 200) {
                throw new IOException("Unauthorized");
            }
            return response.body().string();

        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        loading = ProgressDialog.show(context, source.getResources().getString(R.string.getting_records), null, true, true);
        loading.setCancelable(false);
    }

    @Override
    protected void onPostExecute(String result) {
        loading.dismiss();
        if(result != null) {
            try {

                JSONObject jsonObj = new JSONObject(result);
                JSONArray recordsArray = jsonObj.getJSONArray("records");

                if (recordsArray.length() > 0) {
                    source.createRecordsList(recordsArray);
                } else {
                    Toast.makeText(context, R.string.no_records, Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                Log.e("Parsing error", e.toString());
            }
        } else {
            Toast.makeText(context, R.string.no_records, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}