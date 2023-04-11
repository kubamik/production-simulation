package messages;

public class NotEnough implements MachineMessage {
    public final int timer;

    public NotEnough(int timer) {
        this.timer = timer;
    }
}
