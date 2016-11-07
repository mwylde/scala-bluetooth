package com.micahw.bluetooth

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.scalalogging.Logger
import org.bluez.{Device1, GattCharacteristic1}
import org.freedesktop.DBus.Properties
import org.freedesktop.dbus.{DBusConnection, ObjectManager}

import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class Device(conn : DBusConnection,
             device1: Device1,
             props: Properties) extends DBusObject(conn, device1, classOf[Device1], props) {
  private val log = Logger(classOf[Device])
  private val bus = "org.bluez"

  import VariantConverters._

  private val chars = new ConcurrentHashMap[String, GattChar]()
  private val base = conn.getRemoteObject(bus, "/", classOf[ObjectManager])

  private val shouldBeConnected = new AtomicBoolean(false)
  private val connectionListeners = new TrieMap[(() => Unit), Boolean]

  watch("Connected", (oldValue, newValue) => {
    if (getBool(newValue) && !oldValue.exists(getBool)) {
      connectionListeners.keys.foreach(f => f)
    }
  }, this)

  val reconnector = new Runnable {
    override def run() {
      while (shouldBeConnected.get) {
        this.synchronized {
          try {
            if (!connected) {
              // we're not connected, but we _should_ be, so connect
              connect()
            } else if (connected) {
              wait(100)
            }
          } catch {
            case _: NotConnectedException =>
              connect()
            case e: Exception =>
              log.error("Encountered error trying to reconnect", e)
              wait(5000)
          }
        }
      }
    }
  }

  val reconnectorThread = new AtomicReference[Thread]()

  private def callMethod(f: => Unit) {
    Errors.tryImmediately(
      this.synchronized {
        f
      })
  }

  def connectProfile(uuid : String) {
    callMethod {
      device1.ConnectProfile(uuid)
    }
  }

  def disconnectProfile(uuid : String) {
    callMethod {
      device1.DisconnectProfile(uuid)
    }
  }

  def pair() {
    callMethod {
      device1.Pair()
    }
  }

  def cancelPairing() {
    callMethod {
      device1.CancelPairing()
    }
  }

  def connect(stayConnected: Boolean = false) {
    this.synchronized {
      Errors.tryImmediately({
        // for reasons I don't fully understand, disconnecting before connecting
        // is much more reliable
        device1.Disconnect()
        device1.Connect()
      })
      if (stayConnected) {
        shouldBeConnected.set(true)
        if (reconnectorThread.get() == null) {
          val t = new Thread(reconnector, s"device-reconnector-$address")
          if (reconnectorThread.compareAndSet(null, t)) {
            t.start()
          }
        }
      }
    }
  }

  def disconnect() {
    this.synchronized {
      shouldBeConnected.set(false)
      val t = reconnectorThread.get()
      if (t != null) {
        t.synchronized(t.notify())
        reconnectorThread.set(null)
      }
      Errors.tryImmediately(device1.Disconnect())
    }
  }

  def onConnection(fn: () => Unit) {
    connectionListeners.put(fn, true)
  }

  def stayingConnected: Boolean = {
    shouldBeConnected.get
  }

  def address: String = getString(p("Address"))

  def name: String = p.get("Name").map(getString).getOrElse("")

  def icon: String = getString(p("Icon"))

  def bluetoothClass: Long = getLong(p("Class"))

  def appearance: Int = getInt(p("Appearance"))

  def uuids: List[String] =
    p("UUIDs").getValue.asInstanceOf[java.util.Vector[String]].toList

  def paired: Boolean = getBool(p("Paired"))

  def connected: Boolean = getBool(p("Connected"))

  def trusted: Boolean = getBool(p("Trusted"))
  def trusted_=(v: Boolean) : Future[Boolean] = {
    set("Trusted", v).map(getBool)
  }

  def blocked: Boolean = getBool(p("Blocked"))
  def blocked_=(v: Boolean) : Future[Boolean] = {
    set("Blocked", v).map(getBool)
  }

  def alias: String = getString(p("Alias"))
  def alias_=(v: String): Future[String] = {
    set("Alias", v).map(getString)
  }

  private def getChar(path: String): GattChar = {
    val interface = conn.getRemoteObject(bus, path, classOf[GattCharacteristic1])
    val props = conn.getRemoteObject(bus, path, classOf[Properties])

    new GattChar(conn, interface, props)
  }

  private def indexChar(char: GattChar) {
    // index both by the full UUID and the short UUID if applicable
    chars.put(char.uuid, char)
    if (char.uuid.endsWith("-0000-1000-8000-00805f9b34fb")) {
      chars.put(char.uuid.split("-").head, char)
    }
  }

  def getCharForUUID(charUUID: String): Option[GattChar] = {
    if (!connected) {
      throw NotConnectedException("Not connected")
    }

    val gatt = chars.get(charUUID.toLowerCase)

    if (gatt == null) {
      BluetoothManager.getObjectPaths(base, classOf[GattCharacteristic1])
        .map(getChar)
        .foreach(indexChar)
    }

    Option(chars.get(charUUID.toLowerCase))
  }

  def waitForChar(charUUID: String): Future[GattChar] = {
    val promise = Promise[GattChar]

    getCharForUUID(charUUID)

    promise.future
  }
}
