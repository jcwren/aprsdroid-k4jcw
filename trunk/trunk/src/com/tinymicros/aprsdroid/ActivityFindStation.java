//
//  $Id: ActivitySettings.java 39 2008-12-06 22:22:37Z jcw $
//  $Revision: 39 $
//  $Date: 2008-12-06 17:22:37 -0500 (Sat, 06 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprs/ActivitySettings.java $
//

package com.tinymicros.aprsdroid;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ActivityFindStation extends ListActivity
{
  public static final int ACTIVITYCODE_CENTERONLATLON = 1;

  private static final String TAG = "ActivityFindStation";
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onCreate called");

    final Cursor c = getContentResolver ().query (APRSDatabase.CONTENT_URI, null, null, null, null);

    startManagingCursor (c);

    final SmartCursorAdapter sca = new SmartCursorAdapter (this, android.R.layout.simple_list_item_1, c, new String [] { APRSDatabase.C_CALLSIGN, APRSDatabase._ID, APRSDatabase.C_LATITUDE, APRSDatabase.C_LONGITUDE }, new int [] { android.R.id.text1 }, APRSDatabase.CONTENT_URI, APRSDatabase.S_OBJECTS_CALLSIGN_ASC);
    
    sca.setFilterMode (SmartCursorAdapter.FILTERMODE_CONTAINS);
    
    final ListAdapter adapter = sca;

    getListView ().clearTextFilter ();
    getListView ().setTextFilterEnabled (true);
    setListAdapter (adapter);
  }

  @Override
  public void onListItemClick (ListView l, View v, int position, long id)
  {
    if (position >= 0)
    {
      Cursor c = (Cursor) l.getItemAtPosition (position);
      
      setResult (ACTIVITYCODE_CENTERONLATLON, (new Intent ()
        .putExtra ("com.tinymicros.aprsdroid.object.latitude", c.getInt (c.getColumnIndex (APRSDatabase.C_LATITUDE)))
        .putExtra ("com.tinymicros.aprsdroid.object.longitude", c.getInt (c.getColumnIndex (APRSDatabase.C_LONGITUDE)))));
      
      finish ();
    }  
  }
}