package com.example.vaggelis.assignment_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Evan on 31/03/2018.
 */


//This activity can be accessed by ALL other activities. First of all it loads the floorplan of TLG
//on the map (just once) adn then measures the number of reference points (if there are any, because the
//user might have deleted them all or the database might have not been created yet). Then a marker
//for EACH REFERENCE POINT is added on the map (on the location that each reference point was captured).
//Moreover the user can press on the map and then the latitude and longitude coordinates of the point
//that he/she just pressed are displayed in TexViews on top of the map. Then by pressing the start
//button a marker is added for that reference point and the storing process begins (we store MAC addresses
// for the reference point). Please refer to Programmer's Guide for visualising that. By placing a marker
//for each stored reference point the user has the ability at ANY TIME (after closing for example
// the app and then reopening it) to delete
// ANY of the reference points by simply clicking on a marker representing a reference point
//and pressing "Delete ref point". Finally at each marker (thus, at EACH REFERENCE POINT and
// NOT in any other point on the map) the user can press the camera button and after granting storage
//permission a photo can be taken FOR ANY reference point which are then being displayed
// during indoor positioning on an ImageView as explained in the Indoor_Positioning activity. Note that
//if the user tries to capture a photo at any other point in the map (which is not a reference point)
//then an appropriate message is displayed.
// The photos are being stored inside a folder named "Reference points" in the internal device’s Camera/Pictures folder
// and if the user deletes any of the photos inside this folder then the app will understand that
// and during positioning phase it will simply not display the image of that reference point as it does
//not exist anymore.
public class Indoor_Train extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    TextView latitude, longitude; //Shows the Longitude and Latitude Coordinates of the User when he clicks on the Map
    double Xcoordinate,Ycoordinate; //Stores the X and Y coordinates when you click on the map or the marker
    double Xcoordinate_to_store,Ycoordinate_to_store;  //Stores the X and Y coordinates of the reference point
    double Xcoord,Ycoord; //Used to put markers in the existing reference points
    int macnum=0; //Helps us to put markers in the existing reference points
    FeedReaderDbHelper myDb; //used to instantiate the SQLiteOpenHelper
    WifiManager wifiManager; // we create an object (instance) of class WifiManager
    WifiScanReceiver wifiScanReceiver; // we create an object (instance) of class WifiScanReceiver
    boolean isInserted; //Used to indicate if Wifi BSSID of each access points is inserted into the database
    int counter_1=0; //Makes sure that after 2 consecutive WIFI scans (with a 3s seconds interval apart) we stop updating the table
    int counter_2=0;
    int counter_3=0;
    boolean  matching_finished=false; //used to add RSSI2 in the table
    boolean matching_finished_2; //used to add RSSI2 in the table
    //Different Cursors used for inserting/updating data in the database
    Cursor res_for_updating_table;
    Cursor res_for_reading_table;
    Cursor res_for_RSSI_avg;
    Cursor res_for_updating_MAC_num;
    Cursor res_for_updating_references;
    Cursor num_of_reference_points;
    Cursor res_for_file_image_paths;
    Cursor res_for_fixing_mac_num;
    Cursor res_for_putting_markers_in_all_refernce_points;
    int indexBSSID, indexRSSI, indexID,indexref,indexX,indexY,indexMAC; //Used for updating the table
    float average=0;
    int reference_points=0; //Holds the number of reference points created in the training phase
    SupportMapFragment mapFragment;
    boolean storage_permission=false; //Becomes true when the user allows storage permission and thus the user can capture a photo at a reference point
    private String ImageFileLocation=""; //stores the location of the captured image at a reference point
    public static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE =1; //Request code which is used to deal with storage permissions of the user
    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1; //Request code which is used in order to capture an image from the in-build
    // camera app of the phone
    boolean cantakephoto=false; //used to prevent the user taking a photo for a reference point before the data for that reference point have
    // been added to the table
    boolean overlay_added=false; //Make overlay to be added just once
    boolean ready_to_receive_wifi=false; //becomes true whenever we are ready to reaceive wifi results for a reference point
    Marker mymarker; //Used to put a marker at each reference point that the user selects

    //_____________________________________________________________________________________________//

    //Method called whenever the Activity is created and the TextViews, the FeedReaderDbHelper class,
    //the map fragment and the WIFI manager are initialised. Also the number of reference points is obtained after
    //calling the get_reference_points() method.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor__train);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        latitude = (TextView) findViewById(R.id.LatTextView);
        longitude = (TextView) findViewById(R.id.LongTextView);

        //Initialise FeedReaderDbHelper Class
        myDb = new FeedReaderDbHelper(this);


        //Initialise the WifiManager instance
        wifiManager=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //Now, we check the WIFi status on the mobile phone and if the wifi is OFF we turn it ON.
        if (wifiManager.getWifiState()==wifiManager.WIFI_STATE_DISABLED){
            wifiManager.setWifiEnabled(true);
            Toast.makeText(Indoor_Train.this, "WIFI Enabled", Toast.LENGTH_SHORT).show();

        }
        wifiScanReceiver=new WifiScanReceiver();

        //We will get the number of reference points from the database file (if the database file exists)
        //We do that to NOT allow the use to go to indoor positioning activity without
        //having at least one reference point in the database file
        get_reference_points();


    }

