package org.processmining.stochasticawareconformancechecking.automata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.helperclasses.MarkingSet;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.strategy.HashingStrategy;

public class StochasticPetriNet2StochasticDeterministicFiniteAutomaton {

	/**
	 * For now, the final markings are not used. That is, the only final
	 * markings are deadlock states, and every deadlock state is a final
	 * marking. Furthermore, we assume that silent transitions have no weight.
	 * 
	 * @param net
	 * @param initialMarking
	 * @param finalMarkings
	 * @return
	 * @throws IllegalTransitionException
	 * @throws UnsupportedPetriNetException
	 */
	public static StochasticDeterministicFiniteAutomatonMapped<String> convert(StochasticNet net,
			Marking initialMarking, Iterable<Marking> finalMarkings)
			throws IllegalTransitionException, UnsupportedPetriNetException {
		EfficientStochasticNetSemanticsImpl s = new EfficientStochasticNetSemanticsImpl();

		s.initialize(net.getTransitions(), initialMarking);

		//transform the markings
		s.setCurrentState(initialMarking);
		short[] initialMarkingS = s.getCurrentInternalState().clone();

		List<short[]> finalMarkingsS = new ArrayList<>();
		for (Marking finalMarking : finalMarkings) {
			s.setCurrentState(finalMarking);
			finalMarkingsS.add(s.getCurrentInternalState().clone());
		}

		return convert(net, initialMarkingS, finalMarkingsS, s);
	}

