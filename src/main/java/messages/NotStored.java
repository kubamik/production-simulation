package messages;

public class NotStored implements MachineMessage {
    public final int timer;

    public NotStored(int timer) {
        this.timer = timer;
    }
}
