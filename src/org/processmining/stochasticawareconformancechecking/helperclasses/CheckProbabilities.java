package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;

public class CheckProbabilities {

	public static boolean checkProbabilities(StochasticDeterministicFiniteAutomaton automaton) {
		BigDecimal epsilon = new BigDecimal("1e-" + (automaton.getRoundingMathContext().getPrecision() - 2));

		{
			EdgeIterable it = automaton.getEdgesIterator();
			while (it.hasNext()) {
				it.next();

				if (it.getProbability().compareTo(BigDecimal.ZERO) < 0
						|| it.getProbability().compareTo(BigDecimal.ONE) > 0) {
					return false;
				}
			}
		}

		EdgeIterableOutgoing it = automaton.getOutgoingEdgesIterator(automaton.getInitialState());
		for (int state = 0; state < automaton.getNumberOfStates(); state++) {
			it.reset(state);
			BigDecimal sum = BigDecimal.ZERO;
			while (it.hasNext()) {
				sum = sum.add(it.nextProbability());
			}
			if (sum.subtract(epsilon).compareTo(BigDecimal.ONE) > 0) {
				return false;
			}
		}
		return true;
	}
}
