package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;

public class RelativeEntropy {

	public static void main(String... args) throws CloneNotSupportedException {

		int modelTraces = 1;
		double loopbackprobability = 0;

		//log
		StochasticDeterministicFiniteAutomatonMappedImpl<String> log = new StochasticDeterministicFiniteAutomatonMappedImpl<>();
		{
			int traces = 100;

			int a = 0;
			int finalState = log.addEdge(log.getInitialState(), log.transform("a" + a), 1.0 / traces);
			for (a = 1; a < traces; a++) {
				log.addEdge(log.getInitialState(), log.transform("a" + a), finalState, 1.0 / traces);
			}
		}

		//model
		StochasticDeterministicFiniteAutomatonMappedImpl<String> model = new StochasticDeterministicFiniteAutomatonMappedImpl<>();
		{
			int finalState = 0;
			if (modelTraces > 0) {
				int a = 0;
				finalState = model.addEdge(log.getInitialState(), model.transform("a" + a), 0.01);
				for (a = 1; a < modelTraces; a++) {
					model.addEdge(model.getInitialState(), model.transform("a" + a), finalState, 0.01);
				}
			}

			model.addEdge(finalState, model.transform("loopback"), model.getInitialState(), loopbackprobability);
		}

		Pair<Double, Double> entropy = relativeEntropy(log, model);

		System.out.println("model traces:         " + modelTraces);
		System.out.println("loopback probability: " + loopbackprobability);
		System.out.println(entropy);
	}

	/**
	 * 
	 * @param <X>
	 * @param a
	 * @param b
	 * @return pair of (recall, precision)
	 * @throws CloneNotSupportedException
	 */
	public static <X> Pair<Double, Double> relativeEntropy(StochasticDeterministicFiniteAutomatonMapped<String> a,
			StochasticDeterministicFiniteAutomatonMapped<String> b) throws CloneNotSupportedException {

		a = a.clone();
		b = b.clone();
		FilterZeroEdges.filter(a);
		FilterZeroEdges.filter(b);
		MakeAutomatonChoiceFul.convert(a);
		MakeAutomatonChoiceFul.convert(b);

		StochasticDeterministicFiniteAutomaton projection = Projection.project(a, b);

		double eA = Entropy.entropy(a);
		double eB = Entropy.entropy(b);
		double eP = Entropy.entropy(projection);

		return Pair.of(eP / eA, eP / eB);
	}
}
