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

public class ActivityTools extends Activity
{
  public static final int ACTIVITYCODE_CANCEL = 1;
  public static final int ACTIVITYCODE_JUMPTOSELF = 2;
  public static final int ACTIVITYCODE_FINDSTATION = 3;
  public static final int ACTIVITYCODE_SENDMESSAGE = 4;
  
  private static final String TAG = "ActivityTools";
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onCreate called");

    LayoutInflater vi = (LayoutInflater) getSystemService (Context.LAYOUT_INFLATER_SERVICE);
    View view = vi.inflate (R.layout.tools, null);
    setContentView (view);
    
    findViewById (R.id.toolsReturnToMap).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_CANCEL);
        finish ();
      }
    });
    
    findViewById (R.id.toolsJumpToMyself).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_JUMPTOSELF);
        finish ();
      }
    });

    findViewById (R.id.toolsFindStation).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_FINDSTATION);
        finish ();
      }
    });

    findViewById (R.id.toolsSendMessage).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_SENDMESSAGE);
        finish ();
      }
    });

    findViewById (R.id.toolsHelp).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        startActivity (new Intent (getApplicationContext (), ActivityHelp.class));
      }
    });
    
    findViewById (R.id.toolsAbout).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        startActivity (new Intent (getApplicationContext (), ActivityAbout.class));
      }
    });
  }
}