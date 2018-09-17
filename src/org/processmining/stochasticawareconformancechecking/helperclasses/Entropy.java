package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.util.Arrays;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableIncoming;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton2Dot;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonImpl;

public class Entropy {

	public static int iterations = 1000;

	public static void main(String... args) {

		//construct a stochastic automaton
		StochasticDeterministicFiniteAutomaton sdfa = new StochasticDeterministicFiniteAutomatonImpl();
		//sdfa.addEdge(sdfa.getInitialState(), (short) 5, 0.99998);
		sdfa.addEdge(sdfa.getInitialState(), (short) 6, 0.00001);
		sdfa.addEdge(sdfa.getInitialState(), (short) 7, 0.00001);

		System.out.println(StochasticDeterministicFiniteAutomaton2Dot.toDot(sdfa));

		double entropy = entropy(sdfa);

		System.out.println("entropy: " + entropy);
	}

	public static double entropy(StochasticDeterministicFiniteAutomaton automaton) {
		double[] c = computeCs(automaton);

		double result = 0;
		double l2 = Math.log(2);

		EdgeIterable it = automaton.getEdgesIterator();
		while (it.hasNext()) {
			it.next();
			result += c[it.getSource()] * it.getProbability() * Math.log(it.getProbability());
		}

		return -result / l2;
	}

	private static double[] computeCs(StochasticDeterministicFiniteAutomaton automaton) {
		double[] previous = new double[automaton.getNumberOfStates()];
		double[] current = new double[automaton.getNumberOfStates()];
		double[] s;
		Arrays.fill(current, 0);

		EdgeIterableIncoming incomingEdges = automaton.getIncomingEdgesIterator(-1);

		for (int iteration = 0; iteration < iterations; iteration++) {
			//swap the arrays
			{
				s = previous;
				previous = current;
				current = s;
			}

			Arrays.fill(current, 0);
			current[automaton.getInitialState()] = 1;
			for (int q = 0; q < current.length; q++) {
				incomingEdges.reset(q);

				while (incomingEdges.hasNextSource()) {
					int source = incomingEdges.nextSource();
					current[q] += incomingEdges.getProbability() * previous[source];
				}
			}
		}

		return current;
	}
}
