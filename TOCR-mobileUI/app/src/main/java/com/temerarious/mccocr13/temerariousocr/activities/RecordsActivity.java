package com.temerarious.mccocr13.temerariousocr.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.temerarious.mccocr13.temerariousocr.tasks.FetchRecords;
import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.helpers.RecordsListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RecordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        String amountOfRecords = "10";

        FetchRecords fetchRecords = new FetchRecords(RecordsActivity.this, RecordsActivity.this);
        fetchRecords.execute(amountOfRecords);
    }

    public void createRecordsList(JSONArray recordsJSONArray) {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final ArrayList<String> recordsList = new ArrayList<String>();
        final ArrayList<String> timestampsList = new ArrayList<String>();
        final ArrayList<ArrayList<String>> imagesListsList = new ArrayList<ArrayList<String>>();
        final ArrayList<ArrayList<String>> thumbsListsList = new ArrayList<ArrayList<String>>();

        int totalRecords = recordsJSONArray.length();
        for (int i = 0; i < totalRecords; i++) {

            try {
                JSONObject record = recordsJSONArray.getJSONObject(i);
                String creationTime = record.getString("creation_time");
                timestampsList.add(creationTime);
                String ocr_text = record.getString("ocr_text");
                recordsList.add(ocr_text);

                JSONArray imagesJSONArray = record.getJSONArray("image_fs_ids");
                ArrayList<String> imagesList = new ArrayList<String>();
                ArrayList<String> thumbsList = new ArrayList<String>();

                int totalImages = imagesJSONArray.length();
                for (int j = 0; j < totalImages; j++) {
                    JSONObject imageIDs = imagesJSONArray.getJSONObject(j);
                    String imageID = imageIDs.has("image_fs_id") ? imageIDs.getString("image_fs_id") : "";
                    imagesList.add(imageID);
                    String thumbID = imageIDs.has("thumbnail_fs_id") ? imageIDs.getString("thumbnail_fs_id") : "";
                    thumbsList.add(thumbID);
                }
                imagesListsList.add(imagesList);
                thumbsListsList.add(thumbsList);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(RecordsActivity.this);
        String server_ip = SP.getString("server_ip", getResources().getString(R.string.server_default_ip));

        ListView list = (ListView) findViewById(R.id.records_listview);
        RecordsListAdapter adapter = new RecordsListAdapter(this, recordsList, thumbsListsList, server_ip);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String[] imagesArray = new String[imagesListsList.get(position).size()];
                for (int i = 0; i < imagesArray.length; i++) {
                    imagesArray[i] = imagesListsList.get(position).get(i);
                }

                Intent intent = new Intent(getApplicationContext(), DetailsActivity.class);
                intent.putExtra("ocr_text", recordsList.get(position));
                intent.putExtra("images_array", imagesArray);
                intent.putExtra("timestamp", timestampsList.get(position));
                startActivity(intent);

            }
        });
    }
}
