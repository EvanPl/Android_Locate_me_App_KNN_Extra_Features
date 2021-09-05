package com.example.vaggelis.assignment_2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Evan on 31/03/2018.
 */

//Class for "Define a Schema and Contract" and "Create a Database Using a SQL Helper"
//In this class we define our Reference_Point_Data and also all the methods necessary
//to insert data in tha table, delete data in the table and update data in the table
//PLEASE see Table 1 of the Programmer's Guide to visualise the Table
public class FeedReaderDbHelper extends SQLiteOpenHelper {
        //PLEASE SEE TABLE 1 of the Programmer's Guide to visualise the "Reference_Point_Data" Table
        public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "Reference_Point_Data"; //Table name
        public static final String COORDINATEX = "CoordinateX"; //X Coordinate of reference point
        public static final String COORDINATEY = "CoordinateY"; //Y Coordinate of reference point
        public static final String SSID = "SSID"; //Column which stores the different SSIDs of the WIFI
        public static final String BSSID = "BSSID"; //Column which stores the different BSSIDs (MAC addresses) of the Wifi
        public static final String RSSI_1 = "RSSI_1"; //Column which stores RSSI1
        public static final String RSSI_2 = "RSSI_2"; //Column which stores RSSI2
        public static final String RSSI_avg = "RSSI_avg"; //Column which stores the average of
                                                        // the 2 different RSSIs captured (RSSI1 and RSSI2)

        public static final String MAC_num = "MAC_num"; //Column which stores the number of MAC addresses stored for each reference point
        public static final String REFERENCE_POINTS = "REFERENCE_POINTS"; //Column which stores the number of reference points that the user
                                                                          //has captured during the training phase

        public static final String IMAGE_PATHS = "IMAGE_PATHS"; //Column which stores the path of a captured image at a reference
                                                                // point (if an image has been captured because the user might not capture one)
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER DEFAULT 0";

    private static final String REAL_TYPE = " REAL DEFAULT 0";
    private static final String COMMA_SEP = ",";
    //CREATE COLUMNS of TABLE
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    FeedEntry.COORDINATEX + REAL_TYPE + COMMA_SEP +
                    FeedEntry.COORDINATEY + REAL_TYPE +
                    COMMA_SEP +
                    FeedEntry.SSID + TEXT_TYPE +
                    COMMA_SEP +
                    FeedEntry.BSSID + TEXT_TYPE +
                    COMMA_SEP +
                    FeedEntry.RSSI_1 + INTEGER_TYPE +
                    COMMA_SEP +
                    FeedEntry.RSSI_2 + INTEGER_TYPE +
                    COMMA_SEP +
                    FeedEntry.RSSI_avg + REAL_TYPE +
                    COMMA_SEP +
                    FeedEntry.MAC_num + INTEGER_TYPE +
                    COMMA_SEP +
                    FeedEntry.REFERENCE_POINTS + INTEGER_TYPE +
                    COMMA_SEP +
                    FeedEntry.IMAGE_PATHS + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;

    public static final String DATABASE_NAME ="Fingerprint.db"; //Database name
    public static final int DATABASE_VERSION=1; //Database version of 1

    //Whenever this constructor is called, the database will be created
    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }


