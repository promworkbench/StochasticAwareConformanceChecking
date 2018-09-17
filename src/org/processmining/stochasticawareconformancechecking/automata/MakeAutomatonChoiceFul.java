package org.processmining.stochasticawareconformancechecking.automata;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;

/**
 * An automaton without choices has an entropy of 0. Add a small choice to each
 * final state to prevent this. In place.
 * 
 * @author sander
 *
 */
public class MakeAutomatonChoiceFul {
	public static double epsilon = 0.0000000001;
	public static String escapeActivity = "#*@#)Escape";

	public static void convert(StochasticDeterministicFiniteAutomatonMapped<String> automaton) {
		EdgeIterableOutgoing it = automaton.getOutgoingEdgesIterator(automaton.getInitialState());
		short escapeActivityIndex = automaton.transform(escapeActivity);
		int escapeState = -1;

		for (int state = 0; state < automaton.getNumberOfStates(); state++) {

			//count the outgoing probability
			it.reset(state);
			double outgoingSum = 0;
			while (it.hasNext()) {
				outgoingSum += it.nextProbability();
			}

			if (outgoingSum < 1) {
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
