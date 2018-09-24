package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;
import org.processmining.stochasticawareconformancechecking.helperclasses.Projection.ChooseProbability;

public class RelativeEntropy {

	public static void main(String... args) throws CloneNotSupportedException, UnsupportedAutomatonException {

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
				finalState = model.addEdge(log.getInitialState(), model.transform("a" + a), 1.0 / modelTraces);
				for (a = 1; a < modelTraces; a++) {
					model.addEdge(model.getInitialState(), model.transform("a" + a), finalState, 1.0 / modelTraces);
				}
			}

			model.addEdge(finalState, model.transform("loopback"), model.getInitialState(), loopbackprobability);
		}

		Pair<Double, Double> entropy = relativeEntropy(log, model);
		Pair<Double, Double> entropyHalf = relativeEntropyHalf(log, model);

		System.out.println("model traces:         " + modelTraces);
		System.out.println("loopback probability: " + loopbackprobability);
		System.out.println("full measure: " + entropy);
		System.out.println("half measure: " + entropyHalf);
	}

	/**
	 * 
	 * @param <X>
	 * @param a
	 * @param b
	 * @return pair of (recall, precision)
	 * @throws CloneNotSupportedException
	 * @throws UnsupportedAutomatonException
	 */
	public static <X> Pair<Double, Double> relativeEntropy(StochasticDeterministicFiniteAutomatonMapped<String> a,
			StochasticDeterministicFiniteAutomatonMapped<String> b)
			throws CloneNotSupportedException, UnsupportedAutomatonException {

		if (!CheckProbabilities.checkProbabilities(a)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}
		if (!CheckProbabilities.checkProbabilities(b)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}

		//pre-process the automata
		a = a.clone();
		b = b.clone();
		FilterZeroEdges.filter(a); //filter edges that have zero weight
		FilterZeroEdges.filter(b);

		//check for death paths
		if (CheckDeadPaths.hasDeathPaths(a)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}
		if (CheckDeadPaths.hasDeathPaths(b)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}

		MakeAutomatonChoiceFul.convert(a); //add a small choice to each automaton to prevent zero-entropy
		MakeAutomatonChoiceFul.convert(b);

		StochasticDeterministicFiniteAutomaton projection = Projection.project(a, b, ChooseProbability.Minimum);

		double eA = Entropy.entropy(a);
		double eB = Entropy.entropy(b);
		double eP = Entropy.entropy(projection);

		return Pair.of(eP / eA, eP / eB);
	}

	/**
	 * 
	 * @param <X>
	 * @param a
	 * @param b
	 * @return pair of (recall, precision), computed ignoring the probabilities
	 *         of model/log.
	 * @throws CloneNotSupportedException
	 * @throws UnsupportedAutomatonException
	 */
	public static <X> Pair<Double, Double> relativeEntropyHalf(StochasticDeterministicFiniteAutomatonMapped<String> a,
			StochasticDeterministicFiniteAutomatonMapped<String> b)
			throws CloneNotSupportedException, UnsupportedAutomatonException {

		if (!CheckProbabilities.checkProbabilities(a)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}
		if (!CheckProbabilities.checkProbabilities(b)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}

		//pre-process the automata
		a = a.clone();
		b = b.clone();
		FilterZeroEdges.filter(a); //filter edges that have zero weight
		FilterZeroEdges.filter(b);

		//check for death paths
		if (CheckDeadPaths.hasDeathPaths(a)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}
		if (CheckDeadPaths.hasDeathPaths(b)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}

		MakeAutomatonChoiceFul.convert(a); //add a small choice to each automaton to prevent zero-entropy
		MakeAutomatonChoiceFul.convert(b);

		StochasticDeterministicFiniteAutomaton projectionRecall = Projection.project(a, b, ChooseProbability.A);
		StochasticDeterministicFiniteAutomaton projectionPrecision = Projection.project(a, b, ChooseProbability.B);

		double eA = Entropy.entropy(a);
		double eB = Entropy.entropy(b);
		double ePr = Entropy.entropy(projectionRecall);
		double ePp = Entropy.entropy(projectionPrecision);

		return Pair.of(ePr / eA, ePp / eB);
	}
}
