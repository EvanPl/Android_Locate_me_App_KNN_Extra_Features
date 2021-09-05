package com.example.vaggelis.assignment_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

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
import java.io.IOException;
import java.util.List;

/**
 * Created by Evan on 31/03/2018.
 */

//Activity which can be accessed from ALL other activities AS LONG AS there is at least
//one reference point in the database. This activity determines and prints on the map (with a marker)
//the reference point which is closest to our current location. This is done by comparing the Euclidean
//distance between the RSSIs of our current location with the RSSI_avg of all reference points
//(FOR THE MATCHED BSSIDs/MAC ADDRESSES) and determining the reference point with the lowest distance.
//PLEASE see the PROGRAMMER's guide for further explanation.
//If the back phone button is pressed the user is returned to the Initial_Screen Activity
//If the "Outdoor" button is pressed the user enters the Outdoor Activity
//If the "Train Me More" button iis pressed the user enters the Indoor_Train Activity to further
//train the application
public class Indoor_Positioning extends FragmentActivity implements OnMapReadyCallback,SensorEventListener {

    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    FeedReaderDbHelper myDb; //Used to instantiate the SQLiteOpenHelper
    WifiManager wifiManager; //We create an object (instance) of class WifiManager
    WifiScanReceiver wifiScanReceiver; //We create an object (instance) of class WifiScanReceiver
    int reference_points=0; //Holds the number of reference points created in the training phase (Indoor_Train)
    int MAC_matches; //Holds the MAC address matches between our current position and a reference point

    double distances; //Helps us find the lowest Euclidean distances (which takes account the number of AP matches
                                                                    //between our current position and each reference point)

    double distances_2; //Helps us find the lowest Euclidean distances (which takes account the number of AP matches
                        //between our current position and each reference point)

    double lowest_distance=500; //Lowest Euclidean distance (which takes account the number of AP matches between
                                // our current position and each reference point). For the initial value we put a very
                                //large one (500) to ensure that that the distance of our current position to the first
                                // reference point will be assigned there (to the lowest_distance variable). This makes
                                //the process of finding the lowest distance between our current position and ALL reference
                                //points WITHOUT having to sort any table (in descending order for example).

    double our_x; //Holds the X coordinate of each reference point
    double our_y; //Holds the Y coordinate of each reference point
    double our_x_final; //Holds our current X coordinate position
    double our_y_final; //Holds our current y coordinate position
    Cursor res_for_finding_current_location,num_of_reference_points,res_for_image_paths; //Cursors which helps us implement
                                                                                            // the indoor positioning

    int MACs_of_the_ref; //Holds the MAC addresses of each reference point
                        // (which are stored in the column MAC_num of the database in the first row
                        // of each different reference point) --> SEE Table 1 in the programmer's guide as an example

    int MACs_of_the_ref_sum; //Helps us navigate through the table to obtain correct data for our localization algorithm)
    int IndexMAC_sum; //Integer which stores the index value of the MAC_sum column when we extract it from the database (FeedReaderDbHelper)
    int IndexRSSI_avg; //Integer which stores the index value of the RSSI_avg column when we extract it from the database (FeedReaderDbHelper)
    int Index_X_coor; //Integer which stores the index value of the CoordinateX column when we extract it from the database (FeedReaderDbHelper)
    int Index_Y_coor; //Integer which stores the index value of the CoordinateY column when we extract it from the database (FeedReaderDbHelper)
    int IndexBSSID; //Integer which stores the index value of the BSSID column when we extract it from the database (FeedReaderDbHelper)
    int indexres,indeximagepath; //Indexes used to extract the number of reference points and also any Image Paths
                                //stored in the database (FeedReaderDbHelper)

    boolean AP_match_found=false; //Becomes true each time an AP of our current location is the same with an AP of the reference points
    boolean ALL_REFERENCE_APs_matched=false; //Becomes true if and only if our current APs have matched with ALL the APs of a reference point
    Marker marker;
    private String ImageFileLocation=""; //stores the location of the captured image at a reference point
    ImageView captured_photo; //ImageView used to display a captured image on which the user can tag sensor data (from the compass)

