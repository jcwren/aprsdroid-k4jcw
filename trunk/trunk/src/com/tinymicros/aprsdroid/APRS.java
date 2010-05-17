//
//  $Id: APRS.java 103 2008-12-22 12:12:29Z jcw $
//  $Revision: 103 $
//  $Date: 2008-12-22 07:12:29 -0500 (Mon, 22 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprsdroid/APRS.java $
//

package com.tinymicros.aprsdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class APRS extends MapActivity
{
  public static final String BROADCAST_ACTION_UPDATEPREFERENCES = "com.tinymicros.aprsdroid.UpdatePreferencesEvent";
  
  private static final String TAG = "APRS";
  
  public static final int ACTIVITYTYPE_TOOLS = 0;
  public static final int ACTIVITYTYPE_SETTINGS = 1;
  public static final int ACTIVITYTYPE_ABOUT = 2;
  public static final int ACTIVITYTYPE_OBJECT = 3;
  public static final int ACTIVITYTYPE_MYSELF = 4;
  public static final int ACTIVITYTYPE_NOTIFICATION = 5;
  public static final int ACTIVITYTYPE_FINDSTATION = 6;
  public static final int ACTIVITYTYPE_SENDMESSAGE = 7;
  public static final int ACTIVITYTYPE_HELP = 8;
  
  private MapView mMapView = null;
  private APRSObjectsOverlay mAPRSObjectsOverlay = null;
  private MyMyLocationOverlay mMyLocationOverlay = null;
  private IAPRSServiceFromService mService = null;
  private Intent serviceIntent = null;
  private SQLiteDatabase database = null;
  private APRSPreferences mAPRSPreferences = null;
  private boolean stopService = false;
  private serviceMessage mServiceMessage = new serviceMessage ();
  private static HashMap<String, Integer> iconMap;
  private static HashMap<String, Object> iconCache = new HashMap<String, Object> ();

  //
  //
  //
  @Override
  public void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    
    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onCreate called");
    
    mAPRSPreferences = new APRSPreferences (getApplicationContext ());

    setScreenOrientation ();
    setContentView (R.layout.main);
    
    serviceIntent = new Intent (this, APRSService.class);
    
    APRSDatabase databaseHelper = new APRSDatabase (APRS.this);
    database = databaseHelper.getReadableDatabase ();
    
    startService (serviceIntent);

    createMap ();
  }

  @Override
  public void onResume ()
  {
    super.onResume ();
    
    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onResume called");

    mAPRSPreferences.reload ();
    setScreenOrientation ();
    
    bindService (serviceIntent, mServiceConnection, BIND_AUTO_CREATE);
    registerReceiver (receiverNewData, new IntentFilter (APRSService.BROADCAST_ACTION_NEWDATA));
    registerReceiver (receiverStatusMessage, new IntentFilter (APRSService.BROADCAST_ACTION_STATUSMESSAGE));
    
    mMyLocationOverlay.enableMyLocation ();

    if ((database == null) || !database.isOpen ())
    {
      APRSDatabase databaseHelper = new APRSDatabase (APRS.this);
      database = databaseHelper.getReadableDatabase ();
    }
    
    checkForNotificationReply ();
  }

  @Override
  public void onPause ()
  {
    super.onPause ();
    
    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onPause called");

    if (database != null)
      database.close ();
    
    unregisterReceiver (receiverNewData);
    unregisterReceiver (receiverStatusMessage);
  }

  @Override
  public void onStop ()
  {
    super.onStop ();

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onStop called");

    unbindService (mServiceConnection);
    
    mMyLocationOverlay.disableMyLocation ();
  }

  @Override
  public void onDestroy ()
  {
    super.onDestroy ();

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onDestroy called");

    if (stopService)
    {
      stopService (serviceIntent);
      
      if (APRSDebug.trackCreateResumeEtc)
        Log.d (TAG, "onDestroy stopping APRSService");
    }
  }

  //
  //  Start of messages sent from APRSService class
  //
  private void updateMap ()
  {
    try
    {
      mAPRSObjectsOverlay.AddAPRSObjectList ();
      mMapView.invalidate ();
    }
    catch (final Throwable t)
    {
      mServiceConnection.onServiceDisconnected (null);

      runOnUiThread (new Runnable ()
      {
        public void run ()
        {
          assplode (t);
        }
      });
    }
  }

  private void updateStatus ()
  {
    try
    {
      final String s = mService.getStatusMessage ();

      if (s == null)
        Log.e (TAG, "Null status message received");
      else
        Toast.makeText (this, s, Toast.LENGTH_LONG).show ();
    }
    catch (final Throwable t)
    {
      mServiceConnection.onServiceDisconnected (null);

      runOnUiThread (new Runnable ()
      {
        public void run ()
        {
          assplode (t);
        }
      });
    }
  }

  private String updateMyselfInfo ()
  {
    String myselfInfo = null;
    
    try
    {
      myselfInfo = mService.getMyselfInfo ();
    }
    catch (final Throwable t)
    {
      mServiceConnection.onServiceDisconnected (null);

      runOnUiThread (new Runnable ()
      {
        public void run ()
        {
          assplode (t);
        }
      });
    }
    
    return myselfInfo;
  }
  
  private void assplode (Throwable t)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder (this);

    builder.setTitle ("Exception!").setMessage (t.toString ()).setPositiveButton ("OK", null).show ();
  }

  private ServiceConnection mServiceConnection = new ServiceConnection ()
  {
    public void onServiceConnected (ComponentName className, IBinder service)
    {
      Log.d (TAG, "onServiceConnected() running");
      mService = IAPRSServiceFromService.Stub.asInterface (service);
      mServiceMessage.sendAll ();
    }

    public void onServiceDisconnected (ComponentName className)
    {
      Log.d (TAG, "onServiceDisconnected() running");
      mService = null;
    }
  };
  
  private BroadcastReceiver receiverNewData = new BroadcastReceiver ()
  {
    public void onReceive (Context context, Intent intent)
    {
      runOnUiThread (new Runnable ()
      {
        public void run ()
        {
          updateMap ();
        }
      });
    }
  };

  private BroadcastReceiver receiverStatusMessage = new BroadcastReceiver ()
  {
    public void onReceive (Context context, Intent intent)
    {
      runOnUiThread (new Runnable ()
      {
        public void run ()
        {
          updateStatus ();
        }
      });
    }
  };

  //
  //
  //
  @Override
  public boolean onKeyDown (int keyCode, KeyEvent event)
  {
    switch (keyCode)
    {
      case KeyEvent.KEYCODE_AT :
        jumpToMyselfOnMap ();
        return true;
        
      case KeyEvent.KEYCODE_COMMA :
      case KeyEvent.KEYCODE_H :
        startActivity ((new Intent (this, ActivityHelp.class)));
        return true;
        
      case KeyEvent.KEYCODE_F :
        startActivityForResult (new Intent (this, ActivityFindStation.class), ACTIVITYTYPE_FINDSTATION);
        return true;
        
      case KeyEvent.KEYCODE_M :
        startActivityForResult (new Intent (this, ActivitySendMessage.class), ACTIVITYTYPE_SENDMESSAGE);
        return true;
        
      case KeyEvent.KEYCODE_Q :
        stopService = true;
        finish ();
        return true;
        
      case KeyEvent.KEYCODE_S :
        startActivityForResult (new Intent (this, ActivitySettings.class), ACTIVITYTYPE_SETTINGS);
        return true;

      case KeyEvent.KEYCODE_T :
        startActivityForResult (new Intent (this, ActivityTools.class), ACTIVITYTYPE_TOOLS);
        return true;
        
      case KeyEvent.KEYCODE_V :
        mMapView.setSatellite (!mMapView.isSatellite ());
        return true;

      case KeyEvent.KEYCODE_Z :
        mMapView.displayZoomControls (true);
        return true;

      default :
        return super.onKeyDown (keyCode, event);
    }
  }

  @Override
  public boolean onCreateOptionsMenu (Menu menu)
  {
    super.onCreateOptionsMenu (menu);

    new MenuInflater (getApplication ()).inflate (R.menu.menu, menu);

    //
    // Generate any additional actions that can be performed on the
    // overall list. In a normal install, there are no additional
    // actions found here, but this allows other applications to extend
    // our menu with their own actions.
    //
    Intent intent = new Intent (null, getIntent ().getData ());
    intent.addCategory (Intent.CATEGORY_ALTERNATIVE);
    menu.addIntentOptions (Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName (this, APRS.class), null, intent, 0, null);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected (MenuItem item)
  {
    switch (item.getItemId ())
    {
      case R.id.tools :
        startActivityForResult (new Intent (this, ActivityTools.class), ACTIVITYTYPE_TOOLS);
        return true;

      case R.id.settings :
        startActivityForResult (new Intent (this, ActivitySettings.class), ACTIVITYTYPE_SETTINGS);
        return true;

      case R.id.quit :
        stopService = true;
        finish ();
        return true;
        
      default :
        return super.onOptionsItemSelected (item);
    }
  }

  protected void onActivityResult (int requestCode, int resultCode, Intent data)
  {
    switch (requestCode)
    {
      //
      //  Handle buttons pressed on tools activity
      //
      case ACTIVITYTYPE_TOOLS :
        {
          switch (resultCode)
          {
            case ActivityTools.ACTIVITYCODE_CANCEL :
              break;
              
            case ActivityTools.ACTIVITYCODE_JUMPTOSELF :
              jumpToMyselfOnMap ();
              break;
              
            case ActivityTools.ACTIVITYCODE_FINDSTATION :
              startActivityForResult (new Intent (this, ActivityFindStation.class), ACTIVITYTYPE_FINDSTATION);
              break;
              
            case ActivityTools.ACTIVITYCODE_SENDMESSAGE :
              startActivityForResult (new Intent (this, ActivitySendMessage.class), ACTIVITYTYPE_SENDMESSAGE);
              break;
              
            default :
              Log.e (TAG, "Unknown ACTIVITYTYPE_TOOLS resultCode of " + resultCode);
              break;
          }
        }
        break;
        
      //
      //  Only thing we care about after the settings completes is to reload
      //
      case ACTIVITYTYPE_SETTINGS :
        {
          sendBroadcast (new Intent (BROADCAST_ACTION_UPDATEPREFERENCES));
          mAPRSPreferences.reload ();
          setScreenOrientation ();
        }
        break;
        
      //
      //  About has no buttons to worry about
      //
      case ACTIVITYTYPE_ABOUT :
        break;
        
      //
      //  User clicked on an object, see what s/he wants to do with it
      //
      case ACTIVITYTYPE_OBJECT :
        {
          switch (resultCode)
          {
            case ActivityObject.ACTIVITYCODE_CANCEL :
              break;
              
            case ActivityObject.ACTIVITYCODE_CENTERONLATLON :
              {
                int objectLatitude = data.getIntExtra ("com.tinymicros.aprsdroid.object.latitude", -1);
                int objectLongitude = data.getIntExtra ("com.tinymicros.aprsdroid.object.longitude", -1);
                
                if ((objectLatitude != -1) && (objectLongitude != -1))
                {
                  updateMap ();
                  mMapView.getController ().setCenter (new GeoPoint (objectLatitude, objectLongitude));
                  mMapView.getController ().setZoom (14); 
                }
                else
                  Toast.makeText (this, "Unable to retrieve lat/long for repositioning", Toast.LENGTH_LONG).show ();
              }
              break;
              
            case ActivityObject.ACTIVITYCODE_SENDMESSAGE :
              startActivityForResult (new Intent (this, ActivitySendMessage.class)
                .putExtra ("com.tinymicros.aprsdroid.message.callsign", data.getStringExtra ("com.tinymicros.aprsdroid.message.callsign")),
                ACTIVITYTYPE_SENDMESSAGE);
              break;
              
            default :
              Log.e (TAG, "Unknown ACTIVITYTYPE_OBJECT resultCode of " + resultCode);
              break;
          }
        }
        break;
        
      //
      //  Myself activity has only a return to map button
      //
      case ACTIVITYTYPE_MYSELF :
        break;
        
      //
      //
      //
      case ACTIVITYTYPE_NOTIFICATION :
        {
          switch (resultCode)
          {
            case ActivityNotification.ACTIVITYCODE_RETURNTOMAP :
              break;
              
            case ActivityNotification.ACTIVITYCODE_SENDREPLY :
              {
                String messageCallsign = data.getStringExtra ("com.tinymicros.aprsdroid.message.callsign");
                String messageText = data.getStringExtra ("com.tinymicros.aprsdroid.message.text");
                boolean messageAcknowledge = data.getBooleanExtra ("com.tinymicros.aprsdroid.message.acknowledge", false);
                
                mServiceMessage.addCMA (messageCallsign, messageText, messageAcknowledge);
              }
              break;
              
            case ActivityNotification.ACTIVITYCODE_CENTERONLATLON :
              {
                int objectLatitude = data.getIntExtra ("com.tinymicros.aprsdroid.object.latitude", -1);
                int objectLongitude = data.getIntExtra ("com.tinymicros.aprsdroid.object.longitude", -1);
                
                if ((objectLatitude != -1) && (objectLongitude != -1))
                {
                  updateMap ();
                  mMapView.getController ().setCenter (new GeoPoint (objectLatitude, objectLongitude));
                  mMapView.getController ().setZoom (14); 
                }
                else
                  Toast.makeText (this, "Unable to retrieve lat/long for repositioning", Toast.LENGTH_LONG).show ();
              }
              break;
              
            default :
              Log.e (TAG, "Unknown ACTIVITYTYPE_NOTIFICATION resultCode of " + resultCode);
              break;
          }
        }
        break;
        
      //
      //  
      //
      case ACTIVITYTYPE_FINDSTATION :
      {
        switch (resultCode)
        {
          case ActivityFindStation.ACTIVITYCODE_CENTERONLATLON :
            {
              int objectLatitude = data.getIntExtra ("com.tinymicros.aprsdroid.object.latitude", -1);
              int objectLongitude = data.getIntExtra ("com.tinymicros.aprsdroid.object.longitude", -1);
              
              if ((objectLatitude != -1) && (objectLongitude != -1))
              {
                updateMap ();
                mMapView.getController ().setCenter (new GeoPoint (objectLatitude, objectLongitude));
                mMapView.getController ().setZoom (14); 
              }
              else
                Toast.makeText (this, "Unable to retrieve lat/long for repositioning", Toast.LENGTH_LONG).show ();
            }
            break;
            
          default :
            Log.e (TAG, "Unknown ACTIVITYTYPE_OBJECT resultCode of " + resultCode);
            break;
        }
      }
      break;
        
      //
      //  
      //
      case ACTIVITYTYPE_SENDMESSAGE :
        {
          switch (resultCode)
          {
            case ActivitySendMessage.ACTIVITYCODE_SEND :
            {
              String messageCallsign = data.getStringExtra ("com.tinymicros.aprsdroid.message.callsign");
              String messageText = data.getStringExtra ("com.tinymicros.aprsdroid.message.text");
              boolean messageAcknowledge = data.getBooleanExtra ("com.tinymicros.aprsdroid.message.acknowledge", false);
              
              try
              {
                mService.sendMessage (messageCallsign, messageText, messageAcknowledge);
              }
              catch (RemoteException e)
              {
              }
            }
            break;
            
            case ActivitySendMessage.ACTIVITYCODE_CANCEL  :
              break;
          }
        }        
        break;
        
      default :
        Log.e (TAG, "Unknown requestCode of " + requestCode);
        break;
    }
  }

  //
  //
  //
  @Override
  protected boolean isRouteDisplayed ()
  {
    return false;
  }

  private GeoPoint getPoint (double lat, double lon)
  {
    return new GeoPoint ((int) (lat * 1000000.0), (int) (lon * 1000000.0));
  }

  private class APRSObjectsOverlay extends ItemizedOverlay<OverlayItem>
  {
    private List<OverlayItem> items = new ArrayList<OverlayItem> ();
    private Drawable markerDefault = null;
    
    public APRSObjectsOverlay (Drawable marker)
    {
      super (marker);
      this.markerDefault = marker;
      
      markerDefault.setBounds (0, 0, markerDefault.getIntrinsicWidth (), markerDefault.getIntrinsicHeight ());
      boundCenterBottom (markerDefault);
    }

    public void AddAPRSObject (double lat, double lon, String title, String snippet)
    {
      items.add (new OverlayItem (getPoint (lat, lon), title, snippet));
      populate ();
    }

    public void AddAPRSObjectList ()
    {
      long now = System.currentTimeMillis ();
      Cursor cursor = null;
      
      items.clear ();
      
      if ((database == null) || !database.isOpen ())
      {
        APRSDatabase databaseHelper = new APRSDatabase (APRS.this);
        database = databaseHelper.getReadableDatabase ();
      }
      
      try
      {
        cursor = database.query (APRSDatabase.T_OBJECTS,
                                 APRSDatabase.columnsListOverlayItem,
                                 null, null, 
                                 null, null, APRSDatabase.S_OBJECTS_TIME_ASC);
        
        if (APRSDebug.databaseInfo)
          Log.d (TAG, cursor.getCount () + " objects on overlay");
        
        if (cursor.moveToFirst ())
        {
          final Canvas mCanvas = new Canvas ();
          final Paint paint = new Paint ();
          paint.setColor (Color.WHITE);
          paint.setAntiAlias (false);
          paint.setTypeface (Typeface.SANS_SERIF);
          paint.setTextSize (11);
          paint.setTextAlign (Paint.Align.CENTER);
          paint.setStyle (Paint.Style.STROKE);
          
          do
          {
            final String callsign = cursor.getString (cursor.getColumnIndex (APRSDatabase.C_CALLSIGN));
            GeoPoint mGeoPoint = new GeoPoint (cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_LATITUDE)), cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_LONGITUDE)));
            OverlayItem mOverlayItem = new OverlayItem (mGeoPoint, callsign, callsign);
            
            if (APRSDebug.debugWithCallsign)
              if (callsign.startsWith (APRSDebug.debugCallsign))
                Log.d (TAG, "Updating overlay for " + callsign + ", hasWeather=" + cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_HAS_WEATHER)));
            
            StringBuffer key = new StringBuffer (2);
            key.append ((char) cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_SYMBOL_TABLE)));
            key.append ((char) cursor.getInt (cursor.getColumnIndex (APRSDatabase.C_SYMBOL_CODE)));
            
            if (key.length () == 2)
            {
              String keyAsString = key.toString ();
              
              if (iconMap.containsKey (keyAsString))
              {
                final Drawable marker = getResources ().getDrawable (iconMap.get (keyAsString));
                boundCenterBottom (marker);
                mOverlayItem.setMarker (marker);
              }
              else if (iconCache.containsKey (keyAsString))
              {
                mOverlayItem.setMarker ((BitmapDrawable) iconCache.get (keyAsString));
              }
              else if (Character.isLetter (key.charAt (0)) || Character.isDigit (key.charAt (0)))
              {
                Character overlayCharacter = key.charAt (0);
                key.replace (0, 1, "\\");
                
                if (iconMap.containsKey (key.toString ()))
                {
                  try
                  {
                    Bitmap mBitmap = BitmapFactory.decodeResource (getResources (), iconMap.get (key.toString ()));
                    mBitmap = mBitmap.copy (mBitmap.getConfig (), true);
                    mCanvas.setBitmap (mBitmap);
                    mCanvas.drawText (overlayCharacter.toString (), 8, 12, paint);
                    BitmapDrawable mBitmapDrawable = new BitmapDrawable (mBitmap);
                    boundCenterBottom (mBitmapDrawable);
                    mOverlayItem.setMarker (mBitmapDrawable);
                    iconCache.put (keyAsString, mBitmapDrawable);
                    Log.i (TAG, "Should be overlaying a '" + overlayCharacter + "' on to " + callsign + " as " + key.toString ());
                  }
                  catch (Exception e)
                  {
                    e.printStackTrace ();
                  }
                }
                else
                  Log.w (TAG, "Unsupported symbol table/code " + key.toString () + " (" + callsign + ")");
              }
              else
                Log.w (TAG, "Unsupported symbol table/code " + key.toString () + " (" + callsign + ")");
            }
            
            items.add (mOverlayItem);
          } 
          while (cursor.moveToNext ());
        }
      }
      finally
      {
        if (cursor != null)
          cursor.close ();
      }
      
      if (APRSDebug.timeAPRSObjectsOverlay)
        Log.d (TAG, "AddAPRSObjectList() (before populate()) took " + (System.currentTimeMillis () - now) + "ms");

      populate ();
      
      if (APRSDebug.timeAPRSObjectsOverlay)
        Log.d (TAG, "AddAPRSObjectList() took " + (System.currentTimeMillis () - now) + "ms");
    }
    
    @Override
    protected OverlayItem createItem (int i)
    {
      return (items.get (i));
    }

    @Override
    public void draw (Canvas canvas, MapView mapView, boolean shadow)
    {
      super.draw (canvas, mapView, shadow);
    }

    @Override
    protected boolean onTap (int i)
    {
      long now = System.currentTimeMillis ();
      APRSObject ao = null;
      Cursor cursor = null;
      
      if ((database == null) || !database.isOpen ())
      {
        APRSDatabase databaseHelper = new APRSDatabase (APRS.this);
        database = databaseHelper.getReadableDatabase ();
      }
      
      try
      {
        final String[] callsignArray = new String[] { items.get (i).getSnippet () };
        
        cursor = database.query (APRSDatabase.T_OBJECTS,
                                 APRSDatabase.columnsListAll,
                                 APRSDatabase.C_CALLSIGN + "=?", callsignArray, 
                                 null, null, null);
        
        if (cursor.moveToFirst ())
          ao = new APRSObject (cursor);
      }
      finally
      {
        if (cursor != null)
          cursor.close ();
      }
      
      if (ao != null)
      {
        Intent myIntent = new Intent (getApplicationContext (), ActivityObject.class);
        
        myIntent.putExtra ("com.tinymicros.aprsdroid.object.callsign", ao.getCallsign ());
        myIntent.putExtra ("com.tinymicros.aprsdroid.object.info", ao.getObjectInfoText ());
        myIntent.putExtra ("com.tinymicros.aprsdroid.object.latitude", ao.getLatitude ());
        myIntent.putExtra ("com.tinymicros.aprsdroid.object.longitude", ao.getLongitude ());
        
        startActivityForResult (myIntent, ACTIVITYTYPE_OBJECT);
      }
      else
        Toast.makeText (getApplicationContext (), "Error retrieving " + items.get (i).getSnippet () + " from database", Toast.LENGTH_LONG).show ();
      
      if (APRSDebug.timeOnTap)
        Log.d (TAG, "onTap() took " + (System.currentTimeMillis () - now) + "ms");
      
      return true;
    }

    @Override
    public int size ()
    {
      return items.size ();
    }
  }

  private class MyMyLocationOverlay extends MyLocationOverlay
  {
    private Context context;
    
    MyMyLocationOverlay (Context context, MapView mapView)
    {
      super (context, mapView);
      this.context = context;
    }
    
    @Override
    protected boolean dispatchTap ()
    {
      Intent myIntent = new Intent (context, ActivityMyself.class);
      myIntent.putExtra ("com.tinymicros.aprsdroid.myself.info", updateMyselfInfo ());
      startActivityForResult (myIntent, ACTIVITYTYPE_MYSELF);
      return true;
    }
  }
  
  private void createMap ()
  {
    GeoPoint startingPoint = new GeoPoint (-122084095, 37422006); // 34.191216, -83.941269
    Location lastLocation;
    LocationManager mLocationManager = (LocationManager) getSystemService (LOCATION_SERVICE);

    mMapView = (MapView) findViewById (R.id.map);
    
    ViewGroup zoom = (ViewGroup) findViewById (R.id.zoom);
    zoom.addView (mMapView.getZoomControls ());

    if ((lastLocation = mLocationManager.getLastKnownLocation (LocationManager.GPS_PROVIDER)) != null)
      startingPoint = getPoint (lastLocation.getLatitude(), lastLocation.getLongitude ());
      
    mMapView.getController ().setCenter (startingPoint);
    mMapView.getController ().setZoom (7);

    mMyLocationOverlay = new MyMyLocationOverlay (this, mMapView);
    mMyLocationOverlay.runOnFirstFix (new Runnable () 
    { 
      public void run() 
      {
        mMapView.getController ().animateTo (mMyLocationOverlay.getMyLocation ());
        mMapView.getController().setZoom (16);
      }
    });
    mMapView.getOverlays().add (mMyLocationOverlay);
    mMapView.setClickable (true);
    mMapView.setEnabled (true);

    Drawable marker = getResources ().getDrawable (R.drawable.marker);
    marker.setBounds (0, 0, marker.getIntrinsicWidth (), marker.getIntrinsicHeight ());

    mAPRSObjectsOverlay = new APRSObjectsOverlay (marker);
    mMapView.getOverlays ().add (mAPRSObjectsOverlay);
    mAPRSObjectsOverlay.AddAPRSObjectList ();
  }
  
  //
  //
  //
  private void checkForNotificationReply ()
  {
    Intent mIntent = getIntent ();
    
    if (mIntent.getBooleanExtra ("com.tinymicros.aprsdroid.message.fromnotificationactivity", false))
    {
      String messageCallsign = mIntent.getStringExtra ("com.tinymicros.aprsdroid.message.callsign");
      String messageText = mIntent.getStringExtra ("com.tinymicros.aprsdroid.message.text");
      boolean messageAcknowledge = mIntent.getBooleanExtra ("com.tinymicros.aprsdroid.message.acknowledge", false);
      
      mServiceMessage.addCMA (messageCallsign, messageText, messageAcknowledge);
    }
  }
  
  //
  //
  //
  private class serviceMessage 
  {
    private LinkedList<Object> mServiceMessageQueue;

    serviceMessage ()
    {
      mServiceMessageQueue = new LinkedList<Object> ();
    }
    
    public void addCMA (String callsign, String message, boolean acknowledge)
    {
      HashMap<String,String> m = new HashMap<String,String> ();
      
      m.put ("cs", callsign);
      m.put ("ms", message);
      m.put ("ak", acknowledge ? "t" : "f");
      
      mServiceMessageQueue.add (m);
      
      if (mService != null)
        sendAll ();
    }
    
    @SuppressWarnings("unchecked")
    public void sendAll ()
    {
      while (mServiceMessageQueue.size () > 0)
      {
        try
        {
          HashMap<String,String> hm = (HashMap<String,String>) mServiceMessageQueue.remove ();
         
          mService.sendMessage (hm.get ("cs"), hm.get ("ms"), hm.get ("ak").equalsIgnoreCase ("t") ? true : false);
        }
        catch (NoSuchElementException e)
        {
        }
        catch (RemoteException e)
        {
        }
      }
    }
  }
  
  //
  //
  //
  private void setScreenOrientation ()
  {
    setRequestedOrientation (mAPRSPreferences.cfgOrientation ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  private void jumpToMyselfOnMap ()
  {
    if (mMyLocationOverlay.getMyLocation () != null)
    {
      updateMap ();
      mMapView.getController ().setCenter (mMyLocationOverlay.getMyLocation ());
      mMapView.getController ().setZoom (14);
    }
    else
      Toast.makeText (this, "I don't seem to know where I am!", Toast.LENGTH_LONG).show ();
  }
  
  static
  {
    //
    //   / - Primary Symbol Table (mostly stations)
    //   \ - Alternate Symbol Table (mostly Objects)
    // 0-9 - Numeric overlay. Symbol from Alternate Symbol Table (uncompressed lat/long data format)
    // a-j - Numeric overlay. Symbol from Alternate Symbol Table (compressed lat/long data format only). i.e. a-j maps to 0-9
    // A-Z - Alpha overlay. Symbol from Alternate Symbol Table
    //
    iconMap = new HashMap<String, Integer> ();
    
    //
    //  Primary symbol table
    //
    iconMap.put ("/!", R.drawable.icon_47_033);
    iconMap.put ("/#", R.drawable.icon_47_035);
    iconMap.put ("/$", R.drawable.icon_47_036);
    iconMap.put ("/%", R.drawable.icon_47_037);
    iconMap.put ("/&", R.drawable.icon_47_038);
    iconMap.put ("/'", R.drawable.icon_47_039);
    iconMap.put ("/(", R.drawable.icon_47_040);
    iconMap.put ("/*", R.drawable.icon_47_042);
    iconMap.put ("/+", R.drawable.icon_47_043);
    iconMap.put ("/,", R.drawable.icon_47_044);
    iconMap.put ("/-", R.drawable.icon_47_045);
    iconMap.put ("/.", R.drawable.icon_47_046);
    iconMap.put ("//", R.drawable.icon_47_047);
    iconMap.put ("/0", R.drawable.icon_47_048);
    iconMap.put ("/1", R.drawable.icon_47_049);
    iconMap.put ("/2", R.drawable.icon_47_050);
    iconMap.put ("/3", R.drawable.icon_47_051);
    iconMap.put ("/4", R.drawable.icon_47_052);
    iconMap.put ("/6", R.drawable.icon_47_054);
    iconMap.put ("/7", R.drawable.icon_47_055);
    iconMap.put ("/8", R.drawable.icon_47_056);
    iconMap.put ("/9", R.drawable.icon_47_057);
    iconMap.put ("/;", R.drawable.icon_47_059);
    iconMap.put ("/<", R.drawable.icon_47_060);
    iconMap.put ("/=", R.drawable.icon_47_061);
    iconMap.put ("/>", R.drawable.icon_47_062);
    iconMap.put ("/?", R.drawable.icon_47_063);
    iconMap.put ("/@", R.drawable.icon_47_064);
    iconMap.put ("/A", R.drawable.icon_47_065);
    iconMap.put ("/B", R.drawable.icon_47_066);
    iconMap.put ("/C", R.drawable.icon_47_067);
    iconMap.put ("/E", R.drawable.icon_47_069);
    iconMap.put ("/G", R.drawable.icon_47_071);
    iconMap.put ("/H", R.drawable.icon_47_072);
    iconMap.put ("/I", R.drawable.icon_47_073);
    iconMap.put ("/K", R.drawable.icon_47_075);
    iconMap.put ("/L", R.drawable.icon_47_076);
    iconMap.put ("/M", R.drawable.icon_47_077);
    iconMap.put ("/N", R.drawable.icon_47_078);
    iconMap.put ("/O", R.drawable.icon_47_079);
    iconMap.put ("/P", R.drawable.icon_47_080);
    iconMap.put ("/R", R.drawable.icon_47_082);
    iconMap.put ("/S", R.drawable.icon_47_083);
    iconMap.put ("/T", R.drawable.icon_47_084);
    iconMap.put ("/U", R.drawable.icon_47_085);
    iconMap.put ("/V", R.drawable.icon_47_086);
    iconMap.put ("/W", R.drawable.icon_47_087);
    iconMap.put ("/X", R.drawable.icon_47_088);
    iconMap.put ("/Y", R.drawable.icon_47_089);
    iconMap.put ("/Z", R.drawable.icon_47_090);
    iconMap.put ("/[", R.drawable.icon_47_091);
    iconMap.put ("/\\", R.drawable.icon_47_092);
    iconMap.put ("/]", R.drawable.icon_47_093);
    iconMap.put ("/^", R.drawable.icon_47_094);
    iconMap.put ("/_", R.drawable.icon_47_095);
    iconMap.put ("/`", R.drawable.icon_47_096);
    iconMap.put ("/a", R.drawable.icon_47_097);
    iconMap.put ("/b", R.drawable.icon_47_098);
    iconMap.put ("/c", R.drawable.icon_47_099);
    iconMap.put ("/d", R.drawable.icon_47_100);
    iconMap.put ("/e", R.drawable.icon_47_101);
    iconMap.put ("/f", R.drawable.icon_47_102);
    iconMap.put ("/g", R.drawable.icon_47_103);
    iconMap.put ("/h", R.drawable.icon_47_104);
    iconMap.put ("/i", R.drawable.icon_47_105);
    iconMap.put ("/j", R.drawable.icon_47_106);
    iconMap.put ("/k", R.drawable.icon_47_107);
    iconMap.put ("/l", R.drawable.icon_47_108);
    iconMap.put ("/m", R.drawable.icon_47_109);
    iconMap.put ("/n", R.drawable.icon_47_110);
    iconMap.put ("/o", R.drawable.icon_47_111);
    iconMap.put ("/p", R.drawable.icon_47_112);
    iconMap.put ("/q", R.drawable.icon_47_113);
    iconMap.put ("/r", R.drawable.icon_47_114);
    iconMap.put ("/s", R.drawable.icon_47_115);
    iconMap.put ("/t", R.drawable.icon_47_116);
    iconMap.put ("/u", R.drawable.icon_47_117);
    iconMap.put ("/v", R.drawable.icon_47_118);
    iconMap.put ("/w", R.drawable.icon_47_119);
    iconMap.put ("/x", R.drawable.icon_47_120);
    iconMap.put ("/y", R.drawable.icon_47_121);
    
    //
    //  Alternate symbol table
    //
    iconMap.put ("\\!", R.drawable.icon_92_033);
    iconMap.put ("\\#", R.drawable.icon_92_035);
    iconMap.put ("\\$", R.drawable.icon_92_036);
    iconMap.put ("\\&", R.drawable.icon_92_038);
    iconMap.put ("\\'", R.drawable.icon_92_039);
    iconMap.put ("\\(", R.drawable.icon_92_040);
    iconMap.put ("\\*", R.drawable.icon_92_042);
    iconMap.put ("\\+", R.drawable.icon_92_043);
    iconMap.put ("\\,", R.drawable.icon_92_044);
    iconMap.put ("\\-", R.drawable.icon_92_045);
    iconMap.put ("\\.", R.drawable.icon_92_046);
    iconMap.put ("\\0", R.drawable.icon_92_048);
    iconMap.put ("\\9", R.drawable.icon_92_057);
    iconMap.put ("\\;", R.drawable.icon_92_059);
    iconMap.put ("\\<", R.drawable.icon_92_060);
    iconMap.put ("\\>", R.drawable.icon_92_062);
    iconMap.put ("\\?", R.drawable.icon_92_063);
    iconMap.put ("\\@", R.drawable.icon_92_064);
    iconMap.put ("\\A", R.drawable.icon_92_065);
    iconMap.put ("\\B", R.drawable.icon_92_066);
    iconMap.put ("\\C", R.drawable.icon_92_067);
    iconMap.put ("\\D", R.drawable.icon_92_068);
    iconMap.put ("\\E", R.drawable.icon_92_069);
    iconMap.put ("\\F", R.drawable.icon_92_070);
    iconMap.put ("\\G", R.drawable.icon_92_071);
    iconMap.put ("\\I", R.drawable.icon_92_073);
    iconMap.put ("\\J", R.drawable.icon_92_074);
    iconMap.put ("\\K", R.drawable.icon_92_075);
    iconMap.put ("\\L", R.drawable.icon_92_076);
    iconMap.put ("\\N", R.drawable.icon_92_078);
    iconMap.put ("\\P", R.drawable.icon_92_080);
    iconMap.put ("\\Q", R.drawable.icon_92_081);
    iconMap.put ("\\R", R.drawable.icon_92_082);
    iconMap.put ("\\S", R.drawable.icon_92_083);
    iconMap.put ("\\T", R.drawable.icon_92_084);
    iconMap.put ("\\U", R.drawable.icon_92_085);
    iconMap.put ("\\V", R.drawable.icon_92_086);
    iconMap.put ("\\W", R.drawable.icon_92_087);
    iconMap.put ("\\X", R.drawable.icon_92_088);
    iconMap.put ("\\[", R.drawable.icon_92_091);
    iconMap.put ("\\^", R.drawable.icon_92_094);
    iconMap.put ("\\_", R.drawable.icon_92_095);
    iconMap.put ("\\`", R.drawable.icon_92_096);
    iconMap.put ("\\a", R.drawable.icon_92_097);
    iconMap.put ("\\b", R.drawable.icon_92_098);
    iconMap.put ("\\c", R.drawable.icon_92_099);
    iconMap.put ("\\d", R.drawable.icon_92_100);
    iconMap.put ("\\g", R.drawable.icon_92_103);
    iconMap.put ("\\h", R.drawable.icon_92_104);
    iconMap.put ("\\i", R.drawable.icon_92_105);
    iconMap.put ("\\j", R.drawable.icon_92_106);
    iconMap.put ("\\l", R.drawable.icon_92_108);
    iconMap.put ("\\m", R.drawable.icon_92_109);
    iconMap.put ("\\n", R.drawable.icon_92_110);
    iconMap.put ("\\o", R.drawable.icon_92_111);
    iconMap.put ("\\p", R.drawable.icon_92_112);
    iconMap.put ("\\r", R.drawable.icon_92_114);
    iconMap.put ("\\s", R.drawable.icon_92_115);
    iconMap.put ("\\t", R.drawable.icon_92_116);
    iconMap.put ("\\u", R.drawable.icon_92_117);
    iconMap.put ("\\v", R.drawable.icon_92_118);
    iconMap.put ("\\w", R.drawable.icon_92_119);
    iconMap.put ("\\y", R.drawable.icon_92_121);
    
    //
    // Frequently used overlays
    //
    iconMap.put ("I#", R.drawable.icon_92_035_i);
    iconMap.put ("N#", R.drawable.icon_92_035_n);
    iconMap.put ("S#", R.drawable.icon_92_035_s);
    iconMap.put ("D&", R.drawable.icon_92_038_d);
    iconMap.put ("I&", R.drawable.icon_92_038_i);
    iconMap.put ("N&", R.drawable.icon_92_038_n);
    iconMap.put ("Da", R.drawable.icon_92_097_d);
  }
}