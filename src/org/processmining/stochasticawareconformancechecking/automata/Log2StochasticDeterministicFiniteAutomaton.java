package org.processmining.stochasticawareconformancechecking.automata;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class Log2StochasticDeterministicFiniteAutomaton {
	public static StochasticDeterministicFiniteAutomatonMapped convert(XLog log, XEventClassifier classifier,
			ProMCanceller canceller) throws UnsupportedLogException {

		/**
		 * If the log is empty, there is no SDFA that can represent it.
		 */
		if (log.isEmpty()) {
			throw new UnsupportedLogException("Empty logs are not supported.");
		}

		StochasticDeterministicFiniteAutomatonMapped result = new StochasticDeterministicFiniteAutomatonMappedImpl();

		/**
		 * Strategy: first, create a prefix automaton, while keeping track of
		 * how often each path is taken and when the traces finish. Second,
		 * normalise the numbers in the automaton.
		 */

		TIntDoubleMap state2endingTraces = new TIntDoubleHashMap(10, 0.5f, -1, 0);

		for (XTrace trace : log) {
			addTrace(result, state2endingTraces, transformTrace(result, trace, classifier), canceller);
		}

		/**
		 * Step 2: normalise
		 */
		EdgeIterableOutgoing it = result.getOutgoingEdgesIterator(-1);
		for (int state = 0; state < result.getNumberOfStates(); state++) {
			//first, compute the sum
			double sum = state2endingTraces.get(state);
			it.reset(state);
			while (it.hasNext()) {
				sum += it.nextProbability();
			}

			//second, normalise
			it.reset(state);
			while (it.hasNext()) {
				it.setProbability(it.nextProbability() / sum);
			}
		}

		return result;
	}

	public static short[] transformTrace(StochasticDeterministicFiniteAutomatonMapped automaton, XTrace trace,
			XEventClassifier classifier) {
		short[] result = new short[trace.size()];
		int i = 0;
		for (XEvent event : trace) {
			result[i] = automaton.transform(classifier.getClassIdentity(event));
			i++;
		}
		return result;
	}

	public static int addTrace(StochasticDeterministicFiniteAutomatonMapped automaton,
			TIntDoubleMap state2endingTraces, short[] trace, ProMCanceller canceller) {
		int state = automaton.getInitialState();
		for (int i = 0; i < trace.length; i++) {
			short activity = trace[i];
			state = automaton.addEdge(state, activity, 1);

			if (canceller.isCancelled()) {
				return -1;
			}
		}

		state2endingTraces.put(state, state2endingTraces.get(state) + 1);

		return state;
	}
}
