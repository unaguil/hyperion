package graphcreation.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PowerSet {

	static class AscComparator<E> implements Comparator<Set<E>> {

		@Override
		public int compare(final Set<E> a, final Set<E> b) {
			return (a.size() - b.size());
		}

	}

	static class DescComparator<E> implements Comparator<Set<E>> {

		@Override
		public int compare(final Set<E> a, final Set<E> b) {
			return (b.size() - a.size());
		}

	}

	// From
	// http://jvalentino.blogspot.com/2007/02/shortcut-to-calculating-power-set-using.html
	public static void main(final String[] args) {

		// construct the set S = {a,b,c}
		final Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < 3; i++)
			set.add(Integer.valueOf(i));

		System.out.println("Initial elements: " + set.size());
		System.out.println("Expected elements: " + Math.pow(2, set.size()));
		// form the power set
		final long time = System.currentTimeMillis();
		final Set<Set<Integer>> myPowerSet = PowerSet.powersetAsc(set);
		System.out.println("Time: " + (System.currentTimeMillis() - time));

		// display the power set
		System.out.println(myPowerSet.toString());

		System.out.println("Calculated elements: " + myPowerSet.size());
	}

	/**
	 * Returns the power set from the given set by using a binary counter
	 * ordered by size Example: S = {a,b,c} P(S) = {[], [c], [b], [b, c], [a],
	 * [a, c], [a, b], [a, b, c]}
	 * 
	 * @param set
	 *            String[]
	 * @return LinkedHashSet
	 */
	public static <E> Set<Set<E>> powersetAsc(final Set<E> superSet) {
		final List<Set<E>> list = powerset(superSet);
		Collections.sort(list, new AscComparator<E>());
		return new LinkedHashSet<Set<E>>(list);
	}

	private static <E> List<Set<E>> powerset(final Set<E> superSet) {
		final List<E> listSet = new ArrayList<E>(superSet);
		// create the empty power set
		final Set<Set<E>> power = new LinkedHashSet<Set<E>>();

		// get the number of elements in the set
		final int elements = listSet.size();

		// the number of members of a power set is 2^n
		final int powerElements = (int) Math.pow(2, elements);

		// run a binary counter for the number of power elements
		for (int i = 0; i < powerElements; i++) {

			// convert the binary number to a String containing n digits
			final String binary = intToBinary(i, elements);

			// create a new set
			final LinkedHashSet<E> innerSet = new LinkedHashSet<E>();

			// convert each digit in the current binary number to the
			// corresponding element
			// in the given set
			for (int j = 0; j < binary.length(); j++)
				if (binary.charAt(j) == '1')
					innerSet.add(listSet.get(j));

			// add the new set to the power set
			power.add(innerSet);

		}

		final List<Set<E>> list = new ArrayList<Set<E>>(power);
		list.remove(new LinkedHashSet<E>());
		return list;
	}

	public static <E> Set<Set<E>> powersetDesc(final Set<E> superSet) {
		final List<Set<E>> list = powerset(superSet);
		Collections.sort(list, new DescComparator<E>());
		return new LinkedHashSet<Set<E>>(list);
	}

	/**
	 * Converts the given integer to a String representing a binary number with
	 * the specified number of digits For example when using 4 digits the binary
	 * 1 is 0001
	 * 
	 * @param binary
	 *            int
	 * @param digits
	 *            int
	 * @return String
	 */
	private static String intToBinary(final int binary, final int digits) {

		final String temp = Integer.toBinaryString(binary);
		final int foundDigits = temp.length();
		String returner = temp;
		for (int i = foundDigits; i < digits; i++)
			returner = "0" + returner;

		return returner;
	}
}