    //Variables used for displaying a compass arrow in the Indoor_Positioning Activity
    //______________________________________________________________________________________________________________//
    private SensorManager sm; //sm is an object of class SensorManager. We create an instance of SensorManager class
    private Sensor aSensor; //Represents the accelerometer sensor
    private Sensor mSensor; //Represents the magnetic field sensor
    float [] accelerometerValues = new float[3]; //for storing the data of the accelerometer sensor
    float [] magneticFieldValues=new float[3]; //for storing the data of the magnetic field sensor
    private ImageView compass_arrow; //Holds the compass arrow image
    private float azimuth; //Holds the azimuth angle in degrees
    private static float roll; //Holds the roll angle in degrees
    private float current_azimuth=0; //used for the proper animation of the compass arrow image
    private float[] smoothed=new float[3];  //Smoother values after low-pass filtering the data
                                            //values of the magnetic field and accelerometer sensors

    // ____________________________________________________________________________________________________________//

    //Method called whenever the Activity is created and the ImageViews, the Magnetic Sensor,
    // the FeedReaderDbHelper class, the map fragment and the WIFI manager are initialised.
    // Also the number of reference points is obtained after calling the get_reference_points() method.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor__positioning);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //Get an instance of SensorManger for accessing sensors.
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Determine a default sensor type, in this case is magnetometer (MAGNETIC FIELD SENSOR). We basically initialize the mSensor variable
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Determine a default sensor type, in this case is accelerometer. We basically initialize the aSensor variable
        aSensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        compass_arrow=(ImageView) findViewById(R.id.compass_arrow);
        captured_photo=(ImageView) findViewById(R.id.image);

        myDb = new FeedReaderDbHelper(this); //instantiate FeedReaderHelper class
        wifiManager=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //Now, we check the WIFi status on the mobile phone and if the wifi is OFF we turn it ON.
        if (wifiManager.getWifiState()==wifiManager.WIFI_STATE_DISABLED){
            wifiManager.setWifiEnabled(true);
            Toast.makeText(Indoor_Positioning.this, "WIFI Enabled", Toast.LENGTH_SHORT).show();

        }

        // Here we INITIALISE the WifiScanReceiver class
        wifiScanReceiver=new WifiScanReceiver();

        //Read reference points after making sure that the database exists,
        // EVEN though when enter this activity THERE WILL DEFINITELY be a database
        //created (because if there is no database yet we simply do not allow the user to enter this activity.
        //NOTE THAT WE OBTAIN the number of reference points from the database (FeedReaderDbHelper) in order
        //to know how many comparisons to do each time we are calculating our current position (
        // because each time we do as many comparisons as the number of reference points to obtain
        // the minimum distance between our current position and the reference points)
        get_reference_points();

    }

// _______________________________________________________________________________________________//
    //Read reference points after making sure that the database exists,
    // EVEN though when enter this activity THERE WILL DEFINITELY be a database
    //created (because if there is no database yet we simply do not allow the user to enter this activity.
    //NOTE THAT WE OBTAIN the number of reference points from the database (FeedReaderDbHelper) in order
    //to know how many comparisons to do each time we are calculating our current position (
    // because each time we do as many comparisons as the number of reference points to obtain
    // the minimum distance between our current position and the reference points)
    public void get_reference_points(){
        if (doesDatabaseExist(this,"Fingerprint.db")) {
            //if it exists read the reference points which are stored in the database
            num_of_reference_points = myDb.getReferencepoints();
            indexres = num_of_reference_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.REFERENCE_POINTS);
            num_of_reference_points.moveToFirst();
            reference_points=num_of_reference_points.getInt(indexres);
        }
    }

// _______________________________________________________________________________________________//
    //Method which checks if the database file of the application exists or not and return true
    //if it does exists and false if it does not exist
    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

