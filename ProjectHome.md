APRSDroid is an APRS client for Android.  APRS message handling is done through the openaprs.net server (this requires registration with a valid amateur radio license, and is free).

APRSDroid runs as a service, and handles position reporting, displaying the location of other stations, sending and receiving messages.  A number of configuration options exist, such as selecting update rates, reporting objects only within a certain distance, age of objects, etc.

The beta version was last worked on in December of 2008, after which I got distracted by other shiny objects.  It is basically complete, with one bug (sending a message occasionally causes a hard crash of the application), and one missing feature.

The missing feature is basically what caused me to lose interest, as I couldn't appear to accomplish what I wanted.  This is the ability to select an object on the map and bring up an action menu, as one can do with Google Maps.  There was a lot of traffic in the forums at the time about others also wanting to do this, and not being able to accomplish it.

It was originally written under Android 1.5.  1.6 came out a few months later, and the same binary continues to run.  It is untested under later versions, as 1.6 is the latest version on the G1, and I can't afford a N1.

As I went to register this project, it appears that someone else has co-opted the name @ http://github.com/ge0rg/aprsdroid :)  I had to make a quick decision to rename this to 'aprsdroid-k4jcw'