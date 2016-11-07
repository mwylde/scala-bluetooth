package com.micahw.bluetooth

import org.freedesktop.DBus.Properties
import org.freedesktop.PropertiesChangedSignal
import org.freedesktop.PropertiesChangedSignal.PropertiesChanged
import org.freedesktop.dbus._
import org.freedesktop.dbus.exceptions.DBusExecutionException

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try


object VariantConverters {
  def getBool(v: Variant[_]): Boolean = v.getValue.asInstanceOf[Boolean]
  def getString(v: Variant[_]): String = v.getValue.asInstanceOf[String]
  def getInt(v: Variant[_]): Int = v.getValue.asInstanceOf[UInt16].intValue()
  def getLong(v: Variant[_]): Long = v.getValue.asInstanceOf[UInt32].longValue()
  def getBytes(v: Variant[_]): Seq[Byte] = v.getValue.asInstanceOf[Array[Byte]]
}

sealed trait BluetoothException

sealed trait RetryableBluetoothException extends BluetoothException

case class ConnectionException(message: String)
  extends Exception(message) with RetryableBluetoothException

case class NotConnectedException(message: String)
  extends Exception(message) with BluetoothException

case class UnknownException(e : Throwable)
  extends Exception("Encountered unknown exception", e) with BluetoothException

object Errors {
  def errorMapper(e : DBusExecutionException) = {
    e.getMessage match {
      case "Not connected" =>
        NotConnectedException("Not currently connected to device")
      case "DBus exception while connecting" =>
        ConnectionException("Unable to connect at this time")
      case _ =>
        UnknownException(e)
    }
  }

  def handler[T]: PartialFunction[Throwable, T] = {
    case e: DBusExecutionException =>
      throw errorMapper(e)
    case e =>
      throw e
  }

  def futureHandler[T]: PartialFunction[Throwable, Future[T]] = {
    case e: DBusExecutionException =>
      Future.failed(errorMapper(e))
  }

  def tryImmediately[T](p: => T) : T = {
    Try {
      p
    }.recover(Errors.handler).get
  }

  def tryFuture[T](f: Future[T], p: => Unit): Future[T] = {
    Try {
      p
      f
    }.recover(Errors.futureHandler).get
  }
}

abstract class DBusObject[T <: DBusInterface](conn: DBusConnection,
                                              interface: T,
                                              klass: Class[T],
                                              props: Properties) {
  /**
    * Arguments are (previous value, new value)
    */
  type VarWatcher = (Option[Variant[_]], Variant[_]) => Unit

  val p = new TrieMap[String, Variant[_]]()
  p.putAll(props.GetAll(klass.getName))

  val waiters = new TrieMap[String, ArrayBuffer[Promise[Variant[_]]]]()
  val subscribers = new TrieMap[String, TrieMap[Object, VarWatcher]]()

  conn.addSigHandler(classOf[PropertiesChangedSignal.PropertiesChanged],
    interface, new DBusSigHandler[PropertiesChangedSignal.PropertiesChanged] {
      override def handle(s: PropertiesChanged) {
        this.synchronized {
          s.changedProperties.entrySet().foreach(kv => {
            // update our local cache
            val prev = p.put(kv.getKey, kv.getValue)
            // notify any waiters
            waiters.remove(kv.getKey).foreach(l => l.foreach(_.success(kv.getValue)))
            // and the watchers
            subscribers.get(kv.getKey).foreach(m => m.values.foreach(
              f => f(prev, kv.getValue)))})
        }
      }
    })

  protected def changeFuture(key : String): Future[Variant[_]] = {
    val promise = Promise[Variant[_]]()
    this.synchronized {
      waiters.putIfAbsent(key, ArrayBuffer(promise))
        .foreach(b => b.append(promise))
    }
    promise.future
  }

  protected def set[S](key: String, value: S): Future[Variant[_]] = {
    val future = changeFuture(key)
    Errors.tryFuture(future,
      this.synchronized(props.Set(klass.getName, key, value)))
  }

  protected def watch(key: String, f: VarWatcher, id: Object = new Object) {
    this.subscribers.getOrElseUpdate(key, TrieMap())
      .putIfAbsent(id, f)
  }
}
