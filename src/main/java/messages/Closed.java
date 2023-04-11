package messages;

import akka.actor.typed.ActorRef;

public class Closed implements InformationMessage, NextMessage {
    public final ActorRef<MachineMessage> author;
    public final int timer;

    public Closed(ActorRef<MachineMessage> author, int timer) {
        this.author = author;
        this.timer = timer;
    }
}
