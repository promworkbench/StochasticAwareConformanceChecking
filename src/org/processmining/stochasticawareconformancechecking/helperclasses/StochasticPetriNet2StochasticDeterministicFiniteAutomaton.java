package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

public class StochasticPetriNet2StochasticDeterministicFiniteAutomaton {

	/**
	 * For now, the final markings are not used. That is, the only final
	 * markings are deadlock states, and every deadlock state is a final
	 * marking. Furthermore, we assume that silent transitions have no weight.
	 * 
	 * @param net
	 * @param initialMarking
	 * @return
	 * @throws IllegalTransitionException
	 * @throws UnsupportedPetriNetException
	 */
	public static StochasticDeterministicFiniteAutomatonMapped<String> convert(StochasticNet net,
			Marking initialMarking) throws IllegalTransitionException, UnsupportedPetriNetException {
		EfficientStochasticNetSemanticsImpl s = new EfficientStochasticNetSemanticsImpl();

		s.initialize(net.getTransitions(), initialMarking);

		//transform the markings
		s.setCurrentState(initialMarking);
		short[] initialMarkingS = s.getCurrentInternalState().clone();

		return convert(net, initialMarkingS, s);
	}

	public static StochasticDeterministicFiniteAutomatonMapped<String> convert(StochasticNet net,
			short[] initialMarking, EfficientStochasticNetSemanticsImpl s)
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
			Map<Transition, short[]> enabledTransitions = getEnabledTransitions(s, marking);

			//first, compute the sum of weights
			BigDecimal sumWeights = BigDecimal.ZERO;
			for (Transition transition : enabledTransitions.keySet()) {
				sumWeights.add(new BigDecimal(((TimedTransition) transition).getWeight()));
			}

			//second, put the transitions into the automaton
			for (Entry<Transition, short[]> pair : enabledTransitions.entrySet()) {
				Transition t = pair.getKey();
				short[] newMarking = pair.getValue();
				short activity = result.transform(t.getLabel());
				//for some reason, some transitions do not have weights; if that is the case, distribute the probabilities evenly over all transitions
				BigDecimal probability;
				if (sumWeights.compareTo(BigDecimal.ZERO) > 0) {
					probability = new BigDecimal(((TimedTransition) t).getWeight()).divide(sumWeights,
							result.getRoundingMathContext());
				} else {
					probability = BigDecimal.ONE.divide(new BigDecimal(enabledTransitions.size()),
							result.getRoundingMathContext());
				}
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

	/**
	 * Search through the enabled state spaces following tau transitions for
	 * non-silent transitions that are enabled.
	 * 
	 * @return
	 * @throws IllegalTransitionException
	 * @throws UnsupportedPetriNetException
	 */
	private static Map<Transition, short[]> getEnabledTransitions(EfficientStochasticNetSemanticsImpl semantics,
			short[] startMarking) throws IllegalTransitionException, UnsupportedPetriNetException {
		TObjectIntMap<Transition> shortestPaths = new TObjectIntCustomHashMap<>(new HashingStrategy<Transition>() {
			private static final long serialVersionUID = -1008906392937976598L;

			public int computeHashCode(Transition object) {
				return object.getLabel().hashCode();
			}

			public boolean equals(Transition o1, Transition o2) {
				return o1.getLabel().equals(o2.getLabel());
			}
		}, 10, 0.5f, Integer.MAX_VALUE);
		Map<Transition, short[]> result = new TCustomHashMap<>(new HashingStrategy<Transition>() {
			private static final long serialVersionUID = -1008906392937976598L;

			public int computeHashCode(Transition object) {
				return object.getLabel().hashCode();
			}

			public boolean equals(Transition o1, Transition o2) {
				return o1.getLabel().equals(o2.getLabel());
			}
		});
		TCustomHashSet<short[]> visited = new TCustomHashSet<>(new HashingStrategy<short[]>() {
			private static final long serialVersionUID = 9085136431842993102L;

			public int computeHashCode(short[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(short[] o1, short[] o2) {
				return Arrays.equals(o1, o2);
			}
		});

		ArrayDeque<short[]> localWorklist = new ArrayDeque<>();
		ArrayDeque<Integer> localWorklistSteps = new ArrayDeque<>();
		localWorklist.add(startMarking);
		localWorklistSteps.add(0);
		visited.add(startMarking);

		while (!localWorklist.isEmpty()) {
			short[] marking = localWorklist.poll();
			int steps = localWorklistSteps.poll();

			semantics.setCurrentState(marking);
			Collection<Transition> enabledTransitions = semantics.getExecutableTransitions();

			for (Transition t : enabledTransitions) {
				semantics.setCurrentState(marking);
				semantics.executeExecutableTransition(t);
				if (t.isInvisible()) {
					short[] m = semantics.getCurrentInternalState().clone();
					if (!visited.contains(m)) {
						localWorklist.add(m);
						localWorklistSteps.add(steps + 1);
						visited.add(m);
					}
				} else {
					short[] newMarking = semantics.getCurrentInternalState().clone();
					if (result.containsKey(t)) {
						//this transition was already reached.
						//see if we found a shorter path

						if (steps + 1 < shortestPaths.get(t)) {
							shortestPaths.put(t, steps + 1);
							result.put(t, newMarking);
						}

						//						throw new UnsupportedPetriNetException(
						//								"The Petri net contains two ways to execute the same transition.");
					} else {
						shortestPaths.put(t, steps + 1);
						result.put(t, newMarking);
					}
				}
			}

		}

		return result;
	}
}