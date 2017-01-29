package com.temerarious.mccocr13.temerariousocr.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.helpers.SecureSocket;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class PrepareRemote extends AsyncTask<String,Void,String> {

    public OCRActivity source = null;
    private Context context;
    private boolean runningInBenchmark = false;
    //ProgressDialog loading;

    public PrepareRemote(OCRActivity fl, Context ctx, boolean benchmark) {
        source = fl;
        context = ctx;
        runningInBenchmark = benchmark;
    }

    @Override
    protected String doInBackground(String... params) {

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
        String server_ip = SP.getString("server_ip", context.getResources().getString(R.string.server_default_ip));

        String credentials = Credentials.basic(OCRActivity.token, "");

        String prepare_remote_url = "https://" + server_ip + "/ocr/";
        String images_total = params[0];

        try {

            OkHttpClient client = new OkHttpClient()
                    .setSslSocketFactory(SecureSocket.getSSLContext(context).getSocketFactory())
                    .setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });

            RequestBody requestBody = new MultipartBuilder()
                    .type(MultipartBuilder.FORM)
                    .addFormDataPart("images_total", images_total)
                    .build();

            Request request = new Request.Builder()
                    .url(prepare_remote_url)
                    .header("Authorization", credentials)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.code() != 200) {
                throw new IOException("Unauthorized");
            }
            return response.body().string();

        } catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | KeyStoreException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onPostExecute(String result) {
        if(result != null) {
            try {

                JSONObject jsonObj = new JSONObject(result);

                String uid = jsonObj.getString("uid");
                String next_seq = jsonObj.getString("next_seq");

                UploadImages uploadImages = new UploadImages(source, context, runningInBenchmark);
                uploadImages.execute(uid, next_seq);

            } catch (JSONException e) {
                Log.e("Parsing error", e.toString());
            }
        } else {
            Toast.makeText(context, R.string.remote_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}