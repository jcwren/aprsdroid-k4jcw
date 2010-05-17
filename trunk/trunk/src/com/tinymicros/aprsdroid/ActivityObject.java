//
//  $Id: ActivitySettings.java 39 2008-12-06 22:22:37Z jcw $
//  $Revision: 39 $
//  $Date: 2008-12-06 17:22:37 -0500 (Sat, 06 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprs/ActivitySettings.java $
//

package com.tinymicros.aprsdroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ActivityObject extends Activity
{
  public static final int ACTIVITYCODE_CANCEL = 1;
  public static final int ACTIVITYCODE_CENTERONLATLON = 2;
  public static final int ACTIVITYCODE_SENDMESSAGE = 3;
  
  private static final String TAG = "ActivityObject";
  
  private String objectCallsign = null;
  private String objectInfo = null;
  private int objectLatitude;
  private int objectLongitude;
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    Intent mIntent = getIntent ();
    
    if (mIntent == null)
      Log.e (TAG, "mIntent was null!");
    else 
    {
      objectCallsign = mIntent.getStringExtra ("com.tinymicros.aprsdroid.object.callsign");
      objectInfo = mIntent.getStringExtra ("com.tinymicros.aprsdroid.object.info");
      objectLatitude = mIntent.getIntExtra ("com.tinymicros.aprsdroid.object.latitude", -1);
      objectLongitude = mIntent.getIntExtra ("com.tinymicros.aprsdroid.object.longitude", -1);
    }

    if (objectCallsign == null)
      objectCallsign = "(from unknown)";
    if (objectInfo == null)
      objectInfo = "(no message body)";
    
    LayoutInflater vi = (LayoutInflater) getSystemService (Context.LAYOUT_INFLATER_SERVICE);
    View view = vi.inflate (R.layout.object, null);
    TextView tv = (TextView) view.findViewById (R.id.objectInfo);
    tv.setText (objectInfo);
    setContentView (view);
    
    findViewById (R.id.objectReturnToMap).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_CANCEL);
        finish ();
      }
    });
    
    findViewById (R.id.objectCenterAndZoom).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {      
        setResult (ACTIVITYCODE_CENTERONLATLON, (new Intent ()
          .putExtra ("com.tinymicros.aprsdroid.object.latitude", objectLatitude)
          .putExtra ("com.tinymicros.aprsdroid.object.longitude", objectLongitude)));
        finish ();
      }
    });
    
    findViewById (R.id.objectSendMessage).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_SENDMESSAGE, (new Intent ()
          .putExtra ("com.tinymicros.aprsdroid.message.callsign", objectCallsign)));
        finish ();
      }
    });
  }
}