package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;

public class CheckProbabilities {

	public static boolean checkProbabilities(StochasticDeterministicFiniteAutomaton automaton) {
		{
			EdgeIterable it = automaton.getEdgesIterator();
			while (it.hasNext()) {
				it.next();

				if (!StochasticUtils.isProbability(it.getProbability())) {
					return false;
				}
			}
		}

		EdgeIterableOutgoing it = automaton.getOutgoingEdgesIterator(automaton.getInitialState());
		for (int state = 0; state < automaton.getNumberOfStates(); state++) {
			it.reset(state);

			if (!StochasticUtils.isProbability(StochasticUtils.getTerminationProbability(it, state))) {
				return false;
			}
		}
		return true;
	}
}
