package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.deckfour.xes.model.XLog;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.StochasticLanguage;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.stochasticawareconformancechecking.automata.Log2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;

public class GainEntropy {

	public static Pair<Double, Double> compute(XLog log, StochasticNet pnB, Marking initialMarking,
			ProMCanceller canceller) throws UnsupportedLogException, IllegalTransitionException,
			UnsupportedPetriNetException, UnsupportedAutomatonException {
		StochasticDeterministicFiniteAutomatonMapped automatonA = Log2StochasticDeterministicFiniteAutomaton
				.convert(log, MiningParameters.getDefaultClassifier(), canceller);
		RelativeEntropy.prepareAndCheckAutomaton(automatonA);

		if (canceller.isCancelled()) {
			return null;
		}

		StochasticDeterministicFiniteAutomatonMapped automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
				.convert(pnB, initialMarking);
		RelativeEntropy.prepareAndCheckAutomaton(automatonB);

		if (canceller.isCancelled()) {
			return null;
		}

		System.out.println("computing entropy A...");
		double eA = Entropy.entropy(automatonA);
		System.out.println(eA);

		System.out.println("computing entropy B...");
		double eB = Entropy.entropy(automatonB);
		System.out.println(eB);

		return null;
	}

	public static BigDecimal conjuctiveEntropy(StochasticLanguage languageA,
			StochasticDeterministicFiniteAutomatonMapped automatonB) {
		return null;
	}
}
