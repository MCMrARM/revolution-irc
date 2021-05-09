Revolution IRC
==============
 
Revolution IRC Client is the next-generation IRC client for Android, made with design and functionality in mind. Let's start this revolution!

<a href="https://f-droid.org/packages/io.mrarm.irc/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=io.mrarm.irc" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

---
Changes in debug build: versionName 0.5.4f versionCode 18 (pickedusername plp 20210508 pl: v4):

* New in "0.5.4f" (18): bad regexps in Server Ignore entries are flagged in the list of Ignore 
  Entries, by displaying a _RED_ `@bad:` string in front of their names in the list. If a settings
  `.zip` file is saved with bad re's in it, it will not be checeked on restoring the `.zip` _but_
  it will be checked and flagged by Toast when the server is opened / connected to. 

* Checkbutton `Restrict CTCP VERSION` in server Edit Settings, defaults to 
  not checked, sends `IRC client:default:Android` when checked, instead of full app name and version
  which may reveal too much.
  
* The Ignore list entries can be glob patterns as before or regexps (new).
* The new `Message text` pattern is matched against the whole message text line.
* The hint text in each ignore entry edit field shows `Any ?glob? or /re/` indicating how the user
  can select regexp vs glob. When _no_ `??` or `//` brackets the entry, then the entry is a glob.
  The markers are _stripped_ from the pattern. E.g. `???` is the glob pattern `?`.
* The user pattern entries `//` and `??` both correspond to an empty pattern and match a zero length
  string in that field when messages are matched at runtime.
* Faulty regexps which are not compilable are reported using a LONG toast when an edited Ignore entry
  is Saved, _but are saved as is_. It is the user's responsibility to edit them. Faulty regexp patterns
  will be saved and loaded (!) from saved settings, currently with no errors raised (!)
---
    
This client features a modern Material design as well as many other awesome features:

* Stays in background properly, even on more recent Android versions
* Store chat messages to be displayed after reconnecting to the server later
* Nick/channel/command autocomplete
* Ignore list
* mIRC color formatting support
* SSL certificate exception list
* Command list to run after connecting
* Customization: custom command aliases, notification rules, reconnection interval, chat font, message format, app colors

...and much more!
