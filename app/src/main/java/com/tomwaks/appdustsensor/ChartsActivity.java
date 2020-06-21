package com.tomwaks.appdustsensor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.formatter.ValueFormatter;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChartsActivity extends AppCompatActivity implements View.OnClickListener, OnDateSelectedListener {

    private LineChart chartPM, chartHumi, chartTemp, chartPress;

    String latitude, longitude, location;
    boolean isSelectedDay = false;
    TextView TV_nameLocation, TV_SelectedDay, TV_Date, TV_Location, TV_PM2_5_Line, TV_PM2_5C_Line, TV_PM10_Line, TV_PM10C_Line;
    ProgressBar PB_Loading;
    LineDataSet pm2_5, pm_10, pm2_5C, pm_10C, temp, humi, press;
    List<String> listOfDates  = new ArrayList<>();
    MaterialCalendarView calendar;
    ImageView IV_Calendar, IV_Dust, IV_Humidity, IV_Temperature, IV_Pressure;
    LinearLayout LL_Calendar, LL_Dust, LL_Humidity, LL_Temperature, LL_Pressure, LL_Loc_Date;
    int i_pm2_5 = 0, i_pm_10 = 0, i_pm2_5C = 0, i_pm_10C = 0;
    XAxis xAxisPM, xAxisHumi, xAxisTemp, xAxisPress;
    YAxis yAxisPM, yAxisHumi, yAxisTemp, yAxisPress;
    ArrayList<Entry> valuesPM2_5  = new ArrayList<>();
    ArrayList<Entry> valuesPM_10  = new ArrayList<>();
    ArrayList<Entry> valuesPM2_5C = new ArrayList<>();
    ArrayList<Entry> valuesPM_10C = new ArrayList<>();
    ArrayList<Entry> valuesTemp   = new ArrayList<>();
    ArrayList<Entry> valuesHumi   = new ArrayList<>();
    ArrayList<Entry> valuesPress  = new ArrayList<>();
    List<Integer> PM2_5   = new ArrayList<Integer>();
    List<Integer> PM_10   = new ArrayList<Integer>();
    List<Integer> PM2_5C   = new ArrayList<Integer>();
    List<Integer> PM_10C   = new ArrayList<Integer>();
    List<Integer>  HUMI   = new ArrayList<Integer>();
    List<Integer> PRES    = new ArrayList<Integer>();
    List<Double>  TEMP    = new ArrayList<Double>();
    List<Integer>  TIME   = new ArrayList<Integer>();
    LineData data;
    String dateS = "";
    String URLReturnDates    = "http://dustsensor.h2g.pl/listOfDates.php?KEY=01123581321345589144233";
    String URLReturnDataMeas = "http://dustsensor.h2g.pl/returnData.php?KEY=01123581321345589144233";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        latitude = getIntent().getStringExtra("latitude");
        longitude = getIntent().getStringExtra("longitude");
        location = getIntent().getStringExtra("location");

        if(latitude.length() < 9){
            latitude +="0";
        }
        if(longitude.length() < 9){
            longitude +="0";
        }

        PB_Loading = findViewById(R.id.PB_Loading);

        LL_Calendar     = findViewById(R.id.LL_Calendar);
        LL_Dust         = findViewById(R.id.LL_Dust);
        LL_Humidity     = findViewById(R.id.LL_Humidity);
        LL_Temperature  = findViewById(R.id.LL_Temperature);
        LL_Pressure     = findViewById(R.id.LL_Pressure);

        LL_Loc_Date     = findViewById(R.id.LL_Loc_Date);

        LL_Loc_Date.setVisibility(View.INVISIBLE);

        LL_Dust.setVisibility(View.GONE);
        LL_Humidity.setVisibility(View.GONE);
        LL_Temperature.setVisibility(View.GONE);
        LL_Pressure.setVisibility(View.GONE);

        IV_Calendar     = findViewById(R.id.IV_Calendar);
        IV_Dust         = findViewById(R.id.IV_Dust);
        IV_Humidity     = findViewById(R.id.IV_Humidity);
        IV_Temperature  = findViewById(R.id.IV_Temperature);
        IV_Pressure     = findViewById(R.id.IV_Pressure);

        IV_Calendar.setOnClickListener(this);
        IV_Dust.setOnClickListener(this);
        IV_Humidity.setOnClickListener(this);
        IV_Temperature.setOnClickListener(this);
        IV_Pressure.setOnClickListener(this);

        TV_nameLocation = findViewById(R.id.TV_nameLocation);
        TV_SelectedDay = findViewById(R.id.TV_SelectedDay);

        TV_nameLocation.setText(HtmlCompat.fromHtml("Lokalizacja: <b>"+location+"</b", HtmlCompat.FROM_HTML_MODE_LEGACY));

        TV_Date = findViewById(R.id.TV_Date);
        TV_Location = findViewById(R.id.TV_Location);
        calendar = findViewById(R.id.calendarView);
        calendar.state().edit().commit();

        TV_PM2_5_Line  = findViewById(R.id.TV_PM2_5_Line);
        TV_PM2_5C_Line = findViewById(R.id.TV_PM2_5C_Line);
        TV_PM10_Line   = findViewById(R.id.TV_PM10_Line);
        TV_PM10C_Line  = findViewById(R.id.TV_PM10C_Line);
        TV_PM2_5_Line.setOnClickListener(this);
        TV_PM2_5C_Line.setOnClickListener(this);
        TV_PM10_Line.setOnClickListener(this);
        TV_PM10C_Line.setOnClickListener(this);

        {
            chartPM = findViewById(R.id.chartPM);
            chartPM.setDrawGridBackground(false);
            chartPM.getDescription().setEnabled(false);
            chartPM.setDrawBorders(false);
            chartPM.setDragEnabled(true);
            chartPM.setScaleEnabled(true);

            {
                xAxisPM = chartPM.getXAxis();
                xAxisPM.setEnabled(true);
                xAxisPM.setLabelCount(5);
                xAxisPM.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxisPM.setGranularity(1.0f);
                xAxisPM.setGranularityEnabled(true);
                xAxisPM.setTextColor(Color.BLACK);
                xAxisPM.setValueFormatter(new MyValueFormatter());
            }

            {
                yAxisPM = chartPM.getAxisLeft();
                chartPM.getAxisRight().setEnabled(false);
                yAxisPM.setGranularity(1.0f);
                yAxisPM.setGranularityEnabled(true);
                yAxisPM.setTextColor(Color.BLACK);
                yAxisPM.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            }
            chartPM.getLegend().setEnabled(false);
            chartPM.setMaxVisibleValueCount(40);
        }

        {
            chartHumi = findViewById(R.id.chartHumi);
            chartHumi.setDrawGridBackground(false);
            chartHumi.getDescription().setEnabled(false);
            chartHumi.setDrawBorders(false);
            chartHumi.setDragEnabled(true);
            chartHumi.setScaleEnabled(true);

            {
                xAxisHumi = chartHumi.getXAxis();
                xAxisHumi.setEnabled(true);
                xAxisHumi.setLabelCount(5);
                xAxisHumi.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxisHumi.setGranularity(1.0f);
                xAxisHumi.setGranularityEnabled(true);
                xAxisHumi.setTextColor(Color.BLACK);
                xAxisHumi.setValueFormatter(new MyValueFormatter());
            }

            {
                yAxisHumi = chartHumi.getAxisLeft();
                chartHumi.getAxisRight().setEnabled(false);
                yAxisHumi.setGranularity(1.0f);
                yAxisHumi.setGranularityEnabled(true);
                yAxisHumi.setTextColor(Color.BLACK);
                yAxisHumi.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            }
            chartHumi.getLegend().setEnabled(false);
            chartHumi.setMaxVisibleValueCount(10);
        }

        {
            chartTemp = findViewById(R.id.chartTemp);
            chartTemp.setDrawGridBackground(false);
            chartTemp.getDescription().setEnabled(false);
            chartTemp.setDrawBorders(false);
            chartTemp.setDragEnabled(true);
            chartTemp.setScaleEnabled(true);

            {
                xAxisTemp = chartTemp.getXAxis();
                xAxisTemp.setEnabled(true);
                xAxisTemp.setLabelCount(5);
                xAxisTemp.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxisTemp.setGranularity(1.0f);
                xAxisTemp.setGranularityEnabled(true);
                xAxisTemp.setTextColor(Color.BLACK);
                xAxisTemp.setValueFormatter(new MyValueFormatter());
            }

            {
                yAxisTemp = chartTemp.getAxisLeft();
                chartTemp.getAxisRight().setEnabled(false);
                yAxisTemp.setGranularity(0.1f);
                yAxisTemp.setGranularityEnabled(true);
                yAxisTemp.setTextColor(Color.BLACK);
                yAxisTemp.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            }
            chartTemp.getLegend().setEnabled(false);
            chartTemp.setMaxVisibleValueCount(15);
        }

        {
            chartPress = findViewById(R.id.chartPress);
            chartPress.setDrawGridBackground(false);
            chartPress.getDescription().setEnabled(false);
            chartPress.setDrawBorders(false);
            chartPress.setDragEnabled(true);
            chartPress.setScaleEnabled(true);

            {
                xAxisPress = chartPress.getXAxis();
                xAxisPress.setEnabled(true);
                xAxisPress.setLabelCount(5);
                xAxisPress.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxisPress.setGranularity(1.0f);
                xAxisPress.setGranularityEnabled(true);
                xAxisPress.setTextColor(Color.BLACK);
                xAxisPress.setValueFormatter(new MyValueFormatter());
            }

            {
                yAxisPress = chartPress.getAxisLeft();
                chartPress.getAxisRight().setEnabled(false);
                yAxisPress.setGranularity(1.0f);
                yAxisPress.setGranularityEnabled(true);
                yAxisPress.setTextColor(Color.BLACK);
                yAxisPress.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
                yAxisPress.setValueFormatter(new IntValueFormatter());
            }
            chartPress.getLegend().setEnabled(false);
            chartPress.setMaxVisibleValueCount(15);
        }



        new loadingDates().execute();


        //Log.d("chartA", listOfDates.get(0).substring(0, 4));
        //Log.d("chartA", listOfDates.get(0).substring(6, 8));
       // Log.d("chartA", listOfDates.get(0).substring(8, 9));



//        Log.d("chartsA", listOfDates.get(0).substring(0, 4));
       // Log.d("chartsA", listOfDates.get(0).substring(5, 7));
        //Log.d("chartsA", listOfDates.get(0).substring(8, 10));


        calendar.setOnDateChangedListener(this);


        PB_Loading.setVisibility(View.GONE);

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.IV_Calendar:
                LL_Loc_Date.setVisibility(View.INVISIBLE);
                TV_nameLocation.setText(HtmlCompat.fromHtml("Lokalizacja: <b>"+location+"</b", HtmlCompat.FROM_HTML_MODE_LEGACY));
                TV_nameLocation.setVisibility(View.VISIBLE);
                setVisibilityContent(LL_Calendar);
                IV_Calendar.setImageResource(R.drawable.ic_calendar_selected);
                break;
            case R.id.IV_Dust:
                if(isSelectedDay){
                    setVisibilityContent(LL_Dust);
                    IV_Dust.setImageResource(R.drawable.ic_dust_selected);
                    TV_nameLocation.setVisibility(View.INVISIBLE);
                    TV_Location.setText(Html.fromHtml("<b>Lokalizacja:</b><br>"+location));
                    TV_Date.setText(dateS);
                    LL_Loc_Date.setVisibility(View.VISIBLE);
                    chartPM.fitScreen();
                }else{
                    Toast.makeText(ChartsActivity.this,"Najpierw wybierz termin!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.IV_Humidity:
                if(isSelectedDay) {
                    setVisibilityContent(LL_Humidity);
                    IV_Humidity.setImageResource(R.drawable.ic_humidity_selected);
                    TV_nameLocation.setVisibility(View.INVISIBLE);
                    TV_Location.setText(Html.fromHtml("<b>Lokalizacja:</b><br>"+location));
                    TV_Date.setText(dateS);
                    LL_Loc_Date.setVisibility(View.VISIBLE);
                    chartHumi.fitScreen();
                }else{
                    Toast.makeText(ChartsActivity.this,"Najpierw wybierz termin!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.IV_Temperature:
                if(isSelectedDay){
                    setVisibilityContent(LL_Temperature);
                    IV_Temperature.setImageResource(R.drawable.ic_temperature_selected);
                    TV_nameLocation.setVisibility(View.INVISIBLE);
                    TV_Location.setText(Html.fromHtml("<b>Lokalizacja:</b><br>"+location));
                    TV_Date.setText(dateS);
                    LL_Loc_Date.setVisibility(View.VISIBLE);
                    chartTemp.fitScreen();
                }else{
                    Toast.makeText(ChartsActivity.this,"Najpierw wybierz termin!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.IV_Pressure:
                if(isSelectedDay) {
                    setVisibilityContent(LL_Pressure);
                    IV_Pressure.setImageResource(R.drawable.ic_pressure_selected);
                    TV_nameLocation.setVisibility(View.INVISIBLE);
                    TV_Location.setText(Html.fromHtml("<b>Lokalizacja:</b><br>"+location));
                    TV_Date.setText(dateS);
                    LL_Loc_Date.setVisibility(View.VISIBLE);
                    chartPress.fitScreen();
                }else{
                    Toast.makeText(ChartsActivity.this,"Najpierw wybierz termin!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.TV_PM2_5_Line:
                if(i_pm2_5%2==0){
                    TV_PM2_5_Line.setTextColor(getResources().getColor(R.color.colorGray));
                    pm2_5.setVisible(false);

                }else{
                    TV_PM2_5_Line.setTextColor(getResources().getColor(R.color.colorBlueDark));
                    pm2_5.setVisible(true);
                }
                chartPM.invalidate();
                chartPM.clear();
                chartPM.setData(data);
                i_pm2_5++;
                break;
            case R.id.TV_PM2_5C_Line:
                if(i_pm2_5C%2==0){
                    TV_PM2_5C_Line.setTextColor(getResources().getColor(R.color.colorGray));
                    pm2_5C.setVisible(false);

                }else{
                    TV_PM2_5C_Line.setTextColor(getResources().getColor(R.color.colorBlue));
                    pm2_5C.setVisible(true);
                }
                chartPM.invalidate();
                chartPM.clear();
                chartPM.setData(data);
                i_pm2_5C++;
                break;
            case R.id.TV_PM10_Line:
                if(i_pm_10%2==0){
                    TV_PM10_Line.setTextColor(getResources().getColor(R.color.colorGray));
                    pm_10.setVisible(false);

                }else{
                    TV_PM10_Line.setTextColor(getResources().getColor(R.color.colorRedDark));
                    pm_10.setVisible(true);
                }
                chartPM.invalidate();
                chartPM.clear();
                chartPM.setData(data);
                i_pm_10++;
                break;
            case R.id.TV_PM10C_Line:
                if(i_pm_10C%2==0){
                    TV_PM10C_Line.setTextColor(getResources().getColor(R.color.colorGray));
                    pm_10C.setVisible(false);

                }else{
                    TV_PM10C_Line.setTextColor(getResources().getColor(R.color.colorRed));
                    pm_10C.setVisible(true);
                }
                chartPM.invalidate();
                chartPM.clear();
                chartPM.setData(data);
                i_pm_10C++;
                break;
        }
    }



    private void setVisibilityContent(LinearLayout ll){


        PB_Loading.setVisibility(View.VISIBLE);

        LL_Calendar.setVisibility(View.GONE);
        LL_Dust.setVisibility(View.GONE);
        LL_Humidity.setVisibility(View.GONE);
        LL_Temperature.setVisibility(View.GONE);
        LL_Pressure.setVisibility(View.GONE);


        IV_Calendar.setImageResource(R.drawable.ic_calendar);
        IV_Dust.setImageResource(R.drawable.ic_dust);
        IV_Humidity.setImageResource(R.drawable.ic_humidity);
        IV_Temperature.setImageResource(R.drawable.ic_temperature);
        IV_Pressure.setImageResource(R.drawable.ic_pressure);

        ll.setVisibility(View.VISIBLE);
        PB_Loading.setVisibility(View.GONE);

    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
        dateS = "";
        String dateSS = "";
        if(date.getDay()<10){
            dateS += "0"+date.getDay()+"/";
            int month = date.getMonth()+1;
            if(month<10){
                dateS += "0"+month+"/"+date.getYear();
            }else{
                dateS += month+"/"+date.getYear();
            }
        }else{
            dateS += date.getDay()+"/";
            int month = date.getMonth()+1;
            if(month<10){
                dateS += "0"+month+"/"+date.getYear();
            }else{
                dateS += month+"/"+date.getYear();
            }
        }


        dateSS = date.getYear()+"-";
        if(date.getMonth()+1<10){
            int month = date.getMonth()+1;
            dateSS += "0"+month+"-";
            if(date.getDay()<10){
                dateSS += "0"+date.getDay();
            }else{
                dateSS += date.getDay();
            }
        }else{
            int month = date.getMonth()+1;
            dateSS += month+"-";
            if(date.getDay()<10){
                dateSS += "0"+date.getDay();
            }else{
                dateSS += date.getDay();
            }
        }

        boolean ifContain = false;
        for(String str: listOfDates) {
            if(str.trim().contains(dateSS)) {
                ifContain = true;
                break;
            }
        }

        if(ifContain){
            i_pm2_5 = 0;
            i_pm2_5C = 0;
            i_pm_10 = 0;
            i_pm_10C = 0;

            TV_PM2_5_Line.setTextColor(getResources().getColor(R.color.colorBlueDark));
            TV_PM2_5C_Line.setTextColor(getResources().getColor(R.color.colorBlue));
            TV_PM10_Line.setTextColor(getResources().getColor(R.color.colorRedDark));
            TV_PM10C_Line.setTextColor(getResources().getColor(R.color.colorRed));

            TV_SelectedDay.setText(HtmlCompat.fromHtml("Wybrano: <b>"+dateS+"</b", HtmlCompat.FROM_HTML_MODE_LEGACY));
            new downloadDataMeas().execute();
        }else{

            isSelectedDay = false;
            TV_SelectedDay.setText(HtmlCompat.fromHtml("Brak danych pomiarowych dla:\n <b>"+dateS+"</b", HtmlCompat.FROM_HTML_MODE_LEGACY));
        }



    }


    public class CurrentDateDecorator implements DayViewDecorator {

        private Drawable highlightDrawable;
        private Context context;
        private int y,m,d;

        public CurrentDateDecorator(Context context ,int _y, int _m, int _d) {
            this.context = context;
            y = _y;
            m = _m;
            d = _d;
            highlightDrawable = this.context.getResources().getDrawable(R.drawable.available_data);
        }

        public CurrentDateDecorator(loadingDates loadingDates, int _y, int _m, int _d) {
            this.context = ChartsActivity.this;
            y = _y;
            m = _m;
            d = _d;
            highlightDrawable = this.context.getResources().getDrawable(R.drawable.available_data);

        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.equals(new CalendarDay(y,m-1,d));
        }

        @Override
        public void decorate(DayViewFacade view) {

            view.setBackgroundDrawable(highlightDrawable);
            view.addSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)));
            view.addSpan(new StyleSpan(Typeface.BOLD));
            view.addSpan(new RelativeSizeSpan(1.5f));

        }
    }



    class loadingDates extends AsyncTask<Void, Void, String> {



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {

            if(s.equals("0")){
                for(int i = 0; i<listOfDates.size();i++){
                    calendar.addDecorator(new CurrentDateDecorator(this, Integer.parseInt(listOfDates.get(i).substring(0, 4)), Integer.parseInt(listOfDates.get(i).substring(5, 7)), Integer.parseInt(listOfDates.get(i).substring(8, 10))));
                }
            }


        }

        @Override
        protected String doInBackground(Void... voids) {


            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(URLReturnDates+"&&LATI="+latitude+"&&LONGI="+longitude).build();
            try{
                Response response = client.newCall(request).execute();
                String s = response.body().string();
                JSONArray jsonarray = new JSONArray(s);
                for (int i = 0; i < jsonarray.length(); i++) {
                    JSONObject jsonobject = jsonarray.getJSONObject(i);
                    listOfDates.add(jsonobject.getString("DATE"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("ERROR-Exception-IO", ""+e);
                return "-1";
            }catch (JSONException e) {
                e.printStackTrace();
                Log.d("ERROR-Exception-JSON", ""+e);
                return "-1";
            }

            return "0";
        }
    }


    class downloadDataMeas extends AsyncTask<Void, Void, String> {



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            ArrayList<ILineDataSet> dataSetsPM    = new ArrayList<>();
            ArrayList<ILineDataSet> dataSetsHumi  = new ArrayList<>();
            ArrayList<ILineDataSet> dataSetsTemp  = new ArrayList<>();
            ArrayList<ILineDataSet> dataSetsPress = new ArrayList<>();
            valuesPM2_5.clear();
            valuesPM_10.clear();
            valuesPM2_5C.clear();
            valuesPM_10C.clear();
            valuesHumi.clear();
            valuesTemp.clear();
            valuesPress.clear();

            chartPM.invalidate();
            chartHumi.invalidate();
            chartTemp.invalidate();
            chartPress.invalidate();
            chartHumi.clear();
            chartPM.clear();
            chartTemp.clear();
            chartPress.clear();


            for (int i = 0; i < PM2_5.size(); i++) {
                valuesPM2_5.add(new Entry(TIME.get(i),   PM2_5.get(i)));
                valuesPM_10.add(new Entry(TIME.get(i),   PM_10.get(i)));
                valuesPM2_5C.add(new Entry(TIME.get(i), PM2_5C.get(i)));
                valuesPM_10C.add(new Entry(TIME.get(i), PM_10C.get(i)));
                valuesHumi.add(new Entry(TIME.get(i),    HUMI.get(i).floatValue()));
                valuesTemp.add(new Entry(TIME.get(i),    TEMP.get(i).floatValue()));
                valuesPress.add(new Entry(TIME.get(i),   PRES.get(i).floatValue()));
            }

            pm2_5 = new LineDataSet(valuesPM2_5, "PM2.5");
            pm2_5.setLineWidth(2.5f);
            pm2_5.setColor(getResources().getColor(R.color.colorBlueDark));
            pm2_5.setValueFormatter(new IntValueFormatter());
            pm2_5.setDrawValues(true);
            pm2_5.setDrawCircles(false);
            dataSetsPM.add(pm2_5);

            pm_10 = new LineDataSet(valuesPM_10, "PM10");
            pm_10.setLineWidth(2.5f);
            pm_10.setDrawCircles(false);
            pm_10.setColor(getResources().getColor(R.color.colorRedDark));
            pm_10.setValueFormatter(new IntValueFormatter());
            pm_10.setDrawValues(true);
            dataSetsPM.add(pm_10);

            pm2_5C = new LineDataSet(valuesPM2_5C, "PM2.5C");
            pm2_5C.setLineWidth(2.5f);
            pm2_5C.setDrawCircles(false);
            pm2_5C.setColor(getResources().getColor(R.color.colorBlue));
            pm2_5C.setValueFormatter(new IntValueFormatter());
            pm2_5C.setDrawValues(true);
            dataSetsPM.add(pm2_5C);

            pm_10C = new LineDataSet(valuesPM_10C, "PM10C");
            pm_10C.setLineWidth(2.5f);
            pm_10C.setDrawCircles(false);
            pm_10C.setColor(getResources().getColor(R.color.colorRed));
            pm_10C.setValueFormatter(new IntValueFormatter());
            pm_10C.setDrawValues(true);
            dataSetsPM.add(pm_10C);

            humi = new LineDataSet(valuesHumi, "Huminity");
            humi.setLineWidth(2.5f);
            humi.setDrawCircles(false);
            humi.setColor(getResources().getColor(R.color.colorBlue));
            humi.setValueFormatter(new IntValueFormatter());
            humi.setDrawValues(true);
            dataSetsHumi.add(humi);

            temp = new LineDataSet(valuesTemp, "Temperature");
            temp.setLineWidth(2.5f);
            temp.setDrawCircles(false);
            temp.setColor(getResources().getColor(R.color.colorBlue));
            temp.setDrawValues(true);
            dataSetsTemp.add(temp);

            press = new LineDataSet(valuesPress, "Pressure");
            press.setLineWidth(2.5f);
            press.setDrawCircles(false);
            press.setColor(getResources().getColor(R.color.colorBlue));
            press.setValueFormatter(new IntValueFormatter());
            press.setDrawValues(true);
            dataSetsPress.add(press);





            data = new LineData(dataSetsPM);
            isSelectedDay = true;

            chartPM.setData(data);
            chartHumi.setData(new LineData(dataSetsHumi));
            chartTemp.setData(new LineData(dataSetsTemp));
            chartPress.setData(new LineData(dataSetsPress));
            chartPM.invalidate();
            chartHumi.invalidate();
            chartTemp.invalidate();
            chartPress.invalidate();
        }

        @Override
        protected String doInBackground(Void... voids) {


            OkHttpClient client = new OkHttpClient();
            //Log.d("jjjjjj",URLReturnDataMeas+"&&DAY="+dateS.substring(6, dateS.length())+"-"+dateS.substring(3, 5)+"-"+dateS.substring(0, 2)+"&&L_LATI="+latitude+"&&L_LONG="+longitude );
            Request request = new Request.Builder().url(URLReturnDataMeas+"&&DAY="+dateS.substring(6, dateS.length())+"-"+dateS.substring(3, 5)+"-"+dateS.substring(0, 2)+"&&L_LATI="+latitude+"&&L_LONG="+longitude).build();
            try{
                Response response = client.newCall(request).execute();
                String s = response.body().string();

                PM2_5.clear();
                PM_10.clear();
                HUMI.clear();
                TEMP.clear();
                PRES.clear();
                TIME.clear();
                JSONArray jsonarray = new JSONArray(s);
                for (int i = 0; i < jsonarray.length(); i++) {
                    JSONObject jsonobject = jsonarray.getJSONObject(i);
                    PM2_5.add(jsonobject.getInt("PM2_5"));
                    PM_10.add(jsonobject.getInt("PM10"));
                    PM2_5C.add(jsonobject.getInt("PM2_5_CORR"));
                    PM_10C.add(jsonobject.getInt("PM_10_CORR"));
                    HUMI.add(jsonobject.getInt("HUMIDITY"));
                    TEMP.add(round(jsonobject.getDouble("TEMPERATURE"), 1));
                    PRES.add(jsonobject.getInt("PRESSURE"));
                    TIME.add(convertToMinutes(jsonobject.getString("DATE_SAVE")));
                    // Log.d("jjjj", TIME.get(i)+" "+PM2_5.get(i)+" "+PM_10.get(i)+" "+PM2_5C.get(i)+" "+PM_10C.get(i)+" "+HUMI.get(i)+" "+TEMP.get(i)+" "+PRES.get(i));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("ERROR-Exception-IO", ""+e);
                return "-1";
            }catch (JSONException e) {
                e.printStackTrace();
                Log.d("ERROR-Exception-JSON", ""+e);
                return "-1";
            }
            return "0";
        }
    }



    public int convertToMinutes(String t){
        String[] parts = t.split(" ");
        String part2 = parts[1];
        String[] part3 = part2.split(":");
        int h = Integer.parseInt(part3[0]);
        int m = Integer.parseInt(part3[1]);
        return h*60+m;
    }

    public String convertToHour(float min){
        int h = (int) (min/60);
        int m = (int) (min - 60*h);
        if(m<10){
            return h+":0"+m;
        }else{
            return h+":"+m;
        }
    }

    public class MyValueFormatter extends ValueFormatter
    {

        @Override
        public String getFormattedValue(float value) {
            return convertToHour(value);
        }


    }

    public class IntValueFormatter extends ValueFormatter
    {

        @Override
        public String getFormattedValue(float value) {
            return "" + ((int) value);
        }


    }

    private double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

}

