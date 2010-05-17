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
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ActivitySendMessage extends Activity
{
  public static final int ACTIVITYCODE_SEND = 1;
  public static final int ACTIVITYCODE_CANCEL = 2;
  
  private static final String TAG = "ActivitySendMessage";
  
  private String targetCallsign;
  private EditText messageCallsign;
  private EditText messageText;
  private CheckBox messageAcknowledge;
  private boolean startedByNotification = false;
  
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    if (APRSDebug.trackCreateResumeEtc)
      Log.d (TAG, "onCreate called");
    
    Intent mIntent = getIntent ();
    
    if (mIntent == null)
    {
      Log.e (TAG, "mIntent was null!");
      setResult (ACTIVITYCODE_CANCEL);
      finish ();
      return;
    }
    
    startedByNotification = mIntent.getBooleanExtra ("com.tinymicros.aprsdroid.message.fromnotificationactivity", false);
    
    if ((targetCallsign = mIntent.getStringExtra ("com.tinymicros.aprsdroid.message.callsign")) == null)
      targetCallsign = "";
    
    LayoutInflater vi = (LayoutInflater) getSystemService (Context.LAYOUT_INFLATER_SERVICE);
    View view = vi.inflate (R.layout.send_message, null);
    
    messageCallsign = (EditText) view.findViewById (R.id.sendMessageCallsign);
    messageCallsign.setFilters (new InputFilter[] { new CallsignFilter (), new InputFilter.LengthFilter (9) } );
    messageCallsign.setText (targetCallsign);
    messageText = (EditText) view.findViewById (R.id.sendMessageText);
    messageText.setFilters (new InputFilter[] { new MessageFilter (), new InputFilter.LengthFilter (67) } );
    messageText.requestFocus ();
    messageAcknowledge = (CheckBox) view.findViewById (R.id.sendMessageAcknowledge);
    
    setContentView (view);
  
    findViewById (R.id.sendMessageSend).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        final String callsign = messageCallsign.getText ().toString ();
        final String message = messageText.getText ().toString ();
        
        if ((callsign == null) || (callsign.length () < 3))
          Toast.makeText (ActivitySendMessage.this, "Callsign must be at least 3 characters", Toast.LENGTH_LONG).show ();
        else if ((message == null) || (message.length () < 1))
          Toast.makeText (ActivitySendMessage.this, "Message must be at least 1 character", Toast.LENGTH_LONG).show ();
        else
        {
          Intent mIntent = new Intent ();
          
          mIntent.putExtra ("com.tinymicros.aprsdroid.message.callsign", callsign);
          mIntent.putExtra ("com.tinymicros.aprsdroid.message.text", message);
          mIntent.putExtra ("com.tinymicros.aprsdroid.message.acknowledge", messageAcknowledge.isChecked ());
          
          if (!startedByNotification)
            setResult (ACTIVITYCODE_SEND, mIntent);
          else
          {
            mIntent.putExtra ("com.tinymicros.aprsdroid.message.fromnotificationactivity", true);
            mIntent.setClass (ActivitySendMessage.this, APRS.class);
            startActivity (mIntent);
          }
          
          finish ();
        }
      }
    });
    
    findViewById (R.id.sendMessageCancel).setOnClickListener (new Button.OnClickListener ()
    {
      public void onClick (View v)
      {
        setResult (ACTIVITYCODE_CANCEL);
        finish ();
      }
    });
  }
  
  private class CallsignFilter implements InputFilter 
  {
    @Override
    public CharSequence filter (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) 
    {
      SpannableStringBuilder stripped = null;

      for (int i = end - 1; i >= start; i--) 
      {
          char c = source.charAt (i);

          if (!Character.isDigit (c) && !Character.isLetter (c) && (c != '-'))
          {
              if (end == start + 1)
                  return "";

              if (stripped == null)
                  stripped = new SpannableStringBuilder (source, start, end);

              stripped.delete (i - start, i + 1 - start);
          }
      }

      if (stripped != null)
        return stripped;
      
      return null;
    }
  }

  private class MessageFilter implements InputFilter 
  {
    @Override
    public CharSequence filter (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) 
    {
      SpannableStringBuilder stripped = null;

      for (int i = end - 1; i >= start; i--) 
      {
          char c = source.charAt (i);

          if ((c < ' ') || (c > '~') || (c == '|') || (c == '{') || (c == '~'))
          {
              if (end == start + 1)
                  return "";

              if (stripped == null)
                  stripped = new SpannableStringBuilder (source, start, end);

              stripped.delete (i - start, i + 1 - start);
          }
      }

      if (stripped != null)
        return stripped;
      
      return null;
    }
  }
}