/*__________________________________________________________________________________________________________*/
    //METHOD FOR INSERTING NEW DATA (ROWS) in the TABLE
    public boolean insertData(double  CoordinateX, double CoordinateY, String SSID, String BSSID, int RSSI_1){
        //Make data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COORDINATEX, CoordinateX);
        values.put(FeedEntry.COORDINATEY, CoordinateY);
        values.put(FeedEntry.SSID, SSID);
        values.put(FeedEntry.BSSID, BSSID);
        values.put(FeedEntry.RSSI_1, RSSI_1);
        // Insert the new row
        long result=db.insert(FeedEntry.TABLE_NAME,
                null,
                values);
        //If data is not inserted then insert method will return -1
        if (result==-1) return false;
        else return true;
    }


    /*__________________________________________________________________________________________________________*/
    //METHOD FOR inserting the numbers of reference points (initially, because later on we just update the table)
    public boolean insertref(int reference_points){
        //Make data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedEntry.REFERENCE_POINTS,reference_points);
        // Insert the new row
        long result=db.insert(FeedEntry.TABLE_NAME,
                null,//FeedEntry.COLUMN_NAME_NULLABLE,
                values);
        //If data is nor inserted then insert method will return -1
        if (result==-1) return false;
        else return true;
    }


    /*__________________________________________________________________________________________________________*/
    //read ALL DATA FROM THE TABLE
    public Cursor getAlldata() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res=db.rawQuery("select * from "+FeedEntry.TABLE_NAME,null);
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get MAC_num and _ID Columns
    public Cursor getMacNumandID(){
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {
                FeedEntry._ID,
                FeedEntry.MAC_num,
        };

        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get REFERENCE_POINTS Column
    public Cursor getReferencepoints(){
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {
                FeedEntry.REFERENCE_POINTS,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get CoordinateX, CoordinateY, and MAC_num Columns
    public Cursor getCoordinateY_X_and_Mac_num(){
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry.COORDINATEX,
                FeedEntry.COORDINATEY,
                FeedEntry.MAC_num,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get IMAGE_PATHS Column for a given reference point (with given CoordinateX, CoordinateY coordinates)
    public Cursor getIMAGE_PATHS (double CoordinateX, double CoordinateY) {
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry.IMAGE_PATHS,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                FeedEntry.COORDINATEX+"= '"+CoordinateX+"'" + " AND " + FeedEntry.COORDINATEY+"= '"+CoordinateY+"'", // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get CoordinateX, CoordinateY, BSSID, RRSI_avg and MAC_num Columns
    public Cursor getXY_and_BSSID_andRSSI_avg_and_MAC_num(){
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry.COORDINATEX,
                FeedEntry.COORDINATEY,
                FeedEntry.BSSID,
                FeedEntry.RSSI_avg,
                FeedEntry.MAC_num,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get _ID, RSSI_2 and BSSID Columns for a given location coordinates (CoordinateX, CoordinateY)
    public Cursor getIDandRSSI2andBSSID(double CoordinateX, double CoordinateY) {
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry._ID,
                FeedEntry.BSSID,
                FeedEntry.RSSI_2,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                FeedEntry.COORDINATEX+"= '"+CoordinateX+"'" + " AND " + FeedEntry.COORDINATEY+"= '"+CoordinateY+"'", // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get _ID, RSSI_1 and RSSI_2 Columns for a given location coordinates (CoordinateX, CoordinateY)
    public Cursor getAllRSSIsandIDs (double CoordinateX, double CoordinateY) {
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry.RSSI_1,
                FeedEntry.RSSI_2,
                FeedEntry._ID,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                FeedEntry.COORDINATEX+"= '"+CoordinateX+"'" + " AND " + FeedEntry.COORDINATEY+"= '"+CoordinateY+"'", // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get _ID columns for a given location (CoordinateX, CoordinateY)
    public Cursor getID_of_specific_coordinates (double CoordinateX, double CoordinateY) {
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry._ID,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                FeedEntry.COORDINATEX+"= '"+CoordinateX+"'" + " AND " + FeedEntry.COORDINATEY+"= '"+CoordinateY+"'", // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Get _ID Column
    public Cursor getID () {
        SQLiteDatabase db = this.getReadableDatabase();
        //DEFINE ALL COLUMNS HERE
        String[] projection = {
                FeedEntry._ID,
        };
        Cursor res = db.query(
                FeedEntry.TABLE_NAME, // The table to query
                projection, // The columns to return
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null,// don't filter by row groups
                null // The sort order
        );
        return res;
    }


    /*__________________________________________________________________________________________________________*/
    //Method for updating the MAC_num Column of a given _ID
    public void updateMAC_num(String ID, int MAC_num){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.MAC_num,MAC_num);
        String selection = FeedEntry._ID+" LIKE ?"; //WE ADD THE ID that is why we have the Questionmark

        db.update(FeedEntry.TABLE_NAME,values,selection,new String[]{ID});

    }


    /*__________________________________________________________________________________________________________*/
    //Method for updating the REFERENCE_POINTS Column of a given _ID
    public void UpdateReferencePoints(String ID, int REFERENCE_POINTS){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.REFERENCE_POINTS,REFERENCE_POINTS);
        String selection = FeedEntry._ID+" LIKE ?"; //WE ADD THE ID that is why we have the questionmark

        db.update(FeedEntry.TABLE_NAME,values,selection,new String[]{ID});

    }


    /*__________________________________________________________________________________________________________*/
    //Method for updating the IMAGE_PATHS Column of a given _ID
    public void UpdateImagePaths(String ID, String IMAGE_PATHS){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.IMAGE_PATHS,IMAGE_PATHS);
        String selection = FeedEntry._ID+" LIKE ?"; //WE ADD THE ID that is why we have the questionmark

        db.update(FeedEntry.TABLE_NAME,values,selection,new String[]{ID});

    }


    /*__________________________________________________________________________________________________________*/
    //Method for updating the RSSI_2 Column of a given _ID
    public void updateRSSI2(String ID, int RSSI2){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.RSSI_2,RSSI2);
        String selection = FeedEntry._ID+" LIKE ?"; //WE ADD THE ID that is why we have the questionmark

        db.update(FeedEntry.TABLE_NAME,values,selection,new String[]{ID});

    }


    /*__________________________________________________________________________________________________________*/
    //Method for updating the RSSI_avg Column of a given _ID
    public void updateRSSI_avg(String ID, float RSSI_avg){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.RSSI_avg,RSSI_avg);
        String selection = FeedEntry._ID+" LIKE ?"; //WE ADD THE ID that is why we have the questionmark
        db.update(FeedEntry.TABLE_NAME,values,selection,new String[]{ID});

    }


    /*__________________________________________________________________________________________________________*/
    //Method for updating the MAC_num Column of a given _ID
    public void updateMAC_num_of_faulty(String ID, int MAC_num){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.MAC_num,MAC_num);
        String selection = FeedEntry._ID+" LIKE ?"; //WE ADD THE ID that is why we have the questionmark

        db.update(FeedEntry.TABLE_NAME,values,selection,new String[]{ID});

    }


    /*__________________________________________________________________________________________________________*/
    //Method for deleting a refernce point of given coordinates (CoordinateX, CoordinateY)
    public Integer deletedata(String CoordinateX, String CoordinateY){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(
                FeedEntry.TABLE_NAME, // The table to query
                FeedEntry.COORDINATEX + "=? AND " + FeedEntry.COORDINATEY + "=?", // The columns for the WHERE clause
                new String[]{CoordinateX, CoordinateY}); // The values for the WHERE clause
    }
    /*__________________________________________________________________________________________________________*/
}
