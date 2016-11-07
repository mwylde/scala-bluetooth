package com.micahw.bluetooth

import java.util.Collections

import org.bluez.{Adapter1, Device1}
import org.freedesktop.DBus.Properties
import org.freedesktop.dbus.ObjectManager.{InterfacesAdded, InterfacesRemoved}
import org.freedesktop.dbus._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Await, Future}

private object BluetoothManager {
  def objFromPath(path : DBusInterface): String = path.toString.split(":")(2)

  def getObjects(base: ObjectManager,
                 interface: Class[_ <: DBusInterface]) : Iterable[DBusInterface] = {
    base.GetManagedObjects().entrySet().filter(_.getValue.keys.contains(interface.getName))
      .map(e => e.getKey)
  }

  def getObjectPaths(base: ObjectManager, interface: Class[_ <: DBusInterface]) = {
    getObjects(base, interface).map(e => objFromPath(e))
  }
}

class BluetoothManager {
  import BluetoothManager._

  private val bus = "org.bluez"
  private val conn = DBusConnection.getConnection(DBusConnection.SYSTEM)
  private val base = conn.getRemoteObject(bus, "/", classOf[ObjectManager])

  private val deviceCallbacks = Collections.synchronizedList(
    ArrayBuffer[(Device) => Unit]())

  private val deviceCache = TrieMap[String, Device]()

  conn.addSigHandler(classOf[ObjectManager.InterfacesAdded], base,
    new DBusSigHandler[ObjectManager.InterfacesAdded] {
      override def handle(t: InterfacesAdded) {
        if (t.interfacesAndProperties.containsKey(classOf[Device1].getName)) {
          val device = deviceFromPath(objFromPath(t.objectPath))
          deviceCallbacks.synchronized {
            deviceCallbacks.foreach(c => c(device))
          }
        }
      }
    })

  conn.addSigHandler(classOf[ObjectManager.InterfacesRemoved], base,
    new DBusSigHandler[ObjectManager.InterfacesRemoved] {
      override def handle(t: InterfacesRemoved) {
        if (t.interfaces.contains(classOf[Device1].getName)) {
          deviceCache.remove(objFromPath(t.objectPath))
        }
      }
    }
  )

  def onNewDevice(fn: (Device) => Unit) {
    deviceCallbacks.add(fn)
  }

  def adapters : Iterable[Adapter] = {
    getObjectPaths(base, classOf[Adapter1])
      .map(path => {
        val props = conn.getRemoteObject(bus, path, classOf[Properties])
        val adapter1 = conn.getRemoteObject(bus, path, classOf[Adapter1])
        new Adapter(conn, adapter1, props)
      })
  }

  def defaultAdapter: Option[Adapter] = adapters.headOption

  private def deviceFromPath(path: String): Device = {
    deviceCache.getOrElseUpdate(path, {
      val props = conn.getRemoteObject(bus, path, classOf[Properties])
      val device1 = conn.getRemoteObject(bus, path, classOf[Device1])
      new Device(conn, device1, props)
    })
  }

  def devices : Iterable[Device] = {
    getObjectPaths(base, classOf[Device1])
      .map(deviceFromPath)
  }

  def close(): Unit = {
    conn.disconnect()
  }
}
