//
//  $Id: ActivitySettings.java 39 2008-12-06 22:22:37Z jcw $
//  $Revision: 39 $
//  $Date: 2008-12-06 17:22:37 -0500 (Sat, 06 Dec 2008) $
//  $Author: jcw $
//  $HeadURL: http://tinymicros.com/svn_private/java/gaprsmap/trunk/src/com/tinymicros/aprs/ActivitySettings.java $
//

package com.tinymicros.aprsdroid;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

  /** 
  * This class is a helper class for functionality that
  * should usually be inside {@link SimpleCursorAdapter}. It provides a 
  * filterable Cursoradapter which can be used in an 
  * {@link AutoCompleteTextView}. It expects that the data is accessible via 
  * a {@link ContentURI} (and thus a {@link ContentProvider}). 
  * Furthermore, it expects a projection map with 2 entries: 
  * <ul>  
  * <li>a text column containing the item to show (e.g. in an 
  * {@link AutoCompleteTextView}</li>  
  * <li>the _ID column which is always necessary according to 
  * the docs found in {@link CursorAdapter}</li>  
  * </ul>  
  * @author Rainer Burgstaller (http://rainer.4950.net) 
  */ 
  public class SmartCursorAdapter extends SimpleCursorAdapter
  {
    public static int FILTERMODE_STARTSWITH = 0;
    public static int FILTERMODE_CONTAINS = 1;

    private ContentResolver m_contentResolver;
    private Uri m_filterURI;
    private String m_sortOrder;
    private String[] m_projection;
    private int m_filterMode = FILTERMODE_STARTSWITH;

    /** 
    * constructor to create a smart adapter 
    * @param context see description of 
    * {@link SimpleCursorAdapter#SimpleCursorAdapter(Context, int, Cursor, String[], int[])} 
    * @param layout see description of 
    * {@link SimpleCursorAdapter#SimpleCursorAdapter(Context, int, Cursor, String[], int[])} 
    * @param c the cursor for the normal list (non filtered) 
    * @param from the default projection mapping. It assumes that the first 
    * column (index 0) is of type string and contains the string 
    * to perform the filtering on and which is also to be displayed. 
    * Furthermore, it assumes that the second column is the _ID column. 
    * @param to the view resource id to which the text should be bound. 
    * for {@link AutoCompleteTextView} this should be 
    * <em>android.R.layout.simple_list_item_1</em>  
    * @param filterUri the URI of the content to use for a query 
    * @param sortOrder the default sort order of the filtered content 
    * (used for making queries to the {@link ContentProvider}.query) 
    */ 
    public SmartCursorAdapter (Context context, int layout, Cursor c, String[] from, int[] to, Uri filterUri, String sortOrder)
    {
      super (context, layout, c, from, to);
      
      m_contentResolver = context.getContentResolver ();
      m_filterURI = filterUri;
      m_sortOrder = sortOrder;
      m_projection = from;
    }

    @Override
    public Cursor runQueryOnBackgroundThread (CharSequence constraint)
    {
      StringBuilder buffer = null;
      String[] args = null;
      
      if (constraint != null)
      {
        buffer = new StringBuilder ();
        buffer.append ("UPPER(");
        buffer.append (m_projection [0]);
        buffer.append (") GLOB ?");
        String filter = constraint.toString ().toUpperCase () + "*";
        
        if (m_filterMode == FILTERMODE_CONTAINS)
          filter = "*" + filter;
        
        args = new String [] { filter };
      }

      return m_contentResolver.query (m_filterURI, m_projection, buffer == null ? null : buffer.toString (), args, m_sortOrder);
    }

    @Override
    public String convertToString (Cursor cursor)
    {
      return cursor.getString (0);
    }

    public void setFilterMode (int filterMode)
    {
      m_filterMode = filterMode;
    }
  }
