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

public class ActivityMyself extends Activity
{
  private static final String TAG = "ActivityMyself";
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    String myselfInfo = "(no information available)";
    Intent i = getIntent ();
    
    if (i == null)
      Log.e (TAG, "i was null!");
    else if ((myselfInfo = i.getStringExtra ("com.tinymicros.aprsdroid.myself.info")) == null)
      Log.e (TAG, "didn't get a parameter");
    
    LayoutInflater vi = (LayoutInflater) getSystemService (Context.LAYOUT_INFLATER_SERVICE);
    View view = vi.inflate (R.layout.myself, null);
    TextView tv = (TextView) view.findViewById (R.id.myselfText);
    tv.setText (myselfInfo);
    setContentView (view);
    
    findViewById (R.id.myselfReturnToMap).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        finish ();
      }
    });
  }
}