//_________________________________________________________________________________________________//
    //Method which is used to obtain the number of reference points from the database (Fingerprint.db inside
    //FeedReaderDbHelper.java) (and in case no database exists the reference points by default are 0).
    // We do that in order to display a marker on the map on the location OF EACH REFERENCE POINT ALREADY
   // stored previously in the database, so the user to know where he/she placed the reference points and also
    //to be able to delete any reference point and add a picture to any reference point.
    public void get_reference_points(){
        if (doesDatabaseExist(this,"Fingerprint.db")) {
            //if it exists read the reference points which are stored in the database
            num_of_reference_points = myDb.getReferencepoints();
            indexref = num_of_reference_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.REFERENCE_POINTS);
            num_of_reference_points.moveToFirst();
            reference_points = num_of_reference_points.getInt(indexref);
        }
    }

//_________________________________________________________________________________________________//
    //Method which checks if the database file of the application exists or not and return true if it
    // does exists and false if it does not exist
    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

//_________________________________________________________________________________________________//
    //Method which add the floorplan of TLG ONCE (but in the future any floorplan can be added without
    //affecting the operation of the application AT ALL). Also the method put_marker_at_location_of_each_ref_point()
    //is called which add a marker on the map on the location of EACH reference point. This enables the user
    //to know where he/she placed the reference points and also to the able to delete any reference point at any time
    // and also to add a picture for any reference point, again, at ANY TIME.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);

        //With overlay_added boolean we make sure that we add the overlay on the map ONCE
        //ADD THE FLOORPLAN
        if (overlay_added==false) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.tlg_and_out_a_bit_2);
            LatLng northEast = new LatLng(55.922767, -3.172496);
            LatLng southWest = new LatLng(55.922375, -3.172788);
            LatLngBounds latLngBounds = new LatLngBounds(southWest, northEast);

            GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions();
            // float bearing =groundOverlayOptions.getBearing();
            groundOverlayOptions.bearing(180 - 32);
            groundOverlayOptions.positionFromBounds(latLngBounds);
            groundOverlayOptions.image(bitmapDescriptor);
            groundOverlayOptions.transparency(0.1f);
            mMap.addGroundOverlay(groundOverlayOptions);
            //Toast.makeText(this, "Overlay added", Toast.LENGTH_SHORT).show();
            overlay_added=true;
        }

        //Move the camera on top of Edinburgh City so the user can navigate to TLG in King's
        //Buildings, much quicker (as this app was designed initially for indoor navigation in TLG
        //of King's Buildings in Edinburgh). However, this can be changed or even removed without affecting
        //the operation of the application AT ALL.
        LatLng tlg = new LatLng(55.922767, -3.172496);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(tlg));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(9);
        mMap.animateCamera(zoom);

        //Add a marker on the location of EACH reference point
        put_marker_at_location_of_each_ref_point();


    }

