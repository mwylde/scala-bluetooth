package org.freedesktop.dbus;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.exceptions.DBusException;

@DBusInterfaceName("org.freedesktop.DBus.ObjectManager")
public interface ObjectManager extends DBusInterface {

    class InterfacesAdded extends DBusSignal {

        public final DBusInterface objectPath;
        public final Map<String, Map<String, Variant>> interfacesAndProperties;

        public InterfacesAdded(String path, DBusInterface objectPath,
                Map<String, Map<String, Variant>> interfacesAndProperties)
                throws DBusException {
            super(path, objectPath, interfacesAndProperties);
            this.objectPath = objectPath;
            this.interfacesAndProperties = interfacesAndProperties;
        }
    }

    class InterfacesRemoved extends DBusSignal {

        public final DBusInterface objectPath;
        public final List<String> interfaces;

        public InterfacesRemoved(String path, DBusInterface objectPath,
                List<String> interfaces) throws DBusException {
            super(path, objectPath, interfaces);
            this.objectPath = objectPath;
            this.interfaces = interfaces;
        }
    }

    public Map<DBusInterface, Map<String, Map<String, Variant>>> GetManagedObjects();

}

