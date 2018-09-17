package org.processmining.stochasticawareconformancechecking.automata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import gnu.trove.strategy.HashingStrategy;

public class StochasticPetriNet2StochasticDeterministicFiniteAutomaton {

	/**
	 * For now, the final markings are not used. That is, the only final
	 * markings are deadlock states, and every deadlock state is a final
	 * marking.
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
			Collection<Transition> executableTransitions = s.getExecutableTransitions();

			//gather the total weight of first-order steps
			double sumWeights = getSumOfWeights(s, source);

			//build the automaton
			for (Transition t : executableTransitions) {
				s.setCurrentState(marking);
				s.executeExecutableTransition(t);
				short[] newMarking = s.getCurrentInternalState().clone();
				double probability = ((TimedTransition) t).getWeight() / sumWeights;

				if (t.isInvisible()) {
					/**
					 * Silent steps are not allowed in the automaton. We perform
					 * a forward search to include all steps possible with
					 * tau-transitions from this marking.
					 */

					
					performForwardSearch(s, result, source, newMarking, probability, marking2state, worklist);
				} else {
					short activity = result.transform(t.getLabel());
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

			//			/**
			//			 * After gathering all the outgoing steps, we need to normalise the
			//			 * probabilities, in case tau loops were present.
			//			 */
			//			normaliseProbabilities(result, source, sourceTerminationWeight);
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