//_________________________________________________________________________________________________//
    //Method which adds a marker on the map on the location OF EACH ALREADY STORED reference point. Note that
    //in case the reference points are zero (either because the user has deleted them all or because
    // the database (Fingerprint.db inside FeedReaderDbHelper.java ) does not exist yet, we do not display
    //any markers.
    public void put_marker_at_location_of_each_ref_point(){
        if (reference_points > 0) {
            //Note that if there are no reference points stored then the loop will not be executed which is exactly what we want
            res_for_putting_markers_in_all_refernce_points=myDb.getCoordinateY_X_and_Mac_num();
            indexX=res_for_putting_markers_in_all_refernce_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.COORDINATEX);
            indexY=res_for_putting_markers_in_all_refernce_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.COORDINATEY);
            indexMAC=res_for_putting_markers_in_all_refernce_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.MAC_num);
            res_for_putting_markers_in_all_refernce_points.moveToFirst();
            res_for_putting_markers_in_all_refernce_points.moveToNext();
            int i=0;
            while ((i<reference_points) && (!res_for_putting_markers_in_all_refernce_points.isAfterLast())  ){
                if (i==0){ //Initially we will move to the secong row of the Table which stores all the reference
                           //points information
                    Xcoord=res_for_putting_markers_in_all_refernce_points.getDouble(indexX);
                    Ycoord=res_for_putting_markers_in_all_refernce_points.getDouble(indexY);
                    LatLng myLoc = new LatLng(Xcoord, Ycoord);
                    mymarker=mMap.addMarker(new MarkerOptions().position(myLoc).title("I am a reference point"));
                }
                else{
                    //Note that MAc_num holds the number of MAC addresses stored for each reference point
                    //Thus this number is used to navigate from one reference point to the other and thus, display
                    //the coordinates of each stored reference point on the map (by using a marker).
                    macnum+=res_for_putting_markers_in_all_refernce_points.getInt(indexMAC);
                    res_for_putting_markers_in_all_refernce_points.moveToPosition(macnum+1);
                    if (!res_for_putting_markers_in_all_refernce_points.isAfterLast()) {
                        Xcoord = res_for_putting_markers_in_all_refernce_points.getDouble(indexX);
                        Ycoord = res_for_putting_markers_in_all_refernce_points.getDouble(indexY);
                        LatLng myLoc = new LatLng(Xcoord, Ycoord);
                        mymarker = mMap.addMarker(new MarkerOptions().position(myLoc).title("I am a reference point"));
                    }
                }
                i++;
            }
        }
    }

//_________________________________________________________________________________________________//
    //Method which is executed whenever the user clicks on the MAP (NOTE: ON THE MAP AND NOT ON A MARKER)
    //and displays in TextViews the latitude and longitude coordinates (in degrees)
    //of the point at which the user has clicked on the map. Moreover, since the user has
    //clicked on the map we make cantakephoto=false to prevent the user from taking a photo (since
    //we allow the user to capture a photo for any reference point which is shown with a MARKER on the map)
    @Override
    public void onMapClick(LatLng latLng) {
        cantakephoto=false; //This will become true since the user has stored the reference point data in the database
        Xcoordinate=latLng.latitude;
        Ycoordinate=latLng.longitude;
        latitude.setText("Latitude: "+Xcoordinate);
        longitude.setText("Longitude: "+Ycoordinate);
    }

