package com.tinymicros.aprsdroid;

import java.util.HashMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

class APRSDatabase extends SQLiteOpenHelper implements BaseColumns
{
  private static final String TAG = "APRSDatabase";
  private static final String DATABASE_NAME = "aprs.db";
  private static final int DATABASE_VERSION = 4;
  
  //
  //  Uri stuff
  //
  public static final Uri CONTENT_URI = Uri.parse ("content://com.tinymicros.aprsdroid/object");
  public static final Uri CONTENT_FILTER_URI = Uri.parse ("content://com.tinymicros.aprsdroid/object/filter");
  public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tinymicros.aprsdroid.object";
  public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tinymicros.aprsdroid.object";
  
  //
  // Table names and sort order
  //
  public static final String T_OBJECTS = "objects";
  public static final String I_OBJECTS_CALLSIGN = "indexCallsign";
  public static final String I_OBJECTS_TIME = "indexTime";
  public static final String S_OBJECTS_CALLSIGN_ASC = "callsign ASC";
  public static final String S_OBJECTS_TIME_ASC = "time ASC";
  public static final String S_OBJECTS_TIME_DESC = "time DESC";
  
  //
  // Database columns
  //
  public static final String C_FIELDS_PRESENT = "fieldsPresent";
  public static final String C_ID = "ID";
  public static final String C_CALLSIGN = "callsign";
  public static final String C_COMMENT = "comment";
  public static final String C_SYMBOL_CODE = "symbolCode";
  public static final String C_SYMBOL_TABLE = "symbolTable";
  public static final String C_TYPE = "type";
  public static final String C_LATITUDE = "latitude";
  public static final String C_LONGITUDE = "longitude";
  public static final String C_SPEED = "speed";
  public static final String C_COURSE = "course";
  public static final String C_ALTITUDE = "altitude";
  public static final String C_TEMPERATURE = "temperature";
  public static final String C_WIND_DIRECTION = "windDirection";
  public static final String C_WIND_SPEED = "windSpeed";
  public static final String C_WIND_GUST = "windGust";
  public static final String C_WIND_SUSTAINED = "windSustained";
  public static final String C_BAROMETER = "barometer";
  public static final String C_HUMIDITY = "humidity";
  public static final String C_RAIN_LAST_HOUR = "rainLastHour";
  public static final String C_RAIN_LAST_DAY = "rainLastDay";
  public static final String C_RAIN_LAST_24 = "rainLast24";
  public static final String C_DISTANCE_FROM_ME = "distanceFromMeInFeet";
  public static final String C_HEADING_FROM_ME = "headingFromMe";
  public static final String C_TIME = "time";
  public static final String C_IN_MOTION = "inMotion";
  public static final String C_HAS_WEATHER = "hasWeather";
  
  public static HashMap<String, String> projectionMap;
  
  public static String[] columnsListTime =
  {
    C_TIME
  };
  
  public static String[] columnsListLatLon =
  {
    C_CALLSIGN,
    C_LATITUDE,
    C_LONGITUDE,
  };
  
  public static String[] columnsListOverlayItem =
  {
    C_CALLSIGN,
    C_LATITUDE,
    C_LONGITUDE,
    C_HAS_WEATHER,
    C_IN_MOTION,
    C_HAS_WEATHER,
    C_SYMBOL_TABLE,
    C_SYMBOL_CODE,
  };
  
  public static String[] columnsListAll = 
  {
    C_FIELDS_PRESENT,
    C_CALLSIGN, 
    C_COMMENT, 
    C_SYMBOL_CODE, 
    C_SYMBOL_TABLE,
    C_TYPE,
    C_LATITUDE,
    C_LONGITUDE,
    C_SPEED,
    C_COURSE,
    C_ALTITUDE,
    C_TEMPERATURE,
    C_WIND_DIRECTION,
    C_WIND_SPEED,
    C_WIND_GUST,
    C_WIND_SUSTAINED,
    C_BAROMETER,
    C_HUMIDITY,
    C_RAIN_LAST_HOUR,
    C_RAIN_LAST_DAY,
    C_RAIN_LAST_24,
    C_DISTANCE_FROM_ME,
    C_HEADING_FROM_ME,
    C_TIME,
    C_IN_MOTION,
    C_HAS_WEATHER
  };

  APRSDatabase (Context context)
  {
    super (context, DATABASE_NAME, null, DATABASE_VERSION);
  }
  
