# MBAutomation - Android

`MBAutomationAndroid` is a plugin libary for [MBurger](https://mburger.cloud/), that lets you send automatic push notifications and in-app messages crated from the MBurger platform. It has as dependencies [MBMessagesAndroid](https://github.com/Mumble-SRL/MBMessages-Android) and [MBAudienceAndroid](https://github.com/Mumble-SRL/MBAudience-Android). With this library you can also track user events and views.

Using `MBAutomationAndroid` you can setup triggers for in-app messages and push notifications, in the MBurger dashboard and the SDK will show the coontent automatically when triggers are satisfied.

It depends on `MBAudienceAndroid` because messages can be triggered by location changes or tag changes, coming from this SDK.

It depends on `MBMessagesAndroid` because it contains all the views for the in-app messages and the checks if a message has been already displayed or not.

The data flow from all the SDKs is manage entirely by MBurger, you don't have to worry about it.



# Installation

### Installation with gradle

This plugin only works with the latest Kotlin version of MBurger Client SDK so make sure to ad Kotlin Android Studio plugin and Kotlin dependencies to your Android project.

Add this repository to your project level `build.gradle` file under `allprojects`:

```
maven { url "https://dl.bintray.com/mumbleideas/MBurger-Android/" }
```

Then add **MBurger Kotlin** dependency to your `app build.gradle` file:

```
implementation 'mumble.mburger:mbautomation-android:0.3.26'
```

Lastly add `MBAudience` and `MBMessages` library:

```
implementation 'mumble.mburger:mbmessages-android:0.4.21'
implementation 'mumble.mburger:mbaudience-android:0.3.3'
```



# Initialization

To initialize automation you need to insert `MBAutomation` as a `MBurger` plugins, tipically automation is used in conjunction with the `MBMessages` and `MBAudience` plugins.

```kotlin
val plugins = ArrayList<MBPlugin>()
val pluginAutomation = MBAutomation()
val pluginAudience = MBAudience()
val pluginMessages = MBMessages()
plugins.add(pluginAutomation)
plugins.add(pluginAudience)
plugins.add(pluginMessages)

MBurger.initialize(applicationContext, "MBURGER_KEY", false, plugins)
```

Once you've done this ask MBurger to start plugins in your main activity (be aware that it has to be an `AppCompatActivity`).

```kotlin
MBurger.startPlugins(activity)
```



# Triggers

Every in-appmessage or push notification coming from MBurger can have an array of triggers, those are managed entirely by the MBAutomation SDK that evaluates them and show the mssage only when the conditioon defined by the triggers are matched.

If thre are more than one trigger, they can be evaluated with 2 methods:

- `any`: once one of triggers becomes true the message is displayed to the user
- `all`: all triggers needs to be true in order to show the message.

Here's the list of triggers managed by automation SDK:

#### App opening

`MBTriggerAppOpening`: Becoomes true when the app has been opened n times (`times` property), it's checked at the app startup.

#### Event

`MBTriggerEvent`: Becomes true when an event happens n times (`times` property)

#### Inactive user

`MBTriggerInactiveUser`: Becomes true if a user has not opened the app for n days (`days` parameter)

#### Location

`MBTriggerLocation`: If a user enters a location, specified by `latitude`, `longitude` and `radius`. This trigger can be activated with a ttime delay defined as the `after` property. The location data comes from the [MBAudienceAndroid](https://github.com/Mumble-SRL/MBAudience-Android) SDK.

#### Tag change

`MBTriggerTagChange`: If a tag of the [MBAudienceAndroid](https://github.com/Mumble-SRL/MBAudience-Android) SDK changes and become equals or not to a value. It has a `tag` property (the tag that needs to be checked) and a `value` property (the value that needs to be equal or different in order to activate the trigger)

#### View

`MBTriggerView`: it's activated when a user enters a view n times (`times` property). If the `seconds_on_view` has a value the user needs to stay the seconds defined in order to activate the trigger.



# Add events

You can send events with the `MBAutomation` like this:

```kotlin
MBAutomation.sendEvent(context, "event")
```

You can specify 2 more parameters, both optional: `name` a name that will be displayed in the MBurger dashboard and a dictionary of additional `metadata` to specifymore fields of the event

```kotlin
MBAutomation.sendEvent(context, "event",
                      	name : String? = "name",
                      	metadata: String? = "metadata")
```

Events are saved in a local database and sent to the server every 10 seconds, you can change the frequency setting the `eventsTimerTime` property.



# View Tracking

In MBAutomation the tracking of the views is automatic by using [Application.ActivityLifecycleCallbacks](https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks) to track view automatically on `onActivityCreated`, `onActivityStarted`, `onActivityStopped` and `onActivityDestroyed`. You can disable it changing the static value `trackViewsAutomatically` to false.

The default name for all the Activities is the class name (e.g. if your Activity is called Act_home you will see Act_home as the view). If you want to change the name for an Activity you can change its internal name by setting a title on the Manifest or calling `setTitle` on the onCreate.

If you have disabled the automatic tracking and you still want to track the views you can use this function, passing a `FragmentActivity` and, optionally, how much time the Activity has been seen:

```kotlin
MBAutomation.trackScreenView(this, time: Long = -1L)
```

As the events, views are saved in a local database and sent to the server every 10 seconds and you can change the frequency setting the `eventsTimerTime` property.



# Stop/Pause tracking

To property stop view and events tracking you should call 

```kotlin
MBAutomation.stopAutomation(applicationContext)
```

We suggest to call it when your app goes on background then restart it with

```
MBAutomation.startEventsAndViewsAutomation(applicationContext)
```

when your app returns to foreground.