package org.xbib.rpm.trigger;

import org.xbib.rpm.format.Flags;

/**
 * A TriggerPostUn.
 */
public class TriggerPostUn extends AbstractTrigger implements Trigger {

    @Override
    public int getFlag() {
        return Flags.SCRIPT_TRIGGERPOSTUN;
    }
}
