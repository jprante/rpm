package org.xbib.rpm.trigger;

import java.nio.file.Path;
import java.util.Map;

/**
 *
 */
public interface Trigger {

    Path getScript();

    void setScript(Path script);

    void addDepends(Depends depends);

    Map<String, IntString> getDepends();

    int getFlag();

    /**
     * Simple class to pair an int and a String with each other.
     */
    class IntString {

        private int theInt = 0;

        private String theString = "";

        public IntString(int theInt, String theString) {
            this.theInt = theInt;
            this.theString = theString;
        }

        public int getInt() {
            return this.theInt;
        }

        public void setInt(int theInt) {
            this.theInt = theInt;
        }

        public String getString() {
            return this.theString;
        }

        public void setString(String theString) {
            this.theString = theString;
        }

    }
}
