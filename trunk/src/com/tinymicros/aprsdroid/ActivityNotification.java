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

public class ActivityNotification extends Activity
{
  public static final int ACTIVITYCODE_RETURNTOMAP = 1;
  public static final int ACTIVITYCODE_SENDREPLY = 2;
  public static final int ACTIVITYCODE_CENTERONLATLON = 3;
  
  private static final String TAG = "ActivityNotification";
  
  private String msgFrom = null;
  private String msgTime = null;
  private String msgText = null;
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    Intent mIntent = getIntent ();
    
    if (mIntent == null)
      Log.e (TAG, "i was null!");
    else 
    {
      msgFrom = mIntent.getStringExtra ("com.tinymicros.aprsdroid.notification.callsign");
      msgTime = mIntent.getStringExtra ("com.tinymicros.aprsdroid.notification.time");
      msgText = mIntent.getStringExtra ("com.tinymicros.aprsdroid.notification.text");
    }

    if (msgFrom == null)
      msgFrom = "(from unknown)";
    if (msgTime == null)
      msgTime = "(unknown time)";
    if (msgText == null)
      msgText = "(no message body)";
    
    LayoutInflater vi = (LayoutInflater) getSystemService (Context.LAYOUT_INFLATER_SERVICE);
    View view = vi.inflate (R.layout.msg_notification, null);
    TextView tv = (TextView) view.findViewById (R.id.notificationText);
    tv.setText ("From " + msgFrom + " at " + msgTime + "\n\n" + msgText);
    setContentView (view);
    
    findViewById (R.id.notificationReturnToMap).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_RETURNTOMAP);
        finish ();
      }
    });
    
    findViewById (R.id.notificationReply).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        Intent mIntent = new Intent ();
        mIntent.setClass (ActivityNotification.this, ActivitySendMessage.class);
        mIntent.putExtra ("com.tinymicros.aprsdroid.message.fromnotificationactivity", true);
        mIntent.putExtra ("com.tinymicros.aprsdroid.message.callsign", msgFrom);
        startActivityForResult (mIntent, APRS.ACTIVITYTYPE_SENDMESSAGE);
        finish ();
        //startActivityForResult (new Intent (context, ActivitySendMessage.class)
        //  .putExtra ("com.tinymicros.aprsdroid.message.callsign", msgFrom),
        //  APRS.ACTIVITYTYPE_SENDMESSAGE);
      }
    });
    
    findViewById (R.id.notificationJumpToOnMap).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        finish ();
      }
    });
  }
}