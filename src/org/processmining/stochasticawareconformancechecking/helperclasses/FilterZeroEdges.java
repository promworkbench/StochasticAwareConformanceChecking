package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;

public class FilterZeroEdges {

	/**
	 * Remove the edges that cannot be taken from the automaton.
	 * 
	 * @param automaton
	 */
	public static void filter(StochasticDeterministicFiniteAutomaton automaton) {
		EdgeIterable it = automaton.getEdgesIterator();
		while (it.hasNext()) {
			it.next();
			if (!StochasticUtils.isLargerThanZero(it.getProbability())) {
				it.remove();
			}
		}
	}
}
