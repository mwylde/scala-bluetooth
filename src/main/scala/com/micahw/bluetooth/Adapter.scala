package com.micahw.bluetooth

import org.bluez.Adapter1
import org.freedesktop.DBus.Properties
import org.freedesktop.dbus._

import scala.collection.JavaConversions._
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global


class Adapter(conn : DBusConnection,
              adapter1: Adapter1,
              props: Properties) extends DBusObject(conn, adapter1,
  classOf[Adapter1], props) {
  import VariantConverters._

  def startDiscovery() {
    adapter1.StartDiscovery()
  }

  def stopDiscovery(): Unit = {
    adapter1.StopDiscovery()
  }

  val address = getString(p("Address"))

  def name = getString(p("Name"))

  def bluetoothClass = getLong(p("Class"))

  def alias = getString(p("Alias"))

  def powered = getBool(p("Powered"))
  def powered_=(on: Boolean) : Future[Boolean] = {
    set("Powered", on).map(getBool)
  }

  def discoverable = getBool(p("Discoverable"))
  def discoverable_=(discoverable: Boolean) : Future[Boolean] = {
    set("Discoverable", discoverable).map(getBool)
  }

  def pairable = getBool(p("Pairable"))
  def pairable_=(pairable: Boolean) : Future[Boolean] = {
    set("Pairable", pairable).map(getBool)
  }

  def pairableTimeout = getLong(p("PairableTimeout"))
  def pairableTimeout_=(t: Long) : Future[Long] = {
    set("PairableTimeout", new UInt32(t)).map(getLong)
  }

  def discoverableTimeout = getLong(p("DiscoverableTimeout"))
  def discoverableTimeout_=(t: Long) : Future[Long] = {
    set("DiscoverableTimeout", new UInt32(t)).map(getLong)
  }

  def discovering = getBool(p("Discovering"))

  def uuids = p("UUIDs").getValue.asInstanceOf[java.util.Vector[String]].toList
}
