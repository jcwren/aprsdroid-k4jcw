//
//  $Id: ActivityAbout.java 68 2008-12-13 03:53:53Z jcw $
//  $Revision: 68 $
//  $Date: 2008-12-12 22:53:53 -0500 (Fri, 12 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprsdroid/ActivityAbout.java $
//

package com.tinymicros.aprsdroid;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ActivityAbout extends Activity
{
  private static final String TAG = "About";

  @Override
  protected void onCreate (Bundle icicle)
  {
    super.onCreate (icicle);
    
    setTheme (android.R.style.Theme_Dialog);
    setContentView (R.layout.about);

    String version = getVersionNumber ();
    String name = getApplicationName ();

    setTitle (getString (R.string.about_title, name));

    TextView text = (TextView) findViewById (R.id.text);
    text.setText (getString (R.string.about_text, version));
  }
  
  private String getVersionNumber ()
  {
    String version = "?";

    try
    {
      PackageInfo pi = getPackageManager ().getPackageInfo (getPackageName (), 0);
      version = pi.versionName;
    }
    catch (PackageManager.NameNotFoundException e)
    {
      Log.e (TAG, "Package name not found", e);
    }

    return version;
  }

  private String getApplicationName ()
  {
    String name = "?";

    try
    {
      PackageInfo pi = getPackageManager ().getPackageInfo (getPackageName (), 0);
      name = getString (pi.applicationInfo.labelRes);
    }
    catch (PackageManager.NameNotFoundException e)
    {
      Log.e (TAG, "Package name not found", e);
    }

    return name;
  }
}
