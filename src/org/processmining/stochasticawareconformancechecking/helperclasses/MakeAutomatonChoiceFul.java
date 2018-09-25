package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;

/**
 * An automaton without choices has an entropy of 0. Add a small choice to each
 * final state to prevent this. In place.
 * 
 * @author sander
 *
 */
public class MakeAutomatonChoiceFul {
	public static String escapeActivity = "(()Escape())";

	public static void convert(StochasticDeterministicFiniteAutomatonMapped<String> automaton) {
		EdgeIterableOutgoing it = automaton.getOutgoingEdgesIterator(automaton.getInitialState());
		short escapeActivityIndex = automaton.transform(escapeActivity);
		int escapeState = -1;

		BigDecimal epsilon = new BigDecimal("1e-" + (automaton.getRoundingMathContext().getPrecision() - 5));

		for (int state = 0; state < automaton.getNumberOfStates(); state++) {

			if (state != escapeState) {
				//count the outgoing probability
				it.reset(state);
				BigDecimal termination = BigDecimal.ONE;
				while (it.hasNext()) {
					termination = termination.subtract(it.nextProbability());
				}

				if (termination.compareTo(epsilon) > 0) {
					//this is a final state

					//add a link to the escape state (and add that if necessary)
					if (escapeState < 0) {
						escapeState = automaton.addEdge(state, escapeActivityIndex, epsilon);
					} else {
						automaton.addEdge(state, escapeActivityIndex, escapeState, epsilon);
					}

				}
			}
		}
	}
}
