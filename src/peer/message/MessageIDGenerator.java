package peer.message;

public class MessageIDGenerator {

	private static short counter = Short.MIN_VALUE;

	public static short getNewID() {
		return counter++;
	}
}
