//
//  $Id: APRS.java 10 2008-11-30 23:12:05Z jcw $
//  $Revision: 10 $
//  $Date: 2008-11-30 18:12:05 -0500 (Sun, 30 Nov 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprs/APRS.java $
//

package com.tinymicros.aprsdroid;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class APRSObject implements Comparable<APRSObject>
{
  private static final String TAG = "APRSObject";

  private int fieldsPresent;
  private String ID;
  private String callsign;
  private String comment;
  private byte symbolCode;
  private byte symbolTable;
  private byte type;
  private int latitude;
  private int longitude;
  private int speedInMPH;
  private int course;
  private int altitudeInFeet;
  private int temperatureInF;
  private int windDirection;
  private int windSpeed;
  private int windGust;
  private int windSustained;
  private int barometer;
  private int humidity;
  private int rainLastHourInHundredths;
  private int rainLast24InHundredths;
  private int rainLastDayInHundredths;
  private int distanceFromMeInFeet;
  private int headingFromMe;
  private long time;
  private boolean inMotion;
  private boolean hasMoved;
  private boolean hasWeather;

  private final static int hasNoFields = 0x00000000;
  private final static int hasID = 0x00000001;
  private final static int hasCallsign = 0x00000002;
  private final static int hasComment = 0x00000004;
  private final static int hasSymbolCode = 0x00000008;
  private final static int hasSymbolTable = 0x00000010;
  private final static int hasType = 0x00000020;
  private final static int hasLatitude = 0x00000040;
  private final static int hasLongitude = 0x00000080;
  private final static int hasSpeed = 0x00000100;
  private final static int hasCourse = 0x00000200;
  private final static int hasAltitude = 0x00000400;
  private final static int hasTemperature = 0x00000800;
  private final static int hasWindDirection = 0x00001000;
  private final static int hasWindSpeed = 0x00002000;
  private final static int hasWindGust = 0x00004000;
  private final static int hasWindSustained = 0x00008000;
  private final static int hasBarometer = 0x00010000;
  private final static int hasHumidity = 0x00020000;
  private final static int hasRainLastHour = 0x00040000;
  private final static int hasRainLastDay = 0x00080000;
  private final static int hasRainLast24 = 0x00100000;

  APRSObject ()
  {
    fieldsPresent = hasNoFields;
    ID = null;
    callsign = null;
    comment = null;
    symbolCode = ' ';
    symbolTable = ' ';
    type = ' ';
    latitude = Integer.MIN_VALUE;
    longitude = Integer.MIN_VALUE;
    speedInMPH = Integer.MIN_VALUE;
    course = Integer.MIN_VALUE;
    altitudeInFeet = Integer.MIN_VALUE;
    temperatureInF = Integer.MIN_VALUE;
    windDirection = Integer.MIN_VALUE;
    windSpeed = Integer.MIN_VALUE;
    windGust = Integer.MIN_VALUE;
    windSustained = Integer.MIN_VALUE;
    barometer = Integer.MIN_VALUE;
    humidity = Integer.MIN_VALUE;
    rainLastHourInHundredths = Integer.MIN_VALUE;
    rainLastDayInHundredths = Integer.MIN_VALUE;
    rainLast24InHundredths = Integer.MIN_VALUE;
    distanceFromMeInFeet = Integer.MIN_VALUE;
    headingFromMe = Integer.MIN_VALUE;
    time = Long.MIN_VALUE;
    inMotion = false;
    hasMoved = false;
    hasWeather = false;
  }

  APRSObject (ContentValues values)
  {
    super ();
    
    if (values.containsKey (APRSDatabase.C_FIELDS_PRESENT))
      fieldsPresent = values.getAsInteger (APRSDatabase.C_FIELDS_PRESENT);
    if (values.containsKey (APRSDatabase.C_CALLSIGN))
      callsign = values.getAsString (APRSDatabase.C_CALLSIGN);
    if (values.containsKey (APRSDatabase.C_COMMENT))
      comment = values.getAsString (APRSDatabase.C_COMMENT);
    if (values.containsKey (APRSDatabase.C_SYMBOL_CODE))
      symbolCode = values.getAsByte(APRSDatabase.C_SYMBOL_CODE);
    if (values.containsKey (APRSDatabase.C_SYMBOL_TABLE))
      symbolTable = values.getAsByte(APRSDatabase.C_SYMBOL_TABLE);
    if (values.containsKey (APRSDatabase.C_TYPE))
      type = values.getAsByte(APRSDatabase.C_TYPE);
    if (values.containsKey (APRSDatabase.C_LATITUDE))
      latitude = values.getAsInteger (APRSDatabase.C_LATITUDE);
    if (values.containsKey (APRSDatabase.C_LONGITUDE))
      longitude = values.getAsInteger (APRSDatabase.C_LONGITUDE);
    if (values.containsKey (APRSDatabase.C_SPEED))
      speedInMPH = values.getAsInteger (APRSDatabase.C_SPEED);
    if (values.containsKey (APRSDatabase.C_COURSE))
      course = values.getAsInteger (APRSDatabase.C_COURSE);
    if (values.containsKey (APRSDatabase.C_ALTITUDE))
      altitudeInFeet = values.getAsInteger (APRSDatabase.C_ALTITUDE);
    if (values.containsKey (APRSDatabase.C_TEMPERATURE))
      temperatureInF = values.getAsInteger (APRSDatabase.C_TEMPERATURE);
    if (values.containsKey (APRSDatabase.C_WIND_DIRECTION))
      windDirection = values.getAsInteger (APRSDatabase.C_WIND_DIRECTION);
    if (values.containsKey (APRSDatabase.C_WIND_SPEED))
      windSpeed = values.getAsInteger (APRSDatabase.C_WIND_SPEED);
    if (values.containsKey (APRSDatabase.C_WIND_GUST))
      windGust = values.getAsInteger (APRSDatabase.C_WIND_GUST);
    if (values.containsKey (APRSDatabase.C_WIND_SUSTAINED))
      windSustained = values.getAsInteger (APRSDatabase.C_WIND_SUSTAINED);
    if (values.containsKey (APRSDatabase.C_BAROMETER))
      barometer = values.getAsInteger (APRSDatabase.C_BAROMETER);
    if (values.containsKey (APRSDatabase.C_HUMIDITY))
      humidity = values.getAsInteger (APRSDatabase.C_HUMIDITY);
    if (values.containsKey (APRSDatabase.C_RAIN_LAST_HOUR))
      rainLastHourInHundredths = values.getAsInteger (APRSDatabase.C_RAIN_LAST_HOUR);
    if (values.containsKey (APRSDatabase.C_RAIN_LAST_DAY))
      rainLastDayInHundredths = values.getAsInteger (APRSDatabase.C_RAIN_LAST_DAY);
    if (values.containsKey (APRSDatabase.C_RAIN_LAST_24))
      rainLast24InHundredths = values.getAsInteger (APRSDatabase.C_RAIN_LAST_24);
    if (values.containsKey (APRSDatabase.C_DISTANCE_FROM_ME))
      distanceFromMeInFeet = values.getAsInteger (APRSDatabase.C_DISTANCE_FROM_ME);
    if (values.containsKey (APRSDatabase.C_HEADING_FROM_ME))
      headingFromMe = values.getAsInteger (APRSDatabase.C_HEADING_FROM_ME);
    if (values.containsKey (APRSDatabase.C_TIME))
      time = values.getAsInteger (APRSDatabase.C_TIME);
    if (values.containsKey (APRSDatabase.C_IN_MOTION))
      inMotion = (values.getAsInteger (APRSDatabase.C_IN_MOTION) > 0) ? true : false;
    if (values.containsKey (APRSDatabase.C_HAS_WEATHER))
      hasWeather = (values.getAsInteger (APRSDatabase.C_HAS_WEATHER) > 0) ? true : false;
  }
  
  //
  //  Very inefficient.  Shouldn't need to rely on converting column names. 
  //  We should already know the position of the column within the table.
  //  TODO
  //
  APRSObject (Cursor cursor)
  {
    super ();

    fieldsPresent = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_FIELDS_PRESENT)); 
    callsign = cursor.getString (cursor.getColumnIndex (APRSDatabase.C_CALLSIGN)); 
    comment = cursor.getString (cursor.getColumnIndex (APRSDatabase.C_COMMENT)); 
    symbolCode = Byte.valueOf (cursor.getString (cursor.getColumnIndex (APRSDatabase.C_SYMBOL_CODE)));
    symbolTable = Byte.valueOf (cursor.getString (cursor.getColumnIndex (APRSDatabase.C_SYMBOL_TABLE)));
    type = Byte.valueOf (cursor.getString (cursor.getColumnIndex (APRSDatabase.C_TYPE)));
    latitude = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_LATITUDE)); 
    longitude = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_LONGITUDE)); 
    speedInMPH = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_SPEED)); 
    course = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_COURSE)); 
    altitudeInFeet = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_ALTITUDE)); 
    temperatureInF = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_TEMPERATURE)); 
    windDirection = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_WIND_DIRECTION)); 
    windSpeed = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_WIND_SPEED)); 
    windGust = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_WIND_GUST)); 
    windSustained = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_WIND_SUSTAINED)); 
    barometer = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_BAROMETER)); 
    humidity = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_HUMIDITY)); 
    rainLastHourInHundredths = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_RAIN_LAST_HOUR)); 
    rainLast24InHundredths = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_RAIN_LAST_24)); 
    rainLastDayInHundredths = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_RAIN_LAST_DAY)); 
    distanceFromMeInFeet = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_DISTANCE_FROM_ME)); 
    headingFromMe = cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_HEADING_FROM_ME)); 
    time = cursor.getLong (cursor.getColumnIndex (APRSDatabase.C_TIME));
    inMotion = (cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_IN_MOTION)) > 0) ? true : false;
    hasWeather = (cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_HAS_WEATHER)) > 0) ? true : false;
 	}
  
  private String lastHeard ()
  {
    long age = (System.currentTimeMillis () - time) / 1000;

    if (age < 60)
      return String.format ("%d seconds", age);
    
    return String.format ("%d minute%s", age / 60, ((age / 60) > 1) ? "s" : "");
  }

  public void print (String tag, String header)
  {
    final DateFormat formatter = new SimpleDateFormat ("yyyy/MM/dd hh:mm:ss.SSS");
    final Calendar calendar = Calendar.getInstance ();
    
    calendar.setTimeInMillis (time);
    
    if (tag == null)
      tag = TAG;
    
    Log.d (tag, header);
    Log.d (tag, String.format ("  fieldsPresent=%x", fieldsPresent));
    Log.d (tag, "  callsign=" + callsign);
    Log.d (tag, "  comment=" + comment);
    Log.d (tag, "  symbolCode=" + symbolCode + ", symbolTable=" + symbolTable + ", type=" + type);
    Log.d (tag, "  latitude=" + latitude + ", longitude=" + longitude);
    Log.d (tag, "  speedInMPH=" + speedInMPH + ", course=" + course);
    Log.d (tag, "  altitudeInFeet=" + altitudeInFeet + ", temperatureInF=" + temperatureInF);
    Log.d (tag, "  windDirection=" + windDirection + ", windSpeed=" + windSpeed + ", windGust=" + windGust + ", windSustained=" + windSustained);
    Log.d (tag, "  barometer=" + barometer + ", humidity=" + humidity);
    Log.d (tag, "  rainLastHourInHundredths=" + rainLastHourInHundredths + ", rainLastDayInHundredths=" + rainLastDayInHundredths + ", rainLast24InHundredths=" + rainLast24InHundredths);
    Log.d (tag, "  distanceFromMeInFeet=" + distanceFromMeInFeet + ", headingFromMe=" + headingFromMe);
    Log.d (tag, "  time=" + formatter.format (calendar.getTime ()));
    Log.d (tag, "  inMotion=" + inMotion + ", hasMoved=" + hasMoved + ", hasWeather=" + hasWeather);
  }
  
  public ContentValues aoToContentValues ()
  {
    ContentValues values = new ContentValues ();

    values.put (APRSDatabase.C_FIELDS_PRESENT, fieldsPresent);
    values.put (APRSDatabase.C_CALLSIGN, callsign);
    values.put (APRSDatabase.C_COMMENT, comment);
    values.put (APRSDatabase.C_SYMBOL_CODE, Byte.toString (symbolCode));
    values.put (APRSDatabase.C_SYMBOL_TABLE, Byte.toString (symbolTable));
    values.put (APRSDatabase.C_TYPE, Byte.toString (type));
    values.put (APRSDatabase.C_LATITUDE, latitude);
    values.put (APRSDatabase.C_LONGITUDE, longitude);
    values.put (APRSDatabase.C_SPEED, speedInMPH);
    values.put (APRSDatabase.C_COURSE, course);
    values.put (APRSDatabase.C_ALTITUDE, altitudeInFeet);
    values.put (APRSDatabase.C_TEMPERATURE, temperatureInF);
    values.put (APRSDatabase.C_WIND_DIRECTION, windDirection);
    values.put (APRSDatabase.C_WIND_SPEED, windSpeed);
    values.put (APRSDatabase.C_WIND_GUST, windGust);
    values.put (APRSDatabase.C_WIND_SUSTAINED, windSustained);
    values.put (APRSDatabase.C_BAROMETER, barometer);
    values.put (APRSDatabase.C_HUMIDITY, humidity);
    values.put (APRSDatabase.C_RAIN_LAST_HOUR, rainLastHourInHundredths);
    values.put (APRSDatabase.C_RAIN_LAST_DAY, rainLastDayInHundredths);
    values.put (APRSDatabase.C_RAIN_LAST_24, rainLast24InHundredths);
    values.put (APRSDatabase.C_TIME, time);
    values.put (APRSDatabase.C_DISTANCE_FROM_ME, distanceFromMeInFeet);
    values.put (APRSDatabase.C_HEADING_FROM_ME, headingFromMe);
    values.put (APRSDatabase.C_IN_MOTION, inMotion);
    values.put (APRSDatabase.C_HAS_WEATHER, hasWeather);
    
    return values;
  }
  
  public boolean hasMinimumFields ()
  {
    return ((fieldsPresent & (hasCallsign | hasLatitude | hasLongitude)) == (hasCallsign | hasLatitude | hasLongitude)) ? true : false;
  }

  //
  // If the times are the same, update the inMotion status from the ao record.
  // If the times are different, then it's not a duplicate packet from the
  // network, and the inMotion status will update if the latitude or longitudes
  // are different.
  //
  public boolean updateFrom (APRSObject ao)
  {
    inMotion = (time == ao.time) ? ao.inMotion : false;
    hasWeather |= ao.hasWeather;

    if ((ao.fieldsPresent & hasCallsign) == hasCallsign)
      setCallsign (ao.callsign);
    if ((ao.fieldsPresent & hasComment) == hasComment)
      setComment (ao.comment);
    if ((ao.fieldsPresent & hasSymbolCode) == hasSymbolCode)
      setSymbolCode ((char) ao.symbolCode);
    if ((ao.fieldsPresent & hasSymbolTable) == hasSymbolTable)
      setSymbolTable ((char) ao.symbolTable);
    if ((ao.fieldsPresent & hasType) == hasType)
      setType ((char) ao.type);
    if ((ao.fieldsPresent & hasLatitude) == hasLatitude)
      setLatitude (ao.latitude);
    if ((ao.fieldsPresent & hasLongitude) == hasLongitude)
      setLongitude (ao.longitude);
    if ((ao.fieldsPresent & hasSpeed) == hasSpeed)
      setSpeedInMPH (ao.speedInMPH);
    if ((ao.fieldsPresent & hasCourse) == hasCourse)
      setCourse (ao.course);
    if ((ao.fieldsPresent & hasAltitude) == hasAltitude)
      setAltitudeInFeet (ao.altitudeInFeet);
    if ((ao.fieldsPresent & hasTemperature) == hasTemperature)
      setTemperatureInF (ao.temperatureInF);
    if ((ao.fieldsPresent & hasWindDirection) == hasWindDirection)
      setWindDirection (ao.windDirection);
    if ((ao.fieldsPresent & hasWindSpeed) == hasWindSpeed)
      setWindSpeedInMPH (ao.windSpeed);
    if ((ao.fieldsPresent & hasWindGust) == hasWindGust)
      setWindGustInMPH (ao.windGust);
    if ((ao.fieldsPresent & hasWindSustained) == hasWindSustained)
      setWindSustainedInMPH (ao.windSustained);
    if ((ao.fieldsPresent & hasBarometer) == hasBarometer)
      setBarometer (ao.barometer);
    if ((ao.fieldsPresent & hasHumidity) == hasHumidity)
      setHumidity (ao.humidity);
    if ((ao.fieldsPresent & hasRainLastHour) == hasRainLastHour)
      setRainLastHourInHundredths (ao.rainLastHourInHundredths);
    if ((ao.fieldsPresent & hasRainLastDay) == hasRainLastDay)
      setRainLastDayInHundredths (ao.rainLastDayInHundredths);
    if ((ao.fieldsPresent & hasRainLast24) == hasRainLast24)
      setRainLast24InHundredths (ao.rainLast24InHundredths);

    time = System.currentTimeMillis ();
    
    return inMotion;
  }

  public boolean isDuplicateOf (APRSObject ao)
  {
    return ((ID != null) && (ao.ID != null) && (ID.equals (ao.ID))) ? true : false;
  }

  public int compareTo (APRSObject o)
  {
    return (this.time < o.time) ? -1 : (this.time > time) ? 1 : 0;
  }

  public String getObjectInfoText ()
  {
    String s = callsign;

    if (hasWeather)
    {
      StringBuffer t = new StringBuffer (); // temperature, humidity, barometer
      StringBuffer w = new StringBuffer (); // wind speed, direction, gust
      StringBuffer r = new StringBuffer (); // rain last hour, last day, last 24 hours

      if ((fieldsPresent & hasTemperature) == hasTemperature)
        t.append (temperatureInF + "F degrees");
      if ((fieldsPresent & hasHumidity) == hasHumidity)
        t.append (((t.length () > 0) ? ", " : "") + humidity + "% RH");
      if ((fieldsPresent & hasBarometer) == hasBarometer)
        t.append (((t.length () > 0) ? ", " : "") + barometer + " hPa");

      if ((fieldsPresent & hasWindSpeed) == hasWindSpeed)
        w.append (" " + windSpeed + " MPH");
      if ((fieldsPresent & hasWindDirection) == hasWindDirection)
        w.append (" from " + windDirection + " degrees");
      if ((fieldsPresent & hasWindGust) == hasWindGust)
        w.append ((w.length () > 0) ? (", " + windGust + " MPH gusts") : ("Wind gusting to " + windGust + " MPH"));

      if ((fieldsPresent & hasRainLastHour) == hasRainLastHour)
        r.append (String.format ("%d.%02d\" rain last hour", rainLastHourInHundredths / 100, rainLastHourInHundredths % 100));
      if ((fieldsPresent & hasRainLastDay) == hasRainLastDay)
        r.append (String.format ("%s%d.%02d\" rain last day", (r.length () > 0) ? ", " : "", rainLastDayInHundredths / 100, rainLastDayInHundredths % 100));
      if ((fieldsPresent & hasRainLast24) == hasRainLast24)
        r.append (String.format ("%s%d.%02d\" rain last 24 hours", (r.length () > 0) ? ", " : "", rainLast24InHundredths / 100, rainLast24InHundredths % 100));

      if (t.length () > 0)
        s = s + "\n" + t.toString ();
      if (w.length () > 0)
        s = s + "\nWind is" + w.toString ();
      if (r.length () > 0)
        s = s + "\n" + r.toString ();
    }
    else if (inMotion)
    {
      StringBuffer r = new StringBuffer ();

      if ((fieldsPresent & hasSpeed) == hasSpeed)
        r.append ("Speed " + speedInMPH + " MPH");
      if ((fieldsPresent & hasCourse) == hasCourse)
        r.append (((r.length () > 0) ? ", course" : "Course") + " of " + course + " degrees");

      if (r.length () > 0)
        s = s + "\n" + r.toString ();
    }
    
    s = String.format ("%s\n(%s ago)", s, lastHeard ());
    
    return s;
  }

  //
  //  Only need position and speed/course/altitude info, since a G1 will never
  //  be a weather station.  Do indicate last time we heard ourself, however.
  //
  public String getMyselfInfoText ()
  {
    String s = callsign;
    
    if ((fieldsPresent & (hasLatitude | hasLongitude)) == (hasLatitude | hasLongitude))
      s = String.format ("%s\nLatitude %.6f, Longitude %.6f", s, getLatitudeAsFloat (), getLongitudeAsFloat ());
    
    StringBuffer r = new StringBuffer ();

    if ((fieldsPresent & hasSpeed) == hasSpeed)
      r.append ("Speed " + speedInMPH + " MPH");
    if ((fieldsPresent & hasCourse) == hasCourse)
      r.append (((r.length () > 0) ? ", course" : "Course") + " of " + course + " degrees");
    if ((fieldsPresent & hasAltitude) == hasAltitude)
      r.append (((r.length () > 0) ? ", altitude " : "Altitude ") + altitudeInFeet + " feet");

    if (r.length () > 0)
      s = s + "\n" + r.toString ();
    
    s = String.format ("%s\n(last heard %s ago)", s, lastHeard ());
    
    return s;
  }

  //
  //
  //
  public void setID (String ID)
  {
    this.ID = ID;
    fieldsPresent |= hasID;
  }

  public String getID ()
  {
    return ID;
  }

  //
  //
  //
  public void setCallsign (String callsign)
  {
    if (callsign != null)
    {
      this.callsign = callsign;
      fieldsPresent |= hasCallsign;
    }
  }

  public String getCallsign ()
  {
    return callsign;
  }

  //
  //
  //
  public void setComment (String comment)
  {
    this.comment = comment;
    fieldsPresent |= hasComment;
  }

  public String getComment ()
  {
    return (comment != null) ? comment : "";
  }

  //
  //
  //
  public void setTimeNow ()
  {
    this.time = System.currentTimeMillis ();
  }

  public void setTime (long time)
  {
    this.time = time;
  }

  public long getTime ()
  {
    return time;
  }

  //
  //
  //
  public void setSymbolCode (char symbolCode)
  {
    this.symbolCode = (byte) symbolCode;
    fieldsPresent |= hasSymbolCode;
  }

  public void setSymbolCode (String symbolCode)
  {
    setSymbolCode (symbolCode.charAt (0));
  }

  public char getSymbolCode ()
  {
    return (char) symbolCode;
  }

  public boolean hasSymbolCode ()
  {
    return (fieldsPresent & hasSymbolCode) > 0; 
  }
  
  //
  //
  //
  public void setSymbolTable (char symbolTable)
  {
    this.symbolTable = (byte) symbolTable;
    fieldsPresent |= hasSymbolTable;
  }

  public void setSymbolTable (String symbolTable)
  {
    setSymbolTable (symbolTable.charAt (0));
  }

  public char getSymbolTable ()
  {
    return (char) symbolTable;
  }

  public boolean hasSymbolTable ()
  {
    return (fieldsPresent & hasSymbolTable) > 0; 
  }
  
  //
  //
  //
  public void setType (char type)
  {
    this.type = (byte) type;
    fieldsPresent |= hasType;
  }

  public void setType (String type)
  {
    setType (type.charAt (0));
  }

  public char getType ()
  {
    return (char) type;
  }

  //
  //
  //
  public GeoPoint getGeoPoint ()
  {
    return new GeoPoint (latitude, longitude);
  }

  //
  //
  //
  // FIXME: Need to require some additional variation in lat/long to qualify as
  // in motion.
  public void setLatitude (int latitude)
  {
    if ((longitude != Integer.MIN_VALUE) && (this.latitude != Integer.MIN_VALUE) && (this.latitude != latitude))
    {
      hasMoved = true;
      inMotion = true;
    }
    
    this.latitude = latitude;
    fieldsPresent |= hasLatitude;
  }

  public void setLatitude (float latitude)
  {
    setLatitude ((int) (latitude * 1E6F));
  }
  
  public void setLatitude (double latitude)
  {
    setLatitude ((int) (latitude * 1E6F));
  }
  
  public void setLatitude (String latitude)
  {
    setLatitude ((int) (Float.parseFloat (latitude) * 1E6F));
  }

  public int getLatitude ()
  {
    return latitude;
  }

  public float getLatitudeAsFloat ()
  {
    return (float) latitude / 1E6F;
  }

  //
  //
  //
  // FIXME: Need to require some additional variation in lat/long to qualify as
  // in motion.
  public void setLongitude (int longitude)
  {
    if ((latitude != Integer.MIN_VALUE) && (this.longitude != Integer.MIN_VALUE) && (this.longitude != longitude))
    {
      hasMoved = true;
      inMotion = true;
    }

    this.longitude = longitude;
    fieldsPresent |= hasLongitude;
  }

  public void setLongitude (float longitude)
  {
    setLongitude ((int) (longitude * 1E6F));
  }
  
  public void setLongitude (double longitude)
  {
    setLongitude ((int) (longitude * 1E6F));
  }
  
  public void setLongitude (String longitude)
  {
    setLongitude ((int) (Float.parseFloat (longitude) * 1E6F));
  }

  public int getLongitude ()
  {
    return longitude;
  }

  public float getLongitudeAsFloat ()
  {
    return (float) longitude / 1E6F;
  }

  //
  //
  //
  public void setSpeedInMPH (int speedInMPH)
  {
    this.speedInMPH = speedInMPH;

    if (this.speedInMPH > 3)
      inMotion = true;

    fieldsPresent |= hasSpeed;
  }

  public void setSpeedInKPH (int speedInKPH)
  {
    setSpeedInMPH ((int) ((float) speedInKPH * 0.621371192));
  }

  public void setSpeedInKPH (float speedInKPH)
  {
    setSpeedInMPH ((int) (speedInKPH *  0.621371192));
  }
  
  public void setSpeedInKPH (String speedInKPH)
  {
    setSpeedInKPH (Float.parseFloat (speedInKPH));
  }

  public void setSpeedInMPS (float speedInMPS)
  {
    setSpeedInKPH (speedInMPS * 3.6F);
  }
  
  public int getSpeedInMPH ()
  {
    return speedInMPH;
  }

  public int getSpeedInKPH ()
  {
    return (int) ((float) speedInMPH * 1.609344);
  }
  
  public boolean hasSpeed ()
  {
    return (fieldsPresent & hasSpeed) > 0; 
  }
  
  //
  //
  //
  public void setCourse (int course)
  {
    this.course = course;
    fieldsPresent |= hasCourse;
  }

  public void setCourse (String course)
  {
    setCourse (Integer.parseInt (course));
  }

  public int getCourse ()
  {
    return course;
  }
  
  public boolean hasCourse ()
  {
    return (fieldsPresent & hasCourse) > 0; 
  }
  
  //
  //
  //
  public void setAltitudeInFeet (int altitudeInFeet)
  {
    this.altitudeInFeet = altitudeInFeet;
    fieldsPresent |= hasAltitude;  
  }
  
  public void setAltitudeInMeters (int altitudeInMeters)
  {
    setAltitudeInFeet ((int) ((float) altitudeInMeters * 3.2808399));
  }

  public void setAltitudeInMeters (float altitudeInMeters)
  {
    setAltitudeInFeet ((int) (altitudeInMeters * 3.2808399));
  }

  public void setAltitudeInMeters (String altitudeInFeet)
  {
    setAltitudeInMeters (Float.parseFloat (altitudeInFeet));
  }

  public int getAltitudeInFeet ()
  {
    return altitudeInFeet;
  }
  
  public int getAltitudeInMeters ()
  {
    return (int) ((float) altitudeInFeet * 0.3048);
  }

  public boolean hasAltitude ()
  {
    return (fieldsPresent & hasAltitude) > 0; 
  }
  
  //
  //
  //
  public void setTemperatureInF (int temperatureInF)
  {
    this.temperatureInF = temperatureInF;
    fieldsPresent |= hasTemperature;
  }

  public void setTemperatureInC (int temperatureInC)
  {
    setTemperatureInF ((int) ((float) temperatureInC * 1.8) + 32);
  }

  public void setTemperatureInC (float temperatureInC)
  {
    setTemperatureInF ((int) (temperatureInC * 1.8) + 32);
  }

  public void setTemperatureInC (String temperatureInC)
  {
    setTemperatureInC (Float.parseFloat (temperatureInC));
  }

  public int getTemperatureInF ()
  {
    return temperatureInF;
  }

  //
  //
  //
  public void setWindDirection (int windDirection)
  {
    this.windDirection = windDirection;
    fieldsPresent |= hasWindDirection;
  }

  public void setWindDirection (String windDirection)
  {
    setWindDirection (Integer.parseInt (windDirection));
  }

  public int getWindDirection ()
  {
    return windDirection;
  }

  //
  //
  //
  public void setWindSpeedInMPH (int windSpeedInMPH)
  {
    this.windSpeed = windSpeedInMPH;
    fieldsPresent |= hasWindSpeed;
  }
  
  public void setWindSpeedInKPH (int windSpeedInKPH)
  {
    setWindSpeedInMPH ((int) ((float) windSpeedInKPH * 0.621371192));
  }

  public void setWindSpeedInKPH (float windSpeedInKPH)
  {
    setWindSpeedInMPH ((int) (windSpeedInKPH * 0.621371192));
  }

  public void setWindSpeedInKPH (String windSpeedInKPH)
  {
    setWindSpeedInKPH (Float.parseFloat (windSpeedInKPH));
  }

  public int getWindSpeedInMPH ()
  {
    return windSpeed;
  }

  //
  //
  //
  public void setWindGustInMPH (int windGustInMPH)
  {
    this.windGust = windGustInMPH;
    fieldsPresent |= hasWindGust;
  }

  public void setWindGustInKPH (int windGustInKPH)
  {
    setWindGustInMPH ((int) ((float) windGustInKPH *  0.621371192));
  }

  public void setWindGustInKPH (float windGustInKPH)
  {
    setWindGustInMPH ((int) (windGustInKPH *  0.621371192));
  }

  public void setWindGustInKPH (String windGustInKPH)
  {
    setWindGustInKPH (Float.parseFloat (windGustInKPH));
  }

  public int getWindGust ()
  {
    return windGust;
  }

  //
  //
  //
  public void setWindSustainedInMPH (int windSustainedInMPH)
  {
    this.windSustained = windSustainedInMPH;
    fieldsPresent |= hasWindSustained;
  }

  public void setWindSustainedInKPH (int windSustainedInKPH)
  {
    setWindSustainedInMPH ((int) ((float) windSustainedInKPH *  0.621371192));
  }

  public void setWindSustainedInKPH (float windSustainedInKPH)
  {
    setWindSustainedInMPH ((int) (windSustainedInKPH *  0.621371192));
  }

  public void setWindSustainedInKPH (String windSustainedInKPH)
  {
    setWindSustainedInKPH (Float.parseFloat (windSustainedInKPH));
  }

  public int getWindSustained ()
  {
    return windSustained;
  }

  //
  //
  //
  public void setBarometer (int barometer)
  {
    this.barometer = barometer;
    fieldsPresent |= hasBarometer;
  }

  public void setBarometer (String barometer)
  {
    setBarometer ((int) Float.parseFloat (barometer));
  }

  public int getBarometer ()
  {
    return barometer;
  }

  //
  //
  //
  public void setHumidity (int humidity)
  {
    this.humidity = humidity;
    fieldsPresent |= hasHumidity;
  }

  public void setHumidity (String humidity)
  {
    setHumidity (Integer.parseInt (humidity));
  }

  public int getHumidity ()
  {
    return humidity;
  }

  //
  //
  //
  public void setRainLastHourInHundredths (int rainLastHourInHundredths)
  {
    this.rainLastHourInHundredths = rainLastHourInHundredths;
    fieldsPresent |= hasRainLastHour;
  }

  public void setRainLastHourInMM (float rainLastHourInMM)
  {
    setRainLastHourInHundredths ((int) (rainLastHourInMM * 3.93700787));
  }

  public void setRainLastHourInMM (String rainLastHourInMM)
  {
    setRainLastHourInMM (Float.parseFloat (rainLastHourInMM));
  }

  public float getRainLastHourInInches ()
  {
    return rainLastHourInHundredths;
  }

  //
  //
  //
  public void setRainLast24InHundredths (int rainLast24InHundredths)
  {
    this.rainLast24InHundredths = rainLast24InHundredths;
    fieldsPresent |= hasRainLast24;
  }

  public void setRainLast24InMM (float rainLast24InMM)
  {
    setRainLast24InHundredths ((int) (rainLast24InMM * 3.93700787));
  }

  public void setRainLast24InMM (String rainLast24InMM)
  {
    setRainLast24InMM (Float.parseFloat (rainLast24InMM));
  }

  public int getRainLast24 ()
  {
    return rainLast24InHundredths;
  }

  //
  //
  //
  public void setRainLastDayInHundredths (int rainLastDayInHundredths)
  {
    this.rainLastDayInHundredths = rainLastDayInHundredths;
    fieldsPresent |= hasRainLastDay;
  }

  public void setRainLastDayInMM (float rainLastDayInMM)
  {
    setRainLastDayInHundredths ((int) (rainLastDayInMM * 3.93700787));
  }

  public void setRainLastDayInMM (String rainLastDayInMM)
  {
    setRainLastDayInMM (Float.parseFloat (rainLastDayInMM));
  }

  public int getRainLastDay ()
  {
    return rainLastDayInHundredths;
  }

  //
  //
  //
  public boolean isInMotion ()
  {
    return inMotion;
  }

  //
  //
  //
  public boolean hasMoved ()
  {
    return hasMoved;
  }
  
  public void clearHasMoved ()
  {
    hasMoved = false;
  }
  
  //
  //
  //
  public void setIsWeather ()
  {
    hasWeather = true;
  }

  public boolean hasWeather ()
  {
    return hasWeather;
  }

  //
  //
  //
  public boolean hasSymbol ()
  {
    return (fieldsPresent & (hasSymbolTable | hasSymbolCode)) == (hasSymbolTable | hasSymbolCode);
  }
  
  //
  //
  //
  public void setDistanceFromMeInFeet (int distanceFromMeInFeet)
  {
    this.distanceFromMeInFeet = distanceFromMeInFeet;
  }
  
  public int getDistanceFromMeInFeet ()
  {
    return distanceFromMeInFeet;
  }
  
  public void setHeadingFromMe (int headingFromMe)
  {
    this.headingFromMe = headingFromMe;
  }
  
  public int getHeadingFromMe ()
  {
    return headingFromMe;
  }
}