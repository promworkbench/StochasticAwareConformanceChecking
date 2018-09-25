package org.processmining.stochasticawareconformancechecking.automata;

import java.math.BigDecimal;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class Log2StochasticDeterministicFiniteAutomaton {
	public static StochasticDeterministicFiniteAutomatonMapped<String> convert(XLog log, XEventClassifier classifier,
			ProMCanceller canceller) throws UnsupportedLogException {

		/**
		 * If the log is empty, there is no SDFA that can represent it.
		 */
		if (log.isEmpty()) {
			throw new UnsupportedLogException("Empty logs are not supported.");
		}

		StochasticDeterministicFiniteAutomatonMapped<String> result = new StochasticDeterministicFiniteAutomatonMappedImpl<>();

		/**
		 * Strategy: first, create a prefix automaton, while keeping track of
		 * how often each path is taken and when the traces finish. Second,
		 * normalise the numbers in the automaton.
		 */

		TIntObjectMap<BigDecimal> state2endingTraces = new TIntObjectHashMap<>(10, 0.5f, -1);

		for (XTrace trace : log) {
			addTrace(result, state2endingTraces, transformTrace(result, trace, classifier), canceller);
		}

		/**
		 * Step 2: normalise
		 */
		EdgeIterableOutgoing it = result.getOutgoingEdgesIterator(-1);
		for (int state = 0; state < result.getNumberOfStates(); state++) {
			//first, compute the sum
			BigDecimal sum = state2endingTraces.get(state);
			sum = sum == null ? BigDecimal.ZERO : sum; 
			it.reset(state);
			while (it.hasNext()) {
				sum = sum.add(it.nextProbability());
			}

			//second, normalise
			it.reset(state);
			while (it.hasNext()) {
				it.setProbability(it.nextProbability().divide(sum, it.getRoundingMathContext()));
			}
		}

		return result;
	}

	public static short[] transformTrace(StochasticDeterministicFiniteAutomatonMapped<String> automaton, XTrace trace,
			XEventClassifier classifier) {
		short[] result = new short[trace.size()];
		int i = 0;
		for (XEvent event : trace) {
			result[i] = automaton.transform(classifier.getClassIdentity(event));
			i++;
		}
		return result;
	}

	public static int addTrace(StochasticDeterministicFiniteAutomatonMapped<String> automaton,
			TIntObjectMap<BigDecimal> state2endingTraces, short[] trace, ProMCanceller canceller) {
		int state = automaton.getInitialState();
		for (int i = 0; i < trace.length; i++) {
			short activity = trace[i];
			state = automaton.addEdge(state, activity, BigDecimal.ONE);

			if (canceller.isCancelled()) {
				return -1;
			}
		}

		if (!state2endingTraces.containsKey(state)) {
			state2endingTraces.put(state, BigDecimal.ONE);
		} else {
			state2endingTraces.put(state, state2endingTraces.get(state).add(BigDecimal.ONE));
		}

		return state;
	}
}
