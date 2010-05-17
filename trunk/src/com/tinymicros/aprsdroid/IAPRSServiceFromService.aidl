package com.tinymicros.aprsdroid;

interface IAPRSServiceFromService {
	String getStatusMessage ();
	String getMyselfInfo ();
	void sendMessage (String callsign, String text, boolean acknowledge);
}