package com.temerarious.mccocr13.temerariousocr.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.activities.MainActivity;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.temerarious.mccocr13.temerariousocr.activities.OCRActivity.adapter;
import static com.temerarious.mccocr13.temerariousocr.activities.OCRActivity.spinner;

/**
 * Created by ivan on 09.12.16.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {

    private boolean isConnected = false;
    public static String networkStatus;
    String[] type = {"Local", "Remote", "Benchmark"};
    @Override
    public void onReceive(final Context context, final Intent intent) {

        isNetworkAvailable(context);

    }


    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        if(!isConnected){

                            networkStatus="On";
                            isConnected = true;
                            OCRActivity.button_records.setVisibility(View.VISIBLE);

                            OCRActivity.logoutFB.setVisibility(View.VISIBLE);
                            if (MainActivity.login.equals("basicLogin"))
                                OCRActivity.logoutFB.setVisibility(View.GONE);


                            adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, type);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner.setAdapter(adapter);
                            spinner.setPrompt("Select Type");
                            spinner.setSelection(1);
                        }
                        return true;
                    }
                }
            }
        }
        networkStatus="Off";
        Toast.makeText(getApplicationContext(), R.string.internet_off, Toast.LENGTH_SHORT).show();
        OCRActivity.button_records.setVisibility(View.GONE);
//        OCRActivity.logoutFB.setVisibility(View.GONE);
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, type) {
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
        spinner.setAdapter(adapter);
        spinner.setPrompt("Select Type");
        spinner.setSelection(0);
        isConnected = false;
        //OCRActivity.button_records.setVisibility(View.GONE);
        return false;
    }
}