package com.tomwaks.appdustsensor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConfigActivity extends AppCompatActivity implements  View.OnClickListener{

    SeekBar sbTimeMeas, sbNumsMeas, sbBreakMeas, SB_RadiusOfAccuracy;
    TextView tvTimeMeas, tvNumsMeas, tvBreakMeas, tvAllTime, TV_RadiusOfAccuracy;
    ProgressBar progressBar, progressBarSave;
    Button bSave;

    int timeMeas, numsMeas, breakMeas, radius;
    int step = 1;

    int minTimeMeas  = 10;
    int minNumsMeas  = 1;
    int minBreakMeas = 10;
    int minRadius    = 100;

    int maxTimeMeas  = 90;
    int maxNumsMeas  = 15;
    int maxBreakMeas = 90;
    int maxRadius    = 10000;

    String returnURLs = "http://dustsensor.h2g.pl/returnConfig.php";
    String setURLs = "http://dustsensor.h2g.pl/saveConfig.php?KEY=01123581321345589144233&&";

    boolean statusNetwork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        progressBar     = findViewById(R.id.progressBar);
        progressBarSave = findViewById(R.id.progressBarSave);

        sbTimeMeas          = findViewById(R.id.sbTimeMeas);
        sbNumsMeas          = findViewById(R.id.sbNumsMeas);
        sbBreakMeas         = findViewById(R.id.sbBreakMeas);
        SB_RadiusOfAccuracy = findViewById(R.id.SB_RadiusOfAccuracy);

        sbTimeMeas.setMax((maxTimeMeas - minTimeMeas) / step );
        sbNumsMeas.setMax((maxNumsMeas - minNumsMeas) / step );
        sbBreakMeas.setMax((maxBreakMeas - minBreakMeas) / step );
        SB_RadiusOfAccuracy.setMax((maxRadius - minRadius) / step );

        tvTimeMeas = findViewById(R.id.tvTimeMeas);
        tvNumsMeas = findViewById(R.id.tvNumsMeas);
        tvBreakMeas = findViewById(R.id.tvBreakMeas);
        TV_RadiusOfAccuracy = findViewById(R.id.TV_RadiusOfAccuracy);


        tvAllTime = findViewById(R.id.tvAllTime);

        bSave = findViewById(R.id.bSave);
        bSave.setOnClickListener(this);

        progressBarSave.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.colorPrimary),
                android.graphics.PorterDuff.Mode.SRC_IN);



        new currentSettings().execute();

        sbTimeMeas.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int currentValue = (minTimeMeas+progress)/step;
                tvTimeMeas.setText(currentValue+"s");

                int timeM =   (minTimeMeas+sbTimeMeas.getProgress()) / step;
                int numsM =   (minNumsMeas+sbNumsMeas.getProgress()) / step;
                int breakM =  (minBreakMeas+sbBreakMeas.getProgress()) / step;
                tvAllTime.setText(allTime(timeM, numsM, breakM));

            }
        });
        sbNumsMeas.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int currentValue = (minNumsMeas+progress)/step;
                tvNumsMeas.setText(currentValue+"");

                int timeM =   (minTimeMeas+sbTimeMeas.getProgress()) / step;
                int numsM =   (minNumsMeas+sbNumsMeas.getProgress()) / step;
                int breakM =  (minBreakMeas+sbBreakMeas.getProgress()) / step;
                tvAllTime.setText(allTime(timeM, numsM, breakM));
            }
        });
        sbBreakMeas.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int currentValue = (minBreakMeas+progress)/step;
                tvBreakMeas.setText(currentValue+"s");

                int timeM =   (minTimeMeas+sbTimeMeas.getProgress()) / step;
                int numsM =   (minNumsMeas+sbNumsMeas.getProgress()) / step;
                int breakM =  (minBreakMeas+sbBreakMeas.getProgress()) / step;
                tvAllTime.setText(allTime(timeM, numsM, breakM));
            }
        });
        SB_RadiusOfAccuracy.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int currentValue = (minRadius+progress)/step;
                TV_RadiusOfAccuracy.setText(currentValue+"m");
            }
        });


    }

    class currentSettings extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            if(isNetworkAvailable(ConfigActivity.this)){
                statusNetwork = true;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(returnURLs)
                        .build();

                try{
                    Response response = client.newCall(request).execute();
                    String s = response.body().string();

                    JSONObject jsonData = new JSONObject(s);
                    timeMeas  = jsonData.getInt("timeMeasure");
                    numsMeas  = jsonData.getInt("nMeasures");
                    breakMeas = jsonData.getInt("breakTime");
                    radius    = jsonData.getInt("radius");

                   //Log.d("measP", "timeMeas: "+timeMeas);
                    //Log.d("measP", "numsMeas: "+numsMeas);
                   // Log.d("measP", "breakMeas: "+breakMeas);
                   // Log.d("measP", "breakMeas: "+radius);

                    return s;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("ERROR-Exception-IO", ""+e);
                    return null;
                }catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("ERROR-Exception-JSON", ""+e);
                    return null;
                }
            }else{
                statusNetwork = false;
                return "Brak internetu";
            }

        }
        @Override
        protected void onPostExecute(String result) {
            if(statusNetwork){
                sbTimeMeas.setProgress(timeMeas - minTimeMeas);
                sbNumsMeas.setProgress(numsMeas - minNumsMeas);
                sbBreakMeas.setProgress(breakMeas - minBreakMeas );
                SB_RadiusOfAccuracy.setProgress(radius - minRadius );

                int timeM =   (minTimeMeas+sbTimeMeas.getProgress()) / step;
                int numsM =   (minNumsMeas+sbNumsMeas.getProgress()) / step;
                int breakM =  (minBreakMeas+sbBreakMeas.getProgress()) / step;
                int rad    =  (minRadius+SB_RadiusOfAccuracy.getProgress()) / step;


                tvTimeMeas.setText(timeM+"s");
                tvNumsMeas.setText(numsM+"");
                tvBreakMeas.setText(breakM+"s");
                TV_RadiusOfAccuracy.setText(rad+"m");

                tvAllTime.setText(allTime(timeM, numsM, breakM));
                progressBar.setVisibility(View.GONE);


            }
        }
        @Override
        protected void onPreExecute(){  }

    }


    public String allTime(int timeM, int numsM, int breakM){
        int time = timeM*numsM + breakM;
        int minutes = time/60;
        int seconds = time%60;

        Log.d("timetime", timeM+"*"+numsM+"+"+breakM+"="+time+"("+minutes+"m"+seconds+"s)");



        return String.format("%02dm%02ds", minutes, seconds);
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bSave:
                new saveConfig().execute();
                break;

        }
    }

    class saveConfig extends AsyncTask<Void, Void, String> {

        int timeM = 0;
        int numsM = 0;
        int breakM = 0;
        int radiusM = 0;

        @Override
        protected String doInBackground(Void... params) {
            if(isNetworkAvailable(ConfigActivity.this)){

                statusNetwork = true;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(setURLs+"TIME_MEASURE="+timeM+"&&NUMBERS_MEASURE="+numsM+"&&BREAK_TIME="+breakM+"&&RADIUS="+radiusM)
                        .build();

                try{
                    Response response = client.newCall(request).execute();
                    String s = response.body().string();
                    return s;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("ERROR-Exception-IO", ""+e);
                    return null;
                }
            }else{
                statusNetwork = false;
                return "Brak internetu";
            }

        }
        @Override
        protected void onPostExecute(String result) {
            if(statusNetwork && result.equals("true")){


                progressBarSave.getIndeterminateDrawable().setColorFilter(
                        getResources().getColor(R.color.colorPrimary),
                        android.graphics.PorterDuff.Mode.SRC_IN);
                Toast.makeText(ConfigActivity.this, "Zapisano",
                        Toast.LENGTH_LONG).show();

            }
        }
        @Override
        protected void onPreExecute() {
            progressBarSave.getIndeterminateDrawable().setColorFilter(
                    getResources().getColor(R.color.colorAccent),
                    android.graphics.PorterDuff.Mode.SRC_IN);

            timeM = (minTimeMeas+sbTimeMeas.getProgress()) / step;
            numsM =   (minNumsMeas+sbNumsMeas.getProgress()) / step;
            breakM =  (minBreakMeas+sbBreakMeas.getProgress()) / step;
            radiusM =  (minRadius+SB_RadiusOfAccuracy.getProgress()) / step;
        }
    }
}
