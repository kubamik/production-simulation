import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;

import java.util.Map;

public class Storage extends AbstractBehavior<StorageMessage> {
    public Map<ResourceType, Integer> resources;
    public Storage(ActorContext<StorageMessage> context, Map<ResourceType, Integer> resources) {
        super(context);
        this.resources = resources;
    }

    public static Behavior<StorageMessage> create(Map<ResourceType, Integer> resources) {
        return Behaviors.setup(context -> new Storage(context, resources));
    }

    private Behavior<StorageMessage> onNeeded(Needed needed) {
        if (resources.containsKey(needed.resource)) {
            if (resources.get(needed.resource) >= needed.amount) {
                getContext().getLog().info("sent Resource: {} {} to {}", needed.amount, needed.resource,
                    needed.sender.path().name());
                resources.put(needed.resource, resources.get(needed.resource) - needed.amount);
                needed.sender.tell(new Resource(needed.amount, needed.resource, needed.timer));
            } else {
                getContext().getLog().info("sent NotEnough on {} to {}", needed.timer, needed.sender.path().name());
                needed.sender.tell(new NotEnough(needed.timer));
            }
        } else {
            getContext().getLog().info("sent NotStored on {} to {}", needed.timer, needed.sender.path().name());
            needed.sender.tell(new NotStored(needed.timer));
        }

        return this;
    }

    private Behavior<StorageMessage> onResource(Resource resource) {
        resources.put(resource.resource, resources.getOrDefault(resource.resource, 0) + resource.amount);
        return this;
    }

    private Behavior<StorageMessage> onPrint(Print print) {
        for (Map.Entry<ResourceType, Integer> entry : resources.entrySet()) {
            getContext().getLog().info("{}: {}", entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public Receive<StorageMessage> createReceive() {
        return newReceiveBuilder().onMessage(Needed.class, this::onNeeded)
                .onMessage(Resource.class, this::onResource)
                .onMessage(Print.class, this::onPrint).build();
    }
}