//_________________________________________________________________________________________________//
    //Makes sure that whenever the user presses on a Marker (which represents a STORED reference point)
    // the coordinates of the marker are saved in Xcoordinate and Ycoordinate variables and displayed on TextViews.
    //Moreover, since the user has clicked on a marker (AND THUS A REFERENCE POINT), we make cantakephoto=true
    // to allow the user to take a photo for that marker (or else said for that reference point).
    //Note that if a photo has already been captured for a reference point and the user retakes a photo
    //for the SAME reference point, then the app during Indoor Positioning will display the latest captured
    //photo
    @Override
    public boolean onMarkerClick(final Marker marker) {
        Xcoordinate= marker.getPosition().latitude;
        Ycoordinate=marker.getPosition().longitude;
        latitude.setText("Latitude: "+Xcoordinate);
        longitude.setText("Longitude: "+Ycoordinate);
        cantakephoto=true;
        return true;
    }


    //!(NOTE: PLEASE REFER to the Programmer's Guide for full explanation on how onReceive is implemented)!
    //Method which is executed every time we scan for wifi ACCESS POINTS (wifiManager.startScan()) and
    //stores all the necessary information for each reference point
    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent){
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            if (ready_to_receive_wifi==true) { //When we register the WIFI it can enter onReceive without
                //actually requesting a scan. Thus we put this boolean to ensure that we process
                //the wifi scan results received whenever we need to (meaning whenever we do wifiManager.startScan()

                counter_1++; //Used to help use scan 2 RSSI values for EACH MAC address stored in the database

                counter_3 = 0; //When we create a reference point, during the first WIFI scan we FIRST obtain RSSI1
                               //of K number MAC addresses (which satisfy the criteria shown below)
                               // where, here, K is equal to "counter_2". Then by doing a second WIFI scan we
                               //obtain RSSI2 OF THE SAME K MAC addresses. However, during this second WIFI scan
                               //we might NOT find one or more of those K MAC addresses (as happened in many tests).
                               //Thus, counter_3 stores the number of N MAC addresses for which RSSI2 value obtained.
                               //It is obvious, that when N==K then we obtained an RSSI2 value of ALL the K MAC addresses
                               //stored for a reference point during the first WIFI Scan. PLEASE REFER to the
                               //Programmer's Guide for further explanation.

                Toast.makeText(Indoor_Train.this, "WIFI RESULTS RECEIVED", Toast.LENGTH_SHORT).show();

                //When counter_1==1 it means that we are in the first WIFI scan and thus we INSERT to the Table the K MAC addresses
                // satisfying the criteria shown below (their SSID is either equal to eduroam or central and the WIFI strenght
                // is greater than -70dBm)
                if (counter_1 == 1) {
                    int i = 0;
                    while (i < wifiScanList.size() && counter_2 < 9) { //We will store for each location the 8 strongest BSSIDs
                        if ((wifiScanList.get(i).SSID.equals("eduroam")) || (wifiScanList.get(i).SSID.equals("central"))) {
                            if (wifiScanList.get(i).level > -70) {
                                counter_2++; //gives us the size (number of MAC Addresses) stored for each reference point.
                                isInserted = myDb.insertData(Xcoordinate_to_store, Ycoordinate_to_store,
                                        wifiScanList.get(i).SSID, wifiScanList.get(i).BSSID, wifiScanList.get(i).level);
                            }
                        }
                        i++;
                    }

                    //If the K MAC addresses are inserted we print a Toast saying that 50% of reference point capturing process is
                    // done (that means that RSSI1 value for those K MAC addresses has been obtained)
                    if (isInserted == true) {
                        isInserted = false;
                        Toast.makeText(Indoor_Train.this, "50% DONE", Toast.LENGTH_SHORT).show();
                        res_for_updating_MAC_num = myDb.getID_of_specific_coordinates(Xcoordinate_to_store, Ycoordinate_to_store);

                        //As already said counter_2 holds the number of MAC addresses stored during the first WIFI scan
                        //for a reference point. Thus, now we enter that number to the table (please refer to the
                        // programmer's guide for further explanation)

                        if (res_for_updating_MAC_num.getCount()>0) {
                            //Get the ID and the MAC_num columns for the reference point (CoordinateX, CoordinateY) we are just capturing
                            indexID = res_for_updating_MAC_num.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                            //Update the first row of MAC_num column of our table with the number of MAC addresses stored for this particular
                            // location (counter_2)
                            res_for_updating_MAC_num.moveToFirst(); //We will only update the first row of the MAC_num column with the number
                            // of MAC_addresses captured for this reference point
                            myDb.updateMAC_num(res_for_updating_MAC_num.getString(indexID), counter_2);
                            wifiManager.startScan(); //Now scan the again to obtain RSSI2
                        }

                        //After multiple tests SOMETIMES (approx. 5% chance) even if we store the reference point in the database then this point
                        //was NOT readable from the database. Thus, in this case we do  catch that error by saying to the user to re-train the
                        //application at very close point to the one that did not work.

                        else{ //Make sure that in case the point stored during the first scan is not readable that the user is indormed to re-capture
                              //a reference at a close point and also update the MAC_num (number of MAC addresses for that error point) so that
                              //the app is not affected AT All by that error!
                            Toast.makeText(Indoor_Train.this, "Error-please go train me again at a close point", Toast.LENGTH_SHORT).show();
                            //Now the stored point cannot be read from the database so we will just update its MAC_NUm to ensure that it does
                            // not effect the positioning phase at all
                            res_for_fixing_mac_num=myDb.getMacNumandID();
                            int  MAC_num_temp=0;
                            indexID=res_for_fixing_mac_num.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                            indexMAC=res_for_fixing_mac_num.getColumnIndex(FeedReaderDbHelper.FeedEntry.MAC_num);
                            res_for_fixing_mac_num.moveToFirst();
                            res_for_fixing_mac_num.moveToNext();
                            while (res_for_fixing_mac_num.getInt(indexMAC)!=0){
                                MAC_num_temp+=res_for_fixing_mac_num.getInt(indexMAC);
                                res_for_fixing_mac_num.moveToPosition(MAC_num_temp+1);
                            }
                            //If we come here we are in the faulty point (the one that cannot be read)
                            myDb.updateMAC_num_of_faulty(res_for_fixing_mac_num.getString(indexID), counter_2);
                            ready_to_receive_wifi=false;

                        }

                    }
                    else { //In this case none of the WIFI MAC-ADDRESSES satisfied our restrictions (their SSID to be either equal to
                           //"eduroam" or "central" and their WIFI strength is greater than -70dBm)). Also, we could enter this
                          //else statement if the user has turned location permissions off and thus we cannot obtain the WIFI scan list.
                          //As a result we inform the user that it might be due to the WIFI APs not satisying the restrictions or
                          //simply because location permission are turned OFF.
                        Toast.makeText(Indoor_Train.this, "WIFI APs satisfying the restrictions not found - Please try again - check location permissions", Toast.LENGTH_LONG).show();
                        ready_to_receive_wifi=false;
                        reference_points = 0; //Make reference_points again 0.
                        //Re-update table to have 0 reference points
                        res_for_updating_references= myDb.getID();
                        int indexID= res_for_updating_references.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                        res_for_updating_references.moveToFirst(); //now we are in the very first row of the table which holds the number of reference points
                        myDb.UpdateReferencePoints(res_for_updating_references.getString(indexID),0);
                    }
                }
                //__________________________________________________________________________________________________//
                //Now we are in the second WIFI scan (for obtaining RSSI2 of the already stored MAC Addresses and also the average of RSSI1 and RSSI2
                //which is the value being used in the algorithm for indoor positioning).

                else if (counter_1 == 2 && counter_2 > 0) { //Here we also make sure that at least one access point was added in the list (counter_2>0)

                    //Now we read the ID and BSSID COLUMNS from the database for the reference point whose RSSI1 was just captured (during the first WIFI SCAN).
                    res_for_updating_table = myDb.getIDandRSSI2andBSSID(Xcoordinate_to_store, Ycoordinate_to_store);
                    indexID = res_for_updating_table.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID); //stores the index of the ID column
                    indexRSSI = res_for_updating_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_2); //stores the index of the RSSI_2 column
                                                        // (to ensure that we are pointing at the right column which represents the RSSI_2 column)
                    indexBSSID = res_for_updating_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.BSSID); //stores the index of the BSSID column

                    matching_finished = false; //boolean which is used to update our TABLE (please see Programmer's Guide) with the RSSI2 value
                                               //as quickly as possible
                    //Update RSSI2
                    int j = 0;
                    //This loop stops if 1) All K MAC addresses stored in the first WIFI scan have been updated with their RSSI2 value.
                                       //2) We have gone through ALL the wifi scan list obtained during the second scan
                    while (j < wifiScanList.size() && matching_finished == false) {
                        if ((wifiScanList.get(j).SSID.equals("eduroam")) || (wifiScanList.get(j).SSID.equals("central"))) {
                            res_for_updating_table.moveToFirst();
                            matching_finished_2 = false;

                            //This loop stops whenever 1)A MAC address of the WIFI scan list is matched to a MAC Address that was stored
                                                        //during the first WIFI scan.
                                                     //2)We have gone through all the K MAC addresses that were scanned during the first WIFI scan
                            while ((!res_for_updating_table.isAfterLast()) && (matching_finished_2 == false)) {
                                if (Integer.valueOf(res_for_updating_table.getString(indexRSSI)) == 0) {
                                    if (wifiScanList.get(j).BSSID.equals(res_for_updating_table.getString(indexBSSID))) {
                                        counter_3++;
                                        myDb.updateRSSI2(res_for_updating_table.getString(indexID), wifiScanList.get(j).level);
                                        matching_finished_2 = true;
                                        if (counter_3 == counter_2) {
                                            matching_finished = true; //we have updated the whole RSSI2 table of the table we created initially (when counter_1==1)
                                        }
                                    }
                                }
                                res_for_updating_table.moveToNext();
                            }
                        }
                        j++;
                    }
                    //Ready to update the average value (average of RSSI1 and RSSI2)
                    ready_to_receive_wifi=false;
                    res_for_RSSI_avg = myDb.getAllRSSIsandIDs(Xcoordinate_to_store, Ycoordinate_to_store);
                    int indexRSSI1 = res_for_RSSI_avg.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_1); //stores the index of the RSSI_1 column
                    int indexRSSI2 = res_for_RSSI_avg.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_2); //stores the index of the RSSI_2 column
                    int indexID = res_for_RSSI_avg.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                    res_for_RSSI_avg.moveToFirst();
                    while (!res_for_RSSI_avg.isAfterLast()) {
                        int devideby; //NOTE that as already explained we might not obtain RSSI2 for some (most likely for just one) MAC addresses
                                      //that were stored during the first WIFI scan (during which RSSI1 vlaues are obtained). Thus, if this is
                                      //the case, we make sure to devide by just 1 when calculating the average. Otherwise (both RSS1 and RSSI2
                                      //values have obtained for the MAC address) we devide by 2 when calculating the average
                        if (Float.valueOf(res_for_RSSI_avg.getString(indexRSSI1)) == 0 ||
                                Float.valueOf(res_for_RSSI_avg.getString(indexRSSI2)) == 0) {
                                devideby = 1;
                        } else {
                            devideby = 2;
                        } //ALL RSSI1 of RSSI2 are nonzero and thus we devide by 2
                        average = (Float.valueOf(res_for_RSSI_avg.getString(indexRSSI1)) +
                                Integer.valueOf(res_for_RSSI_avg.getString(indexRSSI2))) / devideby;
                        myDb.updateRSSI_avg(res_for_RSSI_avg.getString(indexID), average);
                        res_for_RSSI_avg.moveToNext();
                        average = 0;
                    }
                    Toast.makeText(Indoor_Train.this, "100% DONE", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

//_________________________________________________________________________________________________//
    //Executed when the button "Show Data is pressed".Displays the whole Table containing ALL the
    // stored information for ALL the reference points stored
    //in the database. In case no database exists yet we display an appropriate message
    public void ShowData(View view) {
        res_for_reading_table = myDb.getAlldata();
        if (res_for_reading_table.getCount() == 0) { //No data to read
            showMessage("ERROR","No data to read");
            return;
        } else { //There is data to read
            //GET DATA ONE BY ONE (row by row)
            StringBuffer buffer = new StringBuffer();
            //Move the cursor to the first row
            res_for_reading_table.moveToFirst();
            while (!res_for_reading_table.isAfterLast()) { //as long as the cursor is not in the last row
                int index1 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                int index2 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.COORDINATEX);
                int index3 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.COORDINATEY);
                int index4 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.SSID);
                int index5 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.BSSID);
                int index6 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_1);
                int index7 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_2);
                int index9 = res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.RSSI_avg);
                int index10= res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.MAC_num);
                int index12=res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.REFERENCE_POINTS);
                int index13=res_for_reading_table.getColumnIndex(FeedReaderDbHelper.FeedEntry.IMAGE_PATHS);
                buffer.append("ID: " + res_for_reading_table.getString(index1) + "\n");
                buffer.append("CoordinateX: " + res_for_reading_table.getDouble(index2) + "\n");
                buffer.append("CoordinateY: " + res_for_reading_table.getDouble(index3) + "\n");
                buffer.append("SSID: " + res_for_reading_table.getString(index4) + "\n");
                buffer.append("BSSID: " + res_for_reading_table.getString(index5) + "\n");
                buffer.append("RSSI1: " + res_for_reading_table.getInt(index6) + "\n");
                buffer.append("RSSI2: " + res_for_reading_table.getInt(index7) + "\n");
                buffer.append("RSSI_avg: " + res_for_reading_table.getFloat(index9) + "\n");
                buffer.append("MAC_num: " + res_for_reading_table.getInt(index10) + "\n");
                buffer.append("Reference Points " + res_for_reading_table.getInt(index12) + "\n");
                buffer.append("Image Path " + res_for_reading_table.getString(index13) + "\n");
                res_for_reading_table.moveToNext();
            }
            showMessage("Data",buffer.toString());
        }

    }

