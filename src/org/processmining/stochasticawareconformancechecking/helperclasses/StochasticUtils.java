package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;

public class StochasticUtils {
	private static final double epsilon = 0.0000000000001;
	private static final double meaningfulEpsilon = 0.000000001;

	public static double getTerminationProbability(EdgeIterableOutgoing it, int state) {
		double result = 0;
		it.reset(state);
		while (it.hasNext()) {
			result += it.nextProbability();
		}

		return 1 - result;
	}

	public static double getTerminationProbability(StochasticDeterministicFiniteAutomaton automaton, int state) {
		return getTerminationProbability(automaton.getOutgoingEdgesIterator(-1), state);
	}

	public static boolean hasTerminationProbability(StochasticDeterministicFiniteAutomaton automaton, int state) {
		return getTerminationProbability(automaton, state) > epsilon;
	}

	public static boolean hasTerminationProbability(EdgeIterableOutgoing it, int state) {
		return getTerminationProbability(it, state) > epsilon;
	}

	public static boolean isProbability(double probability) {
		return probability > -epsilon && probability < 1 + epsilon;
	}

	public static double getEpsilon() {
		return epsilon;
	}

	public static boolean isLargerThanZero(double value) {
		return value > epsilon;
	}

	public static boolean areEqual(double a, double b) {
		return Math.abs(a - b) < epsilon;
	}

	public static double getMeaningfulepsilon() {
		return meaningfulEpsilon;
	}
}
