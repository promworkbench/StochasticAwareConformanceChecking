package org.processmining.stochasticawareconformancechecking.automata;

import gnu.trove.TCollections;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;

public class StochasticDeterministicFiniteAutomatonImpl implements StochasticDeterministicFiniteAutomaton {

	private int maxState;

	private TIntArrayList sources;
	private TIntArrayList targets;
	private TShortArrayList activities;
	private TDoubleArrayList probabilities;

	public StochasticDeterministicFiniteAutomatonImpl() {
		maxState = 0;
		sources = new TIntArrayList();
		targets = new TIntArrayList();
		activities = new TShortArrayList();
		probabilities = new TDoubleArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.markovprocesstree.
	 * stochasticdeterministicfiniteautomaton.SDFA#getInitialState()
	 */
	@Override
	public int getInitialState() {
		return 0;
	}

	/**
	 * 
	 * @return The number of the newly created state.
	 */
	public int addState() {
		maxState++;
		return maxState;
	}

	/**
	 * Used to sort the edges.
	 * 
	 * @param source1
	 * @param target1
	 * @param activity1
	 * @param source2
	 * @param target2
	 * @param activity2
	 * @return
	 */
	private int compare(int source1, short activity1, int source2, short activity2) {
		if (source1 < source2) {
			return 1;
		} else if (source1 > source2) {
			return -1;
		} else if (activity1 < activity2) {
			return 1;
		} else if (activity1 > activity2) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Similar to Arrays.binarySearch, but works on both sources and targets
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	private int binarySearch(int source, short activity) {
		int low = 0;
		int high = sources.size() - 1;
		int mid;
		int midVal;

		while (low <= high) {
			mid = (low + high) >>> 1;
			midVal = compare(source, activity, sources.get(mid), activities.get(mid));

			if (midVal < 0)
				low = mid + 1;
			else if (midVal > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	@Override
	public void addEdge(int source, short activity, int target, double probability) {
		assert (source >= 0);
		assert (target >= 0);

		maxState = Math.max(maxState, target);

		//idea: keep the sources and targets sorted; first on source then on activity
		int from = binarySearch(source, activity);
		if (from >= 0) {
			//edge already present; replace target
			targets.set(from, target);
			probabilities.set(from, probability);
		} else {
			sources.insert(~from, source);
			targets.insert(~from, target);
			activities.insert(~from, activity);
			probabilities.insert(~from, probability);
		}
	}

	@Override
	public int addEdge(int source, short activity, double probability) {
		assert (source >= 0);

		int from = binarySearch(source, activity);
		if (from >= 0) {
			//edge already present; return target
			probabilities.set(from, probabilities.get(from) + probability);
			return targets.get(from);
		} else {
			//edge not present; insert
			int target = addState();
			sources.insert(~from, source);
			targets.insert(~from, target);
			activities.insert(~from, activity);
			probabilities.insert(~from, probability);
			return target;
		}
	}

	public TIntList getSources() {
		return TCollections.unmodifiableList(sources);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.markovprocesstree.
	 * stochasticdeterministicfiniteautomaton.SDFA#getNumberOfStates()
	 */
	@Override
	public int getNumberOfStates() {
		return maxState + 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.markovprocesstree.
	 * stochasticdeterministicfiniteautomaton.SDFA#getEdgesIterator()
	 */
	@Override
	public EdgeIterableImpl getEdgesIterator() {
		return new EdgeIterableImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.markovprocesstree.
	 * stochasticdeterministicfiniteautomaton.SDFA#getOutgoingEdgesIterator(int)
	 */
	@Override
	public EdgeIterableOutgoingImpl getOutgoingEdgesIterator(int state) {
		return new EdgeIterableOutgoingImpl(state);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.markovprocesstree.
	 * stochasticdeterministicfiniteautomaton.SDFA#getIncomingEdgesIterator(int)
	 */
	@Override
	public EdgeIterableIncomingImpl getIncomingEdgesIterator(int state) {
		return new EdgeIterableIncomingImpl(state);
	}

	public String toString() {
		return StochasticDeterministicFiniteAutomaton2Dot.toDot(this).toString();
	}

	public StochasticDeterministicFiniteAutomatonImpl clone() throws CloneNotSupportedException {
		StochasticDeterministicFiniteAutomatonImpl result = (StochasticDeterministicFiniteAutomatonImpl) super.clone();

		result.activities = new TShortArrayList(this.activities);
		result.maxState = this.maxState;
		result.probabilities = new TDoubleArrayList(this.probabilities);
		result.sources = new TIntArrayList(this.sources);
		result.targets = new TIntArrayList(this.targets);

		return result;
	}

	public final class EdgeIterableImpl implements EdgeIterable {
		private TIntIterator itSources = sources.iterator();
		private TIntIterator itTargets = targets.iterator();
		private TShortIterator itActivities = activities.iterator();
		private TDoubleIterator itProbabilities = probabilities.iterator();
		private int source;
		private int target;
		private short activity;
		private double probability;

		public boolean hasNext() {
			return itSources.hasNext();
		}

		public void next() {
			source = itSources.next();
			target = itTargets.next();
			activity = itActivities.next();
			probability = itProbabilities.next();
		}

		public int getSource() {
			return source;
		}

		public int getTarget() {
			return target;
		}

		public short getActivity() {
			return activity;
		}

		public double getProbability() {
			return probability;
		}

		public void remove() {
			itSources.remove();
			itTargets.remove();
			itActivities.remove();
			itProbabilities.remove();
		}
	}

	public class EdgeIterableIncomingImpl implements EdgeIterableIncoming {
		private int target;
		private int next;
		private int current = -1;

		private EdgeIterableIncomingImpl(int target) {
			if (target >= 0) {
				reset(target);
			}
		}

		public void reset(int target) {
			this.target = target;
			current = -1;
			next = targets.indexOf(current + 1, target);
		}

		public boolean hasNextSource() {
			return next >= 0;
		}

		public int nextSource() {
			current = next;
			next = targets.indexOf(current + 1, target);
			assert (current < activities.size());
			return sources.get(current);
		}

		public int getSource() {
			return sources.get(current);
		}

		public double getProbability() {
			return probabilities.get(current);
		}

		public short getActivity() {
			return activities.get(current);
		}
	}

	/**
	 * Iterator to iterate over the outgoing edges of a state.
	 * 
	 * @author sander
	 *
	 */
	public class EdgeIterableOutgoingImpl implements EdgeIterableOutgoing {
		private int source;
		private int next = 0;
		private int current = 0;

		private EdgeIterableOutgoingImpl(int source) {
			if (source >= 0) {
				reset(source);
			}
		}

		/**
		 * Sets the iterator to the beginning of the edges of source. If source
		 * is -1, the iterator will be empty.
		 * 
		 * @param source
		 */
		public void reset(int source) {
			this.source = source;
			if (source == -1) {
				next = sources.size();
				current = sources.size() - 1;
			} else {
				int from = sources.binarySearch(source);
				if (from < 0) {
					return;
				}
				while (from >= 0 && sources.get(from) == source) {
					from--;
				}
				next = from + 1;
				current = -1;
			}
		}

		public boolean hasNext() {
			return next < getSources().size() && getSources().get(next) == source;
		}

		public int getTarget() {
			return targets.get(current);
		}

		public short getActivity() {
			return activities.get(current);
		}

		public double getProbability() {
			return probabilities.get(current);
		}

		public int nextEdge() {
			current = next;
			next++;
			assert (current < activities.size());
			return current;
		}

		public void remove() {
			sources.removeAt(current);
			targets.removeAt(current);
			activities.removeAt(current);
			probabilities.remove(current);
			next--;
			current--;
		}

		public int nextTarget() {
			current = next;
			next++;
			return getTarget();
		}

		public short nextActivity() {
			current = next;
			next++;
			return getActivity();
		}

		public double nextProbability() {
			current = next;
			next++;
			return getProbability();
		}

		public void setProbability(double probability) {
			probabilities.set(current, probability);
		}
	}

	public boolean containsEdge(int source, short activity) {
		int from = binarySearch(source, activity);
		return from >= 0;
	}
}
