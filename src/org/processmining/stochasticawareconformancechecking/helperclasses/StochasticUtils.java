package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;

public class StochasticUtils {
	public static BigDecimal getTerminationProbability(EdgeIterableOutgoing it, int state) {
		BigDecimal result = BigDecimal.ONE;
		it.reset(state);
		while (it.hasNext()) {
			result = result.subtract(it.nextProbability());
		}

		return result;
	}

	public static BigDecimal getTerminationProbability(StochasticDeterministicFiniteAutomaton automaton, int state) {
		EdgeIterableOutgoing it = automaton.getOutgoingEdgesIterator(state);
		BigDecimal result = BigDecimal.ONE;
		while (it.hasNext()) {
			result = result.subtract(it.nextProbability());
		}

		return result;
	}

	public static boolean hasTerminationProbability(StochasticDeterministicFiniteAutomaton automaton, int state) {
		BigDecimal termination = getTerminationProbability(automaton, state);
		return termination.compareTo(getEpsilon(automaton)) > 0;
	}

	public static boolean isLargerThanZero(EdgeIterableOutgoing it, BigDecimal value) {
		return value.compareTo(getEpsilon(it)) > 0;
	}

	public static boolean isLargerThanZero(StochasticDeterministicFiniteAutomaton automaton, BigDecimal value) {
		return value.compareTo(getEpsilon(automaton)) > 0;
	}

	public static boolean hasTerminationProbability(EdgeIterableOutgoing it, int state) {
		BigDecimal termination = getTerminationProbability(it, state);
		return termination.compareTo(getEpsilon(it)) > 0;
	}

	public static BigDecimal getEpsilon(EdgeIterableOutgoing it) {
		return new BigDecimal("1e-" + (it.getRoundingMathContext().getPrecision() - 5));
	}

	public static BigDecimal getEpsilon(StochasticDeterministicFiniteAutomaton automaton) {
		return new BigDecimal("1e-" + (automaton.getRoundingMathContext().getPrecision() - 5));
	}

	public static boolean isProbability(BigDecimal probability, BigDecimal epsilon) {
		return probability.add(epsilon).compareTo(BigDecimal.ZERO) > 0
				&& probability.subtract(epsilon).compareTo(BigDecimal.ONE) < 0;
	}
}