// _______________________________________________________________________________________________//
    //Method in which data from the MAGNETIC FIELD, ACCELEROMETER sensors will be
    //calculated and converted to the degree by which the phone is rotated
    private void calculateOrientation() {
        float[] orientation = new float[3]; //orientation[0] stores the azimuth angle (in radians)
                                            // (0 when phone faces north)
                                            //orientation[1] stores the pitch angle (in radians)
                                            //orientation[2] stores the roll angle (in radians)

        float[] R = new float[9]; //Holds the rotation matrix

        //Compute the rotation matrix R which is used to compute the devices's orientation
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);

        //Now, we compute the device's orientation (based on the rotation matrix).
        //This method returns the azimuth, pitch and roll angles of the phone (in radians).
        SensorManager.getOrientation(R, orientation);

        //Convert the stored data of the "values" array from radians to degrees
        azimuth = (float) Math.toDegrees(orientation[0]); //Azimuth angle in degrees
        azimuth = (azimuth + 360) % 360;
        roll = (float) Math.toDegrees(orientation[2]); //roll angle
        //Call the image_animation() method for the rotation of the compass arrow image
        image_animation();
    }

// _______________________________________________________________________________________________//
    //Method which rotates a compass image based on the azimuth angle calculated in
    //calculateOrientation() method (the roll angle is used to make sure that when the
    // screen of the phone is facing the ground the compass arrow images STILL faces in
    // the right orientation)
    private void image_animation() {
        if (roll>=-90 && roll<=90) { //phone screen is facing up
            Animation anim = new RotateAnimation(-current_azimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            current_azimuth = azimuth;
            anim.setDuration(500); // Amount of time (in milliseconds) for the animation to run
            anim.setRepeatCount(0); // We set the animation to be repeated 0 times
            anim.setFillAfter(true); //Animation applies its transformation after it ends
            compass_arrow.startAnimation(anim);

        }
        else if (roll<-90 || roll>90) {//phone screen is facing down (to the ground) so rotate compass image on the other direction to when phone screen was facing up
            Animation anim = new RotateAnimation(current_azimuth, azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            current_azimuth = azimuth;
            anim.setDuration(500); // Amount of time (in milliseconds) for the animation to run
            anim.setRepeatCount(0); // We set the animation to be repeated 0 times
            anim.setFillAfter(true); //Animation applies its transformation after it ends
            compass_arrow.startAnimation(anim);
        }

    }

// _______________________________________________________________________________________________//
    //Called when sensor values have changed. This method belongs to the SensorEventListener Interface
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Read the MAGNETIC FIELD sensor values from SensorEvent and low pass filter the values.
        //The low pass filtering is done using the LowPassFilter.java File
        if (event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            smoothed=LowPassFilter.filter(event.values,magneticFieldValues );
            magneticFieldValues[0]=smoothed[0];
            magneticFieldValues[1]=smoothed[1];
            magneticFieldValues[2]=smoothed[2];
        }
        //Read the ACCELEROMETER sensor values from SensorEvent and low pass filter the values
        //The low pass filtering is done using the LowPassFilter.java File
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            smoothed=LowPassFilter.filter(event.values,accelerometerValues );
            accelerometerValues[0]=smoothed[0];
            accelerometerValues[1]=smoothed[1];
            accelerometerValues[2]=smoothed[2];
        }
        calculateOrientation();
    }

// _______________________________________________________________________________________________//
    //Method which is being called when the accuracy of a sensor has changed
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

