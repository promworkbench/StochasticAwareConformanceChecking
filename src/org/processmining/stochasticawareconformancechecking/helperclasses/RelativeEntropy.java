package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;
import org.processmining.stochasticawareconformancechecking.helperclasses.Projection.ChooseProbability;

public class RelativeEntropy {

	public static void main(String... args) throws CloneNotSupportedException, UnsupportedAutomatonException {

		int modelTraces = 2;
		BigDecimal loopbackprobability = new BigDecimal("0.2");

		//log
		StochasticDeterministicFiniteAutomatonMappedImpl<String> log = new StochasticDeterministicFiniteAutomatonMappedImpl<>();
		{
			int traces = 100;

			BigDecimal p = BigDecimal.ONE.divide(new BigDecimal(traces), log.getRoundingMathContext());
			int a = 0;
			int finalState = log.addEdge(log.getInitialState(), log.transform("a" + a), p);
			for (a = 1; a < traces; a++) {
				log.addEdge(log.getInitialState(), log.transform("a" + a), finalState, p);
			}
		}

		//model
		StochasticDeterministicFiniteAutomatonMappedImpl<String> model = new StochasticDeterministicFiniteAutomatonMappedImpl<>();
		{
			int finalState = 0;
			if (modelTraces > 0) {
				int a = 0;
				BigDecimal p = BigDecimal.ONE.divide(new BigDecimal(modelTraces), log.getRoundingMathContext());
				finalState = model.addEdge(log.getInitialState(), model.transform("a" + a), p);
				for (a = 1; a < modelTraces; a++) {
					model.addEdge(model.getInitialState(), model.transform("a" + a), finalState, p);
				}
			}

			model.addEdge(finalState, model.transform("loopback"), model.getInitialState(), loopbackprobability);
		}

		Pair<BigDecimal, BigDecimal> entropy = relativeEntropy(log, model);
		Pair<BigDecimal, BigDecimal> entropyHalf = relativeEntropyHalf(log, model);

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
	public static <X> Pair<BigDecimal, BigDecimal> relativeEntropy(
			StochasticDeterministicFiniteAutomatonMapped<String> a,
			StochasticDeterministicFiniteAutomatonMapped<String> b)
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
		
		//check for death paths (should be done after zero-edge filtering
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
		BigDecimal eP = Entropy.entropy(projection);
		if (eP.compareTo(BigDecimal.ZERO) == 0) {
			return Pair.of(BigDecimal.ZERO, BigDecimal.ZERO);
		}
		
		System.out.println("computing entropy A...");
		BigDecimal eA = Entropy.entropy(a);
		System.out.println("computing entropy B...");
		BigDecimal eB = Entropy.entropy(b);

		return Pair.of(eP.divide(eA, a.getRoundingMathContext()), eP.divide(eB, a.getRoundingMathContext()));
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
	public static <X> Pair<BigDecimal, BigDecimal> relativeEntropyHalf(
			StochasticDeterministicFiniteAutomatonMapped<String> a,
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

		BigDecimal eA = Entropy.entropy(a);
		BigDecimal eB = Entropy.entropy(b);
		BigDecimal ePr = Entropy.entropy(projectionRecall);
		BigDecimal ePp = Entropy.entropy(projectionPrecision);

		return Pair.of(ePr.divide(eA, a.getRoundingMathContext()), ePp.divide(eB, a.getRoundingMathContext()));
	}
}
