package org.xbib.rpm.trigger;

import org.xbib.rpm.format.Flags;

/**
 * A TriggerUn.
 */
public class TriggerUn extends AbstractTrigger implements Trigger {

    @Override
    public int getFlag() {
        return Flags.SCRIPT_TRIGGERUN;
    }
}
