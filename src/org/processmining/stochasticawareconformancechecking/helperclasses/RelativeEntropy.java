package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;

public class RelativeEntropy {

	/**
	 * 
	 * @param <X>
	 * @param a
	 * @param b
	 * @return pair of (recall, precision)
	 */
	public static <X> Pair<Double, Double> relativeEntropy(StochasticDeterministicFiniteAutomatonMapped<X> a,
			StochasticDeterministicFiniteAutomatonMapped<X> b) {
		StochasticDeterministicFiniteAutomaton projection = Projection.project(a, b);

		double eA = Entropy.entropy(a);
		double eB = Entropy.entropy(b);
		double eP = Entropy.entropy(projection);

		return Pair.of(eP / eA, eP / eB);
	}
}
