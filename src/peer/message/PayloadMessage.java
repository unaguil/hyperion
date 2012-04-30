package peer.message;

public interface PayloadMessage {

	public PayloadMessage copy();

	public String getType();
}
