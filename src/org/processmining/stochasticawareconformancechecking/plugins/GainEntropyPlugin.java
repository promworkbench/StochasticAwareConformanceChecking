package org.processmining.stochasticawareconformancechecking.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.helperclasses.GainEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedAutomatonException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

public class GainEntropyPlugin {
	/**
	 * A: recall, B: precision
	 * 
	 * @param log
	 * @param pnB
	 * @param canceller
	 * @return
	 * @throws UnsupportedLogException
	 * @throws IllegalTransitionException
	 * @throws UnsupportedPetriNetException
	 * @throws CloneNotSupportedException
	 * @throws UnsupportedAutomatonException
	 */
	public static Pair<Double, Double> compute(XLog log, StochasticNet pnB, ProMCanceller canceller)
			throws UnsupportedLogException, IllegalTransitionException, UnsupportedPetriNetException,
			CloneNotSupportedException, UnsupportedAutomatonException {
		Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
				.guessInitialMarking(pnB);
		final Pair<Double, Double> entropyHalf = GainEntropy.compute(log, pnB, initialMarking, canceller);
		return entropyHalf;
	}
}
