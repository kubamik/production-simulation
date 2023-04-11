package messages;

public class ProductionEndSignal implements MachineMessage {
    public final int timer;

    public ProductionEndSignal(int timer) {
        this.timer = timer;
    }
}
