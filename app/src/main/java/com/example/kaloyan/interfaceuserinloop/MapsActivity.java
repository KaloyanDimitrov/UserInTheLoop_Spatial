package com.example.kaloyan.interfaceuserinloop;

import android.Manifest;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.StrictMode;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.graphics.*;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
//import com.google.maps.android.heatmaps.*;
import com.google.android.gms.maps.*;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimerTask;

import java.util.Timer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final LatLng INITIAL = new LatLng(53.557526, 9.991512); //HagenBecks Dorm

    private ArrayList<Double>latX = new ArrayList<Double>();     //used for pointing to the nearest better signal spot
    private ArrayList<Double>lngY = new ArrayList<Double>();
    private ArrayList<Double>rssi = new ArrayList<Double>();

    private ArrayList<Double>latXsum = new ArrayList<Double>();  //used for summing up nearby data points into big points
    private ArrayList<Double>lngYsum = new ArrayList<Double>();
    private ArrayList<Double>rssisum = new ArrayList<Double>();

    private ArrayList<WeightedLatLng> wLatLng = new ArrayList<WeightedLatLng>();
    private ArrayList<Marker> cellTowerMarkers = new ArrayList<Marker>();
    private ArrayList<LatLng> cellTowerLocations = new ArrayList<LatLng>();

    private int mode;

    private LocationManager locManager;
    private LocationListener locListener;
    private TelephonyManager Tel;
    private MyPhoneStateListener    MyListener;
    private SignalStrength signalStrength;
    private String posicition;
    private String signal;
    private Location defaultLoc;

    private HttpURLConnection urlConnection = null;

    public final static long LOCATION_REFRESH_TIME = 10;        //min time before location is refreshed in miliseconds
    public final static float LOCATION_REFRESH_DISTANCE = 10.0f;    //min distance before location is refreshed in metars




    float[] startPoints = { 0.39f, 0.4f };

    int[] colorsLevel10 = {
            Color.rgb(254, 0, 0), // green (default)
            Color.rgb(255, 0, 0)    // red
    };

    int[] colorsLevel8 = {
            Color.rgb(255, 254, 0),
            Color.rgb(255, 255, 0)    // yellow
    };

    int[] colorsLevel6 = {
            Color.rgb(0, 254, 0),
            Color.rgb(0, 255, 0)    // green
    };
    int[] colorsLevel4 = {
            Color.rgb(0, 254, 255),
            Color.rgb(0, 255, 255)    // tequila blue
    };

    int[] colorsLevel2 = {
            Color.rgb(0, 0, 254),
            Color.rgb(0, 0, 255)    // dark blue
    };


    Gradient gradientLevel10 = new Gradient(colorsLevel10, startPoints); //Strongest signal (Red)
    Gradient gradientLevel8 = new Gradient(colorsLevel8, startPoints);
    Gradient gradientLevel6 = new Gradient(colorsLevel6, startPoints);
    Gradient gradientLevel4 = new Gradient(colorsLevel4, startPoints);
    Gradient gradientLevel2 = new Gradient(colorsLevel2, startPoints); //Weakest signal (dark blue)


    //----------------------------------------------------------------------------------------------------

    private void generateCsvFile(String sFileName)
    {
        try
        {
            FileWriter writer = new FileWriter(sFileName);

            for(int i = 0;i<latX.size();i++){
                writer.append(latX.get(i).toString());
                writer.append(",");
                writer.append(lngY.get(i).toString());
                writer.append(",");
                writer.append(rssi.get(i).toString());
                writer.append('\n');
            }
            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void loadCellTowers(){
        try {
            // Create a URL for the desired page
            //http://r1482a-02.etech.haw-hamburg.de/~w15cpteam1/finalversion.txt
            URL url = new URL("http://r1482a-02.etech.haw-hamburg.de/~w15cpteam1/TOWERS.csv");

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String data;
            String values[];

            while ((data = in.readLine()) != null) {
                //readLine() strips the newline character(s)

                values = data.split(",");
                cellTowerLocations.add(new LatLng( Double.valueOf(values[0]),Double.valueOf(values[1])));

            }
            in.close();


        } catch (Exception e){
            e.printStackTrace();
        }

    }


    private void loadHeatMaps() {

        try {
            // Create a URL for the desired page
            //http://r1482a-02.etech.haw-hamburg.de/~w15cpteam1/finalversion.txt
            URL url = new URL("http://r1482a-02.etech.haw-hamburg.de/~w15cpteam1/csvlist.csv");

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String data;
            String values[];

            while ((data = in.readLine()) != null) {
                //readLine() strips the newline character(s)

                values = data.split(",");

                latX.add(Double.valueOf(values[0]));
                lngY.add(Double.valueOf(values[1]));
                rssi.add(Double.valueOf(values[2]));

                latXsum.add(Double.valueOf(values[0]));
                lngYsum.add(Double.valueOf(values[1]));
                rssisum.add(Double.valueOf(values[2]));
            }
            in.close();

        } catch (Exception e){
            e.printStackTrace();
        }


    }

    private void sumUpPoints(){
        int k = 0;
        double differenceInPosition = 0.016; //the bigger the number, more points will be summed up


            while(k < latXsum.size()){
                int summedPoints = 1;
                double sumLat = 0;
                double sumLng = 0;
                double sumRssi = 0;

                for(int o = k+1; o < latXsum.size();o++){
                    //if points are near by sum them up and and delete the second one
                    if(Math.abs(latXsum.get(k)-latXsum.get(o)) + Math.abs(lngYsum.get(k)-lngYsum.get(o)) < differenceInPosition){
                        summedPoints +=1;
                        sumLat += latXsum.get(o);
                        sumLng += lngYsum.get(o);
                        sumRssi += rssisum.get(o);

                        latXsum.remove(o);
                        lngYsum.remove(o);
                        rssisum.remove(o);
                        o--; //current data at o will be removed and replaced with o+1 so we need to o-- or else we will miss this one point
                    }
                }
                //after comparing to every point and finding all nearby ones we merge them into a single point
                //this single point is not considered for future merging
                if(summedPoints != 1){

                    latXsum.set(k, (latXsum.get(k) + sumLat ) / summedPoints);
                    lngYsum.set(k, (lngYsum.get(k) + sumLng ) / summedPoints);
                    rssisum.set(k, (rssisum.get(k) + sumRssi ) / summedPoints);

                }
                k++;
            }

    }

    // Add the tile overlay to the map.
    private void addHeatMap() {

        List<LatLng> list10 = new ArrayList<LatLng>();
        List<LatLng> list8 = new ArrayList<LatLng>();
        List<LatLng> list6 = new ArrayList<LatLng>();
        List<LatLng> list4 = new ArrayList<LatLng>();
        List<LatLng> list2 = new ArrayList<LatLng>();

        //TODO sumup points into a large point with an average value
        //TODO increase size of points so they look better
        //TODO test and improve colors


        boolean sumUpEnabled = false;
        boolean useWeightedLatLng = true;

        if(sumUpEnabled) sumUpPoints();  //calls the summing up algorithm

        if(useWeightedLatLng){


            for(int i = 0; i < latXsum.size();i++){
                if(rssisum.get(i) > 0.0001)
                    wLatLng.add(new WeightedLatLng(new LatLng(latXsum.get(i), lngYsum.get(i)), rssisum.get(i)/100 )); //use weighted latLng with intensitiy greater than 0 to 1
            }

            if(wLatLng.size() != 0) {
                HeatmapTileProvider wHeatMap = new HeatmapTileProvider.Builder()
                        .weightedData(wLatLng)
                        .build();
                mMap.addTileOverlay(new TileOverlayOptions().tileProvider(wHeatMap));
            }


        }

        else {
            for (int i = 0; i < latXsum.size(); i++) {
                    if (rssisum.get(i) > 80)
                        list10.add(new LatLng(latXsum.get(i), lngYsum.get(i)));
                    else if (rssisum.get(i) > 60)
                        list8.add(new LatLng(latXsum.get(i), lngYsum.get(i)));
                    else if (rssisum.get(i) > 40)
                        list6.add(new LatLng(latXsum.get(i), lngYsum.get(i)));
/*
                else if(rssisum.get(i)>20)
                    list4.add(new LatLng(latXsum.get(i), lngYsum.get(i)));
                else if(rssisum.get(i)>10)
                    list2.add(new LatLng(latXsum.get(i), lngYsum.get(i)));
             */

            }


            // Create a heat map tile provider, passing it the latlngs.
            if (list10.get(0) != null) {
                HeatmapTileProvider hot10 = new HeatmapTileProvider.Builder()
                        .data(list10) //--prints the single heated point on the starting position (line 81)
                        .gradient(gradientLevel10)
                        .opacity(0.5)
                        .radius(20) //between 10-50, default is 20
                        .build();


                mMap.addTileOverlay(new TileOverlayOptions().tileProvider(hot10));
            }

            if (list8.get(0) != null) {
                HeatmapTileProvider hot8 = new HeatmapTileProvider.Builder()
                        //--prints the single heated point on the starting position (line 81)
                        .data(list8)
                        .gradient(gradientLevel8)
                        .opacity(0.5)
                        .radius(20)
                        .build();


                mMap.addTileOverlay(new TileOverlayOptions().tileProvider(hot8));

            }

            if (list6.get(0) != null) {
                HeatmapTileProvider hot6 = new HeatmapTileProvider.Builder()
                        //--prints the single heated point on the starting position (line 81)
                        .data(list6)
                        .gradient(gradientLevel6)
                        .opacity(0.5)
                        .radius(20)
                        .build();


                mMap.addTileOverlay(new TileOverlayOptions().tileProvider(hot6));


            }
/*
            if(list4.get(0) !=null){
            HeatmapTileProvider  hot4 = new HeatmapTileProvider.Builder()
                    //--prints the single heated point on the starting position (line 81)
                    .data(list4)
                    .gradient(gradientLevel4)
                    .opacity(0.5)
                    .radius(20)
                    .build();

            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(hot4));


            }

            if(list2.get(0) !=null){
            HeatmapTileProvider  hot2 = new HeatmapTileProvider.Builder()
                    //--prints the single heated point on the starting position (line 81)
                    .data(list2)
                    .gradient(gradientLevel2)
                    .opacity(0.5)
                    .radius(20)
                    .build();

            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(hot2));


        }
*/


        } //end of else for using normal latLng
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences("settings",MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        mode = settings.getInt("mode",2);

        try {
            if (mode == 0 || mode == 2) {
                threadHeatMap.start();
                threadHeatMap.join();//start thread to open file ASAP otherwise file might not be read in time so heatmaps might not be displayed
            }
            if (mode == 1){
                threadCellTowers.start();
                threadCellTowers.join();
        }
        } catch(Exception e){
            e.printStackTrace();
        }
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        GoogleMapOptions options = new GoogleMapOptions();
        options.mapType(GoogleMap.MAP_TYPE_SATELLITE)
                .compassEnabled(true)
                .zoomControlsEnabled(true)
                .mapToolbarEnabled(true);

        mapFragment.newInstance(options);
        mapFragment.getMapAsync(this);


        MyListener = new MyPhoneStateListener();
        Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        startLocating();

        //which mode
        //True shows CellTowers , False shows Signal Stength Data


//        ToggleButton toggleMode = (ToggleButton)findViewById(R.id.toggleModeButton);
//        if(toggleMode.isChecked())
            // TODO code for Cell Towers
//            ;
//        else
            //TODO code for Signal Strength Map
//            ;


    }

    private void startLocating() {

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //if you have permission get the location
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            defaultLoc = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        locListener = new com.google.android.gms.location.LocationListener() {
            public void onLocationChanged(Location location) {
                //call this when the user has moved
                calculateLocation(location);
            }

            public void onProviderDisabled(String provider) {
            }

            public void onProviderEnabled(String provider) {
                ;
            }

            public void onStatusChanged(String provider,
                                        int status, Bundle extras) {

            }
        };
    }


    private void calculateLocation(Location loc){
        //find the new position of the device and the coresponding signal stength
        if(loc!=null){
            posicition = String.valueOf(loc.getLatitude()) + " , " + String.valueOf(loc.getLongitude());
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        setUpMap();

    }


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    Thread threadHeatMap = new Thread (new Runnable() {
        @Override
        public void run() {
            try {
                loadHeatMaps();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    });

    Thread threadCellTowers = new Thread (new Runnable() {
        @Override
        public void run() {
            try {
                loadCellTowers();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    });


    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(INITIAL, 11)); //higher for more zoom

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                float maxZoomHeatMap = 15.0f;
                float minZoomMarker = 7.0f;

                if (cameraPosition.zoom > maxZoomHeatMap && mode == 0)
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(maxZoomHeatMap));
                if (cameraPosition.zoom < minZoomMarker && mode == 1)             //hide markers if zoomed too far
                    hideMarkers();
                if (cameraPosition.zoom >= minZoomMarker && mode == 1)           //show markers if not too far
                    showMarkers();
            }
        });

        if(mode == 0) //set up Heat Map for mode 0
            addHeatMap();
        if(mode == 1)
            addCellTowers();
        if(mode == 2)
            addHeatMap();
            findBetterSignal();


    }

    private void addCellTowers(){
        int maxNumberOfTowersShown = 100;
        for(int i = 0; i < cellTowerLocations.size() && i < maxNumberOfTowersShown; i++) {
            //adds the markers to the map and to an arraylist for easy change of the visibility later on
            cellTowerMarkers.add(mMap.addMarker(new MarkerOptions()
                    .position(cellTowerLocations.get(i))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.celltowerbettericon))));

            /*
            mMap.addMarker(new MarkerOptions()
                    .position(cellTowerLocations.get(i))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.celltowerbettericon)));*/
        }


    }

    private void hideMarkers(){ //hide them for small zoom levels
        for(int i=0; i<cellTowerMarkers.size();i++){
           cellTowerMarkers.get(i).setVisible(false);
        }
    }

    private void showMarkers(){
        for(int i=0; i<cellTowerMarkers.size();i++){
            cellTowerMarkers.get(i).setVisible(true);
        }
    }

    private void findBetterSignal(){
        Location myLoc = defaultLoc;

        if(myLoc != null){

            SharedPreferences settings = getSharedPreferences("settings",MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();

            LatLng myLatLng = new LatLng(myLoc.getLatitude() , myLoc.getLongitude());

            //TODO add code for getting the signal strength

            double signalStrength = 10;
            double maxDistance = settings.getInt("meter",500);
            double minImprovement = 10;
            double distanceToPoint = 0;
            double differenceInSignal = 0;
            double bestScore = 0;
            double distanceToBestLocation = 0;

            //TODO add factor whether the person would like to move more or have a better signal
            double distanceFactor = 1;
            double signalFactor = 1;

            int positionOfBestPoint = -1;

            for(int i = 0; i < latX.size(); i ++){
                //distance in meters
                distanceToPoint = SphericalUtil.computeDistanceBetween(myLatLng,
                        new LatLng(latX.get(i), lngY.get(i)));
                //distanceToPoint = Math.abs(myLatLng.latitude-latX.get(i)) + Math.abs(myLatLng.longitude -lngY.get(i));
                if(distanceToPoint <= maxDistance)
                {
                    //point is near you now check if it has better signal
                    differenceInSignal = rssi.get(i) - signalStrength;  //positive is better, negative is worse
                    if(differenceInSignal > minImprovement) {
                        //compute a value that would help compare different better points based on how good and how for away they are
                        if(bestScore < (differenceInSignal * signalFactor) / distanceToPoint * distanceFactor )
                            positionOfBestPoint = i;
                            bestScore = (differenceInSignal * signalFactor) / distanceToPoint * distanceFactor ;

                    }
                }
            }

            if(positionOfBestPoint != -1){ //we have a better spot, put a marker on it
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latX.get(positionOfBestPoint), lngY.get(positionOfBestPoint))));
                //distance to point in meters
                distanceToBestLocation = SphericalUtil.computeDistanceBetween(new LatLng(myLoc.getLatitude(),myLoc.getLongitude()),
                                                    new LatLng(latX.get(positionOfBestPoint), lngY.get(positionOfBestPoint)));

                Toast.makeText(this, "You need to walk " + String.valueOf((int)distanceToBestLocation + " meters "),
                        Toast.LENGTH_LONG).show();

            }
            else
             // no better spot found
            {
                Toast.makeText(this, "No better spot was found near by.",
                        Toast.LENGTH_LONG).show();

            }


        }
        //TODO inform user he couldn't be found
    }

    private class MyPhoneStateListener extends PhoneStateListener
    {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength)
        {
            super.onSignalStrengthsChanged(signalStrength);
            signal = " "+String.valueOf(signalStrength.getGsmSignalStrength());

            SharedPreferences settings = getSharedPreferences("settings",MODE_PRIVATE);

            if(settings.getBoolean("shareDataEnabled",true))
                postData(defaultLoc,signal);    //check if user has agreed to share data and only then send to server
            calculateLocation(defaultLoc);

            //TODO make 3 seperate activities, one for cell towers, one for heat map, and one for local file storage only for the current user
            //for current user add a new heat map when u change the location maybe
        }

    }

    public void postData(Location loc, String signal) {

        try {

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);


            URL url = new URL("http://141.22.15.229:7311");
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os));
            writer.write(posicition+signal);
            writer.flush();
            writer.close();
            os.close();

            urlConnection.connect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            urlConnection.getInputStream()));
            String decodedString;
            while ((decodedString = in.readLine()) != null) {
                System.out.println(decodedString);
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
