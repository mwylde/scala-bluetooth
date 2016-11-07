package org.freedesktop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

public interface DBus extends DBusInterface {
    int DBUS_NAME_FLAG_ALLOW_REPLACEMENT = 1;
    int DBUS_NAME_FLAG_REPLACE_EXISTING = 2;
    int DBUS_NAME_FLAG_DO_NOT_QUEUE = 4;
    int DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER = 1;
    int DBUS_REQUEST_NAME_REPLY_IN_QUEUE = 2;
    int DBUS_REQUEST_NAME_REPLY_EXISTS = 3;
    int DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER = 4;
    int DBUS_RELEASE_NAME_REPLY_RELEASED = 1;
    int DBUS_RELEASE_NAME_REPLY_NON_EXISTANT = 2;
    int DBUS_RELEASE_NAME_REPLY_NOT_OWNER = 3;
    int DBUS_START_REPLY_SUCCESS = 1;
    int DBUS_START_REPLY_ALREADY_RUNNING = 2;

    String Hello();

    String[] ListNames();

    boolean NameHasOwner(String var1);

    String GetNameOwner(String var1);

    UInt32 GetConnectionUnixUser(String var1);

    UInt32 StartServiceByName(String var1, UInt32 var2);

    UInt32 RequestName(String var1, UInt32 var2);

    UInt32 ReleaseName(String var1);

    void AddMatch(String var1) throws DBus.Error.MatchRuleInvalid;

    void RemoveMatch(String var1) throws DBus.Error.MatchRuleInvalid;

    String[] ListQueuedOwners(String var1);

    UInt32 GetConnectionUnixProcessID(String var1);

    Byte[] GetConnectionSELinuxSecurityContext(String var1);

    void ReloadConfig();

    public interface Binding {
        public static final class TestStruct extends Struct {
            @Position(0)
            public final String a;
            @Position(1)
            public final UInt32 b;
            @Position(2)
            public final Short c;

            public TestStruct(String var1, UInt32 var2, Short var3) {
                this.a = var1;
                this.b = var2;
                this.c = var3;
            }
        }

        public static final class Triplet<A, B, C> extends Tuple {
            @Position(0)
            public final A a;
            @Position(1)
            public final B b;
            @Position(2)
            public final C c;

            public Triplet(A var1, B var2, C var3) {
                this.a = var1;
                this.b = var2;
                this.c = var3;
            }
        }

        public interface TestSignals extends DBusInterface {
            @DBus.Description("Sent in response to a method call")
            public static class Triggered extends DBusSignal {
                public final UInt64 a;

                public Triggered(String var1, UInt64 var2) throws DBusException {
                    super(var1, new Object[]{var2});
                    this.a = var2;
                }
            }
        }

        public interface Tests extends DBusInterface {
            @DBus.Description("Returns whatever it is passed")
            <T> Variant<T> Identity(Variant<T> var1);

            @DBus.Description("Returns whatever it is passed")
            byte IdentityByte(byte var1);

            @DBus.Description("Returns whatever it is passed")
            boolean IdentityBool(boolean var1);

            @DBus.Description("Returns whatever it is passed")
            short IdentityInt16(short var1);

            @DBus.Description("Returns whatever it is passed")
            UInt16 IdentityUInt16(UInt16 var1);

            @DBus.Description("Returns whatever it is passed")
            int IdentityInt32(int var1);

            @DBus.Description("Returns whatever it is passed")
            UInt32 IdentityUInt32(UInt32 var1);

            @DBus.Description("Returns whatever it is passed")
            long IdentityInt64(long var1);

            @DBus.Description("Returns whatever it is passed")
            UInt64 IdentityUInt64(UInt64 var1);

            @DBus.Description("Returns whatever it is passed")
            double IdentityDouble(double var1);

            @DBus.Description("Returns whatever it is passed")
            String IdentityString(String var1);

            @DBus.Description("Returns whatever it is passed")
            <T> Variant<T>[] IdentityArray(Variant<T>[] var1);

            @DBus.Description("Returns whatever it is passed")
            byte[] IdentityByteArray(byte[] var1);

            @DBus.Description("Returns whatever it is passed")
            boolean[] IdentityBoolArray(boolean[] var1);

            @DBus.Description("Returns whatever it is passed")
            short[] IdentityInt16Array(short[] var1);

            @DBus.Description("Returns whatever it is passed")
            UInt16[] IdentityUInt16Array(UInt16[] var1);

            @DBus.Description("Returns whatever it is passed")
            int[] IdentityInt32Array(int[] var1);

            @DBus.Description("Returns whatever it is passed")
            UInt32[] IdentityUInt32Array(UInt32[] var1);

            @DBus.Description("Returns whatever it is passed")
            long[] IdentityInt64Array(long[] var1);

