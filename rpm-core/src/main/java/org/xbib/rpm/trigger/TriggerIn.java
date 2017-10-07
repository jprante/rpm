package org.xbib.rpm.trigger;

import org.xbib.rpm.format.Flags;

/**
 * A TriggerIn.
 */
public class TriggerIn extends AbstractTrigger implements Trigger {

    @Override
    public int getFlag() {
        return Flags.SCRIPT_TRIGGERIN;
    }
}
