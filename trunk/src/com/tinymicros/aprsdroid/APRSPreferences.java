//
//  $Id: APRS.java 10 2008-11-30 23:12:05Z jcw $
//  $Revision: 10 $
//  $Date: 2008-11-30 18:12:05 -0500 (Sun, 30 Nov 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprs/APRS.java $
//

package com.tinymicros.aprsdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class APRSPreferences extends Object
{
  private static final String TAG = "APRSPrefs";
 
  public String oaEmail;
  public String oaPassword;
  public Integer oaSSID;
  public String oaServer;
  public Integer oaPort;
  public boolean oaXmitPosition;
  public boolean oaMessaging;
  public boolean oaCenterOnMyPosition;
  public boolean oaFilterWxStations;
  public boolean oaFilterFixedStations;
  public boolean oaFilterMobileStations;
  public Integer oaUpdateRate;
  public Integer oaObjectRadius;
  public Integer oaMaxObjects;
  public Integer oaObjectAgeLimit;
  public boolean cfgOrientation;
  public boolean alertPositionSend;
  public boolean alertPositionSendLED;
  public boolean alertMessageReceived;
  public boolean alertMessageReceivedLED;
  
  private boolean credentialsChanged;
  private boolean filtersChanged;
  private boolean objectRadiusChanged;
  private Context context;

  APRSPreferences (Context context)
  {
    this.context = context;
    
    load ();
  }

  public void load ()
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (context);

    if (!prefs.getBoolean ("preferencesSet", false))
      writeDefaults ();
    
    reload ();
  }

  public void reload ()
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (context);

    String oaEmailOld = oaEmail;
    String oaPasswordOld = oaPassword;
    boolean oaFilterWxStationsOld = oaFilterWxStations;
    boolean oaFilterFixedStationsOld = oaFilterFixedStations;
    boolean oaFilterMobileStationsOld = oaFilterMobileStations;
    Integer oaObjectRadiusOld = oaObjectRadius;
    
    oaEmail = prefs.getString ("oaEmail", context.getString (R.string.oaEmailDefault));
    oaPassword = prefs.getString ("oaPassword", context.getString (R.string.oaPasswordDefault));
    oaSSID = Integer.parseInt (prefs.getString ("oaSSID", context.getString (R.string.oaSSIDDefault)));
    oaServer = prefs.getString ("oaServer", context.getString (R.string.oaServerDefault));
    oaPort = Integer.parseInt (prefs.getString ("oaPort", context.getString (R.string.oaPortDefault)));
    oaXmitPosition = prefs.getBoolean ("oaXmitPosition", Boolean.parseBoolean (context.getString (R.string.oaXmitPositionDefault)));
    oaMessaging = prefs.getBoolean ("oaMessaging", Boolean.parseBoolean (context.getString (R.string.oaMessagingDefault)));
    oaCenterOnMyPosition = prefs.getBoolean ("oaCenterOnMyPosition", Boolean.parseBoolean (context.getString (R.string.oaCenterOnMyPositionDefault)));
    oaFilterWxStations = prefs.getBoolean ("oaFilterWxStations", Boolean.parseBoolean (context.getString (R.string.oaFilterWxStationsDefault)));
    oaFilterFixedStations = prefs.getBoolean ("oaFilterFixedStations", Boolean.parseBoolean (context.getString (R.string.oaFilterFixedStationsDefault)));
    oaFilterMobileStations = prefs.getBoolean ("oaFilterMobileStations", Boolean.parseBoolean (context.getString (R.string.oaFilterMobileStationsDefault)));
    oaUpdateRate = Integer.parseInt (prefs.getString ("oaUpdateRate", context.getString (R.string.oaUpdateRateDefault)));
    oaObjectRadius = Integer.parseInt (prefs.getString ("oaObjectRadius", context.getString (R.string.oaObjectRadiusDefault)));
    oaMaxObjects = Integer.parseInt (prefs.getString ("oaMaxObjects", context.getString (R.string.oaMaxObjectsDefault)));
    oaObjectAgeLimit = Integer.parseInt (prefs.getString ("oaObjectAgeLimit", context.getString (R.string.oaObjectAgeLimitDefault)));
    cfgOrientation = prefs.getBoolean ("cfgOrientation", Boolean.parseBoolean (context.getString (R.string.cfgOrientationDefault)));
    alertPositionSend = prefs.getBoolean ("alertPositionSend", Boolean.parseBoolean (context.getString (R.string.alertPositionSendDefault)));
    alertPositionSendLED = prefs.getBoolean ("alertPositionSendLED", Boolean.parseBoolean (context.getString (R.string.alertPositionSendLEDDefault)));
    alertMessageReceived = prefs.getBoolean ("alertMessageReceived", Boolean.parseBoolean (context.getString (R.string.alertMessageReceivedDefault)));
    alertMessageReceivedLED = prefs.getBoolean ("alertMessageReceivedLED", Boolean.parseBoolean (context.getString (R.string.alertMessageReceivedLEDDefault)));
    
    credentialsChanged = (!oaEmail.equals (oaEmailOld) || !oaPassword.equals (oaPasswordOld)) ? true : false;
    filtersChanged = ((oaFilterWxStationsOld != oaFilterWxStations) || (oaFilterFixedStationsOld != oaFilterFixedStations) || (oaFilterMobileStationsOld != oaFilterMobileStations)) ? true : false;
    objectRadiusChanged = (oaObjectRadiusOld != oaObjectRadius) ? true : false;
  }

  public boolean credentialsChanged ()
  {
    return credentialsChanged;
  }
  
  public boolean filtersChanged ()
  {
    return filtersChanged;
  }

  public boolean objectRadiusChanged ()
  {
    return objectRadiusChanged;
  }
  
  private void writeDefaults ()
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (context);
    SharedPreferences.Editor editor = prefs.edit ();

    editor.clear ();

    editor.putString ("oaEmail", context.getString (R.string.oaEmailDefault));
    editor.putString ("oaPassword", context.getString (R.string.oaPasswordDefault));
    editor.putString ("oaSSID", context.getString (R.string.oaSSIDDefault));
    editor.putString ("oaServer", context.getString (R.string.oaServerDefault));
    editor.putString ("oaPort", context.getString (R.string.oaPortDefault));
    editor.putBoolean ("oaXmitPosition", Boolean.parseBoolean (context.getString (R.string.oaXmitPositionDefault)));
    editor.putBoolean ("oaMessaging", Boolean.parseBoolean (context.getString (R.string.oaMessagingDefault)));
    editor.putBoolean ("oaCenterOnMyPosition", Boolean.parseBoolean (context.getString (R.string.oaCenterOnMyPositionDefault)));
    editor.putBoolean ("oaFilterWxStations", Boolean.parseBoolean (context.getString (R.string.oaFilterWxStationsDefault)));
    editor.putBoolean ("oaFilterFixedStations", Boolean.parseBoolean (context.getString (R.string.oaFilterFixedStationsDefault)));
    editor.putBoolean ("oaFilterMobileStations", Boolean.parseBoolean (context.getString (R.string.oaFilterMobileStationsDefault)));
    editor.putString ("oaUpdateRate", context.getString (R.string.oaUpdateRateDefault));
    editor.putString ("oaObjectRadius", context.getString (R.string.oaObjectRadiusDefault));
    editor.putString ("oaMaxObjects", context.getString (R.string.oaMaxObjectsDefault));
    editor.putString ("oaObjectAgeLimit", context.getString (R.string.oaObjectAgeLimitDefault));
    editor.putBoolean ("cfgOrientation", Boolean.parseBoolean (context.getString (R.string.cfgOrientationDefault)));
    editor.putBoolean ("alertPositionSend", Boolean.parseBoolean (context.getString (R.string.alertPositionSendDefault)));
    editor.putBoolean ("alertPositionSendLED", Boolean.parseBoolean (context.getString (R.string.alertPositionSendLEDDefault)));
    editor.putBoolean ("alertMessageReceived", Boolean.parseBoolean (context.getString (R.string.alertMessageReceivedDefault)));
    editor.putBoolean ("alertMessageReceivedLED", Boolean.parseBoolean (context.getString (R.string.alertMessageReceivedLEDDefault)));
    editor.putBoolean ("preferencesSet", true);

    editor.commit ();
  }

  private void printEx (final String tag)
  {
    Log.d (tag, "oaEmail=" + oaEmail + ", oaPassword=" + oaPassword);
    Log.d (tag, "oaServer=" + oaServer + ", oaPort=" + oaPort);
    Log.d (tag, "oaSSID=" + oaSSID);
    Log.d (tag, "oaXmitPosition=" + oaXmitPosition);
    Log.d (tag, "oaMessaging=" + oaMessaging);
    Log.d (tag, "oaCenterOnMyPosition=" + oaCenterOnMyPosition);
    Log.d (tag, "oaFilterWxStations=" + oaFilterWxStations);
    Log.d (tag, "oaFilterFixedStations=" + oaFilterFixedStations);
    Log.d (tag, "oaFilterMobileStations=" + oaFilterMobileStations);
    Log.d (tag, "oaUpdateRate=" + oaUpdateRate);
    Log.d (tag, "oaObjectRadius=" + oaObjectRadius);
    Log.d (tag, "oaMaxObjects=" + oaMaxObjects);
    Log.d (tag, "oaObjectAgeLimit=" + oaObjectAgeLimit);
    Log.d (tag, "cfgOrientation=" + cfgOrientation);
    Log.d (tag, "alertPositionSend=" + alertPositionSend + ", alertPositionSendLED=" + alertPositionSendLED);
    Log.d (tag, "alertMessageReceived=" + alertMessageReceived + ", alertMessageReceivedLED" + alertMessageReceivedLED);
  }
  
  public void print ()
  {
    printEx (TAG);
  }
  
  public void print (String tag)
  {
    printEx (tag);
  }
}