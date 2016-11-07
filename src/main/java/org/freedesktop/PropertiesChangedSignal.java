package org.freedesktop;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

@DBusInterfaceName("org.freedesktop.DBus.Properties")
public interface PropertiesChangedSignal extends DBusInterface {

    public static class PropertiesChanged extends DBusSignal {
        public final Map<String, Variant> changedProperties;
        public final List<String> invalidatedProperties;

        public PropertiesChanged(String path,
                                 String dbusInterface,
                                 Map<String, Variant> changedProperties,
                                 List<String> invalidatedProperties) throws DBusException {
            super(path, changedProperties, invalidatedProperties);
            this.changedProperties = changedProperties;
            this.invalidatedProperties = invalidatedProperties;
        }
    }
}
