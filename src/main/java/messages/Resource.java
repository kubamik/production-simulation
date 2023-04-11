package messages;

public class Resource implements NextMessage {
    public final int amount;
    public final ResourceType resource;
    public final int timer;

    public Resource(int amount, ResourceType resource, int timer) {
        this.amount = amount;
        this.resource = resource;
        this.timer = timer;
    }
}