//_________________________________________________________________________________________________//
    //Used to display the whole Table containing ALL the stored information for ALL the reference
    // points stored in the database. In case no database exists yet we display an appropriate message
    public void showMessage(String title, String Message){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

//_________________________________________________________________________________________________//
    //We register the Wifi receiver and check if storage permissions have been granted. This enables the
    // user in case he/she denies storage permissions to just pause the app WITHOUT closing it go the
    // phone setting, enable the storage permissions and then return to the app which now it will
    // recognise that storage permission have been granted
    protected void onResume(){
        super.onResume();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); //We register the WIFI
        // only once (when we capture the first reference point)
        Toast.makeText(Indoor_Train.this, "Wifi receiver register - ready to go", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                storage_permission=true;
            }
        }
    }

//_________________________________________________________________________________________________//
    //In case this activity is paused we unregister the WIFI receiver
    protected void onPause(){
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the "Start" button is pressed and we start scanning for WIFI data
    //to store reference point. When the user first accesses this activity without having pressed on the map
    //to define a reference point, then Xcoordinate == 0 and Ycoordinate == 0) and thus we inform the
    //user to specify a reference point by touching the screen. In this method we also place a marker
    //on the location the user added the reference point
    public void start(View view){
        if (Xcoordinate == 0 && Ycoordinate == 0) {
            Toast.makeText(Indoor_Train.this, "Please specify the reference point by touching the screen", Toast.LENGTH_SHORT).show();
        } else {
            //The 2 next commands are for making sure that if the user
            //presses somewhere on the map during the time we capture data for a
            //reference point, the app will continue operating correctly
            Xcoordinate_to_store=Xcoordinate;
            Ycoordinate_to_store=Ycoordinate;
            counter_1 = 0; //We are ready to scan another reference points
            counter_2 = 0; //Holds the number of MAC addresses stored for each reference point (obtained during the first WIFI scan)

            //Place marker on the location the user added the reference point
            LatLng myLoc = new LatLng(Xcoordinate_to_store, Ycoordinate_to_store);
            mymarker = mMap.addMarker(new MarkerOptions().position(myLoc).title("I am a reference point"));
            cantakephoto = true; //used to prevent the user taking a photo for a reference point before the data for that
            // reference point have been added to the table

            reference_points++; //Holds the number of reference points added by the user
            //Here, if the database exists we update the number of reference point in the first row of the
            // REFERENCE_POINTS column in our the TABLE with what it was before plus 1. If the database does
            // not exist we just insert the number 1 to in the first row of the REFERENCE_POINTS column in our the TABLE
            if (doesDatabaseExist(this,"Fingerprint.db")){
                res_for_updating_references= myDb.getID();
                int indexID= res_for_updating_references.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                res_for_updating_references.moveToFirst(); //now we are in the very first row of the table which holds the number of reference points
                myDb.UpdateReferencePoints(res_for_updating_references.getString(indexID),reference_points);
            }
            else{
                myDb.insertref(reference_points);
            }
            //Also display with the toast the coordinate of the reference point (Latitude, Longitude) that is about#
            //to be stored in the database
            Toast.makeText(Indoor_Train.this, "Location: (" + Xcoordinate_to_store + "," + Ycoordinate_to_store + ")", Toast.LENGTH_SHORT).show();
            ready_to_receive_wifi=true; //becomes true whenever we are ready to collect data from wifi
            wifiManager.startScan();
        }
    }

//_________________________________________________________________________________________________//
    //Method which is executed when "Locate Me Indoors" button is pressed and we are transferred from the TRAINING phase
    // to the POSITIONING phase IF AND ONLY IF there is at least 1 reference point captured during
    //the training phase
    public void Indoor_Locate(View view){
        if (reference_points==0){
            Toast.makeText(Indoor_Train.this, "Please train me first", Toast.LENGTH_SHORT).show();
        }
        else {
            Intent indoor_positioning = new Intent(Indoor_Train.this, Indoor_Positioning.class);
            startActivity(indoor_positioning);
        }

    }

//_________________________________________________________________________________________________//
    //Method which is executed when the "Take Photo" button is pressed which opens the in-build camera
    // for the user to capture a photo at any reference point. If storage permission has not
    //been granted or no reference point has been defined we do not allow the user to capture a photo.
    //Also we do not allow the user to capture a photo in case the point that he/she tries to take a photo for, does
    //not belong to the reference point stored in the database
    public void takephoto(View view) {
        request_storage_permission();
        if (storage_permission==true) {
            if (reference_points == 0 || (Xcoordinate == 0 && Ycoordinate == 0) || (cantakephoto == false))
                Toast.makeText(this, "Please give/store a reference point first", Toast.LENGTH_SHORT).show();
            else {
                //Intent the EXISTING camera application and return control to the calling application
                Intent photo = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                photo.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(photo, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }
    }

//_________________________________________________________________________________________________//
    //Used to request storage permissions which allow the user to capture a photo at any reference point
    public void request_storage_permission(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        else {
            storage_permission=true; //user has already granted storage permissions
        }
    }

//_________________________________________________________________________________________________//
    //Here we handle the storage permission request and we inform the user with a Toast if he denies
    //permissions (for version of MarshMallow and higher)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults[0]==PackageManager.PERMISSION_DENIED){//Permission is denied
                    //Inform the user that storage permission is needed to take a picture at a reference point
                    Toast.makeText(getApplicationContext(),"Storage permission must be granted to take a picture at a reference point", Toast.LENGTH_LONG).show();
                    storage_permission=false;
                }
                else {
                    storage_permission=true;
                }
                break;
        }
    }