// _______________________________________________________________________________________________//
    //Method which add the floorplan of TLG ONCE (but in the future any floorplan can be added without
    //affecting the operation of the application AT ALL). Also our current position is displayed on
    //GOOGLE MAPS using a marker. Moreover in this method, IF AND ONLY IF there is an IMAGE stored
    //for the reference point WHICH REPRESENTS our current position, then we display that image
    //on an ImageView on the top right of the screen.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setCompassEnabled(false); //remove the in-build compass since we are displaying our
                                                       //own compass in real time on the top left of the screen

        if (our_x_final!=0 && our_y_final!=0){
            //Remover the previous marker if it exists and place the marker in the new location
            if (marker!=null){
                marker.remove();
            }

            //read the PATH_NAMES column and see if there is an Image Stored for the reference point
            // which represents our current position. If it is, clear the ImageView and display that photo stored.
            res_for_image_paths= myDb.getIMAGE_PATHS(our_x_final,our_y_final);
            indeximagepath=res_for_image_paths.getColumnIndex(FeedReaderDbHelper.FeedEntry.IMAGE_PATHS);
            res_for_image_paths.moveToFirst();
            captured_photo.setImageBitmap(null);
            if (res_for_image_paths.getString(indeximagepath)!=null){
                ImageFileLocation=res_for_image_paths.getString(indeximagepath);
                rotateImage(setReducedSize()); //display the image in an ImageView in the right orientation
            }

            //Display a marker on the map to the reference point which represents our current location
            LatLng myLoc = new LatLng(our_x_final, our_y_final);
            marker=mMap.addMarker(new MarkerOptions().position(myLoc).title("You are here!"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc));

        }
        else { // If we enter the "else" we are in the very beginning and thus we load the
                // FLOORPLAN on the map. This is to avoid reloading again again the floorplan and thus
                //using eventually all the memory of the phone (for this task).
            BitmapDescriptor bitmapDescriptor= BitmapDescriptorFactory.fromResource(R.drawable.tlg_and_out_a_bit_2);
            LatLng northEast= new LatLng(55.922767,-3.172496);
            LatLng southeWest= new LatLng(55.922375,-3.172788);
            LatLngBounds latLngBounds = new LatLngBounds(southeWest,northEast);
            GroundOverlayOptions groundOverlayOptions= new GroundOverlayOptions();
            groundOverlayOptions.bearing(180-32);
            groundOverlayOptions.positionFromBounds(latLngBounds);
            groundOverlayOptions.image(bitmapDescriptor);
            groundOverlayOptions.transparency(0.1f);
            mMap.addGroundOverlay(groundOverlayOptions);
        }
    }

