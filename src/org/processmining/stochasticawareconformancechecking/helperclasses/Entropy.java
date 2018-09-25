package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import org.nevec.rjm.BigDecimalMath;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableIncoming;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton2Dot;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonImpl;

public class Entropy {

	public static int iterations = 1000;

	public static void main(String... args) throws UnsupportedAutomatonException {

		//construct a stochastic automaton
		StochasticDeterministicFiniteAutomaton sdfa = new StochasticDeterministicFiniteAutomatonImpl();
		//sdfa.addEdge(sdfa.getInitialState(), (short) 5, 0.99998);
		sdfa.addEdge(sdfa.getInitialState(), (short) 6, new BigDecimal("0.00001"));
		sdfa.addEdge(sdfa.getInitialState(), (short) 7, new BigDecimal("0.00001"));

		System.out.println(StochasticDeterministicFiniteAutomaton2Dot.toDot(sdfa));

		BigDecimal entropy = entropy(sdfa);

		System.out.println("entropy: " + entropy);
	}

	public static BigDecimal entropy(StochasticDeterministicFiniteAutomaton automaton)
			throws UnsupportedAutomatonException {

		if (!CheckProbabilities.checkProbabilities(automaton)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}

		if (CheckDeadPaths.hasDeathPaths(automaton)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}

		BigDecimal[] c = computeCs(automaton);

		BigDecimal result = BigDecimal.ZERO;
		BigDecimal l2 = BigDecimalMath.log(new BigDecimal("2"));

		EdgeIterable it = automaton.getEdgesIterator();
		MathContext mc = automaton.getRoundingMathContext();
		while (it.hasNext()) {
			it.next();
			//result += c[it.getSource()] * it.getProbability() * Math.log(it.getProbability());
			BigDecimal nlogn = it.getProbability().multiply(BigDecimalMath.log(it.getProbability()), mc);
			result = result.add(c[it.getSource()].multiply(nlogn), mc);
		}

		return result.divide(l2, mc).negate();
	}

	private static BigDecimal[] computeCs(StochasticDeterministicFiniteAutomaton automaton) {
		BigDecimal[] previous = new BigDecimal[automaton.getNumberOfStates()];
		BigDecimal[] current = new BigDecimal[automaton.getNumberOfStates()];
		BigDecimal[] s;
		Arrays.fill(current, BigDecimal.ZERO);

		EdgeIterableIncoming incomingEdges = automaton.getIncomingEdgesIterator(-1);

		for (int iteration = 0; iteration < iterations; iteration++) {
			//swap the arrays
			{
				s = previous;
				previous = current;
				current = s;
			}

			Arrays.fill(current, BigDecimal.ZERO);
			current[automaton.getInitialState()] = BigDecimal.ONE;
			for (int q = 0; q < current.length; q++) {
				incomingEdges.reset(q);

				while (incomingEdges.hasNextSource()) {
					int source = incomingEdges.nextSource();
					current[q] = current[q].add(incomingEdges.getProbability().multiply(previous[source],
							automaton.getRoundingMathContext()));
				}
			}
		}

		return current;
	}
}