//_________________________________________________________________________________________________//
    //Method for saving the captured image (of any reference point) to the internal storage of the phone.
    //Also in this method we save the path of the saved image to the first row of the IMAGE_PATHS Column
    //of the reference point that the image was taken
    File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final File mediaStorageDir;
        //mediaStorageDir=new File("/storage/emulated/sdcard0/DCIM/Initial_Photos/");
        mediaStorageDir= new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Reference points");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdir()){
                Log.d("LocateMe","failed to create a directory"); //In case the directory does not exist then send a DEBUG log message
                return null;
            }
        }
        File medialFile;
        medialFile=new File(mediaStorageDir.getPath()+File.separator+timeStamp+".jpg");
        ImageFileLocation = medialFile.getAbsolutePath(); //ImageFileLocation holds the path of our saved image

        //Save this path as a string in the first row of the Column IMAGE_PATHS which corresponds to
        //the (XCOORDINATE,YCOORDINATE) reference point
        res_for_file_image_paths= myDb.getID_of_specific_coordinates(Xcoordinate, Ycoordinate);
        int indexID= res_for_file_image_paths.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
        res_for_file_image_paths.moveToFirst();
        myDb.UpdateImagePaths(res_for_file_image_paths.getString(indexID),ImageFileLocation);
        cantakephoto=false;
        return medialFile;
    }

