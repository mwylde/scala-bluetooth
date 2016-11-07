import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.micahw.bluetooth.{BluetoothManager, Device, NotConnectedException}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.convert.decorateAsScala._
import scala.collection.JavaConversions._
import scala.util.Random


case class Color(r : Int, g : Int, b : Int)
object Color {
  val red = Color(255, 0, 0)
  val green = Color(0, 255, 0)
  val blue = Color(0, 0, 255)
  val orange = Color(250, 60, 0)

  def fromHtml(rgb : String) = {
    val bs = rgb.getBytes
    Color(Integer.parseInt(rgb.substring(1, 3), 16),
      Integer.parseInt(rgb.substring(3, 5), 16),
      Integer.parseInt(rgb.substring(5, 7), 16))
  }
}

case class MagicLightStatus(power : Boolean,
                            mode: Byte,
                            color: Color,
                            warm: Double)

object MagicLight {
  def warm(level : Double) = {
    val levelByte = Math.round(level * 255).toInt
    List(0x56, 0x00, 0x00, 0x00, levelByte, 0x0f, 0xaa)
  }

  def power(on : Boolean) = {
    val powerByte = if (on) { 0x23 } else { 0x24 }
    List(0xcc, powerByte, 0x33, 0x3)
  }

  def color(color : Color) : List[Int] = {
    List(0x56, color.r, color.g, color.b, 0x00, 0xf0, 0xaa)
  }

  val fullWarm = warm(1.0)
  val off = power(false)
  val on = power(true)

  val status = List(0xEF, 0x01, 0x77)
}

/**
  * This class allows interaction with a brand of RGB bluetooth lights marketed
  * under various names (e.g.; https://www.amazon.com/gp/product/B00SMLKKD6)
  * and typically controlled using the "MagicLight" app.
  */
class MagicLight(manager : BluetoothManager, val device : Device) {
  import MagicLight._

  val cmdChar = "0000ffe9"
  val statusChar = "0000ffe4"
  val watchingStatus = new AtomicBoolean

  val log = Logger(classOf[MagicLight])

  private val statusCache = new AtomicReference[MagicLightStatus]()

  device.onConnection(connected)

  val running = new AtomicBoolean(true)

  val statusThread = new Thread(new Runnable {
    override def run() {
      while (running.get) {
        try {
          if (device.connected) {
            refreshStatus()
          }
        } catch {
          case e : Exception =>
            log.error(s"Encountered exception refreshing status for ${device.address}", e)
        }

        this.synchronized {
          wait(1000)
        }
      }
    }
  }, s"light-status-${device.address}")

  def connect() {
    log.info(s"Connecting to ${device.address}")

    device.connect(stayConnected = true)

    this.synchronized {
      // this probably does nothing because we're probably not connected yet
      // what it _does_ do is work around a race condition in the dbus java lib
      write(status)

      if (statusThread.getState == Thread.State.NEW) {
        statusThread.start()
      }
    }
  }

  private def statusWatcher(value : Seq[Byte]) {
    log.debug(s"Got value: $value from ${device.address}")
    if (value.size == 12 && value(0) == 0x66 && value(1) == 0x15) {
      this.synchronized {
        statusCache.set(MagicLightStatus(
          value(2) == 0x23,
          value(3),
          Color(value(6) & 0xFF, value(7) & 0xFF, value(8) & 0xFF),
          (value(9) & 0xFF) / 256.0))
      }
      log.debug(s"Updated status: ${statusCache.get} for ${device.address}")
    }
  }

  private def connected() {
    // listen to the status char
    try {
      device.getCharForUUID(statusChar).foreach(char => {
        if (!char.notifying) {
          char.startNotify()
          char.watchValue(statusWatcher)
        }
      })
    } catch {
      case _ : NotConnectedException =>
        log.error("Not connected yet")
    }
  }

  def disconnect() {
    device.disconnect()
  }

  def close() {
    running.set(false)
    statusThread.synchronized(statusThread.notify())
    disconnect()
  }

  def write(message : List[Int]): Boolean = {
    try {
      device.getCharForUUID(cmdChar)
        .map(_.writeValue(message.map(_.toByte)))
        .isDefined
    } catch {
      case e: NotConnectedException =>
        if (device.stayingConnected) {
          // will attempt to reconnect
          false
        } else {
          throw e
        }
    }
  }

  def setColor(c : Color): Boolean = {
    this.synchronized {
      if (write(color(c))) {
        getStatus.foreach(sc =>
          statusCache.set(MagicLightStatus(sc.power, sc.mode, c, sc.warm)))
        true
      } else {
        false
      }
    }
  }

  def setWarm(level: Double): Boolean = {
    this.synchronized {
      if (write(warm(level))) {
        getStatus.foreach(sc =>
          statusCache.set(MagicLightStatus(sc.power, sc.mode, sc.color, level)))
        true
      } else {
        false
      }
    }
  }

  def setPower(on: Boolean): Boolean = {
    this.synchronized {
      if (write(power(on))) {
        getStatus.foreach(sc =>
          statusCache.set(MagicLightStatus(on, sc.mode, sc.color, sc.warm)))
        true
      } else {
        false
      }
    }
  }

  private def refreshStatus() {
    connected()
    this.synchronized {
      write(status)
    }
  }

  def getStatus : Option[MagicLightStatus] = {
    Option(statusCache.get())
  }
}

object App {
  @annotation.tailrec
  def retry[T](n: Int, until : T)(fn: => T): T = {
    util.Try { fn } match {
      case util.Success(x) if x == until => x
      case _ if n > 1 => retry(n - 1, until)(fn)
      case util.Failure(e) => throw e
      case _ => throw new RuntimeException("Failed to converge")
    }
  }

  def main(args: Array[String]) {
    val manager = new BluetoothManager
    val adapter = manager.defaultAdapter.getOrElse({
      Console.err.println("No bluetooth adapter found")
      System.exit(1)
      throw new RuntimeException()
    })

    if (!adapter.powered) {
      try {
        retry(30, true) {
          Await.result(adapter.powered = true, 1.second)
        }
      } catch {
        case _ : IllegalStateException =>
          Console.err.println(s"Unable to power on bluetooth adapter ${adapter.name}")
          System.exit(1)
      }
    }

    val deviceFilter = (d : Device) => d.name.startsWith("LEDBlue")
    val devices = manager.devices.filter(deviceFilter)

    val lights = new ConcurrentHashMap[String, MagicLight](
      devices.map(d => (d.address, new MagicLight(manager, d))).toMap
    ).asScala

    lights.values.foreach(l => {
      l.connect()
    })

    manager.onNewDevice((d) => {
      if (deviceFilter(d)) {
        println("adding device")
        val light = new MagicLight(manager, d)
        light.connect()
        lights.put(d.address, light)
      }
    })

    try {
      Range(0, 10).foreach((i) => {
        lights.values.toList.toParArray.foreach(d => d.setWarm(1))
        Thread.sleep(100)
        lights.values.toList.toParArray.foreach(d => d.setWarm(0.5))
        Thread.sleep(100)
      })

      lights.values.toList.toParArray.foreach(d => d.setWarm(1))

      lights.values.foreach(l => println(l.getStatus))
    } finally {
      println("Closing")
      lights.values.foreach(_.close())
      manager.close()
    }
  }
}
