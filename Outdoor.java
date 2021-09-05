package com.example.vaggelis.assignment_2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;

/**
 * Created by Evan on 31/03/2018.
 */

/*Accessed by ALL other activities of the app.This activity uses the
 FusedLocationProviderClient and displays the location
 (latitude and longitude) of the user in high accuracy (using GPS) on the map.
 Note that the location is updated every 8 seconds (to make sure that it does
 not require a lot of phone battery) or every 5 seconds if the app can find the location
 from somewhere else (e.g. another app running in the background). Note that when the
  activity is first created WIFI is turned ON for the map to be refreshed. Moreover,
  the latitude and longitude as well as the speed in km/h are display on top of the map in text form.
  Moreover, this activity loads the number of reference points stored in database
  (if the database exists) and thus if the reference points are greater than zero
  whenever the user presses the "Indoor Locate" button it goes to the Indoor_Positioning
  activity. If, though, the reference points are zero (if they have all been deleted or
  if the database does not exist yet), the user is not allowed to go to the Indoor_Positioning
  Activity YET. Furthermore, the user by pressing the "Indoor Train" button, goes
  to the Indoor_Train Activity. Finally, the user can return to Initial_Screen by
  pressing the back button of the phone.*/
public class Outdoor extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    //Used to request location updates
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationCallback mLocationCallback;

    FeedReaderDbHelper myDb; //Used to instantiate the SQLiteOpenHelper
    private Double myLat, myLat2; //Stores the Latitude (in Degrees Format)
    private Double myLong, myLong2; //Store the Longitude (in Degrees Format)
    private float Myspeed; //Stores the speed in m/s over ground
    int lat_deg; //holds latitude degrees
    int lat_min; //holds latitude minutes
    double lat_sec; //holds latitude seconds
    int longi_deg; //holds longitude degrees
    int longi_min; //holds longitude minutes
    double longi_sec; //holds longitude seconds
    private String lat; //is N or S (if we are either on the up or down Hemisphere)
    private String logit; //holds W or E (if we either are on the left or right Hemisphere)

    int reference_points = 0; //holds the number of reference points stored in the database
                              //NOTE THAT if the database does not exist then reference
                              //points are by default 0.

    int indexres; //Index which points in the REFERENCE_POINTS column of the database to obtain
                  //the number of the reference points stored in the database (if one exists)

    Cursor num_of_reference_points; //Cursor which helps us obtain the number of reference points
                                    //from the database, if one exists
    SupportMapFragment mapFragment; //Fragment in which the Google Map is displayed
    TextView latitude, longitude, speedkmh; //TextViews used for displaying the longitude and
                                            // latitude coordinates
    WifiManager wifi; //Note, that it is important to make sure that the Wifi is ON in order for
                      //the map image to be loaded. Thus, we create an object (instance)
                      //of class WifiManager.

    Marker Cur_location_Marker; //Used to display a marker on the current location of the phone
                                //on the map

    boolean overlay_added=false; //Used to make sure that the floorplan of TLG room is added
                                 //on top of the map just ONCE.

    //____________________________________________________________________________________________//

    //Method called whenever the Activity is created and the TextViews, the FeedReaderDbHelper class,
    // the map fragment and the WIFI manager are initialised. Also the number of reference points is obtained after
    // calling the get_reference_points() method and the FusedLocationProviderClient is initialed
    //after calling the  initialize_fused_location_prov_client() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outdoor);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //Match the TextView variables with the appropriate TextView IDs
        latitude = (TextView) findViewById(R.id.LatTextView);
        longitude = (TextView) findViewById(R.id.LongTextView);
        speedkmh = (TextView) findViewById(R.id.speedkmhTextView);

        myDb = new FeedReaderDbHelper(this); //instantiate FeedReaderHelper class


        //Initialise the WifiManager instance in order for the map to be refreshed
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Now, we check the WIFi status on the mobile phone and if the wifi is OFF we turn it ON because it is needed for the map image to be loaded
        if (wifi.getWifiState() == wifi.WIFI_STATE_DISABLED) {
            wifi.setWifiEnabled(true);
            Toast.makeText(getApplicationContext(), "Wifi Enabled", Toast.LENGTH_SHORT).show();

        }

        //We will get the number of reference points from the database file (if the database file exists)
        //We do that to NOT allow the use to go to indoor positioning activity without
        //having at least one reference point in the database file
        get_reference_points();


        //Initialised the FusedLocationProviderClient
        initialize_fused_location_prov_client();


    }

