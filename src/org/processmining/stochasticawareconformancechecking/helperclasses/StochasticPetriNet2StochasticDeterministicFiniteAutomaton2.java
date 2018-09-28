package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.strategy.HashingStrategy;

public class StochasticPetriNet2StochasticDeterministicFiniteAutomaton2 {

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

	private static StochasticDeterministicFiniteAutomatonMapped<String> convert(StochasticNet net,
			short[] initialMarking, EfficientStochasticNetSemanticsImpl s) throws IllegalTransitionException {
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
			Map<Transition, Pair<short[], BigDecimal>> enabledTransitions = getEnabledTransitions(s, marking,
					result.getRoundingMathContext(), result);

			System.out.println(enabledTransitions);

			//second, put the transitions into the automaton
			for (Entry<Transition, Pair<short[], BigDecimal>> pair : enabledTransitions.entrySet()) {
				Transition t = pair.getKey();
				short[] newMarking = pair.getValue().getA();
				short activity = result.transform(t.getLabel());
				BigDecimal probability = pair.getValue().getB();
				if (StochasticUtils.isLargerThanZero(result, probability)) {
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
		}

		return result;
	}

	/**
	 * Search through the enabled state spaces following tau transitions for
	 * non-silent transitions that are enabled.
	 * 
	 * @param mc
	 * @param automaton
	 * 
	 * @return
	 * @throws IllegalTransitionException
	 * @throws UnsupportedPetriNetException
	 */
	private static Map<Transition, Pair<short[], BigDecimal>> getEnabledTransitions(
			EfficientStochasticNetSemanticsImpl semantics, short[] startMarking, MathContext mc,
			StochasticDeterministicFiniteAutomaton automaton) throws IllegalTransitionException {

		Map<Transition[], short[]> path2marking = getPaths(semantics, startMarking);
		List<Transition[]> paths = new ArrayList<>(path2marking.keySet());

		//gather the probabilities for each transition
		BigDecimal termination = BigDecimal.ZERO;
		THashMap<Transition, BigDecimal> probabilities = new THashMap<>();
		Iterator<Transition[]> it = paths.iterator();
		for (int i = 0; i < paths.size(); i++) {
			Transition[] path = it.next();

			Transition t = path[path.length - 1];

			if (t.isInvisible()) {
				//the last transition on this path is silent, so this path adds to the termination probability. 
				termination = termination.add(getPathProbability(paths, i, mc, automaton));
			} else {
				//the last transition on this path is an activity, so this path adds to the probability of that activity.
				BigDecimal probability = getPathProbability(paths, i, mc, automaton);

				probabilities.putIfAbsent(t, BigDecimal.ZERO);
				probabilities.put(t, probabilities.get(t).add(probability));
			}
		}

		//keep track of the shortest path for each transition (prevents concurrent silent transitions to interfere)
		THashMap<Transition, Transition[]> shortestPaths = new THashMap<>();
		{
			TObjectIntMap<Transition> shortestPathsLengths = new TObjectIntHashMap<>(10, 0.5f, Integer.MAX_VALUE);
			for (Transition[] path : paths) {
				Transition t = path[path.length - 1];

				if (!t.isInvisible()) {
					int shortestPath = shortestPathsLengths.get(t);
					if (path.length < shortestPath) {
						shortestPathsLengths.put(t, path.length);
						shortestPaths.put(t, path);
					}
				}
			}
		}

		//gather the result
		Map<Transition, Pair<short[], BigDecimal>> result = new THashMap<>();
		for (Transition t : shortestPaths.keySet()) {
			Transition[] path = shortestPaths.get(t);
			result.put(t, Pair.of(path2marking.get(path), probabilities.get(t)));
		}

		return result;
	}

	/**
	 * 
	 * @param paths
	 * @param pathIndex
	 * @param automaton
	 * @return the probability of the path
	 */
	private static BigDecimal getPathProbability(List<Transition[]> paths, int pathIndex, MathContext mc,
			StochasticDeterministicFiniteAutomaton automaton) {
		BitSet prefixMatches = new BitSet(paths.size());
		prefixMatches.set(0, paths.size());
		Transition[] path = paths.get(pathIndex);

		BigDecimal result = BigDecimal.ONE;

		for (int round = 0; round < path.length; round++) {

			//compute the total sum of this round
			BigDecimal sumWeightRound = BigDecimal.ZERO;
			int numberOfEnabledTransitions = 0;
			{
				THashSet<Transition> enabledTransitions = new THashSet<>();
				for (int pathIndex2 = prefixMatches.nextSetBit(0); pathIndex2 >= 0; pathIndex2 = prefixMatches
						.nextSetBit(pathIndex2 + 1)) {
					enabledTransitions.add(paths.get(pathIndex2)[round]);
				}
				for (Transition t : enabledTransitions) {
					sumWeightRound = sumWeightRound.add(new BigDecimal(((TimedTransition) t).getWeight()));
					numberOfEnabledTransitions++;
				}
			}

			//add the probability of this step to the final result
			{
				Transition t = path[round];
				if (StochasticUtils.isLargerThanZero(automaton, sumWeightRound)) {
					BigDecimal probabilityThisStep = new BigDecimal(((TimedTransition) t).getWeight())
							.divide(sumWeightRound, mc);
					result = result.multiply(probabilityThisStep, mc);
				} else {
					//if the sum weight is zero, then all transitions have no probabilities. Distribute evenly.
					BigDecimal probabilityThisStep = BigDecimal.ONE.divide(new BigDecimal(numberOfEnabledTransitions),
							automaton.getRoundingMathContext());
					result = result.multiply(probabilityThisStep, mc);
				}
			}

			//next, limit the paths that are not prefixes of pathIndex
			for (int pathIndex2 = prefixMatches.nextSetBit(0); pathIndex2 >= 0; pathIndex2 = prefixMatches
					.nextSetBit(pathIndex2 + 1)) {
				if (!paths.get(pathIndex2)[round].equals(path[round]) || paths.get(pathIndex2).length == round + 1) {
					//if this round's transition is not the same, or the path is too short for a next round, exclude it.
					prefixMatches.clear(pathIndex2);
				}
			}
		}
		return result;
	}

	/**
	 * Get all paths ending in a non-silent transition and the marking they end
	 * up in.
	 * 
	 * @param semantics
	 * @param startMarking
	 * @return
	 * @throws IllegalTransitionException
	 */
	private static Map<Transition[], short[]> getPaths(EfficientStochasticNetSemanticsImpl semantics,
			short[] startMarking) throws IllegalTransitionException {
		Map<Transition[], short[]> result = getPathsSet();
		Transition[] pathUpTillNow = new Transition[0];
		Set<short[]> visited = getMarkingSet();
		visited.add(startMarking);
		getPaths(semantics, startMarking, result, visited, pathUpTillNow);
		return result;
	}

	/**
	 * Explore the startMarking for paths towards non-silent transitions.
	 * 
	 * @param semantics
	 * @param startMarking
	 * @param result
	 * @param pathUpTillNow
	 * @throws IllegalTransitionException
	 */
	private static void getPaths(EfficientStochasticNetSemanticsImpl semantics, short[] marking,
			Map<Transition[], short[]> result, Set<short[]> visited, Transition[] pathUpTillNow)
			throws IllegalTransitionException {
		semantics.setCurrentState(marking);
		Collection<Transition> enabledTransitions = semantics.getExecutableTransitions();

		//if we can terminate in this marking, add a corresponding path, but not an empty one
		if (enabledTransitions.isEmpty() && pathUpTillNow.length != 0) {
			result.put(pathUpTillNow, marking);
		}

		for (Transition t : enabledTransitions) {
			semantics.setCurrentState(marking);
			semantics.executeExecutableTransition(t);

			Transition[] newPath = Arrays.copyOf(pathUpTillNow, pathUpTillNow.length + 1);
			newPath[newPath.length - 1] = t;

			short[] newMarking = semantics.getCurrentInternalState().clone();

			if (t.isInvisible()) {
				//we encountered a new invisible transition; follow
				if (!visited.contains(newMarking)) {
					visited.add(newMarking);
					getPaths(semantics, newMarking, result, visited, newPath);
				}
			} else {
				//we reached the end of a path; add the path to the set
				result.put(newPath, newMarking);
			}
		}
	}

	private static Map<Transition[], short[]> getPathsSet() {
		return new TCustomHashMap<>(new HashingStrategy<Transition[]>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public int computeHashCode(Transition[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(Transition[] o1, Transition[] o2) {
				return Arrays.equals(o1, o2);
			}
		});
	}

	private static Set<short[]> getMarkingSet() {
		return new TCustomHashSet<>(new HashingStrategy<short[]>() {
			private static final long serialVersionUID = 9085136431842993102L;

			public int computeHashCode(short[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(short[] o1, short[] o2) {
				return Arrays.equals(o1, o2);
			}
		});
	}
}