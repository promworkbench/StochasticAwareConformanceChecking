package org.processmining.stochasticawareconformancechecking.helperclasses;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;
import org.processmining.stochasticawareconformancechecking.helperclasses.Projection.ChooseProbability;

public class RelativeEntropy {

	public static void main(String... args) throws CloneNotSupportedException, UnsupportedAutomatonException {

		for (int logTraces = 1; logTraces <= 100; logTraces++) {
			for (double loopBack = 0; loopBack < 1; loopBack += 0.01) {

			}
		}
		int modelTraces = 2;
		double loopbackprobability = 0.2;

		//log
		StochasticDeterministicFiniteAutomatonMappedImpl log = new StochasticDeterministicFiniteAutomatonMappedImpl();
		{
			int traces = 100;

			double p = 1.0 / traces;
			int a = 0;
			int finalState = log.addEdge(log.getInitialState(), log.transform("a" + a), p);
			for (a = 1; a < traces; a++) {
				log.addEdge(log.getInitialState(), log.transform("a" + a), finalState, p);
			}
		}

		//model
		StochasticDeterministicFiniteAutomatonMappedImpl model = new StochasticDeterministicFiniteAutomatonMappedImpl();
		{
			int finalState = 0;
			if (modelTraces > 0) {
				int a = 0;
				double p = 1.0 / modelTraces;
				finalState = model.addEdge(log.getInitialState(), model.transform("a" + a), p);
				for (a = 1; a < modelTraces; a++) {
					model.addEdge(model.getInitialState(), model.transform("a" + a), finalState, p);
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
	public static Pair<Double, Double> relativeEntropy(StochasticDeterministicFiniteAutomatonMapped a,
			StochasticDeterministicFiniteAutomatonMapped b)
			throws CloneNotSupportedException, UnsupportedAutomatonException {

		if (!CheckProbabilities.checkProbabilities(a)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}
		if (!CheckProbabilities.checkProbabilities(b)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}

		a = a.clone();
		b = b.clone();
		FilterZeroEdges.filter(a); //filter edges that have zero weight
		FilterZeroEdges.filter(b); //filter edges that have zero weight

		//check for death paths (should be done after zero-edge filtering)
		if (CheckDeadPaths.hasDeathPaths(a)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}
		if (CheckDeadPaths.hasDeathPaths(b)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}

		MakeAutomatonChoiceFul.convert(a); //add a small choice to each automaton to prevent zero-entropy
		MakeAutomatonChoiceFul.convert(b);//add a small choice to each automaton to prevent zero-entropy

		System.out.println("projecting...");
		StochasticDeterministicFiniteAutomaton projection = Projection.project(a, b, ChooseProbability.Minimum);

		System.out.println("computing entropy projection...");
		double eP = Entropy.entropy(projection);
		System.out.println(eP);
		System.out.println("computing entropy A...");
		double eA = Entropy.entropy(a);
		System.out.println(eA);
		System.out.println("computing entropy B...");
		double eB = Entropy.entropy(b);
		System.out.println(eB);

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
	public static Pair<Double, Double> relativeEntropyHalf(StochasticDeterministicFiniteAutomatonMapped a,
			StochasticDeterministicFiniteAutomatonMapped b)
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
