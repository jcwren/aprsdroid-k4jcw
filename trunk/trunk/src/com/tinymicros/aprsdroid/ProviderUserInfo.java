//
//  $Id: ActivitySettings.java 39 2008-12-06 22:22:37Z jcw $
//  $Revision: 39 $
//  $Date: 2008-12-06 17:22:37 -0500 (Sat, 06 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprs/ActivitySettings.java $
//

package com.tinymicros.aprsdroid;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class ProviderUserInfo extends ContentProvider
{
  private static final int OBJECT = 1;
  private static final int OBJECT_ID = 2;

  private static APRSDatabase databaseHelper = null;
  private static final UriMatcher sUriMatcher;
  
  @Override
  public boolean onCreate ()
  {
    databaseHelper = new APRSDatabase (getContext ());
    return true;
  }

  @Override
  public String getType (Uri uri)
  {
    switch (sUriMatcher.match (uri))
    {
      case OBJECT :
        return APRSDatabase.CONTENT_TYPE;

      case OBJECT_ID :
        return APRSDatabase.CONTENT_ITEM_TYPE;
        
      default :
        throw new IllegalArgumentException ("Unknown URI " + uri);
    }
  }
  
  @Override
  public Cursor query (Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
  {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder ();

    switch (sUriMatcher.match (uri))
    {
      case OBJECT :
        qb.setTables (APRSDatabase.T_OBJECTS);
        qb.setProjectionMap (APRSDatabase.projectionMap);
        break;

      case OBJECT_ID :
        qb.setTables (APRSDatabase.T_OBJECTS);
        qb.setProjectionMap (APRSDatabase.projectionMap);
        qb.appendWhere (APRSDatabase._ID + "=" + uri.getPathSegments ().get (1));
        break;

      default :
        throw new IllegalArgumentException ("Unknown URI " + uri);
    }

    String orderBy = TextUtils.isEmpty (sortOrder) ? APRSDatabase.S_OBJECTS_CALLSIGN_ASC : sortOrder;

    SQLiteDatabase database = databaseHelper.getReadableDatabase ();
    Cursor c = qb.query (database, projection, selection, selectionArgs, null, null, orderBy);

    c.setNotificationUri (getContext ().getContentResolver (), uri);
    return c;
  }

  //
  //  Don't need these, since the content provider is only used by ActivityFindStation
  //
  @Override
  public Uri insert (Uri uri, ContentValues initialValues) 
  {
    return null;
  }
  
  @Override
  public int delete (Uri uri, String where, String[] whereArgs) 
  {
    return 0;
  }

  @Override
  public int update (Uri uri, ContentValues values, String selection, String[] selectionArgs)
  {
    return 0;
  }
  
  static 
  {
    sUriMatcher = new UriMatcher (UriMatcher.NO_MATCH);
    sUriMatcher.addURI ("com.tinymicros.aprsdroid", "object", OBJECT);
    sUriMatcher.addURI ("com.tinymicros.aprsdroid", "object/#", OBJECT_ID);
  }
}