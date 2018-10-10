package co.weatherappwidas;

import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.BindView;
import co.weatherappwidas.Data.updateWeatherView;

import static co.weatherappwidas.Data.apicallWeatherInfo.getJSON;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, updateWeatherView {

    private static final int REQUEST_CODE_PERMISSION = 100;

    public static SupportMapFragment mapFragment;

    static GoogleMap googleMap;

    public String cityName="";

    public Handler mHandler;

    @BindView(R.id.edt_location)
    EditText edtLocation;

    @BindView(R.id.current_temperature_field)
    TextView current_temperature_field;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            ActivityCompat.requestPermissions(this, new String[]{

                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,

            }, REQUEST_CODE_PERMISSION);

        }


        mHandler = new Handler();

        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap));

        mapFragment.getMapAsync(this);

        googleMap = mapFragment.getMap();


        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_SHORT).show();


                double lat = point.latitude;
                double lng = point.longitude;

                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(lat, lng, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {

                    cityName = addresses.get(0).getAddressLine(0);

                    Log.d("sss", "Display city name" + cityName);

                    Snackbar snackbar = Snackbar
                            .make(getWindow().getDecorView().getRootView(), "Selected City name" + cityName, Snackbar.LENGTH_LONG);

                    snackbar.show();

                    MarkerOptions markerOptions = new MarkerOptions();

                    markerOptions.position(point);

                    markerOptions.title(point.latitude + ":" + point.longitude);

                    googleMap.clear();

                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));

                    googleMap.addMarker(markerOptions);

                    updateWeatherData(cityName);

                } catch (IndexOutOfBoundsException e)

                {
                    Snackbar snackbar = Snackbar
                            .make(getWindow().getDecorView().getRootView(), "Error getting location data", Snackbar.LENGTH_LONG);

                    snackbar.show();

                    Log.e("sss", "Error in the JSON data"+e.getMessage().toString());

                }


            }
        });


        edtLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                updateWeatherData(s.toString());

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap1) {
        googleMap = googleMap1;
    }


    public void updateWeatherData(final String city) {
        new Thread() {
            public void run() {

                final JSONObject json = getJSON(getApplicationContext(), city);

                if (json == null) {
                    mHandler.post(new Runnable() {
                        public void run() {

                            Snackbar snackbar = Snackbar
                                    .make(getWindow().getDecorView().getRootView(), "Error", Snackbar.LENGTH_LONG);

                            snackbar.show();
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        public void run() {
                            renderWeather(json);
                        }
                    });
                }
            }
        }.start();
    }


    private void renderWeather(JSONObject json) {
        try {

            JSONObject details = json.getJSONArray("weather").getJSONObject(0);

            JSONObject main = json.getJSONObject("main");

            DateFormat df = DateFormat.getDateTimeInstance();

            String updatedOn = df.format(new Date(json.getLong("dt") * 1000));

            current_temperature_field.setText(json.getString("name").toUpperCase(Locale.US) +
                            ", " + json.getJSONObject("sys").getString("country") + "\n" +
                            details.getString("description").toUpperCase(Locale.US) +
                            "\n" + "Humidity: " + main.getString("humidity") + "%" +
                            "\n" + "Pressure: " + main.getString("pressure") + " hPa" +
                            "\n" + String.format("%.2f", main.getDouble("temp")) + " ℃" +
                            "\n" + "Last update: " + updatedOn);
        }

        catch (Exception e) {

            Log.e("sss", "Error in the JSON data"+e.getMessage().toString());
        }
    }


}
