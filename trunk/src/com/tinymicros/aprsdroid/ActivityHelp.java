//
//  $Id: ActivityAbout.java 68 2008-12-13 03:53:53Z jcw $
//  $Revision: 68 $
//  $Date: 2008-12-12 22:53:53 -0500 (Fri, 12 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprsdroid/ActivityAbout.java $
//

package com.tinymicros.aprsdroid;

import android.app.Activity;
import android.os.Bundle;

public class ActivityHelp extends Activity
{
  private static final String TAG = "Help";

  @Override
  protected void onCreate (Bundle icicle)
  {
    super.onCreate (icicle);
    
    setTheme (android.R.style.Theme_Dialog);
    setContentView (R.layout.help);
  }
}
