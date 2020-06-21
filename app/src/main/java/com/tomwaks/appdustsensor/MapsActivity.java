package com.tomwaks.appdustsensor;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMapLoadedCallback {


    int statusSensor = -1;
    String lastActiveSensor = "";
    double lastLatitude = 0;
    double lastLongitude = 0;

    double selectedLatitude = 0;
    double selectedLongitude = 0;

    private GoogleMap mMap;
    ProgressBar pbLoading;
    ImageView tvGoToCharts, ivMenu;

    private MarkerOptions options = new MarkerOptions();
    private CircleOptions circle = new CircleOptions();
    ArrayList<LatLng> latlngs = new ArrayList<>();
    List<String> lastMeas = new ArrayList<>();
    List<String> numbersMeas = new ArrayList<>();

    private Dialog logIn;
    TextView tvInfoLocation, err, tvStatusDevice, tvLocationOrDate;
    EditText pin;
    Button logInClose;

    String location = "oczekiwanie";

    String URLStatusSensor = "http://dustsensor.h2g.pl/statusSensor.php?KEY=01123581321345589144233";
    String URLLocations = "http://dustsensor.h2g.pl/locations.php";
    String URLUpdateLocations = "http://dustsensor.h2g.pl/settingLocations.php";

    Handler handler = new Handler();
    Runnable runnable;

    int delay = 30*1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();

        dialog_to_config();

        new checkStatus().execute();
        pbLoading.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onResume() {
        //start handler as activity become visible

        handler.postDelayed( runnable = new Runnable() {
            public void run() {
                new checkStatus().execute();

                handler.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    private void init() {
        /*TextView*/
        tvInfoLocation = findViewById(R.id.tvInfoLocation);

        tvStatusDevice = findViewById(R.id.tvStatusDevice);

        tvLocationOrDate = findViewById(R.id.tvLocationOrDate);

        tvGoToCharts = findViewById(R.id.tvGoToCharts);
        tvGoToCharts.setOnClickListener(this);
        tvGoToCharts.setEnabled(false);

        /*ImageView*/
        ivMenu = findViewById(R.id.btnMenu);
        ivMenu.setOnClickListener(this);

        /*ProgressBar*/
        pbLoading = findViewById(R.id.pbLoading);
    }

    private void dialog_to_config() {

        logIn = new Dialog(MapsActivity.this);
        logIn.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logIn.setCancelable(false);
        logIn.setContentView(R.layout.dialog_login);

        err = (TextView) logIn.findViewById(R.id.tvErr);
        pin = (EditText) logIn.findViewById(R.id.etPin);
        logInClose = (Button) logIn.findViewById(R.id.logInClose);

        logInClose.setOnClickListener(this);


        pin.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 4) {
                    if (s.toString().equals("1234")) {
                        err.setText("PIN poprawny!");
                        err.setTextColor(getResources().getColor(R.color.colorGreen));

                        Log.d("status", "true");
                        Intent myIntent = new Intent(MapsActivity.this, ConfigActivity.class);
                        MapsActivity.this.startActivity(myIntent);
                        pin.setText("");
                        err.setText("Błędny PIN!");
                        logIn.dismiss();

                    } else {
                        err.setTextColor(getResources().getColor(R.color.colorRed));
                    }
                } else {

                    err.setTextColor(getResources().getColor(R.color.colorPrimary));
                    //err.setHeight(0);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_dark));
        new downloadLocations().execute();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MapsActivity.this, "Nie można określić Twojej lokalizacji", Toast.LENGTH_SHORT).show();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.2297, 21.0122), 10f));
        }else{

            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationChangeListener(myLocationChangeListener);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng lat = marker.getPosition();

                selectedLatitude  = lat.latitude;
                selectedLongitude = lat.longitude;


                String address = getAddress(lat);
                location = address;
                tvInfoLocation.setText("Wybrana lokalizacja:\n" + address);
                tvGoToCharts.setImageResource(R.drawable.ic_goto);
                tvGoToCharts.setEnabled(true);
                tvGoToCharts.setBackgroundColor(getResources().getColor(R.color.colorWhite));


                return false;
            }
        });

        mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {

                tvInfoLocation.setText("Nie wybrano lokalizacji!");
                tvGoToCharts.setImageResource(R.drawable.ic_dont_goto);
                tvGoToCharts.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                tvGoToCharts.setEnabled(false);
            }
        });

        mMap.setOnMapLoadedCallback(this);

    }

    private String getAddress(LatLng l) {
        String a = "none";

        Geocoder geo = new Geocoder(MapsActivity.this, Locale.getDefault());
        try {
            List<Address> address = geo.getFromLocation(l.latitude, l.longitude, 1);
            if (address != null && address.size() > 0) {
                a = address.get(0).getThoroughfare() + " " + address.get(0).getSubThoroughfare()+", "+address.get(0).getLocality();
                if (a.toLowerCase().contains("null null".toLowerCase())) {
                    a = "Na terenie: "+address.get(0).getLocality();
                }
            }
        } catch (IOException e) {
            a = "nieokreślona";
        }


        return a;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnMenu:
                PopupMenu popup = new PopupMenu(MapsActivity.this, ivMenu);
                popup.getMenuInflater().inflate(R.menu.menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("Konfiguracja")) {
                            logIn.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                            logIn.show();
                        }

                        if (item.getTitle().equals("Odśwież")) {
                            finish();
                            startActivity(getIntent());
                        }

                        return true;
                    }
                });

                popup.show();//showing popup menu
                break;

            case R.id.tvGoToCharts:
                Intent intent_to_charts = new Intent(MapsActivity.this, ChartsActivity.class);
                intent_to_charts.putExtra("latitude",  String.valueOf(selectedLatitude));
                intent_to_charts.putExtra("longitude", String.valueOf(selectedLongitude));
                Toast.makeText(MapsActivity.this, "Ładowanie widoku", Toast.LENGTH_SHORT).show();
                intent_to_charts.putExtra("location", location);
                startActivity(intent_to_charts);
                break;
            case R.id.logInClose:
                logIn.dismiss();
                break;
        }
    }

    @Override
    public void onMapLoaded() {
        pbLoading.setVisibility(View.GONE);
    }

    class downloadLocations extends AsyncTask<Void, Void, String> {



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            if(s.equals("0")){
                super.onPostExecute(s);
                int j = 0;
                for (LatLng point : latlngs) {
                    options.position(point);
                    options.title("Liczba pomiarów: " + numbersMeas.get(j));
                    options.snippet("Ostatni pomiar z dnia: "+lastMeas.get(j));
                    j++;

                    circle.center(point);
                    circle.radius(1000);
                    circle.fillColor(0x221ABB9C);
                    circle.strokeWidth(1);
                    mMap.addCircle(circle);
                    mMap.addMarker(options);


                }
            }

        }

        @Override
        protected String doInBackground(Void... voids) {


            OkHttpClient client = new OkHttpClient();
            Request requestUpdate = new Request.Builder().url(URLUpdateLocations).build();
            try{
                Response responseS = client.newCall(requestUpdate).execute();
                Request request = new Request.Builder().url(URLLocations).build();
                try{
                    Response response = client.newCall(request).execute();
                    String s = response.body().string();
                    JSONArray jsonarray = new JSONArray(s);
                    for (int i = 0; i < jsonarray.length(); i++) {
                        JSONObject jsonobject = jsonarray.getJSONObject(i);
                        latlngs.add(new LatLng(jsonobject.getDouble("LATI"), jsonobject.getDouble("LONG")));
                        lastMeas.add(jsonobject.getString("DATE").substring(0, jsonobject.getString("DATE").length()-3));
                        numbersMeas.add(jsonobject.getString("NUMB"));
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


            } catch (IOException e) {
                e.printStackTrace();
                Log.d("ERROR-Exception-IO", ""+e);
                return "-3";
            }


            return "0";
        }
    }


    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }



    class checkStatus extends AsyncTask<Void, Void, String> {



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);




            if(s.equals("1")){
                tvStatusDevice.setText(Html.fromHtml("<b>STATUS:</b> <font color='green'>ON</font>"));
                tvLocationOrDate.setText(Html.fromHtml("<b>Lokalizacja:</b><br>"+getAddress(new LatLng(lastLatitude, lastLongitude))));
            }

            if(s.equals("0")){
                tvStatusDevice.setText(Html.fromHtml("<b>STATUS:</b> <font color='red'>OFF</font>"));
                tvLocationOrDate.setText(Html.fromHtml("<b>Ostatnio dostępny:</b>\n"+lastActiveSensor.substring(0, lastActiveSensor.length()-3)));

            }


        }

        @Override
        protected String doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(URLStatusSensor).build();
            try{
                Response response = client.newCall(request).execute();
                String s = response.body().string();
                s = s.substring(1,s.length()-1);

                JSONObject jsonData = new JSONObject(s);
                statusSensor      = jsonData.getInt("STATUS");
                lastActiveSensor  = jsonData.getString("LAST_ACTIVE");
                lastLatitude      = jsonData.getDouble("LAST_LATITUDE");
                lastLongitude     = jsonData.getDouble("LAST_LONGITUDE");


                if(statusSensor == 1){
                    return "1";
                }else{
                    return "0";
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
        }
    }



    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            if(mMap != null){
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 10.0f));
            }
        }
    };

}
