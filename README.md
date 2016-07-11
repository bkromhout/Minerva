![# Minerva](https://bkromhout.github.io/Minerva/logo_name_web_version_optimized.svg)

<center>
[![Get it on Google Play](http://steverichey.github.io/google-play-badge-svg/img/en_get.svg)](https://play.google.com/store/apps/details?id=com.bkromhout.minerva)
</center>

### Table of Contents
* [What is Minerva?](#what_is_minerva)
* [What *isn’t* Minerva?](#what_isnt_minerva)
* [How did Minerva come to be?](#history)
* [Why “Minerva”?](#the_name)
* [Developer Notes](#dev_notes)  
    * [Libraries](#libs)
    * [`Ruqus`](#ruqus)
    * [`realm-recyclerview-lite`](#rrvl)
    * [Building](#building)
* [FAQ](#faq)
* [Legalese](#legalese)


<a name="what_is_minerva"/>
## What is Minerva?
**Minerva is an Android app which helps you to easily organize your eBook library in powerful ways**

* 📁 Doesn’t move files; only watches a “library folder”
    * You can set up Minerva so that it checks and imports your library each time it starts
* 📖 Smart importing of eBook files
    * Books are marked as “New” when they are first imported
    * Re-importing the same book will mark it as “Updated” in your library if it has changed
    * Based off of path within library folder combined with a hash of the file
* 🏷 Book tagging using customizable tags (name, text color, background color)
* 📚 Normal lists: You add, remove, and reorder the books in the list
* 💡Smart lists: Minerva populates the list based on the results of a query, and automatically keeps it up-to-date as your library grows and changes
* ⭐️ Book ratings
* 🔎 Power search system
    * Construct queries that run the gamut from basic to complex
    * Search based on title, author, description, chapter count, tags, rating, etc.  
    (Pretty much anything Minerva knows about your books can be used as a parameter)
    * Sort the search results based on any of those same options
    * Save power searches as Smart lists
* Dark-themed Material Design

Minerva is supported on devices running at least Android Lollipop (5.0).


<a name="what_isnt_minerva"/>
## What *isn’t* Minerva?
**Minerva *isn’t* a reading app.** It focuses solely on helping you manage your library how you like, then steps out of the way and allows you to use your favorite reading app to *actually read*.

This was an explicit choice on my part. While it *would* be nice to have a seamless experience, there are already a number of *superb* reading apps on the Play Store. The developers of said apps have clearly put a ton of time and effort into creating something great, so I have very few qualms with making Minerva delegate to them for reading.

If you still want further reasoning, [see this part of the FAQ](#not_a_reader).


<a name="history"/>
## How did Minerva come to be?
As I mentioned, there are many excellent reading apps on the Play Store; I’ve tried out a lot of them in an effort to find one which satisfies my needs.

It quickly became apparent, however, that even the apps offering the best reading experiences suffered when it came to library management. They were either too simplistic and futile, too heavy-handed and inflexible, too overcomplicated and kludgy, or some combination of the three.

I felt that there was a clear gap in functionality when it came to library management, and I tossed around the idea of creating an app to bridge that gap for a while. Finally, my preferred reading app forced my hand; I opened it one day, and it scanned my library folder as usual... and then proceeded to re-import *all* of my books and destroy the paltry amount of organization I had put into place.

Fueled by annoyance, I coded for a solid 11 hours. A little over six months and 650 commits later, I felt Minerva was ready for a 1.0.0 release.


<a name="the_name"/>
## Why “Minerva”?
My reasons for choosing to name the app “Minerva” were twofold:
1. For no reason other than pure whim, I was hoping to use the name of one of the ancient Greek or Roman deities. “Minerva” is [the Roman goddess of wisdom](https://en.wikipedia.org/wiki/Minerva).
2. “Minerva” is also a reference to Minerva McGonagall, one of my favorite characters from J.K. Rowling’s *Harry Potter* series. The *Harry Potter* universe was a big part of my life for a long time, and there’s no doubt that I owe much of my love for books to it.

(If it isn’t obvious, the latter was the real determining factor; I’d have named it “Athena” otherwise 😉)


<a name="dev_notes"/>
## Developer Notes
<a name="libs"/>
### Libraries
Minerva wouldn’t be possible without some seriously awesome libraries made by equally awesome people. There are quite a few, but here are a couple which stand out in particular:

* [`epublib`](https://github.com/psiegman/epublib) is the library Minerva uses to read ePub files. The original library was created by Paul Siegmann, but Minerva actually uses [my own fork of epublib](https://github.com/bkromhout/epublib) (which was actually forked from a *different* fork of the original, though that one is no longer accessible for me to link to).
* [`Realm`](https://github.com/realm/realm-java) is an incredible third-party database library which Minerva uses instead of Android’s built-in SQLite database.
* Again, there are many others I haven’t mentioned here. I encourage you to look at the app’s build.gradle file or the “About” screen in the app if you’re interested.

During the development of Minerva, I actually ended up making two Android libraries which are just as integral to Minerva as the two listed above. I highly encourage you to go and look at their repositories if they sound interesting or helpful, but since I *did* create them with Minerva in mind, I thought I’d provide a bit of information about them here as well.

<a name="ruqus"/>
### Ruqus
[`Ruqus`](https://github.com/bkromhout/ruqus) is the library which makes Minerva’s power search and smart list features possible. I won’t go into too much detail about it here since it’s a bit complicated, but there are two key things it provides:
1. It gives ***users*** a way to dynamically build queries against a Realm database using a custom view view, `RealmQueryView`. The developer maintains control over which models (and fields, of course) are available to the user.
2. It gives ***developers*** a class called `RealmUserQuery`, which is what holds those user-built queries. `RealmUserQuery` implements `Parcelable`, as well as providing two `String` output formats:  
    * An non-user-friendly `String`-based representation which can be used to reconstruct a `RealmUserQuery` object
    * A user-friendly `String` describing the query

These things together present opportunity for some really powerful features. While I created `Ruqus` specifically with Minerva in mind, I figured that the concept was pretty neat, and one that was definitely worth sharing with other developers.

If `Ruqus` sounds interesting or useful to you, I encourage you to at least look its README, and then check it out and tell me what you think if you feel so inclined. I’m quite proud of it—it’s probably one of my favorite projects I’ve done so far due to the challenges it presented me with—but I’ll be the first to admit that there is definitely room for improvement and evolution.

<a name="rrvl"/>
### realm-recyclerview-lite
[`realm-recyclerview-lite`](https://github.com/bkromhout/realm-recyclerview-lite) is the thankless star of the show in Minerva, being responsible for taking data from Realm and actually displaying it in `RecyclerView`s, as well as keeping it up-to-date without requiring much additional work on my part. It actually does even more than that (the drag-and-drop functionality of normal lists, for example), but I’ll let you read the README for yourself.

`realm-recyclerview-lite` is actually the result of what I originally intended to be simple fork-and-PR effort to add a single feature to Thorben Primke’s [`realm-recyclerview`](https://github.com/thorbenprimke/realm-recyclerview) library.

While I will continue to give him credit and thanks for the original, the reality of the matter is that the two libraries have diverged drastically. At this point, almost none of the code in `realm-recyclerview-lite` actually remains from `realm-recyclerview`.

Feel free to check the `realm-recyclerview-lite` repo for further details about this point if you wish to, I certainly don’t want it to cause any confusion.

<a name="building"/>
### Building
***If you’re trying to build Minerva from source, please note that my fork of epublib is required for building it, as it is linked to the Minerva project locally.*** I did this on purpose so that I can work on the epublib project while still in the same Android Studio window I have Minerva open in, but I am aware it will make it harder for others to build the project. Sorry!


<a name="faq"/>
## FAQ
#### How come Lollipop is Minerva’s minimum version of Android?
When I started designing Minerva, I knew for a fact that I wanted to follow [Google’s Material Design spec](https://material.google.com/) as much as I possibly could; attempting to do this while supporting Android versions prior to Lollipop is a huge pain in a number of areas.  
Since Minerva is—and always will be—a personal project targeted at a single user (me), I made the decision to only support the more recent, modern versions of Android.

#### Minerva doesn’t see my books!
Make sure that your eBook files are in your library folder; Minerva only checks that folder when importing. Also, please be aware that ***Minerva currently only supports ePub files, sorry for any inconvenience!***

#### Minerva failed to import one or more of my books
See the answer to the above. If that’s not relevant to you, then please feel free to open an issue here, and I’ll look into it. *Please be sure to fill out all relevant areas of the issue template, or I likely won’t be able to help you.*

<a name="not_a_reader"/>
#### Okay, but really, why don’t you want Minerva to be a reading app as well?
In truth, there’s a *ton* of technical junk that would have to happen in order to even implement reading functionality which would be just-good-enough to satisfy me as the developer. I’m not saying that I’m necessarily opposed to doing that work, but the front-loading required to even begin to think about it requires a lot of time which I simply don’t have.

So, while I *do* actually have an open issue, #23, for making Minerva an eBook reader, I feel no pressing need to do so. Even if I do implement reading functionality at some point, it *won’t* be as feature-rich as some reader apps are; at the end of the day, Minerva is still a hobby project which I make no money off of that is targeted at me, and I just don’t care about many of the extra features those apps have.


<a name="legalese"/>
## Legalese
```
Android, Google Play, and the Google Play logo are trademarks of Google Inc.
```

<!-- Special characters (for easy copy-paste):
    Right single quote: ’ (Or, Shift+Alt+])
    Left double quote:  “ (Or, Alt+[ to surround)
    Right double quote: ”
-->
