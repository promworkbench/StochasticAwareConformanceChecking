package org.processmining.stochasticawareconformancechecking.helperclasses;

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

	public static void convert(StochasticDeterministicFiniteAutomatonMapped automaton) {
		short escapeActivityIndex = automaton.transform(escapeActivity);
		int escapeState = -1;
		double epsilon = StochasticUtils.getMeaningfulepsilon();

		for (int state = 0; state < automaton.getNumberOfStates(); state++) {

			if (state != escapeState) {
				if (StochasticUtils.hasTerminationProbability(automaton, state)) {
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
