# Introduction #

I originally started this project back in 2008, and made quite a bit of headway in a fairly short amount of time.  After a while (like so many other things), I lost interest and did no further development on it.  Instead of letting the bits on my hard drive, I'm hoping someone might be interested in picking this project up.

# Details #

APRSDroid supports:
  * Sending and receiving position reports via the OpenAPRS.net servers
  * Sending and receiving messages between stations
  * Displaying objects on the map
  * Object filtering, based on distance, age, number of objects, etc.
  * Searching for other stations
  * Various alerts

# What's Missing? #

When a ballon is displayed on Google Maps, touching the ballon will cause a bubble to pop up with additional information.  At the time I wrote APRSDroid, it appears to have been not possible for a user application to implement this same functionality.

As it stands, touching an object on the map brings up a new window with information about it.  I'd really prefer that it popped up a bubble with the station details.  This may now be possible in 1.6 or later.

# What's Broken? #

To the best of my knowledge, there is one bug.  Occasionally when attempting to send a message, the application will crash hard.  I have not looked into what may be causing this.
