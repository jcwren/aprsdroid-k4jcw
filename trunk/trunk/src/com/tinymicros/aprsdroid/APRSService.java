//
//  $Id: APRSService.java 100 2008-12-20 03:24:36Z jcw $
//  $Revision: 100 $
//  $Date: 2008-12-19 22:24:36 -0500 (Fri, 19 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprsdroid/APRSService.java $
//

package com.tinymicros.aprsdroid;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class APRSService extends Service implements Runnable
{
  public static final String BROADCAST_ACTION_NEWDATA = "com.tinymicros.aprsdroid.NewDataEvent";
  public static final String BROADCAST_ACTION_STATUSMESSAGE = "com.tinymicros.aprsdroid.StatusMessageEvent";
  
  private static final String TAG = "APRSSvc";
  private static final int ENABLEPOSITIONXMIT_CLEAR = 0x0000;
  private static final int ENABLEPOSITIONXMIT_ENABLEDBYUSER = 0x0001;
  private static final int ENABLEPOSITIONXMIT_LOGGEDIN = 0x0002;
  private static final int ENABLEPOSITIONXMIT_HAVESERVICE = 0x0004;
  private static final int ENABLEPOSITIONXMIT_HAVEFIX = 0x0008;
  private static final int ENABLEPOSITIONXMIT_HAVEALL = 0x000f;

  private String VERSION;
  private Intent broadcastNewData = new Intent (BROADCAST_ACTION_NEWDATA);
  private Intent broadcastStatusMessage = new Intent (BROADCAST_ACTION_STATUSMESSAGE);
  private SocketChannel channel;
  private Selector readSelector;
  private final ByteBuffer readBuffer = ByteBuffer.allocate (8192);
  private final ByteBuffer writeBuffer = ByteBuffer.allocate (512);
  private CharsetDecoder asciiDecoder;
  private volatile Thread myThread = null;
  private LocationManager mLocationManager = null;
  private LocationListener mLocationListener = null;
  private SQLiteDatabase database = null;
  private Intent serviceIntent = null;
  private IAPRSServiceToService service = null;
  private Timer timer = new Timer ();
  private serverWatchdogTimerTask serverWatchdog = null;
  private APRSPreferences mAPRSPreferences = null;
  private APRSObject myAO = new APRSObject ();
  private String statusMessage = null;
  private int enablePositionXmit = ENABLEPOSITIONXMIT_CLEAR;
  private boolean updateDistancesRequired = false;
  private NotificationManager mServiceRunningNotificationManager = null;
  
  //
  //
  //
  @Override
  public void onCreate ()
  {
    super.onCreate ();

    try
    {
      PackageInfo pi = this.getPackageManager ().getPackageInfo (this.getPackageName (), 0);
      
      VERSION = getString (R.string.app_name) + " " + pi.versionName;
      
      Log.d (TAG, "Package name: " + pi.packageName);
      Log.d (TAG, "Version name: " + pi.versionName);
      Log.d (TAG, "Version code: " + pi.versionCode);
      Log.d (TAG, "Current version: " + VERSION);
    } 
    catch (NameNotFoundException e) 
    {
      Log.e (TAG, "NameNotFoundException", e);
    }
    
    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "OnCreate called");

    myAO.setLatitude (37.422006);
    myAO.setLongitude (-122.084095);
    
    mAPRSPreferences = new APRSPreferences (getApplicationContext ());
    enablePositionXmit = (enablePositionXmit & ~ENABLEPOSITIONXMIT_ENABLEDBYUSER) | (mAPRSPreferences.oaXmitPosition ? ENABLEPOSITIONXMIT_ENABLEDBYUSER : 0);
    
    serviceIntent = new Intent (this, APRS.class);
    bindService (serviceIntent, svcConn, BIND_AUTO_CREATE);
    registerReceiver (receiverUpdatePreferences, new IntentFilter (APRS.BROADCAST_ACTION_UPDATEPREFERENCES));
    
    createGPSListener ();
    
    APRSDatabase databaseHelper = new APRSDatabase (APRSService.this);
    database = databaseHelper.getWritableDatabase ();
    
    scrubDatabase ();
    filterDatabase ();
    createTimerTasks ();
    serviceRunningNotification ();
    
    if (myThread == null) 
    {
      myThread = new Thread (this);
      myThread.start ();
    }
  }

  @Override
  public void onDestroy ()
  {
    super.onDestroy ();
    
    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onDestroy called");

    if (timer != null)
      timer.cancel ();
    
    myThread = null;
    
    unbindService (svcConn);
    unregisterReceiver (receiverUpdatePreferences);
    
    database.close ();
    mLocationManager.removeUpdates (mLocationListener);
    
    mServiceRunningNotificationManager.cancel (R.string.aprsdroidServiceRunning);
  }

  public void stop ()
  {
    myThread = null;
  }
  
  public void run ()
  {
    Thread thisThread = Thread.currentThread ();

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "Thread starting");
    
    while (myThread == thisThread)
    {
      while ((myThread == thisThread) && !connectToServer ())
      {
        if (APRSDebug.serverTalk)
          Log.w (TAG, "Server connect failure, sleeping 5 seconds");
        
        try
        {
          Thread.sleep (5000);
        }
        catch (Exception e)
        {
        }
      }
      
      while (myThread == thisThread)
        if (!APRSPass ())
          break;

      try
      {
        channel.close ();
      }
      catch (IOException ioe)
      {
      }
    }

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "Thread terminating");
  }

  //
  //  Schedule our recurring tasks
  //
  private class serverWatchdogTimerTask extends TimerTask
  {
    public void run ()
    {
      try
      {
        channel.close ();
      }
      catch (IOException ioe)
      {
      }
    }
  }
  
  private void createTimerTasks ()
  {
    TimerTask scrubTimer = new TimerTask () 
    {
      public void run () 
      {
        scrubDatabase ();
        sendBroadcast (broadcastNewData);
      }
    };
    
    TimerTask updateDistanceTimer = new TimerTask ()
    {
      public void run () 
      {
        if (updateDistancesRequired)
        {
          updateDistances (myAO.getLatitudeAsFloat (), myAO.getLongitudeAsFloat ());
          updateDistancesRequired = false;
        }
      }
    };
    
    TimerTask positionXmitTimer = new TimerTask () 
    {
      public void run () 
      {
        if ((enablePositionXmit & ENABLEPOSITIONXMIT_HAVEALL) == ENABLEPOSITIONXMIT_HAVEALL)
        {
          String s = ".cp la:" + myAO.getLatitudeAsFloat () + "|ln:" + myAO.getLongitudeAsFloat () + "|";
          
          if (myAO.hasAltitude ())
            s += "at:" + myAO.getAltitudeInMeters () + "|";
          if (myAO.hasSpeed ())
            s += "sp:" + myAO.getSpeedInKPH () + "|";
          if (myAO.hasCourse ())
            s += "cr:" + myAO.getCourse () + "|";
            
          // FIXME get symbol table and code from preferences, once we allow icon choosing
          s += "tb:" + (myAO.hasSymbolTable () ? myAO.getSymbolTable () : '/') + "|";
          s += "cd:" + (myAO.hasSymbolCode () ? myAO.getSymbolCode () : '-') + "|";
          s += "cm:" + VERSION + "\n"; 
          
          putLine (s);

          if (mAPRSPreferences.alertPositionSend)
            playMorseFile (R.raw.morse_p);
        }
      }
    };
    
    TimerTask sendLiveviewTimer = new TimerTask ()
    {
      public void run ()
      {
        sendLiveviewIfHasMoved ();
      }
    };
    
    timer.scheduleAtFixedRate (scrubTimer, 0, 60 * 1000);
    timer.scheduleAtFixedRate (updateDistanceTimer, 0, 10 * 1000);
    timer.scheduleAtFixedRate (positionXmitTimer, 0, mAPRSPreferences.oaUpdateRate * 1000);
    timer.scheduleAtFixedRate (sendLiveviewTimer, 0, 15 * 1000);
  }
  
  //
  //  Stuff to send messages to APRS class
  //
  private final IAPRSServiceFromService.Stub binderToAPRS = new IAPRSServiceFromService.Stub ()
  {
    public String getStatusMessage ()
    {
      return getStatusMessageImpl ();
    }
    
    public String getMyselfInfo ()
    {
      return getMyselfInfoImpl ();
    }
    
    public void sendMessage (String callsign, String message, boolean acknowledge)
    {
      Map<String,String> m = new HashMap<String,String> ();
      
      m.put ("cs", callsign);
      m.put ("ms", message);
      m.put ("ak", acknowledge ? "t" : "f");
      
      mHandler.sendMessage (mHandler.obtainMessage (0, m));
    }
  };
  
  synchronized private String getStatusMessageImpl ()
  {
    return statusMessage;
  }
  
  synchronized private String getMyselfInfoImpl ()
  {
    return myAO.getMyselfInfoText ();
  }
  
  private Handler mHandler = new Handler () 
  {
    @SuppressWarnings("unchecked")
    @Override public void handleMessage (Message msg) 
    {
      switch (msg.what) 
      {
        case 0 :
          HashMap<String,String> hm = (HashMap<String,String>) msg.obj;
          sendAPRSMessage (hm.get ("cs"), hm.get ("ms"), hm.get ("ak").equalsIgnoreCase ("t") ? true : false);
          break;
          
        default :
          super.handleMessage (msg);
       }
    }
  };
  
  @Override
  public IBinder onBind (Intent intent)
  {
    return (binderToAPRS);
  }
  
  //
  //  Stuff to handle messages sent from APRS class
  //
   private ServiceConnection svcConn = new ServiceConnection ()
  {
    public void onServiceConnected (ComponentName className, IBinder binder)
    {
      service = IAPRSServiceToService.Stub.asInterface (binder);
    }

    public void onServiceDisconnected (ComponentName className)
    {
      service = null;
    }
  };
  
  private BroadcastReceiver receiverUpdatePreferences = new BroadcastReceiver ()
  {
    public void onReceive (Context context, Intent intent)
    {
      mAPRSPreferences.reload ();
      
      if (filterDatabase ())
        sendBroadcast (broadcastNewData);

      enablePositionXmit = (enablePositionXmit & ~ENABLEPOSITIONXMIT_ENABLEDBYUSER) | (mAPRSPreferences.oaXmitPosition ? ENABLEPOSITIONXMIT_ENABLEDBYUSER : 0);
      
      if (mAPRSPreferences.credentialsChanged ())
        sendLogin ();
      
      if (mAPRSPreferences.filtersChanged () || mAPRSPreferences.objectRadiusChanged ())
      {
        sendLocationHistory ();
        sendWeatherHistory ();
      }
      
      sendMessaging ();
    }
  };
  
  //
  //
  //
  private class APRSLocationListener implements LocationListener
  {
    @Override
    public void onLocationChanged (Location location)
    {
      if (location.getProvider ().equals (LocationManager.GPS_PROVIDER))
        enablePositionXmit |= ENABLEPOSITIONXMIT_HAVEFIX;
      
      myAO.setLatitude (location.getLatitude ());
      myAO.setLongitude (location.getLongitude ());
      
      if (location.hasAltitude ())
        myAO.setAltitudeInMeters ((int) location.getAltitude ());
      
      if (location.hasSpeed ())
        myAO.setSpeedInMPS (location.getSpeed ());
      
      if (location.hasBearing ())
        myAO.setCourse ((int) location.getBearing ());
    }

    @Override
    public void onProviderDisabled (String provider)
    {
    }

    @Override
    public void onProviderEnabled (String provider)
    {
    }

    @Override
    public void onStatusChanged (String provider, int status, Bundle extras)
    {
      if (status == LocationProvider.AVAILABLE)
        enablePositionXmit |= ENABLEPOSITIONXMIT_HAVESERVICE;
      else
        enablePositionXmit &= ~ENABLEPOSITIONXMIT_HAVESERVICE;
    }
  }
  
  private void createGPSListener ()
  {
    Location lastLocation;
    Criteria c = new Criteria ();

    c.setAccuracy (Criteria.ACCURACY_FINE);
    c.setAltitudeRequired (true);
    c.setSpeedRequired (true);
    c.setBearingRequired (true);
    c.setCostAllowed (false);
    c.setPowerRequirement (Criteria.NO_REQUIREMENT);

    mLocationListener = new APRSLocationListener ();
    mLocationManager = (LocationManager) getSystemService (LOCATION_SERVICE);
    mLocationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER, 10000, 100.0f, mLocationListener);
    
    if ((lastLocation = mLocationManager.getLastKnownLocation (LocationManager.GPS_PROVIDER)) != null)
    {
      enablePositionXmit |= ENABLEPOSITIONXMIT_HAVEFIX;
      
      myAO.setLatitude (lastLocation.getLatitude ());
      myAO.setLongitude (lastLocation.getLongitude ());
      
      if (lastLocation.hasAltitude ())
        myAO.setAltitudeInMeters ((int) lastLocation.getAltitude ());
      
      if (lastLocation.hasBearing ())
        myAO.setCourse ((int) lastLocation.getBearing ());

      Log.d (TAG, "Last known position was " + lastLocation.getLatitude () + "," + lastLocation.getLongitude () + " at " + millisToDateTime (lastLocation.getTime ()));
    }
  }
  
  //
  //
  //
  private boolean connectToServer ()
  {
    InetSocketAddress socketAddress = new InetSocketAddress (mAPRSPreferences.oaServer, mAPRSPreferences.oaPort);
    
    try
    {
      channel = SocketChannel.open (socketAddress);
    }
    catch (UnresolvedAddressException uae)
    {
      statusMessage = "Unable to resolve " + mAPRSPreferences.oaServer;
      sendBroadcast (broadcastStatusMessage);
      return false;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
      return false;
    }

    try
    {
      channel.configureBlocking (false);

      readSelector = Selector.open ();
      channel.register (readSelector, SelectionKey.OP_READ, new StringBuffer ());

      asciiDecoder = Charset.forName ("US-ASCII").newDecoder ();
    }
    catch (Exception e)
    {
      statusMessage = e.getLocalizedMessage ();
      sendBroadcast (broadcastStatusMessage);
      return false;
    }

    if (serverWatchdog != null)
      serverWatchdog.cancel ();
    
    timer.schedule (serverWatchdog = new serverWatchdogTimerTask (), 120 * 1000);
    
    return true;
  }

  private void channelWrite (SocketChannel channel, ByteBuffer writeBuffer)
  {
    long nbytes = 0;
    long toWrite = writeBuffer.remaining ();

    try
    {
      while (nbytes != toWrite)
      {
        nbytes += channel.write (writeBuffer);

        try
        {
          Thread.sleep (10L);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace ();
        }
      }
    }
    catch (ClosedChannelException cce)
    {
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }

    writeBuffer.rewind ();
  }

  private void putLine (String s)
  {
    if ((s == null) || !channel.isConnected ())
      return;
    
    if (APRSDebug.serverTransmit)
      Log.d (TAG, s);

    writeBuffer.clear ();
    writeBuffer.put (s.getBytes ());
    writeBuffer.flip ();
    channelWrite (channel, writeBuffer);
  }

  /*
  private int parseNameField (String s, int l, int i, String[] results)
  {
    StringBuffer name = new StringBuffer ();
    StringBuffer field = new StringBuffer ();
    boolean isEscaped = false;

    for (; (i < l) && (s.charAt (i) != ':'); i++)
      name.append (s.charAt (i));

    for (++i; i < l; i++)
    {
      if (!isEscaped && (s.charAt (i) == '|'))
        break;

      if (!isEscaped && (s.charAt (i) == '\\'))
        isEscaped = true;
      else
      {
        field.append (s.charAt (i));
        isEscaped = false;
      }
    }
    
    results [0] = name.toString ();
    results [1] = field.toString ();
    
    return i;
  }
  */
  
  private String parse109 (String s)
  {
    for (int l = s.length (), i = 0; i < l; i++)
    {
      StringBuffer name = new StringBuffer ();
      StringBuffer field = new StringBuffer ();
      boolean isEscaped = false;

      for (; (i < l) && (s.charAt (i) != ':'); i++)
        name.append (s.charAt (i));

      for (++i; i < l; i++)
      {
        if (!isEscaped && (s.charAt (i) == '|'))
          break;

        if (!isEscaped && (s.charAt (i) == '\\'))
          isEscaped = true;
        else
        {
          field.append (s.charAt (i));
          isEscaped = false;
        }
      }

      if (name.toString ().equals ("CL"))
        return field.toString ();
    }

    return null;
  }

  private Map<String,String> parse300 (String s)
  {
    Map<String,String> hm = new HashMap<String,String> ();
    
    for (int l = s.length (), i = 0; i < l; i++)
    {
      StringBuffer name = new StringBuffer ();
      StringBuffer field = new StringBuffer ();
      boolean isEscaped = false;

      for (; (i < l) && (s.charAt (i) != ':'); i++)
        name.append (s.charAt (i));

      for (++i; i < l; i++)
      {
        if (!isEscaped && (s.charAt (i) == '|'))
          break;

        if (!isEscaped && (s.charAt (i) == '\\'))
          isEscaped = true;
        else
        {
          field.append (s.charAt (i));
          isEscaped = false;
        }
      }

      hm.put (name.toString (), field.toString ());
    }
    
    return hm;
  }
  
  private ArrayList<ArrayList<String>> parse304308 (String s)
  {
    ArrayList<ArrayList<String>> record = new ArrayList<ArrayList<String>> ();

    for (int l = s.length (), i = 0; i < l; i++)
    {
      StringBuffer name = new StringBuffer ();
      StringBuffer field = new StringBuffer ();
      boolean isEscaped = false;

      for (; (i < l) && (s.charAt (i) != ':'); i++)
        name.append (s.charAt (i));

      for (++i; i < l; i++)
      {
        if (!isEscaped && (s.charAt (i) == '|'))
          break;

        if (!isEscaped && (s.charAt (i) == '\\'))
          isEscaped = true;
        else
        {
          field.append (s.charAt (i));
          isEscaped = false;
        }
      }

      record.add (new ArrayList<String> ());

      record.get (record.size () - 1).add (name.toString ());
      record.get (record.size () - 1).add (field.toString ());
    }

    return record;
  }

  private int parse305309 (String s)
  {
    for (int l = s.length (), i = 0; i < l; i++)
    {
      StringBuffer name = new StringBuffer ();
      StringBuffer field = new StringBuffer ();
      boolean isEscaped = false;

      for (; (i < l) && (s.charAt (i) != ':'); i++)
        name.append (s.charAt (i));

      for (++i; i < l; i++)
      {
        if (!isEscaped && (s.charAt (i) == '|'))
          break;

        if (!isEscaped && (s.charAt (i) == '\\'))
          isEscaped = true;
        else
        {
          field.append (s.charAt (i));
          isEscaped = false;
        }
      }

      if (name.toString ().equals ("RS"))
        return Integer.parseInt (field.toString ());
    }

    return 0;
  }

  //
  //  TODO: Maybe move this into the APRSObject class?
  //
  private APRSObject convert304308ArrayListToAPRSObject (ArrayList<ArrayList<String>> record)
  {
    try
    {
      APRSObject ao = new APRSObject ();

      for (int l = record.size (), i = 0; i < l; i++)
      {
        final String name = record.get (i).get (0);
        final String field = record.get (i).get (1);

        if ((name.length () == 2) && (field.length () > 0))
        {
          try 
          {
            if (name.equals ("ID"))
              ao.setID (field);
            else if (name.equals ("SR"))
              ao.setCallsign (field);
            else if (name.equals ("CM"))
              ao.setComment (field);
            else if (name.equals ("CD"))
              ao.setSymbolCode (field);
            else if (name.equals ("TB"))
              ao.setSymbolTable (field);
            else if (name.equals ("TY"))
              ao.setType (field);
            else if (name.equals ("LA"))
              ao.setLatitude (field);
            else if (name.equals ("LN"))
              ao.setLongitude (field);
            else if (name.equals ("SP"))
              ao.setSpeedInKPH (field);
            else if (name.equals ("CR"))
              ao.setCourse (field);
            else if (name.equals ("AT"))
              ao.setAltitudeInMeters (field);
            else if (name.equals ("TM"))
              ao.setTemperatureInC (field);
            else if (name.equals ("WD"))
              ao.setWindDirection (field);
            else if (name.equals ("WS"))
              ao.setWindSpeedInKPH (field);
            else if (name.equals ("WG"))
              ao.setWindGustInKPH (field);
            else if (name.equals ("WU"))
              ao.setWindSustainedInKPH (field);
            else if (name.equals ("BA"))
              ao.setBarometer (field);
            else if (name.equals ("HU"))
              ao.setHumidity (field);
            else if (name.equals ("RH"))
              ao.setRainLastHourInMM (field);
            else if (name.equals ("RD"))
              ao.setRainLastDayInMM (field);
            else if (name.equals ("RM"))
              ao.setRainLast24InMM (field);
          }
          catch (NumberFormatException nfe)
          {
            Log.e (TAG, "Error parsing " + name + " with data \"" + field + "\"");
          }
          catch (Exception e)
          {
            e.printStackTrace ();
          }
        }
      }

      //
      //  FIXME!  This is a  kludge since the WX fields seems to have disappeared
      //
      if (ao.hasSymbolCode () &&(ao.getSymbolTable () == '/') && (ao.getSymbolCode () == '_'))
        ao.setIsWeather ();
   
      if (ao.hasMinimumFields ())
      {
        ao.setTimeNow ();
        return ao;
      }

      return null;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
      return null;
    }
  }

  private void storeToDatabase (APRSObject ao)
  {
    Cursor cursor = null;

    if (mAPRSPreferences.oaFilterWxStations && ao.hasWeather ())
    {
      if (APRSDebug.showDiscards)
        Log.i (TAG, "Discarding " + ao.getCallsign () + " (weather)");
      
      return;
    }
    
    if (mAPRSPreferences.oaFilterFixedStations && !ao.hasWeather () && !ao.isInMotion ())
    {
      if (APRSDebug.showDiscards)
        Log.i (TAG, "Discarding " + ao.getCallsign () + " (fixed)");
      
      return;
    }
    
    if (mAPRSPreferences.oaFilterMobileStations && ao.isInMotion ())
    {
      if (APRSDebug.showDiscards)
        Log.i (TAG, "Discarding " + ao.getCallsign () + " (mobile)");
      
      return;
    }
    
    try
    {
      final String[] callsignArray = new String[] { ao.getCallsign () };
      
      cursor = database.query (APRSDatabase.T_OBJECTS,
                               APRSDatabase.columnsListAll,
                               APRSDatabase.C_CALLSIGN + "=?", callsignArray, 
                               null, null, null);

      if (cursor.getCount () == 0)
      {
        if (APRSDebug.databaseInsertMerge)
          Log.d (TAG, ao.getCallsign () + " not in database (inserting)");
        
        if (APRSDebug.debugWithCallsign)
          if (ao.getCallsign ().startsWith (APRSDebug.debugCallsign)) 
            ao.print (APRSDebug.debugCallsign, "current ao");
        
        database.insert (APRSDatabase.T_OBJECTS, APRSDatabase.C_TIME, ao.aoToContentValues ());
        updateDistancesRequired = true;
      }
      else if (cursor.getCount () == 1)
      {
        if (APRSDebug.databaseInsertMerge)
          Log.d (TAG, ao.getCallsign () + " in database (merging)");
        
        if (APRSDebug.debugWithCallsign)
          if (ao.getCallsign ().startsWith (APRSDebug.debugCallsign)) 
            ao.print (APRSDebug.debugCallsign, "current ao");
        
        cursor.moveToFirst ();
        APRSObject aoOld = new APRSObject (cursor);
        
        if (APRSDebug.debugWithCallsign)
          if (ao.getCallsign ().startsWith (APRSDebug.debugCallsign)) 
            aoOld.print (APRSDebug.debugCallsign, "aoOld from database");
        
        if (aoOld.updateFrom (ao))
          updateDistancesRequired = true;
        
        if (APRSDebug.debugWithCallsign)
          if (ao.getCallsign ().startsWith (APRSDebug.debugCallsign)) 
            aoOld.print (APRSDebug.debugCallsign, "updated aoOld");
        
        database.update (APRSDatabase.T_OBJECTS, aoOld.aoToContentValues (), APRSDatabase.C_CALLSIGN + "=?", callsignArray);
      }
      else
        Log.e (TAG, ao.getCallsign () + " in database " + cursor.getCount () + " times!  WTF?!?");
    }
    finally
    {
      if (cursor != null)
        cursor.close ();
    }
  }
  
  //
  //
  //
  private void scrubDatabase ()
  {
    final long now = System.currentTimeMillis ();
    final long objectAgeLimit = now - (mAPRSPreferences.oaObjectAgeLimit * 60 * 1000);
    long oldestMaxObject = Long.MIN_VALUE;
    long thisTime = 0;
    int rows = 0;
    Cursor cursor = null;
    
    try 
    {
      cursor = database.query (APRSDatabase.T_OBJECTS, 
                               APRSDatabase.columnsListTime, 
                               null, null, 
                               null, null, 
                               APRSDatabase.S_OBJECTS_TIME_DESC, mAPRSPreferences.oaMaxObjects.toString ());
  
      if (cursor.getCount () >= mAPRSPreferences.oaMaxObjects)
      {
        cursor.moveToLast ();
        oldestMaxObject = cursor.getLong (cursor.getColumnIndex (APRSDatabase.C_TIME));
        
        if (APRSDebug.databaseScrub)
          Log.d (TAG, "oldestMaxObject = " + millisToDateTime (oldestMaxObject));
      }

      if (APRSDebug.databaseScrub)
        Log.d (TAG, "objectAgeLimit = " + millisToDateTime (objectAgeLimit));
      
      thisTime = Math.max (objectAgeLimit, oldestMaxObject);
      
      if (APRSDebug.databaseScrub)
        Log.d (TAG, "Using " + millisToDateTime (thisTime) + " as time");
      
      rows = database.delete (APRSDatabase.T_OBJECTS, APRSDatabase.C_TIME + " < " + thisTime, null);
    }
    catch (IllegalStateException ise)
    {
      Log.e (TAG, "IllegalStateException, probably database is closing while we're exiting");
    }
    finally
    {
      if (cursor != null)
        cursor.close ();
    }
      
    if (APRSDebug.databaseScrub)
      Log.d (TAG, "scrubDatabase(): " + rows + " rows deleted");
      
    if (APRSDebug.timeDatabaseScrub)
      Log.d (TAG, "scrubDatabase() took " + (System.currentTimeMillis () - now) + "ms");
  }
  
  private boolean filterDatabase ()
  {
    final long now = System.currentTimeMillis ();
    int rows = 0;
    
    try 
    {
      if (mAPRSPreferences.oaFilterWxStations)
        rows += database.delete (APRSDatabase.T_OBJECTS, APRSDatabase.C_HAS_WEATHER + " > 0", null);
      
      if (mAPRSPreferences.oaFilterMobileStations)
        rows += database.delete (APRSDatabase.T_OBJECTS, APRSDatabase.C_IN_MOTION + " > 0", null);
      
      if (mAPRSPreferences.oaFilterFixedStations)
        rows += database.delete (APRSDatabase.T_OBJECTS, APRSDatabase.C_HAS_WEATHER + " = 0 AND " + APRSDatabase.C_IN_MOTION + " = 0", null);
    }
    catch (IllegalStateException ise)
    {
      Log.e (TAG, "IllegalStateException, probably database is closing while we're exiting");
    }
    finally
    {
    }
      
    if (APRSDebug.databaseFilter)
      Log.d (TAG, "filterDatabase(): " + rows + " rows deleted");
      
    if (APRSDebug.timeDatabaseFilter)
      Log.d (TAG, "filterDatabase() took " + (System.currentTimeMillis () - now) + "ms");
    
    return rows > 0 ? true : false;
  }
  
  private void updateDistances (double myLatitude, double myLongitude)
  {
    final long now = System.currentTimeMillis ();
    Cursor cursor = null;

    try
    {
      cursor = database.query (APRSDatabase.T_OBJECTS, 
          APRSDatabase.columnsListLatLon, 
          null, null, 
          null, null, 
          APRSDatabase.S_OBJECTS_CALLSIGN_ASC); 
      
      if (cursor.moveToFirst ())
      {
        do
        {
          ContentValues values = new ContentValues ();
          final double aoLatitude = (double) cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_LATITUDE)) / 1E6D;
          final double aoLongitude = (double) cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_LONGITUDE)) / 1E6D;
          final String[] callsignArray = new String[] { cursor.getString (cursor.getColumnIndex (APRSDatabase.C_CALLSIGN)) };
          int[] results = { 0, 0 };
          
          calculateDistanceAndHeading (myLatitude, myLongitude, aoLatitude, aoLongitude, results);
          
          values.put (APRSDatabase.C_DISTANCE_FROM_ME, results [0]);
          values.put (APRSDatabase.C_HEADING_FROM_ME, results [1]);
          
          database.update (APRSDatabase.T_OBJECTS, values, APRSDatabase.C_CALLSIGN + "=?", callsignArray);
        }
        while (cursor.moveToNext ());
      }
    }
    finally
    {
      if (cursor != null)
        cursor.close ();
    }
    
    if (APRSDebug.timeUpdateDistances)
      Log.d (TAG, "updateDistances() took " + (System.currentTimeMillis () - now) + "ms");
  }

  //
  //
  //
  private void sendLiveview ()
  {
    putLine (".lv cn:" + myAO.getLatitudeAsFloat () + "," + myAO.getLongitudeAsFloat () + "," + (int) ((float) mAPRSPreferences.oaObjectRadius * 1.609344) + "\n");
  }

  private void sendLiveviewIfHasMoved ()
  {
    if (myAO.hasMoved ())
    {
      myAO.clearHasMoved ();
      sendLiveview ();
    }
  }
  
  private void sendLogin ()
  {
    putLine (".ln " + mAPRSPreferences.oaEmail + " " + mAPRSPreferences.oaPassword + " " + VERSION + "\n");
  }

  private String lcwcParameters ()
  {
    return "cn:" + myAO.getLatitudeAsFloat () + "," + myAO.getLongitudeAsFloat () + 
           "|rg:" + (int) ((float) mAPRSPreferences.oaObjectRadius * 1.609344) + 
           "|lm:" + mAPRSPreferences.oaMaxObjects + 
           "|ag:" + (mAPRSPreferences.oaObjectAgeLimit * 60) + "\n"; 
  }

  private void sendLocationHistory ()
  {
    putLine (".lc " + lcwcParameters ());
  }
  
  private void sendWeatherHistory ()
  {
    putLine (".wc " + lcwcParameters ());
  }
  
  private void sendMessaging ()
  {
    putLine (".ms on:" + (mAPRSPreferences.oaMessaging ? "yes" : "no") + "|SR:" + myAO.getCallsign () + "-" + mAPRSPreferences.oaSSID + "\n");
  }
  
  private void sendAPRSMessage (String callsign, String message, boolean acknowledge)
  {
    putLine (".cm to:" + callsign + "|sr:" + myAO.getCallsign () + "-" + mAPRSPreferences.oaSSID + "|ms:" + message + "\n");
  }
  
  //
  //
  //
  private void parseServerResponse (String s)
  {
    if (APRSDebug.serverReceive)
      Log.d (TAG, s);
    
    try
    {
      int response = Integer.parseInt (s.substring (0, 3));

      switch (response)
      {
        case 1 : // Signon
          break;

        case 2 : // Please login
          putLine (".ln " + mAPRSPreferences.oaEmail + " " + mAPRSPreferences.oaPassword + " " + VERSION + "\n");
          break;

        case 101 : // Already logged in
          Log.d (TAG, "Huh... Already logged in");
          
        case 100 : // Login successful
          enablePositionXmit |= ENABLEPOSITIONXMIT_LOGGEDIN;
          sendLocationHistory ();
          sendWeatherHistory ();
          sendLiveview ();
          break;

        case 102 : // Account must be verified to use this feature
          statusMessage = "Account must be verified!";
          sendBroadcast (broadcastStatusMessage);
          enablePositionXmit &= ENABLEPOSITIONXMIT_LOGGEDIN;
          break;
          
        case 103 : // Account is not active
          statusMessage = "Account is not active";
          sendBroadcast (broadcastStatusMessage);
          enablePositionXmit &= ENABLEPOSITIONXMIT_LOGGEDIN;
          break;
          
        case 104 : // Login failed, check your username and password
          statusMessage = "Login failed, check email and password";
          sendBroadcast (broadcastStatusMessage);
          enablePositionXmit &= ENABLEPOSITIONXMIT_LOGGEDIN;
          break;
          
        case 105 : // You have admin access
          Log.d (TAG, "Didn't expect this! Response was " + response);
          break;

        case 109 : // User info
          myAO.setCallsign (parse109 (s.substring (4)));
          sendMessaging ();
          break;
          
        case 106 : // Ping?
          {
            putLine (".pn\n");
            
            if (serverWatchdog != null)
              serverWatchdog.cancel ();
            
            timer.schedule (serverWatchdog = new serverWatchdogTimerTask (), 120 * 1000);
          }
          break;

        case 300 : // Message received
          {
            Map<String,String> hm = parse300 (s.substring (4));
            
            sendNewMessageNotification (hm.get ("SR"), hm.get ("MS"), millisToDateTime (Long.parseLong (hm.get ("CT")) * 1000));
          }
          break;
          
        case 301 : // Message table count
        case 302 : // Messaging on
        case 303 : // Messaging off
          break;
          
        case 304 : // Position table reply
        case 308 : // Weather table reply
        case 318 : // Live view reply
          {
            ArrayList<ArrayList<String>> record = parse304308 (s.substring (4));
            APRSObject ao = convert304308ArrayListToAPRSObject (record);
            
            if (APRSDebug.debugWithCallsign)
              if (ao.getCallsign ().startsWith (APRSDebug.debugCallsign))
                Log.d (APRSDebug.debugCallsign, s);
            
            //
            //  Don't store our own info into the database
            //
            if (ao.getCallsign ().equals (myAO.getCallsign ()))
              myAO.setTime (ao.getTime ());
            else
            {
              if (response == 308)
                ao.setIsWeather ();
            
              storeToDatabase (ao);
            }
          }
          break;

        case 305 : // Position table count
          {
            if (APRSDebug.serverSummary && !APRSDebug.serverReceive)
              Log.d (TAG, s);
            
            if (parse305309 (s.substring (4)) > 0)
              sendBroadcast (broadcastNewData);
          }
          break;
          
        case 309 : // Weather table count
          {
            if (APRSDebug.serverSummary && !APRSDebug.serverReceive)
              Log.d (TAG, s);
            
            if (parse305309 (s.substring (4)) > 0)
              sendBroadcast (broadcastNewData);
          }
          break;

        case 316 : // Live view off
        case 317 : // Live view on
          break;
          
        case 500 : // OK
        case 501 : // Message sent
        case 502 : // Position sent
          break;

        case 900 : // Connection timed out
        case 901 : // Shutting down
        case 902 : // Reloading module, please reconnect
        case 903 : // Killed (usually excess flood)
        case 999 : // Internal dcc.openaprs.net error
          Log.w (TAG, "Closing connection due to " + response + " code");
          channel.close ();
          enablePositionXmit &= ~ENABLEPOSITIONXMIT_LOGGEDIN;
          break;

        default :
          break;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace ();
      System.exit (1);
    }
  }

  private void scanReadBuffer ()
  {
    readBuffer.rewind ();

    for (int l = readBuffer.limit (), i = 0; i < l; i++)
    {
      int c = (int) readBuffer.get ();

      if ((c != '\n') && (c != '\r') && ((c < ' ') || (c > 0x7f)))
      {
        readBuffer.rewind ();

        System.out.println ("At position " + i + " value " + c + " was found");

        for (int m = readBuffer.limit (), j = 0; j < m; j++)
        {
          int d = (int) readBuffer.get ();

          if ((d < ' ') || (d >= 0x7f))
            System.out.printf (" (0x%02x) ", d);
          else
            System.out.printf ("%c", d);
        }

        System.out.println ();

        return;
      }
    }
  }

  private StringBuffer readLine (SocketChannel channel, StringBuffer sb)
  {
    readBuffer.clear ();

    try
    {
      if (channel.read (readBuffer) == -1)
      {
        Log.d (TAG, "Disconnected from server: end-of-stream");
        channel.close ();
        return null;
      }

      try
      {
        readBuffer.flip ();
        sb.append (asciiDecoder.decode (readBuffer).toString ());
        readBuffer.clear ();
      }
      catch (MalformedInputException mie)
      {
        scanReadBuffer ();
      }
      catch (Exception e)
      {
        channel.close ();
        return null;
      }
    }
    catch (Exception e)
    {
      try
      {
        channel.close ();
      }
      catch (Exception e2)
      {
      }
      
      return null;
    }

    return sb;
  }

  private ArrayList<String> breakString (StringBuffer sb)
  {
    ArrayList<String> stringList = new ArrayList<String> ();
    int k;

    while (((k = sb.indexOf ("\n")) != -1) || ((k = sb.indexOf ("\r")) != -1))
    {
      if (k > 0)
        stringList.add (sb.substring (0, k).toString ());

      sb.delete (0, k + 1);
    }

    return stringList;
  }

  //
  //  FIXME need to do something if we haven't heard from the server in a while
  //
  private boolean APRSPass ()
  {
    try
    {
      if (!channel.isOpen ())
        return false;
      
      if (readSelector.select (1000) != 0)
      {
        Set<SelectionKey> readyKeys = readSelector.selectedKeys ();
        Iterator<SelectionKey> i = readyKeys.iterator ();

        while (i.hasNext ())
        {
          SelectionKey key = (SelectionKey) i.next ();
          i.remove ();
          StringBuffer sb;

          if ((sb = readLine ((SocketChannel) key.channel (), (StringBuffer) key.attachment ())) == null)
            return false;

          ArrayList<String> stringList = breakString (sb);

          for (int l = stringList.size (), q = 0; q < l; q++)
            parseServerResponse (stringList.get (q));
        }
      }
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace ();
      return false;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    
    return true;
  }
  
  private void calculateDistanceAndHeading (double latitude1, double longitude1, double latitude2, double longitude2, int[] results)
  {
    double azimuth;
    final double distance;
    final double beta;
    final double cosBeta;
    final double cosAzimuth;
    final double radToDeg = 180.0 / Math.PI;
    final double degToRad = Math.PI / 180.0;
    final double earthRadius = 3958.9 * 5280;      // Use 3958.9=miles, 6371.0=Km;

    latitude1  *= degToRad;
    longitude1 *= degToRad;
    latitude2  *= degToRad;
    longitude2 *= degToRad;

    final double sinLat1 = Math.sin (latitude1);
    final double sinLat2 = Math.sin (latitude2);
    final double cosLat1 = Math.cos (latitude1);
    final double cosLat2 = Math.cos (latitude2);
    final double lon2MinusLon1 = longitude2 - longitude1;

    if (Math.abs (latitude1) < 90.0)
    {
      cosBeta = (sinLat1 * sinLat2) + (cosLat1 * cosLat2) * Math.cos (lon2MinusLon1);

      if (cosBeta >= 1.0)
      {
        results [0] = 0;
        results [1] = 0;

        return;
      }

      //
      //  Antipodes  (return miles, 0 degrees)
      //
      if (cosBeta <= -1.0)
      {
        results [0] = (int) (earthRadius * Math.PI);
        results [1] = 0;

        return;
      }

      beta = Math.acos (cosBeta);
      distance = beta * earthRadius;
      cosAzimuth = (sinLat2 - sinLat1 * cosBeta) / (cosLat1 * Math.sin (beta));

      if (cosAzimuth >= 1.0)
        azimuth = 0.0;
      else if (cosAzimuth <= -1.0)
        azimuth = 180.0;
      else
        azimuth = Math.acos (cosAzimuth) * radToDeg;

      if (Math.sin (lon2MinusLon1) < 0.0)
        azimuth = 360.0 - azimuth;

      results [0] = (int) distance;
      results [1] = (int) azimuth;

      return;
    }

    //
    //  If P1 is north or south pole, then azimuth is undefined
    //
    if (Math.signum (latitude1) == Math.signum (latitude2))
      distance = earthRadius * (Math.PI / 2.0 - Math.abs (latitude2));
    else
      distance = earthRadius * (Math.PI / 2.0 + Math.abs (latitude2));

    results [0] = (int) distance;
    results [1] = 0;
  }
  
  private String millisToDateTime (long millis)
  {
    final DateFormat formatter = new SimpleDateFormat ("yyyy/MM/dd hh:mm:ss.SSS");
    final Calendar calendar = Calendar.getInstance ();
    
    calendar.setTimeInMillis (millis);
    
    return formatter.format (calendar.getTime ());
  }
 
  private void serviceRunningNotification () 
  {
    mServiceRunningNotificationManager = (NotificationManager) getSystemService (NOTIFICATION_SERVICE);
    final PendingIntent mPendingIntent = PendingIntent.getActivity (this, 0, new Intent (this, APRS.class), 0);
    final Notification mNotification = new Notification (R.drawable.service_running, null, System.currentTimeMillis ());

    mNotification.defaults = 0;
    mNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
    mNotification.setLatestEventInfo (this, getText (R.string.aprsdroidServiceLabel), getText (R.string.aprsdroidServiceRunning), mPendingIntent);
    mServiceRunningNotificationManager.notify (R.string.aprsdroidServiceRunning, mNotification);
  }
  
  private void sendNewMessageNotification (String msgFromCallsign, String msgText, String msgTime)
  {
    final NotificationManager mNotificationManager = (NotificationManager) getSystemService (NOTIFICATION_SERVICE);
    
    final Intent mIntent = new Intent (this, ActivityNotification.class);
    mIntent.addFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
    mIntent.putExtra ("com.tinymicros.aprsdroid.notification.callsign", msgFromCallsign);
    mIntent.putExtra ("com.tinymicros.aprsdroid.notification.text", msgText);
    mIntent.putExtra ("com.tinymicros.aprsdroid.notification.time", msgTime);
    
    final PendingIntent mPendingIntent = PendingIntent.getActivity (this, 0, mIntent, 0);

    final Notification mNotification = new Notification (R.drawable.stat_msg, msgText, System.currentTimeMillis ());

    mNotification.setLatestEventInfo (this, "Message from " + msgFromCallsign, msgText, mPendingIntent);

    if (mAPRSPreferences.alertMessageReceived)
    {
      //mNotification.audioStreamType = AudioManager.STREAM_SYSTEM;
      //mNotification.sound = Uri.parse ("android.resource://" + packageName + "/" + R.raw.morse_sms);  TODO: One day, this may work
      //mNotification.sound = Uri.parse (getResources ().getResourceName (R.raw.morse_sms));            TODO: Or this...
      playMorseFile (R.raw.morse_msg);
    }

    if (mAPRSPreferences.alertMessageReceivedLED)
    {
      mNotification.flags |= (Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONLY_ALERT_ONCE); 
      mNotification.ledARGB = 0x000000ff;
      mNotification.ledOffMS = 0;
      mNotification.ledOnMS = 500;
    }
    
    mNotificationManager.notify (R.layout.msg_notification, mNotification);
  }
  
  private void playMorseFile (int fileResId)
  {
    try 
    {
      MediaPlayer mp = MediaPlayer.create (getApplicationContext (), fileResId);
      mp.start ();
    }
    catch (Exception e)
    {
    }   
  }
}