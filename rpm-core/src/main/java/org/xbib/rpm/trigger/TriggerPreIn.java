package org.xbib.rpm.trigger;

import org.xbib.rpm.format.Flags;

/**
 * A TriggerPreIn.
 */
public class TriggerPreIn extends AbstractTrigger implements Trigger {

    @Override
    public int getFlag() {
        return Flags.SCRIPT_TRIGGERPREIN;
    }
}
