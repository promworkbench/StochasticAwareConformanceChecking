package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.util.Arrays;
import java.util.BitSet;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableIncoming;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton2Dot;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonImpl;

import gnu.trove.stack.array.TIntArrayStack;

public class Entropy {

	public static int checkEqualityEveryIterations = 10;

	public static void main(String... args) throws UnsupportedAutomatonException {

		//construct a stochastic automaton
		StochasticDeterministicFiniteAutomaton sdfa = new StochasticDeterministicFiniteAutomatonImpl();
		//sdfa.addEdge(sdfa.getInitialState(), (short) 5, 0.99998);
		sdfa.addEdge(sdfa.getInitialState(), (short) 6, 0.75);
		sdfa.addEdge(sdfa.getInitialState(), (short) 7, 0.25);

		System.out.println(StochasticDeterministicFiniteAutomaton2Dot.toDot(sdfa));

		double entropy = entropy(sdfa);

		System.out.println("entropy: " + entropy);
	}

	public static double entropy(StochasticDeterministicFiniteAutomaton automaton)
			throws UnsupportedAutomatonException {

		if (!CheckProbabilities.checkProbabilities(automaton)) {
			throw new UnsupportedAutomatonException("Automaton's probabilities are out of range.");
		}

		if (CheckDeadPaths.hasDeathPaths(automaton)) {
			throw new UnsupportedAutomatonException("Automaton contains death paths.");
		}

		double[] c = computeCs(automaton);

		double result = 0;
		double l2 = Math.log(2);

		EdgeIterable it = automaton.getEdgesIterator();
		while (it.hasNext()) {
			it.next();
			//BigDecimal probability = it.getProbability().add(addFactor);
			double probability = it.getProbability();
			//result += c[it.getSource()] * it.getProbability() * Math.log(it.getProbability());
			//BigDecimal nlogn = probability.multiply(BigDecimalMath.log(probability), mc).divide(l2, mc);
			double nlogn = probability * Math.log(probability) / l2;
			//result = result.add(c[it.getSource()].multiply(nlogn, mc));
			result += c[it.getSource()] * nlogn;
		}

		/**
		 * Add termination nlogn entropy to result, but only for reachable
		 * states.
		 */
		BitSet visited = new BitSet(automaton.getNumberOfStates());
		visited.set(automaton.getInitialState());
		TIntArrayStack worklist = new TIntArrayStack();
		worklist.push(automaton.getInitialState());
		EdgeIterableOutgoing it2 = automaton.getOutgoingEdgesIterator(automaton.getInitialState());
		while (worklist.size() > 0) {
			int state = worklist.pop();

			//add entropy
			//BigDecimal termination = StochasticUtils.getTerminationProbability(it2, state).add(addFactor);
			double termination = StochasticUtils.getTerminationProbability(it2, state);
			if (StochasticUtils.isLargerThanZero(termination)) {
				//BigDecimal nlogn = termination.multiply(BigDecimalMath.log(termination), mc).divide(l2, mc);
				double nlogn = termination * Math.log(termination) / l2;
				//result = result.add(c[state].multiply(nlogn, mc));
				result += c[state] * nlogn;
			}

			//set-up visiting other states
			it2.reset(state);
			while (it2.hasNext()) {
				int targetState = it2.nextTarget();

				if (!visited.get(targetState)) {
					visited.set(targetState);
					worklist.push(targetState);
				}
			}
		}

		return -result;
	}

	private static double[] computeCs(StochasticDeterministicFiniteAutomaton automaton) {
		double[] previous = new double[automaton.getNumberOfStates()];
		double[] current = new double[automaton.getNumberOfStates()];
		double[] s;
		Arrays.fill(current, 0);

		EdgeIterableIncoming incomingEdges = automaton.getIncomingEdgesIterator(-1);

		do {
			System.out.println(" new iteration");
			for (int iteration = 0; iteration < checkEqualityEveryIterations; iteration++) {

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
		} while (!areEqual(previous, current));

		return current;
	}

	public static boolean areEqual(double[] previous, double[] current) {
		for (int i = 0; i < previous.length; i++) {
			if (!StochasticUtils.areEqual(previous[i], current[i])) {
				System.out.println(" unequal at " + i + " of " + current.length);
				return false;
			}
		}
		return true;
	}
}
