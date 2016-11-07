package com.micahw.bluetooth

import org.bluez.GattCharacteristic1
import org.freedesktop.DBus.Properties
import org.freedesktop.dbus.exceptions.{DBusException, DBusExecutionException}
import org.freedesktop.dbus.{DBusConnection, Variant}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class GattChar(conn: DBusConnection,
               char1: GattCharacteristic1,
               props: Properties) extends DBusObject(
  conn, char1, classOf[GattCharacteristic1], props) {
  import VariantConverters._

  def readValue(): Seq[Byte] = {
    Errors.tryImmediately(
      this.synchronized(char1.ReadValue().map(_.toByte)))
  }

  def writeValue(data: Seq[Byte]) {
    Errors.tryImmediately(this.synchronized(
      char1.WriteValue(java.util.Arrays.asList(
        data.map(b => b.asInstanceOf[java.lang.Byte]): _*))))
  }

  def startNotify() {
    Errors.tryImmediately(
      try {
        this.synchronized {
          char1.StartNotify()
        }
      } catch {
        case e : DBusExecutionException =>
          if (e.getMessage == "Already notifying") {
            return
          } else {
            throw e
          }
      })
  }

  def stopNotify() {
    Errors.tryImmediately(this.synchronized(char1.StopNotify()))
  }

  def uuid: String = getString(p("UUID"))

  def value: Seq[Byte] = p.get("Value").map(getBytes).getOrElse(Seq())

  def watchValue(f: Seq[Byte] => Unit) {
    watch("Value", (_, v) => f(getBytes(v)), f)
  }

  def nextValue(): Future[Seq[Byte]] = {
    changeFuture("Value").map(getBytes)
  }

  def notifying: Boolean = getBool(p("Notifying"))

  def flags: Set[String] = p("Flags").getValue
    .asInstanceOf[java.util.Vector[String]].toSet

}