	public static StochasticDeterministicFiniteAutomatonMapped<String> convert(StochasticNet net,
			short[] initialMarking, Iterable<short[]> finalMarkings, EfficientStochasticNetSemanticsImpl s)
			throws IllegalTransitionException, UnsupportedPetriNetException {
		StochasticDeterministicFiniteAutomatonMappedImpl<String> result = new StochasticDeterministicFiniteAutomatonMappedImpl<>();

		ArrayDeque<short[]> worklist = new ArrayDeque<>();

		TObjectIntMap<short[]> marking2state = new TObjectIntCustomHashMap<>(new HashingStrategy<short[]>() {
			private static final long serialVersionUID = -6010145318118370427L;

			public int computeHashCode(short[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(short[] o1, short[] o2) {
				return Arrays.equals(o1, o2);
			}
		}, 10, 0.5f, -1);

		//put the initial state
		marking2state.put(initialMarking, result.getInitialState());
		worklist.add(initialMarking);

		while (!worklist.isEmpty()) {
			short[] marking = worklist.remove();
			int source = marking2state.get(marking);

			s.setCurrentState(marking);
			Set<TransitionMarkingPair> enabledTransitions = getEnabledTransitions(s, marking);

			//first, compute the sum of weights
			double sumWeights = 0;
			for (TransitionMarkingPair pair : enabledTransitions) {
				sumWeights += ((TimedTransition) pair.transition).getWeight();
			}

			//second, put the transitions into the automaton
			for (TransitionMarkingPair pair : enabledTransitions) {
				Transition t = pair.transition;
				short[] newMarking = pair.marking;
				short activity = result.transform(t.getLabel());
				double probability = ((TimedTransition) t).getWeight() / sumWeights;
				int target = marking2state.get(newMarking);
				if (target == marking2state.getNoEntryValue()) {
					target = result.addEdge(source, activity, probability);
					marking2state.put(newMarking, target);

					worklist.add(newMarking);
				} else {
					result.addEdge(source, activity, target, probability);
				}
			}
		}

		return result;
	}

	private static double getSumOfWeights(EfficientStochasticNetSemanticsImpl semantics, int state) {
		double sumWeights = 0;
		for (Transition t : semantics.getExecutableTransitions()) {
			sumWeights += ((TimedTransition) t).getWeight();
		}
		return sumWeights;
	}

	private static class TransitionMarkingPair {
		Transition transition;
		short[] marking;

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(marking);
			result = prime * result + ((transition == null) ? 0 : transition.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TransitionMarkingPair other = (TransitionMarkingPair) obj;
			if (!Arrays.equals(marking, other.marking))
				return false;
			if (transition == null) {
				if (other.transition != null)
					return false;
			} else if (!transition.equals(other.transition))
				return false;
			return true;
		}
	}

	/**
	 * search through the enabled state spaces following tau transitions for
	 * non-silent transitions that are enabled.
	 * 
	 * @return
	 * @throws IllegalTransitionException
	 */
	private static Set<TransitionMarkingPair> getEnabledTransitions(EfficientStochasticNetSemanticsImpl semantics,
			short[] startMarking) throws IllegalTransitionException {
		Set<TransitionMarkingPair> result = new THashSet<>();
		TCustomHashSet<short[]> visited = new TCustomHashSet<>(new HashingStrategy<short[]>() {
			public int computeHashCode(short[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(short[] o1, short[] o2) {
				return Arrays.equals(o1, o2);
			}
		});

		ArrayDeque<short[]> localWorklist = new ArrayDeque<>();
		localWorklist.add(startMarking);
		visited.add(startMarking);

		while (!localWorklist.isEmpty()) {
			short[] marking = localWorklist.poll();

			semantics.setCurrentState(marking);
			Collection<Transition> enabledTransitions = semantics.getExecutableTransitions();

			for (Transition t : enabledTransitions) {
				semantics.setCurrentState(marking);
				semantics.executeExecutableTransition(t);
				if (t.isInvisible()) {
					short[] m = semantics.getCurrentInternalState().clone();
					if (!visited.contains(m)) {
						localWorklist.add(m);
						visited.add(m);
					}
				} else {
					TransitionMarkingPair pair = new TransitionMarkingPair();
					pair.marking = semantics.getCurrentInternalState().clone();
					pair.transition = t;
					result.add(pair);
				}
			}

		}

		return result;
	}

	/**
	 * Walk through silent transitions.
	 * 
	 * @param semantics
	 * @param result
	 * @param startState
	 * @param exploreMarking
	 * @param probabilityUpToExploreState
	 * @param marking2state
	 * @param globalWorklist
	 * @return The probability that the net terminates in this state.
	 * @throws UnsupportedPetriNetException
	 * @throws IllegalTransitionException
	 */
	private static double performForwardSearch(EfficientStochasticNetSemanticsImpl semantics,
			StochasticDeterministicFiniteAutomatonMappedImpl<String> result, int startState, short[] exploreMarking,
			double probabilityUpToExploreState, TObjectIntMap<short[]> marking2state,
			ArrayDeque<short[]> globalWorklist) throws UnsupportedPetriNetException, IllegalTransitionException {
		ArrayDeque<Pair<short[], Double>> localWorklist = new ArrayDeque<>();
		localWorklist.add(Pair.of(exploreMarking, probabilityUpToExploreState));
		double stateTerminationWeight = 0;

		short[] localVisited = MarkingSet.create(exploreMarking);

		while (!localWorklist.isEmpty()) {
			Pair<short[], Double> p = localWorklist.poll();
			short[] marking = p.getA();
			double markingProbability = p.getB();

			semantics.setCurrentState(marking);
			Collection<Transition> enabledTransitions = semantics.getExecutableTransitions();

			//gather weights
			double sumWeights = getSumOfWeights(semantics, startState);

			if (enabledTransitions.isEmpty()) {
				/**
				 * This is an end state. Add the probability of ending in this
				 * state to the starting state from which we started the
				 * tau-exploration.
				 */
				stateTerminationWeight += markingProbability;
			} else {
				for (Transition t : enabledTransitions) {
					semantics.setCurrentState(marking);
					semantics.executeExecutableTransition(t);
					double probability = (((TimedTransition) t).getWeight() / sumWeights) * markingProbability;

					short[] nextMarking = semantics.getCurrentInternalState().clone();

					if (t.isInvisible()) {
						//silent transition; extend the search

						if (MarkingSet.contains(localVisited, nextMarking)) {
							/**
							 * In this tau-search, we encountered the same
							 * marking twice. Hence, there is a tau loop. We do
							 * not support these.
							 */
							//throw new UnsupportedPetriNetException();
						} else {
							/**
							 * We reached a new state by following just
							 * tau-transitions. Queue this state for later.
							 */
							localWorklist.add(Pair.of(nextMarking, probability));

							localVisited = MarkingSet.add(localVisited, nextMarking);
						}
					} else {
						//normal transition, add to automaton
						short activity = result.transform(t.getLabel());

						if (result.containsEdge(startState, activity)) {
							throw new UnsupportedPetriNetException();
						}

						int target = marking2state.get(nextMarking);
						if (target == marking2state.getNoEntryValue()) {
							target = result.addEdge(startState, activity, probability);
							marking2state.put(nextMarking, target);

							globalWorklist.add(nextMarking);
						} else {
							result.addEdge(startState, activity, target, probability);
						}
					}
				}
			}
		}
		return stateTerminationWeight;
	}

	//	private static void normaliseProbabilities(StochasticDeterministicFiniteAutomatonMappedImpl<String> result,
	//			int state, double stateTerminationWeight) {
	//
	//		EdgeIterableOutgoingImpl it = result.getOutgoingEdgesIterator(state);
	//
	//		//gather the sum of probabilities
	//		double sum = stateTerminationWeight;
	//		while (it.hasNext()) {
	//			sum += it.nextProbability();
	//		}
	//
	//		//normalise
	//		it.reset(state);
	//		while (it.hasNext()) {
	//			double p = it.nextProbability();
	//			it.setProbability(p / sum);
	//		}
	//	}
}