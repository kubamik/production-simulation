import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class Machine extends AbstractBehavior<MachineMessage> {
    private final Map<ResourceType, Integer> neededResources;
    private final ResourceType createdResource;
    private final int amount;
    private final double failureRate;
    private final int instanceLimit;
    private final int time;

    private int maxTimer = 0;
    private int instanceCount = 0;
    private boolean closed = false;
    private boolean alreadyClosed = false;
    private final Map<ResourceType, Integer> resources;

    private final ActorRef<StorageMessage> storage;

    private final ActorRef<? super NextMessage> next;

    private final ActorRef<InformationMessage> manager;


    public static Behavior<MachineMessage> create(Map<ResourceType, Integer> neededResources, ResourceType createdResource,
                                                  int amount, double failureRate, int instanceLimit,
                                                  int time, ActorRef<StorageMessage> storage,
                                                  ActorRef<? super NextMessage> next,
                                                  ActorRef<InformationMessage> manager) {
        return Behaviors.setup(context -> {
            context.setLoggerName(context.getSelf().path().name());
            return new Machine(context, neededResources, createdResource, amount, failureRate,
                    instanceLimit, time, storage, next, manager);
        });
    }

    protected Machine(ActorContext<MachineMessage> context, Map<ResourceType, Integer> neededResources,
                      ResourceType createdResource, int amount, double failureRate, int instanceLimit, int time,
                      ActorRef<StorageMessage> storage, ActorRef<? super NextMessage> next,
                      ActorRef<InformationMessage> manager) {
        super(context);
        this.neededResources = neededResources;
        this.createdResource = createdResource;
        this.amount = amount;
        this.failureRate = failureRate;
        this.instanceLimit = instanceLimit;
        this.time = time;
        this.resources = new EnumMap<>(ResourceType.class);
        this.storage = storage;
        this.next = next;
        this.manager = manager;
    }

    private void start(int timer) {
        instanceCount++;
        for (Map.Entry<ResourceType, Integer> entry : neededResources.entrySet()) {
            resources.put(entry.getKey(), resources.get(entry.getKey()) - entry.getValue());
        }
        getContext().getLog().info("sent ProductionStartedMessage on {} to {}", timer, manager.path().name());
        manager.tell(new ProductionStartedMessage(getContext().getSelf(), time, timer));
    }

    private void tryStart(int timer) {
        if (instanceCount >= instanceLimit) {
            return;
        }
        int needed = 0;
        for (Map.Entry<ResourceType, Integer> entry : neededResources.entrySet()) {
            if (resources.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                Needed msg = new Needed(entry.getValue() - resources.getOrDefault(entry.getKey(), 0),
                        entry.getKey(), getContext().getSelf(), timer);
                getContext().getLog().info("sent Needed: {} {} on {} to {}", msg.resource, msg.amount,
                        msg.timer, storage.path().name());
                storage.tell(msg);
                needed++;
            }
        }
        if (needed == 0 && instanceCount < instanceLimit) {
            start(timer);
        }
    }

    public Behavior<MachineMessage> onFinish(ProductionEndSignal message) {
        instanceCount--;
        Random random = new Random();
        if (message.timer > maxTimer) {
            maxTimer = message.timer;
        }
        if (random.nextDouble() > failureRate) {
            getContext().getLog().info("sent Resource: {} {} on {} to {}", amount, createdResource,
                    maxTimer, next.path().name());
            next.tell(new Resource(amount, createdResource, maxTimer));
        }
        tryStart(maxTimer);
        return this;
    }

    public Behavior<MachineMessage> onResource(Resource resource) {
        if (resource.timer > maxTimer) {
            maxTimer = resource.timer;
        }
        resources.put(resource.resource, resources.getOrDefault(resource.resource, 0) + resource.amount);
        tryStart(maxTimer);
        return this;
    }

    public Behavior<MachineMessage> maybeClose(int timer) {
        if (!alreadyClosed && closed && instanceCount == 0) {
            alreadyClosed = true;
            getContext().getLog().info("sent Closed on {} to {}", timer, next.path().name());
            next.tell(new Closed(getContext().getSelf(), timer));
            getContext().getLog().info("sent Closed on {} to {}", timer, manager.path().name());
            manager.tell(new Closed(getContext().getSelf(), timer));
            return Behaviors.stopped();
        }
        return this;
    }

    private Behavior<MachineMessage> onProductionStartSignal(ProductionStartSignal signal) {
        tryStart(0);
        return this;
    }


    @Override
    public Receive<MachineMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(Resource.class, this::onResource)
                .onMessage(ProductionStartSignal.class, this::onProductionStartSignal)
                .onMessage(ProductionEndSignal.class, this::onFinish)
                .onMessage(Closed.class, message -> {
                    closed = true;
                    if (message.timer > maxTimer) {
                        maxTimer = message.timer;
                    }
                    tryStart(maxTimer);
                    return this;
                })
                .onMessage(NotEnough.class, message -> {
                    closed = true;
                    if (message.timer > maxTimer) {
                        maxTimer = message.timer;
                    }
                    return maybeClose(maxTimer);
                })
                .onMessage(NotStored.class, message -> {
                    if (message.timer > maxTimer) {
                        maxTimer = message.timer;
                    }
                    return maybeClose(maxTimer);
                })
                .build();
    }
}