// _______________________________________________________________________________________________//
    // Method which calculates the user's current location. This is done by constantly calculating the Euclidean distance BETWEEN
    //the RSSIs of our current position and the RSSI_avg of ALL the reference points (for the matched BSSIDs) and each
    // time we display on the map the reference point whose distance (in RSSI terms) from our current position is the
    // minimum (this is the reference point which is closer to our current position and thus is it accepted to be our current position).
    //NOTE: PLEASE READ THE PROGRAMMER's Guide for further details an the equation used to compare the RSII_avg
    //of all the reference points to the RSSI of our current position (for all the matched MAC addresses)
    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent){
            List<ScanResult> wifiScanList=wifiManager.getScanResults();
            //Get all the rows of the X, Y, RSSI_avg, MAC_num and BSSID columns
            res_for_finding_current_location = myDb.getXY_and_BSSID_andRSSI_avg_and_MAC_num();
            IndexMAC_sum=res_for_finding_current_location.getColumnIndex(FeedReaderDbHelper.FeedEntry.MAC_num);
            IndexRSSI_avg=res_for_finding_current_location.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_avg);
            Index_X_coor=res_for_finding_current_location.getColumnIndex(FeedReaderDbHelper.FeedEntry.COORDINATEX);
            Index_Y_coor=res_for_finding_current_location.getColumnIndex(FeedReaderDbHelper.FeedEntry.COORDINATEY);
            IndexBSSID=res_for_finding_current_location.getColumnIndex(FeedReaderDbHelper.FeedEntry.BSSID);

            //Move the cursor initially to the second row and extract the MAC_num of the first reference point
            //(the number of MAC addresses assigned to this reference point) and assign it to MACs_of_the_ref variable
            // PLEASE SEE TABLE 1 IN PROGRAMMER'S GUIDE FOR VISUALISING THIS
            res_for_finding_current_location.moveToFirst();
            res_for_finding_current_location.moveToNext(); //Now we are in the second row (PLEASE look Table 1 in the programmer's guide)
            MACs_of_the_ref=Integer.valueOf(res_for_finding_current_location.getString(IndexMAC_sum));
            our_x=res_for_finding_current_location.getDouble(Index_X_coor);
            our_y=res_for_finding_current_location.getDouble(Index_Y_coor);
            MACs_of_the_ref_sum=0; //Used to navigate from one reference point to the other
            lowest_distance=500; //Initialize lowest distance for calculating current position again (put a very big values that the first time
                                 //the distance between our current positing and the first reference point will be assigned as the lowest distance).
                                 //Thu, in the end we will have the reference point with the minimum RSII distance WITHOUT having to do any sorting

            for (int i=0; i<reference_points; i++){ //We will implement our positioning algorithm comparing the current location to ALL reference
                                                    // points and determine which (X,Y) coordinates of the reference point to return.
                                                    //Note that we return the (X,Y) Coordinates of the reference point which is closer (in RSSI)
                                                    //to our current position (Please see Programmer's Guide for further details)
                int j=0;
                int APs_of_this_ref=1;
                if (i>0) {
                    //Move the cursor to the first row of the next REFERENCE point
                    res_for_finding_current_location.moveToPosition(MACs_of_the_ref_sum+1);

                    //Obtain the MAC addresses number of the new reference point
                    MACs_of_the_ref=Integer.valueOf(res_for_finding_current_location.getString(IndexMAC_sum));

                    our_x=res_for_finding_current_location.getDouble(Index_X_coor); //Store the X coordinate of that reference point
                    our_y=res_for_finding_current_location.getDouble(Index_Y_coor); //Store the Y coordinate of that reference point
                }
                distances=0;
                distances_2=0;
                AP_match_found=false;
                ALL_REFERENCE_APs_matched=false;
                MAC_matches=0; //EACH TIME this variable stores the MAC address matches of our current position to
                               //a reference point
                while (j<wifiScanList.size() && !ALL_REFERENCE_APs_matched){
                    if ( (wifiScanList.get(j).SSID.equals("eduroam")) || (wifiScanList.get(j).SSID.equals("central")) ){
                        //SCAN the MAC addresses of the reference point (which are in total MACs_of_the_ref)
                        // and stop if a match between a current access point (MAC address/BSSID) and a reference
                        // access point has found or if we have scanned  all the access points of this reference point
                        AP_match_found=false;
                        if (i==0) res_for_finding_current_location.moveToFirst();
                        else res_for_finding_current_location.moveToPosition(MACs_of_the_ref_sum+1);
                        //SCAN all the reference point access points (APs) and obtain the number of AP matches between
                        // our current location and the reference point
                        while ((APs_of_this_ref!=MACs_of_the_ref+1) && (!AP_match_found)  ){
                            //If the mac address of our current position matches any of the MAC addresses of
                            // this reference point then we increase MAC_matches by 1 and we stop scanning the
                            // MACs of the reference point until we move to the NEXT MAC of our CURRENT location
                            if (wifiScanList.get(j).BSSID.equals(res_for_finding_current_location.getString(IndexBSSID))) { //A match of AP found
                                MAC_matches++;
                                distances+=(res_for_finding_current_location.getFloat(IndexRSSI_avg) - (float) wifiScanList.get(j).level)*
                                        (res_for_finding_current_location.getFloat(IndexRSSI_avg) - (float) wifiScanList.get(j).level);
                                AP_match_found=true;
                            }
                            APs_of_this_ref++; //Counts the number of rows scanned for each reference point
                                               //(note that each reference location has as many rows as MAC addresses)
                            res_for_finding_current_location.moveToNext();
                        }
                    }
                    j++;
                    APs_of_this_ref=1;
                    if (MAC_matches==MACs_of_the_ref) ALL_REFERENCE_APs_matched=true;
                }
                MACs_of_the_ref_sum+=MACs_of_the_ref;
                distances_2=Math.sqrt(distances/MAC_matches); //CALCULATE THE EUCLIDEAN DISTANCE
                                                              //PLEASE see the Programmer Guide for further
                                                              //explanation of this equation

                //If the Euclidean distance of this referee point if smaller that
                //the previous Euclidean distance then assign this reference point
                //as our POTENTIAL current location. When we have gone through
                //ALL the reference point the variables our_x_final and
                //our_y_final will hold the reference point closest to us (which is our current position)
                //WITHOUT the need to do any sorting
                if ((distances_2)<lowest_distance){
                    lowest_distance=distances_2;
                    our_x_final=our_x;
                    our_y_final=our_y;
                }
            }
            updatemap();
            wifiManager.startScan(); //continue scanning to find the user's next location

        }
    }

