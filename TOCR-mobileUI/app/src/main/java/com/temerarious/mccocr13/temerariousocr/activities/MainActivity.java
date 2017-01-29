package com.temerarious.mccocr13.temerariousocr.activities;

/**
 * Created by ivan on 21.11.16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.temerarious.mccocr13.temerariousocr.fragments.FacebookFragment;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.helpers.NetworkChangeReceiver;
import com.temerarious.mccocr13.temerariousocr.tasks.BasicAuthentication;

public class MainActivity extends AppCompatActivity {

    public static String login="";
    private NetworkChangeReceiver receiver;
    private boolean isConnected = false;
    public static String networkStatus="On";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);


        if (fragment == null) {
            fragment = new FacebookFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }

    }

    public void startLogin(View view) {
        EditText etLogUsername = (EditText) findViewById(R.id.etLogUsername);
        EditText etLogPassword = (EditText) findViewById(R.id.etLogPassword);

        String username = etLogUsername.getText().toString();
        String password = etLogPassword.getText().toString();

        BasicAuthentication basicAuthentication = new BasicAuthentication(MainActivity.this, MainActivity.this);
        basicAuthentication.execute(username, password);
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



}