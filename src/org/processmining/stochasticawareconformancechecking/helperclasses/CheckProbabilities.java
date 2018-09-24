package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;

public class CheckProbabilities {

	public static double epsilon = 0.0001;

	public static boolean checkProbabilities(StochasticDeterministicFiniteAutomaton automaton) {
		EdgeIterable it = automaton.getEdgesIterator();
		while (it.hasNext()) {
			it.next();
			if (it.getProbability() + epsilon < 0 || it.getProbability() - epsilon > 1) {
				return false;
			}
		}
		return true;
	}
}
