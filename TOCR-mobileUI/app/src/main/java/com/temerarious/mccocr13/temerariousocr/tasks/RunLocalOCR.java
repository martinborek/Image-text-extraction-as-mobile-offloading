package com.temerarious.mccocr13.temerariousocr.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.temerarious.mccocr13.temerariousocr.R;
import com.temerarious.mccocr13.temerariousocr.activities.BenchmarkActivity;
import com.temerarious.mccocr13.temerariousocr.activities.OCRActivity;

/**
 * Created by fabiano.brito on 06/12/2016.
 */

public class RunLocalOCR extends AsyncTask<String,Void,String> {

    public OCRActivity source = null;
    private Context context;
    private boolean runningInBenchmark = false;
    private ProgressDialog loading;

    public RunLocalOCR(OCRActivity fl, Context ctx, boolean benchmark) {
        source = fl;
        context = ctx;
        runningInBenchmark = benchmark;
    }

    @Override
    protected String doInBackground(String... params) {

        String partialOCRResult = "";

        for (int i = 0; i < source.imageStream.size(); i++) {
            long tStart = System.currentTimeMillis();
            partialOCRResult += source.ocrInitializer.runOCR(source.imageStream.get(i));
            long tEnd = System.currentTimeMillis();
            double delta = (tEnd - tStart) / 1000.0;
            if (runningInBenchmark) {
                source.benchmarkResults.setLocalElapsedTime(i, delta);
            }
        }

        return partialOCRResult;
    }

    @Override
    protected void onPreExecute() {
        int stringId = runningInBenchmark ? R.string.running_benchmark : R.string.running_local_ocr;
        loading = ProgressDialog.show(context, source.getResources().getString(stringId), null, true, true);
        loading.setCancelable(false);
    }

    @Override
    protected void onPostExecute(String result) {
        loading.dismiss();
        if(result != null) {
            if (runningInBenchmark) {
                source.asyncTaskConcluded();
            } else {
                source.displayTranslatedText(result);
            }
        } else {
            Toast.makeText(context, R.string.local_ocr_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}