            @DBus.Description("Returns whatever it is passed")
            UInt64[] IdentityUInt64Array(UInt64[] var1);

            @DBus.Description("Returns whatever it is passed")
            double[] IdentityDoubleArray(double[] var1);

            @DBus.Description("Returns whatever it is passed")
            String[] IdentityStringArray(String[] var1);

            @DBus.Description("Returns the sum of the values in the input list")
            long Sum(int[] var1);

            @DBus.Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
            Map<String, List<String>> InvertMapping(Map<String, String> var1);

            @DBus.Description("This method returns the contents of a struct as separate values")
            DBus.Binding.Triplet<String, UInt32, Short> DeStruct(DBus.Binding.TestStruct var1);

            @DBus.Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
            List<Variant<Object>> Primitize(Variant<Object> var1);

            @DBus.Description("inverts it\'s input")
            boolean Invert(boolean var1);

            @DBus.Description("triggers sending of a signal from the supplied object with the given parameter")
            void Trigger(String var1, UInt64 var2);

            @DBus.Description("Causes the server to exit")
            void Exit();
        }

        public interface TestClient extends DBusInterface {
            @DBus.Description("when the trigger signal is received, this method should be called on the sending process/object.")
            void Response(UInt16 var1, double var2);

            @DBus.Description("Causes a callback")
            public static class Trigger extends DBusSignal {
                public final UInt16 a;
                public final double b;

                public Trigger(String var1, UInt16 var2, double var3) throws DBusException {
                    super(var1, new Object[]{var2, Double.valueOf(var3)});
                    this.a = var2;
                    this.b = var3;
                }
            }
        }

        public interface SingleTests extends DBusInterface {
            @DBus.Description("Returns the sum of the values in the input list")
            UInt32 Sum(byte[] var1);
        }
    }

    public interface GLib {
        @Target({ElementType.METHOD})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface CSymbol {
            String value();
        }
    }

    public interface Method {
        @Target({ElementType.METHOD})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface Error {
            String value();
        }

        @Target({ElementType.METHOD})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface NoReply {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Deprecated {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Description {
        String value();
    }

    public interface Error {
        public static class AccessDenied extends DBusExecutionException {
            public AccessDenied(String var1) {
                super(var1);
            }
        }

        public static class NoReply extends DBusExecutionException {
            public NoReply(String var1) {
                super(var1);
            }
        }

        public static class MatchRuleInvalid extends DBusExecutionException {
            public MatchRuleInvalid(String var1) {
                super(var1);
            }
        }

        public static class ServiceUnknown extends DBusExecutionException {
            public ServiceUnknown(String var1) {
                super(var1);
            }
        }

        public static class UnknownObject extends DBusExecutionException {
            public UnknownObject(String var1) {
                super(var1);
            }
        }

        public static class UnknownMethod extends DBusExecutionException {
            public UnknownMethod(String var1) {
                super(var1);
            }
        }
    }

    public static class NameAcquired extends DBusSignal {
        public final String name;

        public NameAcquired(String var1, String var2) throws DBusException {
            super(var1, new Object[]{var2});
            this.name = var2;
        }
    }

    public static class NameLost extends DBusSignal {
        public final String name;

        public NameLost(String var1, String var2) throws DBusException {
            super(var1, new Object[]{var2});
            this.name = var2;
        }
    }

    public static class NameOwnerChanged extends DBusSignal {
        public final String name;
        public final String old_owner;
        public final String new_owner;

        public NameOwnerChanged(String var1, String var2, String var3, String var4) throws DBusException {
            super(var1, new Object[]{var2, var3, var4});
            this.name = var2;
            this.old_owner = var3;
            this.new_owner = var4;
        }
    }

    public interface Local extends DBusInterface {
        public static class Disconnected extends DBusSignal {
            public Disconnected(String var1) throws DBusException {
                super(var1, new Object[0]);
            }
        }
    }

    public interface Properties extends DBusInterface {
        <A> A Get(String var1, String var2);

        <A> void Set(String var1, String var2, A var3);

        Map<String, Variant> GetAll(String var1);

//        public static class PropertiesChanged extends DBusSignal {
//            public final Map<String, Variant> changedProperties;
//            public final String[] invalidatedProperties;
//
//            public PropertiesChanged(String path,
//                                     Map<String, Variant> changedProperties,
//                                     String[] invalidatedProperties) throws DBusException {
//                super(path, changedProperties, invalidatedProperties);
//                this.changedProperties = changedProperties;
//                this.invalidatedProperties = invalidatedProperties;
//            }
//        }
    }

    public interface Introspectable extends DBusInterface {
        String Introspect();
    }

    public interface Peer extends DBusInterface {
        void Ping();
    }

}

