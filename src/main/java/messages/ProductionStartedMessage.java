package messages;

import akka.actor.typed.ActorRef;

public class ProductionStartedMessage implements InformationMessage {
    public final ActorRef<MachineMessage> machine;
    public final int time;
    public final int timer;

    public ProductionStartedMessage(ActorRef<MachineMessage> machine, int time, int timer) {
        this.machine = machine;
        this.time = time;
        this.timer = timer;
    }
}
