package org.xbib.rpm.trigger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for an RPM Trigger.
 */
public abstract class AbstractTrigger implements Trigger {

    protected Path script;

    protected List<Depends> depends = new ArrayList<>();

    @Override
    public Path getScript() {
        return script;
    }

    @Override
    public void setScript(Path script) {
        this.script = script;
    }

    @Override
    public void addDepends(Depends depends) {
        this.depends.add(depends);
    }

    @Override
    public Map<String, IntString> getDepends() {
        Map<String, IntString> dependsMap = new HashMap<>();
        for (Depends d : this.depends) {
            dependsMap.put(d.getName(), new IntString(d.getComparison(), d.getVersion()));
        }
        return dependsMap;
    }

    @Override
    public abstract int getFlag();
}
