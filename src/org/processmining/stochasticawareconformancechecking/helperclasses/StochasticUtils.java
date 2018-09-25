package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

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

	public static boolean hasTerminationProbability(EdgeIterableOutgoing it, int state) {
		BigDecimal termination = getTerminationProbability(it, state);
		return termination.compareTo(getEpsilon(it)) > 0;
	}

	public static BigDecimal getEpsilon(EdgeIterableOutgoing it) {
		return new BigDecimal("1e-" + (it.getRoundingMathContext().getPrecision() - 5));
	}
}
