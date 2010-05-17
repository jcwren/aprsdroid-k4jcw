package com.tinymicros.aprsdroid;

public final class APRSDebug
{
  //
  //  Display Log.d messages regarding database transactions
  //
  public static final boolean databaseInfo = true;
  public static final boolean databaseInsertMerge = false;
  public static final boolean databaseScrub = false;
  public static final boolean databaseFilter = false;

  //
  //  Timing messages
  //
  public static final boolean timeOnTap = true;
  public static final boolean timeDatabaseScrub = true;
  public static final boolean timeDatabaseFilter = true;
  public static final boolean timeAPRSObjectsOverlay = true;
  public static final boolean timeUpdateDistances = true;
  
  //
  // Enables Log.d messages for onCreate, onDestroy, etc messages in APRS and
  // APRSService.
  //
  public static final boolean trackCreateResumeEtc = true;

  //
  //
  //
  public static final boolean showDiscards = true;
  
  //
  // Enables Log.d output of exchanges between us and server
  //
  public static final boolean serverTalk = true;
  public static final boolean serverTransmit = true;
  public static final boolean serverReceive = true;
  public static final boolean serverSummary = true;

  //
  // When enabled, the selected callsign's APRSObject record is printed when a
  // database transaction occurs.
  //
  public static final boolean debugWithCallsign = false;
  public static final String debugCallsign = "W4EPI-4";
}