  @Override
  public void onCreate (SQLiteDatabase db)
  {
    if (APRSDebug.databaseInfo)
      Log.d (TAG, "Creating database");
    
    db.execSQL ("CREATE TABLE " + T_OBJECTS + " (" + 
                _ID + " INTEGER PRIMARY KEY," + 
                C_FIELDS_PRESENT + " INTEGER," +
                C_CALLSIGN + " TEXT," + 
                C_COMMENT + " TEXT," + 
                C_SYMBOL_CODE + " TEXT," + 
                C_SYMBOL_TABLE + " TEXT," +
                C_TYPE + " TEXT," +
                C_LATITUDE + " INTEGER," +
                C_LONGITUDE + " INTEGER, " +
                C_SPEED + " INTEGER," +
                C_COURSE + " INTEGER," +
                C_ALTITUDE + " INTEGER," +
                C_TEMPERATURE + " INTEGER," +
                C_WIND_DIRECTION + " INTEGER," +
                C_WIND_SPEED + " INTEGER," +
                C_WIND_GUST + " INTEGER," +
                C_WIND_SUSTAINED + " INTEGER," +
                C_BAROMETER + " INTEGER," +
                C_HUMIDITY + " INTEGER," +
                C_RAIN_LAST_HOUR + " INTEGER," +
                C_RAIN_LAST_DAY + " INTEGER," +
                C_RAIN_LAST_24 + " INTEGER," +
                C_DISTANCE_FROM_ME + " INTEGER," +
                C_HEADING_FROM_ME + " INTEGER," +
                C_TIME + " INTEGER," +
                C_IN_MOTION + " INTEGER," +
                C_HAS_WEATHER + " INTEGER" +
                ");"); 
    
    db.execSQL ("CREATE INDEX " + I_OBJECTS_CALLSIGN + " ON " + T_OBJECTS + " (" + C_CALLSIGN + ");");
    db.execSQL ("CREATE INDEX " + I_OBJECTS_TIME + " ON " + T_OBJECTS + " (" + C_TIME + ");");
  }

  @Override
  public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion)
  {
    Log.w (TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
    db.execSQL ("DROP TABLE IF EXISTS " + T_OBJECTS);
    onCreate (db);
  }
  
  static
  {
    projectionMap = new HashMap<String, String> ();
    
    projectionMap.put (_ID, _ID);
    projectionMap.put (C_FIELDS_PRESENT, C_FIELDS_PRESENT);
    projectionMap.put (C_CALLSIGN, C_CALLSIGN);
    projectionMap.put (C_COMMENT, C_COMMENT);
    projectionMap.put (C_SYMBOL_CODE, C_SYMBOL_CODE);
    projectionMap.put (C_SYMBOL_TABLE, C_SYMBOL_TABLE );
    projectionMap.put (C_TYPE, C_TYPE); 
    projectionMap.put (C_LATITUDE, C_LATITUDE); 
    projectionMap.put (C_LONGITUDE, C_LONGITUDE );
    projectionMap.put (C_SPEED, C_SPEED); 
    projectionMap.put (C_COURSE, C_COURSE );
    projectionMap.put (C_ALTITUDE, C_ALTITUDE); 
    projectionMap.put (C_TEMPERATURE, C_TEMPERATURE); 
    projectionMap.put (C_WIND_DIRECTION, C_WIND_DIRECTION); 
    projectionMap.put (C_WIND_SPEED, C_WIND_SPEED); 
    projectionMap.put (C_WIND_GUST, C_WIND_GUST); 
    projectionMap.put (C_WIND_SUSTAINED, C_WIND_SUSTAINED);
    projectionMap.put (C_BAROMETER, C_BAROMETER); 
    projectionMap.put (C_HUMIDITY, C_HUMIDITY); 
    projectionMap.put (C_RAIN_LAST_HOUR, C_RAIN_LAST_HOUR); 
    projectionMap.put (C_RAIN_LAST_DAY, C_RAIN_LAST_DAY); 
    projectionMap.put (C_RAIN_LAST_24, C_RAIN_LAST_24); 
    projectionMap.put (C_DISTANCE_FROM_ME, C_DISTANCE_FROM_ME); 
    projectionMap.put (C_HEADING_FROM_ME, C_HEADING_FROM_ME); 
    projectionMap.put (C_TIME, C_TIME);
    projectionMap.put (C_IN_MOTION, C_IN_MOTION);
    projectionMap.put (C_HAS_WEATHER, C_HAS_WEATHER);
  }
}
