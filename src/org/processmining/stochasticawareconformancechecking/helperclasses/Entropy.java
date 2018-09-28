package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.BitSet;

import org.nevec.rjm.BigDecimalMath;
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
		sdfa.addEdge(sdfa.getInitialState(), (short) 6, new BigDecimal("0.75"));
		sdfa.addEdge(sdfa.getInitialState(), (short) 7, new BigDecimal("0.25"));

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

		/**
		 * Our BigDecimalMath-package does not handle precision properly: we
		 * need to ensure the input has enough digits.
		 */
		BigDecimal addFactor = new BigDecimal("0e-" + automaton.getRoundingMathContext().getPrecision());

		BigDecimal result = BigDecimal.ZERO;
		BigDecimal l2 = BigDecimalMath.log(new BigDecimal("2").add(addFactor));

		EdgeIterable it = automaton.getEdgesIterator();
		MathContext mc = automaton.getRoundingMathContext();
		while (it.hasNext()) {
			it.next();
			BigDecimal probability = it.getProbability().add(addFactor);
			//result += c[it.getSource()] * it.getProbability() * Math.log(it.getProbability());
			BigDecimal nlogn = probability.multiply(BigDecimalMath.log(probability), mc).divide(l2, mc);
			result = result.add(c[it.getSource()].multiply(nlogn, mc));
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
			BigDecimal termination = StochasticUtils.getTerminationProbability(it2, state).add(addFactor);
			if (StochasticUtils.isLargerThanZero(it2, termination)) {
				BigDecimal nlogn = termination.multiply(BigDecimalMath.log(termination), mc).divide(l2, mc);
				result = result.add(c[state].multiply(nlogn, mc));
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

		return result.negate();
	}

	private static BigDecimal[] computeCs(StochasticDeterministicFiniteAutomaton automaton) {
		BigDecimal[] previous = new BigDecimal[automaton.getNumberOfStates()];
		BigDecimal[] current = new BigDecimal[automaton.getNumberOfStates()];
		BigDecimal[] s;
		Arrays.fill(current, BigDecimal.ZERO);

		BigDecimal epsilon = StochasticUtils.getEpsilon(automaton);

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
		} while (!areEqual(previous, current, epsilon));

		return current;
	}

	public static boolean areEqual(BigDecimal[] previous, BigDecimal[] current, BigDecimal epsilon) {
		for (int i = 0; i < previous.length; i++) {
			if (previous[i].subtract(current[i]).abs().compareTo(epsilon) > 0) {
				return false;
			}
		}
		return true;
	}
}
