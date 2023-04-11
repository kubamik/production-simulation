import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Manager extends AbstractBehavior<InformationMessage> {
    private ActorRef<StorageMessage> storageRef;
    private Set<ActorRef<?>> closedMachines = new HashSet<>();
    private int maxTimer = 0;

    private double speedup;

    public static Behavior<InformationMessage> create() {
        return Behaviors.setup(Manager::new);
    }

    public Manager(ActorContext<InformationMessage> context) {
        super(context);
    }

    private Behavior<InformationMessage> onStart(SimulationStartSignal startMessage) {
        speedup = startMessage.speedup;
        getContext().getLog().info("Simulation started with speedup: {}", speedup);
        Behavior<StorageMessage> storage = Storage.create(startMessage.resources);
        storageRef = getContext().spawn(storage, "Storage");

        Behavior<MachineMessage> bottler = Machine.create(Map.of(ResourceType.FILTERED_WINE, 75, ResourceType.BOTTLE, 1),
                ResourceType.WINE_BOTTLE, 1, 0.05, 1,
                5 * 60, storageRef, storageRef, getContext().getSelf());

        Behavior<MachineMessage> filter = Machine.create(Map.of(ResourceType.UNFILTERED_WINE, 2500),
                ResourceType.FILTERED_WINE, 2400, 0, 10,
                12 * 60 * 60, storageRef, getContext().spawn(bottler, "Bottler"),
                getContext().getSelf());

        Behavior<MachineMessage> fermentator = Machine.create(Map.of(ResourceType.GRAPE_JUICE, 1500,
                        ResourceType.SUGAR, 200, ResourceType.WATER, 800), ResourceType.UNFILTERED_WINE,
                2500, 0.05, 10, 14 * 24 * 60 * 60,
                storageRef, getContext().spawn(filter, "Filter"), getContext().getSelf());

        Behavior<MachineMessage> stamper = Machine.create(Map.of(ResourceType.GRAPE, 1500), ResourceType.GRAPE_JUICE,
                1000, 0, 1, 12 * 60 * 60, storageRef,
                getContext().spawn(fermentator, "Fermentator"), getContext().getSelf());

        getContext().getLog().info("sent ProductionStartSignal to Stamper");
        getContext().spawn(stamper, "Stamper").tell(new ProductionStartSignal());
        return this;
    }

    private Behavior<InformationMessage> onProductionStart(ProductionStartedMessage productionStartedMessage) {
        Logger log = getContext().getLog();
        getContext().getSystem().scheduler().scheduleOnce(
                Duration.ofMillis(Math.round(productionStartedMessage.time * 1000 / speedup)),
                () -> {
                    log.info("sent ProductionEndSignal on {} to {}",
                        productionStartedMessage.timer + productionStartedMessage.time,
                        productionStartedMessage.machine.path().name());
                    productionStartedMessage.machine.tell(
                            new ProductionEndSignal(productionStartedMessage.timer + productionStartedMessage.time));
                },
                getContext().getSystem().executionContext());
        return this;
    }

    public Behavior<InformationMessage> onClosed(Closed message) {
        closedMachines.add(message.author);
        if (message.timer > maxTimer) {
            maxTimer = message.timer;
        }
        if (closedMachines.size() == 4) {
            finish();
            return Behaviors.stopped();
        }
        return this;
    }

    private void finish() {
        Duration duration = Duration.ofSeconds(maxTimer);
        getContext().getLog().info("Simulation finished: {}d {}h {}m {}s", duration.toDaysPart(),
                duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
        getContext().getLog().info("sent Print to Storage");
        storageRef.tell(new Print());
        getContext().getSystem().terminate();
    }

    @Override
    public Receive<InformationMessage> createReceive() {
        return newReceiveBuilder().onMessage(SimulationStartSignal.class, this::onStart)
                .onMessage(ProductionStartedMessage.class, this::onProductionStart)
                .onMessage(Closed.class, this::onClosed).build();
    }
}
