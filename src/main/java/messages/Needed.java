package messages;

import akka.actor.typed.ActorRef;

public class Needed implements StorageMessage {
    public final int amount;
    public final ResourceType resource;
    public final ActorRef<MachineMessage> sender;

    public final int timer;

    public Needed(int amount, ResourceType resource, ActorRef<MachineMessage> sender, int timer) {
        this.amount = amount;
        this.resource = resource;
        this.sender = sender;
        this.timer = timer;
    }
}