//_________________________________________________________________________________________________//
    //Method which takes three variables and will be executed whenever an image was captured from the in-build phone camera.
    //If the image is successfully saved on the phone, we inform the user via a message. Similarly,
    //we inform the  user in the case the image capture was either cancelled by the user
    //or was not successful.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
            if (resultCode==RESULT_OK) {
                Toast.makeText(this,"Image successfully captured and saved",Toast.LENGTH_SHORT).show();
            }
            else if (resultCode==RESULT_CANCELED){
                //user cancelled the image captured
                Toast.makeText(this,"Image capture cancelled",Toast.LENGTH_SHORT).show();
            }
            else{
                //image capture failed, advice user
                Toast.makeText(this,"Image capture failed",Toast.LENGTH_SHORT).show();

            }
        }
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the OUTDOOR button is pressed and we go to the Outdoor Activity
    public void Outdoor_Locate(View view){
        Intent back=new Intent(Indoor_Train.this,Outdoor.class);
        startActivity(back);
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the "Delete Ref Point" button is pressed and deletes a reference point
    //specified by the user after clicking on the reference point (marker) that he/she wants to delete
    // (note that as already said the reference points are represented by markers on the map).
    //In case the user tries to delete a point which is not is not stored in the database (if for
    // instance the user clicks anywhere in the map and not in a marker/reference point) then we inform
    // the user with an appropriate message
    public void delete (View view){
        //We check if the database exists. If it does then we check the number of reference points
        // and if there are greater than 0 then we proceed to check if we can delete the requested reference point
        if (doesDatabaseExist(this,"Fingerprint.db")) {
            if (myDb.deletedata(String.valueOf(Xcoordinate), String.valueOf(Ycoordinate)) > 0) {
                Toast.makeText(getApplicationContext(), "Deleted successfully", Toast.LENGTH_LONG).show();
                //Now update the column which holds the number of reference points to what it was before -1
                res_for_updating_references= myDb.getID();
                int indexID= res_for_updating_references.getColumnIndex(FeedReaderDbHelper.FeedEntry._ID);
                res_for_updating_references.moveToFirst();
                reference_points--; //make number of reference points to be 1 less and then update the table
                // with the new value of reference points
                myDb.UpdateReferencePoints(res_for_updating_references.getString(indexID),reference_points);
            }else { //It means that the user tried to delete a point on the map which does not belong to the reference points
                Toast.makeText(getApplicationContext(), "This point is not in the database", Toast.LENGTH_LONG).show();
            }
        }
    }

//_________________________________________________________________________________________________//
    //Method which is being executed whenever we click the back arrow of the phone and we return to the Initial_Screen Activity
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            Intent back=new Intent(Indoor_Train.this,Initial_Screen.class);
            startActivity(back);
        }
        return super.onKeyUp( keyCode, event );
    }

}
