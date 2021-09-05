package com.example.vaggelis.assignment_2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Evan on 31/03/2018.
 */

/* This is the activity that the user sees when the app is initially run and allows to access all the
3 other activities of the app (Indoor_Train, Indoor_Positioning and Outdoor) by pressing the appropriate button.
This activity initially asks the user for location permissions (for android version of Marshmallow and higher) as in order
to locate the user either indoor or outdoors location permissions must be granted. Later on, the activity
loads the number of reference points stored in the database, if a database exists. Then the user can
press "Locate Me Outdoors" Button to go to the Outdoor Activity, "Train Me indoors" to go to the Indoor_Train Activity
and "Locate Me Indoors" button to go to the Indoor_Positioning activity. NOTE that in case the reference points are zero
either because the database does not exist or simply because the user has deleted all the reference points (as in this app
the user can delete any of the reference points at any time as discussed in the Indoor_Train Activity) then, whenever
the user presses the "Locate Me Indoors" button, a Toast appears informing the user that the app need be trained first for indoor
localisation. */

public class Initial_Screen extends AppCompatActivity {

    FeedReaderDbHelper myDb; //Used to instantiate the SQLiteOpenHelper
    int indexref; //Holds the index of the REFERENCE_POINTS column inside the database
    Cursor num_of_reference_points; //Used to access the REFERENCE_POINTS column inside the database
    int reference_points=0; //Holds the number of reference points
    boolean location_permission=false;
    public static final int ACCESS_FINE_LOCATION_REQUEST_CODE=2; //Request code which is used to deal
                                                                // with location permissions of the user

    //______________________________________________________________________________________________________//

    //Method which is executed when the activity is initially run (or else said when the activity is created)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial__screen);


        //Initialise FeedReaderDbHelper Class
        myDb = new FeedReaderDbHelper(this);

        //In order to obtain wifi results and thus to able to locate the user inside we need to have location permissions.
        //Also we ask the user for storage permissions to allow him to capture a photo at a reference point
        request_location_permissions();

        //We will get the number of reference points from the database file (if the database file exists)
        //We do that to NOT allow the use to go to indoor positioning activity without
        //having at least one reference point in the database file
        get_reference_points();

    }

//_________________________________________________________________________________________________//
    //Method which requests location permission from the user (for Android versions of Marshmallow and higher)
    public void request_location_permissions(){
        //Request from the user to allow access to location which is need to scan WIFI
        // (for devices operating on versions of Marshmallow and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Request the user to allow access to location
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_REQUEST_CODE);
            }
            else {location_permission=true;}
        }
    }

//_________________________________________________________________________________________________//
    //Here we handle the location permission request and we inform the user that for indoor training,
    //and outdoor as well as indoor positioning location permission must be granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case ACCESS_FINE_LOCATION_REQUEST_CODE:
                if (grantResults[0]==PackageManager.PERMISSION_DENIED){//Permission is denied
                    //Inform the user that we need to have the permission and we return to the main activity
                    Toast.makeText(getApplicationContext(),"Location permission must be granted to start outdoor/indoor localisation and training", Toast.LENGTH_LONG).show();
                    location_permission=false;
                }
                else location_permission=true;
                break;
        }
    }

//_________________________________________________________________________________________________//
    //Method which is used to obtain the number of reference points from the database (Fingerprint.db inside
    //FeedReaderDbHelper.java)
    // (and in case no database exists the reference points by default are 0).
    // We do that BECAUSE IN CASE no database exists OR the reference points are zero
    //(as the user can delete at any time any reference point) then WE DO NOT allow the user to access
    //Indoor_Positioning Activity and we ask him/her to train the application first (by going to Indoor_Train activity).
    public void get_reference_points(){
        //Check if the database exists and if does load the number of reference points
        if (doesDatabaseExist(this,"Fingerprint.db")) {
                //if it exists read the reference points which are stored in the database (if there are any, because the user might have deleted them)
                num_of_reference_points = myDb.getReferencepoints();
                indexref = num_of_reference_points.getColumnIndex(FeedReaderDbHelper.FeedEntry.REFERENCE_POINTS);
                num_of_reference_points.moveToFirst();
                reference_points = num_of_reference_points.getInt(indexref);
        }
    }

//_________________________________________________________________________________________________//
    //Method which checks if the database file of the application exists or not and returns true
    // if it does exist and false if it does not exist
    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the "Locate Me Outdoors" button is pressed and by using an intent we go to
    //the Outdoor Activity IF AND ONLY IF location permissions have been granted. IF location permissions have NOT
    //been granted we tell the user by using a Toast that he/she needs to grant permissions fist for Outdoor Localisation
    public void outdoorlocate(View view){
        if (location_permission==true) {
            Intent outdoorlocate = new Intent(Initial_Screen.this, Outdoor.class);
            startActivity(outdoorlocate);
        }
        else {
            Toast.makeText(Initial_Screen.this, "You need to grant location permissions first", Toast.LENGTH_SHORT).show();
        }
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the "Train Me Indoors" button is pressed and by using an intent we go to
    //the Indoor_Train Activity IF AND ONLY IF location permissions have been granted. IF location permissions have NOT
    //been granted we tell the user by using a Toast that he/she needs to grant permissions first for Indoor Training
    public void indoortrain(View view){
        if (location_permission==true) {
            Intent indoortrain = new Intent(Initial_Screen.this, Indoor_Train.class);
            startActivity(indoortrain);
        }
        else {
            Toast.makeText(Initial_Screen.this, "You need to grant location permissions first", Toast.LENGTH_SHORT).show();
        }
    }

//_________________________________________________________________________________________________//
    //Method which is executed when the "Locate Me Indoors" button is pressed and by using an intent we go to
    //the Indoor_Train Activity IF AND ONLY IF location permissions have been granted and ALSO if there is at least ONE reference
    //point stored in the database. IF location permissions have NOT been granted we tell the user by using a Toast that he/she needs
    //to grant permissions first for Indoor Training. MOREOVER, if reference points==0 then by using a Toast we inform the user
    //that he/she needs to train the app first (as already explained)
    public void indoorlocate(View view){
        //We will let the user go to Indoor_Positioning Activity if and only if there is at least one reference point stored in the database
        if (reference_points>0) {
            if (location_permission==true) {
                Intent indoorlocate = new Intent(Initial_Screen.this, Indoor_Positioning.class);
                startActivity(indoorlocate);
            }
            else {
                Toast.makeText(Initial_Screen.this, "You need to grant location permissions first", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(Initial_Screen.this, "Please give some reference points first", Toast.LENGTH_SHORT).show();
        }
    }

//_________________________________________________________________________________________________//
    //Method executed when the app is RESUMING (after PAUSING IT). Every time we resume we check if location
    //permissions have been granted. We do this because in case the user denies location permissions,
    //he can simply pause the app (by pressing the middle button of the phone for example), go to the phone settings,
    //grant location permission for this app and then when he returns to this app (which is still opened but paused),
    //the app will recognise that location permissions have been granted.
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            location_permission = true;
        }
    }
}