// _______________________________________________________________________________________________//
    //When the activity Resumes after exiting/pausing/stopping it we register the WIFI receiver
    //and the SensorManager (which handles the magnetic field and accelerometer sensors)
    protected void onResume(){
        super.onResume();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        sm.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this,aSensor,SensorManager.SENSOR_DELAY_FASTEST);
        wifiManager.startScan(); //Start scanning
    }

// _______________________________________________________________________________________________//
    //Unregister the WIFI SCAN receiver and the SensorManager
    // (which handles the magnetic field and accelerometer sensors)
    protected void onPause(){
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
        sm.unregisterListener(this,mSensor);
        sm.unregisterListener(this,aSensor);

    }

// _______________________________________________________________________________________________//
    //Update Marker each time a new indoor location (new closest reference point to our current location)
    // is obtained
    public void updatemap(){
            mapFragment.getMapAsync(this);
    }


    //Method which is excited when the "Train Me More" button is pressed
    public void training_phase(View view){
        Intent indoor_training = new Intent(Indoor_Positioning.this, Indoor_Train.class);
        startActivity(indoor_training);
    }

// _______________________________________________________________________________________________//
    //Method which returns a scaled image (in the form of a bitmap) of the captured image (from the in-build camera app).
    //We do this in order to ensure that the captured image can be
    //displayed on the ImageView for a camera with any megapixels number (e.g for 16MP camera, 12MP etc).
    private Bitmap setReducedSize (){
        //Get the width and height of the ImageView
        int targetImageViewWidth = captured_photo.getWidth();
        int targetImageViewHeight = captured_photo.getHeight();
        //Get the width and height of the captured image
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //Get a sample of the stored image (so not actually loading the image) to get the size of that image
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ImageFileLocation,bmOptions);
        int cameraImageWidth = bmOptions.outWidth;
        int cameraImageHeight = bmOptions.outHeight;

        //Now find the minimum difference between the size of the ImageView and the size of the captured photo
        // (from the in-build camera app).
        //Then we will feed this difference to when we create the bitmap to be displayed on the ImageView
        int scaleFactor = Math.max(cameraImageWidth/targetImageViewWidth,cameraImageHeight/targetImageViewHeight);
        bmOptions.inSampleSize=scaleFactor;
        bmOptions.inJustDecodeBounds=false; //set it to false so we can actually load the bitmap (our stored image)
        //Now return the scaled image (in the form of a bitmap)
        return BitmapFactory.decodeFile(ImageFileLocation,bmOptions);

    }

 // _______________________________________________________________________________________________//
    //Method which is responsible for correcting the orientation of the image that is being displayed on the
    // ImageView and also responsible for displaying the captured image on the ImageView
    //Note that it takes a variable of type Bitmap (which in our case will be the scaled captured image).
    private void rotateImage (Bitmap bitmap) {
        if (bitmap!=null) { //this means that the user has not deleted the photo (at a reference point) from the folder that was saved
            ExifInterface exifInterface = null;
            try {
                exifInterface = new ExifInterface(ImageFileLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Now get the orientation of the captured image (from the in-build camera)
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
            //Now check the orientation
            switch (orientation) {
                //Now if the saved image has a rotation of 90 degrees we will rotate it by the same amount (90 degrees)
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                default:
                    matrix.setRotate(90);
                    break;
            }
            //Now feed the orientation to the bitmap that will be displayed on the ImageView
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            captured_photo.setImageBitmap(rotatedBitmap);
        }
        else{ //If we enter "else" it means that the user has deleted the photo from the folder that was saved when captured
            captured_photo.setImageBitmap(null);
        }
    }

// _______________________________________________________________________________________________//
    //Method which is executed when the "Outdoor" button is pressed and we return to the Outdoor Activity
    public void Outdoor_Locate(View view){
        Intent back=new Intent(Indoor_Positioning.this,Outdoor.class);
        startActivity(back);
    }

// _______________________________________________________________________________________________//
    //Method which is being executed whenever we click the back arrow of the phone and we
    // return to the initial screen
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            Intent back=new Intent(Indoor_Positioning.this,Initial_Screen.class);
            startActivity(back);
        }
        return super.onKeyUp( keyCode, event );
    }
}
