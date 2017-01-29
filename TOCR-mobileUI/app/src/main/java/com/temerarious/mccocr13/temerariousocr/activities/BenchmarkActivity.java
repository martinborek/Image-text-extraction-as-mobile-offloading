package com.temerarious.mccocr13.temerariousocr.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.temerarious.mccocr13.temerariousocr.R;

import java.util.Locale;

public class BenchmarkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark);
        
        String seconds = getString(R.string.seconds);
        String bytes = getString(R.string.bytes);
        String linebreak = getString(R.string.linebreak);

        LinearLayout ll = (LinearLayout) findViewById(R.id.benchmark_linear_layout);

        int indexMax, indexMin;
        double maxValue = 0, minValue;

        String totalProcessedImages = getString(R.string.total_processed_images) + String.valueOf(OCRActivity.benchmarkResults.getNumberOfFiles()) + linebreak;
        TextView totalImagesView = new TextView(this);
        totalImagesView.setText(Html.fromHtml(totalProcessedImages));
        ll.addView(totalImagesView);

        String localResults = getString(R.string.local_header);
        for (int i = 0; i < OCRActivity.imageStream.size(); i++) {
            localResults += getString(R.string.image_number) + String.valueOf(i + 1) + ": " + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getLocalElapsedTime(i)) + seconds;
        }
        localResults += getString(R.string.total_time_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getLocalTotal()) + seconds;
        localResults += getString(R.string.average_time_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getLocalAverage()) + seconds;
        localResults += getString(R.string.deviations_time_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getLocalDeviation()) + seconds;
        localResults += getString(R.string.maximum_time_of) + OCRActivity.benchmarkResults.getLocalMaxIndex() + getString(R.string.linebreak);
        localResults += getString(R.string.minimum_time_of) + OCRActivity.benchmarkResults.getLocalMinIndex() + getString(R.string.linebreak);

        TextView localResultView = new TextView(this);
        localResultView.setText(Html.fromHtml(localResults));
        ll.addView(localResultView);

        String remoteResults = getString(R.string.remote_header);
        for (int i = 0; i < OCRActivity.imageStream.size(); i++) {
            remoteResults += getString(R.string.image_number) + String.valueOf(i + 1) + ": " + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getRemoteElapsedTime(i)) + seconds;
        }
        remoteResults += getString(R.string.total_time_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getRemoteTotal()) + seconds;
        remoteResults += getString(R.string.average_time_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getRemoteAverage()) + seconds;
        remoteResults += getString(R.string.deviations_time_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getRemoteDeviation()) + seconds;
        remoteResults += getString(R.string.maximum_time_of) + OCRActivity.benchmarkResults.getRemoteMaxIndex() + getString(R.string.linebreak);
        remoteResults += getString(R.string.minimum_time_of) + OCRActivity.benchmarkResults.getRemoteMinIndex() + getString(R.string.linebreak);

        TextView remoteResultView = new TextView(this);
        remoteResultView.setText(Html.fromHtml(remoteResults));
        ll.addView(remoteResultView);

        String dataResults = getString(R.string.data_header);
        for (int i = 0; i < OCRActivity.imageStream.size(); i++) {
            dataResults += getString(R.string.image_number) + String.valueOf(i + 1) + ": " + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getDataExchanged(i)) + bytes;
        }
        dataResults += getString(R.string.total_data_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getDataExchangedTotal()) + bytes;
        dataResults += getString(R.string.average_data_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getDataExchangedAverage()) + bytes;
        dataResults += getString(R.string.deviations_data_of) + String.format(Locale.getDefault(), "%.2f", OCRActivity.benchmarkResults.getDataExchangedDeviation()) + bytes;
        dataResults += getString(R.string.maximum_data_of) + OCRActivity.benchmarkResults.getDataExchangedMaxIndex() + getString(R.string.linebreak);
        dataResults += getString(R.string.minimum_data_of) + OCRActivity.benchmarkResults.getDataExchangedMinIndex() + getString(R.string.linebreak);

        TextView dataResultView = new TextView(this);
        dataResultView.setText(Html.fromHtml(dataResults));
        ll.addView(dataResultView);

    }
}
