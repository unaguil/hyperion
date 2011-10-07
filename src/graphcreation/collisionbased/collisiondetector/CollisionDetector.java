package graphcreation.collisionbased.collisiondetector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class CollisionDetector {

	// gets the parameters which are colliding in the current node
	public static Set<Collision> getParametersColliding(final Set<Parameter> newParameters, final Set<Parameter> allParameters, final boolean checkAddedParameters, final Taxonomy taxonomy) {
		// Get old parameters (all parameters - new parameters)
		final Set<Parameter> oldParameters = new HashSet<Parameter>(allParameters);
		oldParameters.removeAll(newParameters);

		// Filter parameters using I/O attribute
		final Set<OutputParameter> newOutputParams = getOutputParams(newParameters);
		final Set<InputParameter> newInputParams = getInputParams(newParameters);
		final Set<OutputParameter> oldOutputParams = getOutputParams(oldParameters);
		final Set<InputParameter> oldInputParams = getInputParams(oldParameters);

		// Detect collisions
		final Set<Collision> collisions = new HashSet<Collision>();
		// Among new output parameters and old input parameters
		collisions.addAll(computeCollisions(newOutputParams, oldInputParams, taxonomy));
		// Among old output parameters and new input parameters
		collisions.addAll(computeCollisions(oldOutputParams, newInputParams, taxonomy));

		if (checkAddedParameters)
			// Among new output parameters and new input parameters
			collisions.addAll(computeCollisions(newOutputParams, newInputParams, taxonomy));

		return collisions;
	}

	// Gets those parameters which are input parameters
	private static Set<InputParameter> getInputParams(final Set<Parameter> parameters) {
		final Set<InputParameter> inputParameters = new HashSet<InputParameter>();
		for (final Parameter p : parameters)
			if (p instanceof InputParameter)
				inputParameters.add((InputParameter) p);
		return inputParameters;
	}

	// Gets those parameters which are output parameters
	private static Set<OutputParameter> getOutputParams(final Set<Parameter> parameters) {
		final Set<OutputParameter> outputParameters = new HashSet<OutputParameter>();
		for (final Parameter p : parameters)
			if (p instanceof OutputParameter)
				outputParameters.add((OutputParameter) p);
		return outputParameters;
	}

	// Obtains the collisions among the passed parameters
	private static List<Collision> computeCollisions(final Set<OutputParameter> outputParameters, final Set<InputParameter> inputParameters, final Taxonomy taxonomy) {
		final List<Collision> collisions = new ArrayList<Collision>();
		for (final OutputParameter output : outputParameters)
			for (final InputParameter input : inputParameters)
				// If a parameters are related through taxonomy a collision is
				// detected
				if (taxonomy.areRelated(input.getID(), output.getID()))
					collisions.add(new Collision(input, output));
		return collisions;
	}
}
