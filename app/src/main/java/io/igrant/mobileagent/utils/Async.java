package io.igrant.mobileagent.utils;

import android.os.AsyncTask;
import android.util.Log;

final class Async extends AsyncTask<Void, Void, String> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Log.d("async", " 1 ");
    }

    @Override
    protected String doInBackground(Void... voids) {

        Log.d("async", " 2 ");

        publishProgress();

        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);

        Log.d("async", " 3 ");
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        Log.d("async", " 4 ");
    }

}
