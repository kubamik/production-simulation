import akka.actor.typed.ActorSystem;
import messages.InformationMessage;
import messages.ResourceType;
import messages.SimulationStartSignal;

import java.util.EnumMap;
import java.util.Map;

public class Zad3 {
    public static void main(String[] args) {
        ActorSystem<InformationMessage> system = ActorSystem.create(Manager.create(), "Manager");
        Map<ResourceType, Integer> resources = new EnumMap<>(
                Map.of(ResourceType.GRAPE, 50000, ResourceType.SUGAR, 50000,
                ResourceType.WATER, 50000, ResourceType.BOTTLE, 10000));
        double speedup = 100000;
        system.tell(new SimulationStartSignal(speedup, resources));
        system.log().info("sent SimulationStartSignal: {}, {} to Manager", resources, speedup);
    }
}
