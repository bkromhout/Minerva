![# Minerva][Minerva Logo]

<p align="center">
    <a href="https://play.google.com/store/apps/details?id=com.bkromhout.minerva">
        <img src="http://steverichey.github.io/google-play-badge-svg/img/en_get.svg" alt="Get it on Google Play"/>
    </a>
</p>

### Table of Contents
* [What is Minerva?](#what_is_minerva)
* [What *isn’t* Minerva?](#what_isnt_minerva)
* [How did Minerva come to be?](#history)
* [Why “Minerva”?](#the_name)
* [Developer Notes](#dev_notes)  
    * [Building](#building)
    * [Libraries](#libs)
    * [`Ruqus`](#ruqus)
    * [`realm-recyclerview-lite`](#rrvl)
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

Minerva sports a dark, Material Design theme, and is supported on devices running at least Android Lollipop (5.0+). [Click here for the changelog.][CHANGELOG]


<a name="what_isnt_minerva"/>
## What *isn’t* Minerva?
**Minerva *isn’t* a reading app.** It focuses solely on helping you manage your library how you like, then uses your favorite reading app for the actual reading.

This was done on purpose; while it *would* be nice to have a seamless experience, there are already a number of superb reading apps on the Play Store, and I chose to focus my efforts on making the best library management app rather than trying to re-invent the wheel.

[See this part of the FAQ](#not_a_reader) if you still want more information.


<a name="history"/>
## How did Minerva come to be?
As mentioned, there are many excellent reading apps on the Play Store; I’ve tried out a lot of them in an effort to find one which satisfies my needs.

What I quickly discovered was that the apps which offered the best reading experience all suffered from a common flaw: Their library management features. All of them were either too simplistic and futile, too heavy-handed and inflexible, too overcomplicated and kludgy, or some combination of the three.

I felt that this represented a clear gap in functionality, and I tossed around the idea of creating an app to bridge that gap for a while. What finally forced my hand was—fittingly—my preferred reading app; I opened it one day, and it started the usual scan of my library folder... and then proceeded in re-importing *all* of my books and destroying the paltry amount of organization I had put into place.

A little over six months and 650 commits later, I felt Minerva was ready for a 1.0.0 release.


<a name="the_name"/>
## Why “Minerva”?
There were two reasons I chose to name the app “Minerva”:
* “Minerva” is [the Roman goddess of wisdom][Roman Minerva], and I was looking to name the app after one of the ancient Greek or Roman deities, for no real reason other than pure whim.
* “Minerva” is also a reference to Minerva McGonagall, one of my favorite characters from J.K. Rowling’s *Harry Potter* series. The *Harry Potter* universe was a big part of my life for a long time, and there’s no doubt that I owe much of my love for books to it.

(If it isn’t blatantly obvious, the latter was the real deciding factor; I’d have named it “Athena” otherwise 😉)


<a name="dev_notes"/>
## Developer Notes
I always try to include a section targeted at fellow developers (as well as those who simply have a strong sense of curiosity—I know how that works); enjoy!

<a name="building"/>
### Building Minerva
***If you’re trying to build Minerva from source, please note that [my fork of epublib][My epublib] is required for building it, as it is linked to the Minerva project locally.*** I did this on purpose so that I can work on the epublib project while still in the same Android Studio window I have Minerva open in, but I am aware it will make it harder for others to build the project. Sorry!

<a name="libs"/>
### Libraries
Minerva wouldn’t be possible without some seriously awesome libraries made by equally awesome people. There are quite a few, but here are a couple which stand out in particular:

* **[epublib][epublib]** is the library Minerva uses to read ePub files. The original library was created by Paul Siegmann, but Minerva actually uses **[my own fork of epublib][My epublib]** (which was actually forked from a *different* fork of the original, though that one is no longer accessible for me to link to).
* **[Realm][Realm]** is an incredible third-party database library which Minerva uses instead of Android’s built-in SQLite database.
* Again, there are many others I haven’t mentioned here. I encourage you to look at [the app’s build.gradle file][build.gradle] or the “About” screen in the app if you’re interested.

During the development of Minerva, I actually ended up making two Android libraries which are just as integral to Minerva as the two listed above. I highly encourage you to go and look at their repositories if they sound interesting or helpful. Since I *did* create them with Minerva in mind, I’ll provide a bit of information about them here as well.

<a name="ruqus"/>
### Ruqus
**[Ruqus](Ruqus)** is the library which makes Minerva’s power search and smart list features possible. I won’t go into too much detail about it here since it’s a bit complicated, but there are two key things it provides:
* It gives ***users*** a way to dynamically build queries against a Realm database using a custom view view, `RealmQueryView`. The developer maintains control over which models (and fields, of course) are available to the user.
* It gives ***developers*** a class called `RealmUserQuery`, which is what holds those user-built queries. `RealmUserQuery` implements `Parcelable`, as well as providing two `String` output formats:  
    * An non-user-friendly `String`-based representation which can be used to reconstruct a `RealmUserQuery` object
    * A user-friendly `String` describing the query

These things together present opportunity for some really powerful features. While I created **Ruqus** specifically with Minerva in mind, I figured that the concept was pretty neat, and one that was definitely worth sharing with other developers.

If **Ruqus** sounds interesting or useful to you, I encourage you to at least look its README, and then check it out and tell me what you think if you feel so inclined.

**Ruqus** is one of my favorite projects I’ve done so far due to the challenges it presented me with, so I’m quite proud of it. However, I’ll be the first to admit that there is definitely room for improvement.

<a name="rrvl"/>
### realm-recyclerview-lite
**[realm-recyclerview-lite][RRVL]** is the thankless star of the show in Minerva, being responsible for taking data from Realm and actually displaying it in `RecyclerView`s, and then keeping it up-to-date without requiring much additional work on my part. It actually does even more than that (the drag-and-drop functionality of normal lists, for example), but I’ll let you read the README for yourself.

**realm-recyclerview-lite** is actually the result of what I originally intended to be simple fork-and-PR effort to add a single feature to Thorben Primke’s [realm-recyclerview][RRV] library.

I will continue to give him credit for the original, but the reality of the matter is that the two libraries have diverged drastically; at this point, almost none of the code from realm-recyclerview actually remains in **realm-recyclerview-lite**.

Feel free to check the **realm-recyclerview-lite** repo for further details about this if you wish to, I certainly don’t want it to cause any confusion.


<a name="faq"/>
## FAQ
#### How come Lollipop is Minerva’s minimum version of Android?
When I started designing Minerva, I knew for a fact that I wanted to follow [Google’s Material Design spec][Material Design] as much as I possibly could; attempting to do this while supporting Android versions prior to Lollipop is a huge pain in a number of areas.  
Since Minerva is—and always will be—a personal project targeted at a single user (me), I made the decision to only support the more recent, modern versions of Android.

#### Minerva doesn’t see my books!
Make sure that your eBook files are in your library folder; Minerva only checks that folder when importing. Also, please be aware that ***Minerva currently only supports ePub files, sorry for any inconvenience!***

#### Minerva failed to import one or more of my books
See the answer to the above. If that’s not relevant to you, then please feel free to open an issue here, and I’ll look into it. *Please be sure to fill out all relevant areas of the issue template, or I likely won’t be able to help you.*

<a name="not_a_reader"/>
#### Okay, but really, why don’t you want Minerva to be a reading app as well?
Let me clarify something; it’s not that I *don’t want* Minerva to also be a reading app; it’s that doing so did not feel like a good use of my time for the initial release since that wasn’t [my original goal](#history).

There’s a *ton* of investigation and learning—and frustration—that would have to occur on my part to implement a reader in Minerva. Even after all that, it’d still likely be “just good enough” to satisfy me as the developer. I’m not saying that I’m opposed to doing that work, just that the front-loading and effort required to even begin to design such a feature requires a lot of time which I simply don’t have right now.

So, while I *do* actually have an open issue, [#23][Reader Issue], for making Minerva an eBook reader, I feel no pressing need to do so. Even if I do implement reading functionality at some point, it *won’t* be as feature-rich as some reader apps are; I just don’t care about many of the extra features those apps have.

**TL;DR:** I would love for Minerva to be a reading app too! But at the end of the day it’s still a hobby project; it makes me no money, it’s targeted primarily at me, and I don’t have enough time available to me to give this the effort it requires. Perhaps one day, but I promise nothing.


<a name="legalese"/>
## Legalese
**Minerva:**
```
Copyright 2016-Present Brenden Kromhout

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

**Google:**
```
Android, Google Play, and the Google Play logo are trademarks of Google Inc.
```

[Minerva Logo]: https://bkromhout.github.io/Minerva/logo_name_web_version_optimized.svg
[CHANGELOG]: CHANGELOG.md
[Roman Minerva]: https://en.wikipedia.org/wiki/Minerva
[epublib]: https://github.com/psiegman/epublib
[My epublib]: https://github.com/bkromhout/epublib
[Realm]: https://github.com/realm/realm-java
[build.gradle]: app/build.gradle
[Ruqus]: https://github.com/bkromhout/ruqus
[RRVL]: https://github.com/bkromhout/realm-recyclerview-lite
[RRV]: https://github.com/thorbenprimke/realm-recyclerview
[Material Design]: https://material.google.com/
[Reader Issue]: https://github.com/bkromhout/Minerva/issues/23

<!-- Special characters (for easy copy-paste):
    Right single quote: ’ (Or, Shift+Alt+])
    Left double quote:  “ (Or, Alt+[ to surround)
    Right double quote: ”
-->
