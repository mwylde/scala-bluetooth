# scala-bluetooth

This project contains experimental Scala binding to bluez (Linux's
bluetooth stack) over DBus (a popular Linux IPC
system), with support for Bluetooth low energy
(BLE). Use this library if you've ever wanted to control your BLE
smart lights, get battery status from your headphones, 
or connect to your smart toaster from Scala.

Bluetooth [is complicated](https://learn.adafruit.com/introduction-to-bluetooth-low-energy?view=all) and DBus
 [is _extremely_ complicated](https://dbus.freedesktop.org/doc/dbus-specification.html). This library aims to hide most of the complication of dealing with DBus, but does not abstract
 much over the underlying Bluetooth concepts.
 
 Enough talk. You just want to make a sweet light display:
 
 ```scala
 object DanceParty {
  def main(args: Array[String]) {
    val manager = new BluetoothManager
    val adapter = manager.defaultAdapter.get
    if (!adapter.powered) {
      Await.result(adapter.powered = true, 10 seconds)
    }
    adapter.startDiscovery()
    Thread.sleep(1000)

    val lights = manager.devices.filter(_.name.startsWith("LEDBlue"))
    lights.foreach(_.connect(stayConnected = true))

    Range(0, 100).foreach((i) => {
      val color = (Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
      val message = List(0x56, color._1, color._2, color._3, 0x00, 0xf0, 0xaa)
      lights.toParArray.foreach(light => {
        light.getCharForUUID("0000ffe9")
          .foreach((char) => char.writeValue(message.map(_.toByte)))
      })
      Thread.sleep(200)
    })

    lights.foreach(_.disconnect())
    manager.close()
  }
}
 ```

See [here](https://github.com/mwylde/scala-bluetooth/tree/master/examples) for
more examples.

### Setup

Releases are hosted on maven central. To add to your build:

```scala
libraryDependencies += "com.micahw" %% "scala-bluetooth" % "0.0.1"
```

Unfortunately, support for BLE over DBus is still experimental
in bluez, so you will need to run the daemon in experimental mode:

```
$ sudo bluetoothd -e
```

You will also need to install dbus-java. For Ubuntu, this looks like:

```
$ sudo apt-get install libdbus-java
```

And make sure that the dbus jar is on the classpath (for Ubuntu, this looks like
`java -cp /usr/share/java/dbus-2.8.jar/dbus-2.8.jar`). The dbus java also relies
on the native libunix-java.so, so make sure that your JVM knows where to find
that. The easiest way to do this system wide is to link it into to /usr/lib.
In Ubuntu, it's placed by default in /usr/lib/jni, so this can be done with

```
sudo ln -s /usr/lib/jni/libunix-java.so /usr/lib/libunix-java.so
```
