package peer.message;

public class MessageIDGenerator {

	private static int counter = 0;

	public static int getNewID() {
		return counter++;
	}
}
