/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package peer.conditionregister;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import util.timer.Timer;
import util.timer.TimerTask;

/**
 * This generic class provides a register whose elements are checked for removal
 * using some condition. The removal condition can be specified during
 * construction.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 * @param <T>
 *            the type of objects to be stored in the timed register which must
 *            correctly implement hashCode() and equals().
 */
public final class ConditionRegister<T> implements TimerTask {

	// Inner class used to store data and time stamp information.
	private class Entry<O> {

		// Data stored by the entry
		public final O e;

		// Time stamp of the entry. It could be used to check entries for
		// removal.
		public final long timestamp;

		public Entry(final O e, final long timestamp) {
			this.e = e;
			this.timestamp = timestamp;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof Entry))
				return false;

			final Entry<O> entry = (Entry<O>) o;

			return this.e.equals(entry.e);
		}

		@Override
		public int hashCode() {
			return e.hashCode();
		}

		@Override
		public String toString() {
			return e.toString();
		}

		public O getEntry() {
			return e;
		}
	}

	/***
	 * This interface defines the method for check removal condition.
	 * 
	 * @param <T>
	 *            Class used during removal condition checking.
	 */
	public interface RemovalCondition<T> {

		public boolean checkRemoval(T data, long timestamp);
	}

	/**
	 * This interface defines an equality condition for object containment.
	 * 
	 * @param <T>
	 *            Class used during equality checking
	 */
	public interface EqualityCondition<T> {

		public boolean areEquals(T a, T b);
	}

	/**
	 * This is an implementation of the RemovalCondition which uses the time
	 * stamp to decide if the entry must be removed.
	 * 
	 */
	class TimeRemovalCondition implements RemovalCondition<T> {

		private final long period;

		public TimeRemovalCondition(final long time) {
			this.period = time;
		}

		@Override
		public boolean checkRemoval(final T data, final long timestamp) {
			return (System.currentTimeMillis() - timestamp) >= period;
		}
	}

	// Set which contains the data stored in the register.
	private final Set<Entry<T>> entries = Collections.synchronizedSet(new HashSet<Entry<T>>());

	// The removal condition using to check entries
	private final RemovalCondition<T> removalCondition;

	// the removal thread
	private final Timer removalThread;

	/**
	 * Constructor used to create a register. It will start a timer to launch
	 * removal checks.
	 * 
	 * @param removalPeriod
	 *            the period used to check the entries for removal
	 */
	public ConditionRegister(final long removalPeriod) {
		removalCondition = new TimeRemovalCondition(removalPeriod);
		removalThread = new Timer(500, this);
	}

	/**
	 * Constructor used to create a register. It will start a timer to launch
	 * removal checks.
	 * 
	 * @param removalPeriod
	 *            the period used to check the entries for removal
	 * @param removalCondition
	 *            condition used to check entries for removal
	 */
	public ConditionRegister(final long removalPeriod, final RemovalCondition<T> removalCondition) {
		this.removalCondition = removalCondition;
		removalThread = new Timer(500, this);
	}

	/**
	 * Starts the elements periodic removal.
	 */
	public void start() {
		removalThread.start();
	}

	/**
	 * Checks if the register contains the specified entry.
	 * 
	 * @param entry
	 *            the entry to check if it is contained in the register
	 * @param equalityCondition
	 *            an implementation which tells how objects are checked for
	 *            equality
	 * @return true if the entry is contained in the register, false otherwise.
	 */
	public boolean contains(final T entry, final EqualityCondition<T> equalityCondition) {
		for (final Entry<T> currentEntry : entries)
			if (equalityCondition.areEquals(entry, currentEntry.getEntry()))
				return true;
		return false;
	}

	/**
	 * Checks if the register contains the specified entry. Uses the object
	 * equals method
	 * 
	 * @param entry
	 *            the entry to check if it is contained in the register
	 * @return true if the entry is contained in the register, false otherwise.
	 */
	public boolean contains(final T entry) {
		final Entry<T> searchedEntry = new Entry<T>(entry, System.currentTimeMillis());
		return entries.contains(searchedEntry);
	}

	/**
	 * Adds a new entry to the register
	 * 
	 * @param entry
	 *            the entry to be added
	 */
	public void addEntry(final T entry) {
		entries.add(new Entry<T>(entry, System.currentTimeMillis()));
	}

	/**
	 * Gets the registered entries
	 * 
	 * @return the registered entries
	 */
	public Set<T> getEntries() {
		final Set<T> currentEntries = new HashSet<T>();

		synchronized (entries) {
			for (final Entry<T> entry : entries)
				currentEntries.add(entry.e);
		}

		return currentEntries;
	}
	
	public boolean remove(T entry) {
		return entries.remove(new Entry<T>(entry, System.currentTimeMillis()));
	}

	/**
	 * Stops the internal timer of the register.
	 */
	public void stopAndWait() {
		removalThread.stopAndWait();
	}

	// Checks the entries for removal
	private void checkEntries() {
		synchronized (entries) {
			for (final Iterator<Entry<T>> it = entries.iterator(); it.hasNext();) {
				final Entry<T> e = it.next();
				if (removalCondition.checkRemoval(e.e, e.timestamp))
					it.remove();
			}
		}
	}

	@Override
	public void perform() {
		checkEntries();
	}

	@Override
	public String toString() {
		String result;
		synchronized (entries) {
			result = entries.toString();
		}
		return result;
	}
}
