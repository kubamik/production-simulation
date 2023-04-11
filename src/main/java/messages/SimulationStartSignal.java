package messages;

import java.util.Map;

public class SimulationStartSignal implements InformationMessage {
    public final double speedup;
    public final Map<ResourceType, Integer> resources;

    public SimulationStartSignal(double speedup, Map<ResourceType, Integer> resources) {
        this.speedup = speedup;
        this.resources = resources;
    }
}
