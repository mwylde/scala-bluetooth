package org.bluez;

import org.freedesktop.dbus.DBusInterface;

import java.util.List;

public interface GattDescriptor1 extends DBusInterface {
    public List<Byte> ReadValue();
    public void WriteValue(List<Byte> value);
}