//_________________________________________________________________________________________________//
    //Method which initialises the FusedLocationProviderClient and whenever it is  called by
    //the requestLocationUpdates() method (either every 8s or 5s as explained in the description
    //of the Activity in the very begging) it transforms the latitude and longitude from degrees
    //into integer degrees, minutes and seconds which are then displayed on TextViews (together with
    // the speed  of the usr in km/h)
    public void initialize_fused_location_prov_client(){
        //Initialize FusedLocationProviderClient
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest(); //used for obtaining the location
        locationRequest.setInterval(8 * 1000);  //Set the desired interval for active location
                                                //updates. We will look every 15s in the location
                                                //provider in order to avoid battery drainage

        locationRequest.setFastestInterval(5 * 1000);  //This controls the fastest rate at which the
        //application will receive location updates,which might be faster than setInterval (when
        //for instance our app can obtain the location from another app (which requires no battery usage))

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //For the location we
        //aim for accuracy (thus we use the GPS sensor)

        //Callback method needed as part of the FusedLocationProvider to update the User Interface
        // with the new location each time
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    //Update the UI with location data
                    if (location != null) {
                        myLat = location.getLatitude(); //get the latitude
                        myLong = location.getLongitude(); //get the longitude
                        Myspeed = location.getSpeed(); //get the speed (m/s)over the ground
                        updatemap();
                        myLong2 = myLong;
                        myLat2 = myLat;
                        lat_deg = myLat2.intValue();
                        if (lat_deg > 0) lat = "N"; //We are on top Hemisphere
                        else if (lat_deg < 0) lat = "S"; //We are on the down Hemisphere
                        myLat2 = Math.abs(myLat2);
                        myLat2 *= 60;
                        myLat2 -= (Math.abs(lat_deg) * 60);
                        lat_min = myLat2.intValue();
                        myLat2 *= 60;
                        myLat2 -= (Math.abs(lat_min) * 60);
                        lat_sec = myLat2;
                        latitude.setText(lat_deg + "°" + lat_min + "'" + String.format("%.2f", lat_sec) + "''" + lat);
                        //Now for the longitude (to split degrees in degrees, minutes and seconds)
                        longi_deg = myLong2.intValue();
                        if (longi_deg > 0) logit = "E"; //We are on right Hemisphere
                        else if (longi_deg < 0) logit = "W"; //We are on the left Hemisphere
                        myLong2 = Math.abs(myLong2);
                        myLong2 *= 60;
                        myLong2 -= (Math.abs(longi_deg) * 60);
                        longi_min = myLong2.intValue();
                        myLong2 *= 60;
                        myLong2 -= (Math.abs(longi_min) * 60);
                        longi_sec = myLong2;
                        longitude.setText(longi_deg + "°" + longi_min + "'" + String.format("%.2f", longi_sec) + "''" + logit);
                        speedkmh.setText(String.valueOf(Myspeed * 3.6) + " km/h"); //display speed of the user in km/h
                    }
                }
            }
        };

    }

//_________________________________________________________________________________________________//
    //Method which is used to obtain the number of reference points from the database (Fingerprint.db inside
   //FeedReaderDbHelper.java))
    // (and in case no database exists the reference points by default are 0). We do that
    // BECAUSE IN CASE no database exists OR the reference points are zero (as the user can delete at any
    // time any reference point) then WE DO NOT allow the user to access
    //Indoor_Positioning Activity and we ask him/her to train the application first (by going to Indoor_Train activity).
    public void get_reference_points(){
        if (doesDatabaseExist(this, "Fingerprint.db")) {
            //if it exists read the reference points which are stored in the database
            num_of_reference_points = myDb.getReferencepoints();
            indexres = num_of_reference_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.REFERENCE_POINTS);
            num_of_reference_points.moveToFirst();
            reference_points = num_of_reference_points.getInt(indexres);
        }
    }

