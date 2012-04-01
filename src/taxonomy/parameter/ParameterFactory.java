package taxonomy.parameter;

import taxonomy.Taxonomy;

public class ParameterFactory {

	public static Parameter createParameter(final String fullID, Taxonomy taxonomy) throws InvalidParameterIDException {
		checkID(fullID);

		if (isInput(fullID))
			return new InputParameter(taxonomy.encode(getID(fullID)));
		else if (isOutput(fullID))
			return new OutputParameter(taxonomy.encode(getID(fullID)));
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
