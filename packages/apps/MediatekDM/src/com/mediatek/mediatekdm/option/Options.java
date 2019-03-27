
package com.mediatek.mediatekdm.option;

public final class Options {
    public static final boolean USEDIRECTINTERNET = false;
    public static final boolean USESMSREGISTER = true;
    public static final boolean USESCHEDPOLLING = false;

    public static final class Polling {
        /***
         * polling interval = INTERVAL_BASE + random(INTERVAL_RANDOM) in
         * seconds.
         */
        public static final int INTERVAL_BASE = 10 * 24 * 3600 * 1000;
        public static final int INTERVAL_RANDOM = 7 * 24 * 3600 * 1000;
    }

    public static final class DLTimeoutWait {
        public static final boolean FEATUREON = true;
        public static final long WAIT_INTERVAL = 5 * 60 * 1000;
    }
}