//_________________________________________________________________________________________________//
    //Method which checks if the database file of the application exists or not and return true
    //if it does exists and false if it does not exist
    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

//_________________________________________________________________________________________________//
    //Method which updates the Map with the new Marker on the new location of the user
    public void updatemap() {
        mapFragment.getMapAsync(this);
    }

//_________________________________________________________________________________________________//
    //Method which handles what it is displayed on Google maps and also adds the floorplan of TLG
    //on top of the map (but in the future any floorplan can be added)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        //mMap.clear();
        mMap.setMapType(mMap.MAP_TYPE_HYBRID);

        //Overlay_added boolean makes sure that we add the overlay of the TLG floorplan just ONCE.
        //ADD THE FLOORPLAN
        if ( overlay_added==false) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.tlg_and_out_a_bit_2);
            LatLng northEast = new LatLng(55.922767, -3.172496);
            LatLng southeWest = new LatLng(55.922375, -3.172788);
            LatLngBounds latLngBounds = new LatLngBounds(southeWest, northEast);
            GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions();
            groundOverlayOptions.bearing(180 - 32);
            groundOverlayOptions.positionFromBounds(latLngBounds);
            groundOverlayOptions.image(bitmapDescriptor);
            groundOverlayOptions.transparency(0.1f);
            mMap.addGroundOverlay(groundOverlayOptions);
            overlay_added=true;
        }

        //Remover the previous marker if it exists and place the marker in the new location
        if (Cur_location_Marker != null) {
            Cur_location_Marker.remove();
        }

        //Add a marker to the new (Lat, Long) location
        LatLng myLoc = new LatLng(myLat, myLong);
        Cur_location_Marker = mMap.addMarker(new MarkerOptions().position(myLoc).title("You are here!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc)); //move map's camera to that location;
    }

//_________________________________________________________________________________________________//
    //Method which is used to request location updates (every 8s or 5s as explained in the description)
    //of this Activity (on the very top)
    private void requestLocationUpdates() {
        //It is required from Android to do this check BUT if we enter this activity it means that
        // the user already has given location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
            return;
        }
    }

//_________________________________________________________________________________________________//
    //Request Location Updates when the Activity is Resumed
    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
    }

//_________________________________________________________________________________________________//
    //Method excuted whenever we pause/exit/stop this Activity (Outdoor Activity)
    @Override
    protected void onPause() {
        super.onPause();
        // Remove Location Updates if we were pause the app
        stopLocationUpdates();
    }

//_________________________________________________________________________________________________//
    //Method which stops location updates whenever we exit/stop/pause the outdoor activity
    public void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the INDOOR LOCATE button is pressed (and we enter the indoor
    // positioning phase). NOTE THAT IN case the reference points are zero (either they have been deleted
    // or the database does not exist yet) then we display an appropriate Toast and we tell the user
    //to train the app first
    public void Indoor_Locate (View view){
        if (reference_points!=0){
            Intent indoor_positioning=new Intent(Outdoor.this, Indoor_Positioning.class);
            startActivity(indoor_positioning);
        }
        else{
            Toast.makeText(getApplicationContext(), "Please train me first", Toast.LENGTH_LONG).show();
        }
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the INDOOR TRAIN button is pressed (and we enter the indoor training phase)
    public void Indoor_Train(View view){
        Intent indoor_train=new Intent(Outdoor.this, Indoor_Train.class);
        startActivity(indoor_train);
    }

//_________________________________________________________________________________________________//
    //Method which is executed whenever we click the back arrow of the phone and we return to the initial screen
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            Intent back=new Intent(Outdoor.this,Initial_Screen.class);
            startActivity(back);
        }
        return super.onKeyUp( keyCode, event );
    }
}
