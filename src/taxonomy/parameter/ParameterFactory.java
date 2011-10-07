package taxonomy.parameter;

public class ParameterFactory {

	public static Parameter createParameter(final String fullID) throws InvalidParameterIDException {
		checkID(fullID);

		if (isInput(fullID))
			return new InputParameter(getID(fullID));
		else if (isOutput(fullID))
			return new OutputParameter(getID(fullID));
		else
			throw new InvalidParameterIDException("Parameter should have the following format: I-id or O-id. " + fullID + " received.");
	}

	private static void checkID(final String fullID) throws InvalidParameterIDException {
		if (fullID.length() < 3)
			throw new InvalidParameterIDException("Parameter should have the following format: I-id or O-id. " + fullID + " received.");
	}

	private static boolean isInput(final String fullID) {
		return fullID.substring(0, 2).equals("I-");
	}

	private static boolean isOutput(final String fullID) {
		return fullID.substring(0, 2).equals("O-");
	}

	private static String getID(final String fullID) {
		return fullID.substring(2, fullID.length());
	}
}
