package org.processmining.stochasticawareconformancechecking.automata;

public interface StochasticDeterministicFiniteAutomaton extends Cloneable {

	int getInitialState();

	/**
	 * 
	 * @return The highest index of any state in the automaton + 1. Does not
	 *         necessarily count the number of states, but gives an upper bound.
	 */
	int getNumberOfStates();

	boolean containsEdge(int stateFrom, short activity);

	EdgeIterable getEdgesIterator();

	EdgeIterableOutgoing getOutgoingEdgesIterator(int state);

	EdgeIterableIncoming getIncomingEdgesIterator(int state);

	/**
	 * Add an edge to the graph. If there is already an activity-edge outgoing
	 * of source, its target is replaced by the given target. The probability is
	 * always replaced.
	 * 
	 * @param source
	 * @param activity
	 * @param target
	 * @param probability
	 * @return The index of the target state (which may be newly created).
	 */
	int addEdge(int source, short activity, double probability);

	/**
	 * Adds an edge to the graph. Returns the (possibly new) target state. If
	 * the edge was already present, the probability is added to the existing
	 * edge.
	 * 
	 * @param source
	 * @param activity
	 * @param probability
	 * @return
	 */
	void addEdge(int source, short activity, int target, double probability);

	int addState();

	public StochasticDeterministicFiniteAutomaton clone() throws CloneNotSupportedException;

	public interface EdgeIterable {

		public boolean hasNext();

		public void next();

		public int getSource();

		public int getTarget();

		public short getActivity();

		public double getProbability();

		public void remove();
	}

	public interface EdgeIterableIncoming {

		public void reset(int target);

		public boolean hasNextSource();

		public int nextSource();

		public int getSource();

		public double getProbability();

		public short getActivity();
	}

	public interface EdgeIterableOutgoing {

		/**
		 * Sets the iterator to the beginning of the edges of source. If source
		 * is -1, the iterator will be empty.
		 * 
		 * @param source
		 */
		public void reset(int source);

		public boolean hasNext();

		public int getTarget();

		public short getActivity();

		public double getProbability();

		public int nextEdge();

		public void remove();

		public int nextTarget();

		public short nextActivity();

		public double nextProbability();

		public void setProbability(double probability);
	}
}