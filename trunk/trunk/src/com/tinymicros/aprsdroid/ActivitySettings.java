//
//  $Id: ActivitySettings.java 67 2008-12-13 03:21:34Z jcw $
//  $Revision: 67 $
//  $Date: 2008-12-12 22:21:34 -0500 (Fri, 12 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprsdroid/ActivitySettings.java $
//

package com.tinymicros.aprsdroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class ActivitySettings extends PreferenceActivity
{
  private static final String TAG = "ActivitySettings";
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onCreate called");
    
    addPreferencesFromResource (R.xml.settings);
  }
}
