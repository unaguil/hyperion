package peer.message;

public class MessageIDGenerator {

	private static long counter = 0;

	public static long getNewID() {
		return counter++;
	}
}
