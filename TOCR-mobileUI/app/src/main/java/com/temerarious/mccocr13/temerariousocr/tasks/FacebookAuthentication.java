package com.temerarious.mccocr13.temerariousocr.tasks;

/**
 * Created by ivan on 07.12.16.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.temerarious.mccocr13.temerariousocr.activities.MainActivity;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.fragments.FacebookFragment;
import com.temerarious.mccocr13.temerariousocr.helpers.SecureSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class FacebookAuthentication extends AsyncTask<String,Void,String> {

    public FacebookFragment source = null;
    private Context context;
    ProgressDialog loading;
    String HERE_USER_TOKEN="";

    public FacebookAuthentication(FacebookFragment fl, Context ctx) {
        source = fl;
        context = ctx;
    }

    @Override
    protected String doInBackground(String... params) {

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences sharedPref = source.getActivity().getSharedPreferences("sessionData", Context.MODE_PRIVATE);
        HERE_USER_TOKEN = sharedPref.getString("token", "");
        Log.d("LOG", HERE_USER_TOKEN);
        String server_ip = SP.getString("server_ip", context.getResources().getString(R.string.server_default_ip));
        String prepare_remote_url = "https://" + server_ip + "/fb_token?token="+HERE_USER_TOKEN;

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
                    .url(prepare_remote_url)
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
    protected void onPreExecute() {
        loading = ProgressDialog.show(context, source.getResources().getString(R.string.authenticating), null, true, true);
        loading.setCancelable(false);
    }

    @Override
    protected void onPostExecute(String result) {
        loading.dismiss();
        try {

            if(result == null) {
                throw new IOException("Response was null");
            }

            JSONObject jsonObj = new JSONObject(result);
            String token = jsonObj.getString("token");

            SharedPreferences sharedPref = source.getActivity().getSharedPreferences("sessionData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("token", token);
            Log.d("Facebook token2", token);
            editor.apply();
            Toast.makeText(context, R.string.authentication_ok, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(context, OCRActivity.class);
            context.startActivity(intent);


        } catch (JSONException | IOException e) {
            Log.e("Parsing error", e.toString());
            Toast.makeText(context, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }


}

