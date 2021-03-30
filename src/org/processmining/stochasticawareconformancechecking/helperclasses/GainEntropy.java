package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.deckfour.xes.model.XLog;
import org.processmining.earthmoversstochasticconformancechecking.algorithms.XLog2StochasticLanguage;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.Activity2IndexKey;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.StochasticLanguage;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.StochasticTraceIterator;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.log.StochasticLanguageLog;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.stochasticawareconformancechecking.automata.Log2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;

public class GainEntropy {

	public static Pair<Double, Double> compute(XLog log, StochasticNet pnB, Marking initialMarking,
			ProMCanceller canceller) throws UnsupportedLogException, IllegalTransitionException,
			UnsupportedPetriNetException, UnsupportedAutomatonException, CloneNotSupportedException {
		StochasticDeterministicFiniteAutomatonMapped automatonA = Log2StochasticDeterministicFiniteAutomaton
				.convert(log, MiningParameters.getDefaultClassifier(), canceller);
		RelativeEntropy.prepareAndCheckAutomaton(automatonA);

		if (canceller.isCancelled()) {
			return null;
		}

		StochasticDeterministicFiniteAutomatonMapped automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
				.convert(pnB, initialMarking);

		StochasticDeterministicFiniteAutomatonMapped automatonBadjusted = automatonB.clone();
		RelativeEntropy.prepareAndCheckAutomaton(automatonBadjusted);

		if (canceller.isCancelled()) {
			return null;
		}

		Activity2IndexKey activityKey = new Activity2IndexKey();
		StochasticLanguageLog languageA = XLog2StochasticLanguage.convert(log, MiningParameters.getDefaultClassifier(),
				activityKey, canceller);

		if (canceller.isCancelled()) {
			return null;
		}

		System.out.println("computing entropy A...");
		double eA = Entropy.entropy(automatonA);
		System.out.println(eA);

		if (canceller.isCancelled()) {
			return null;
		}

		System.out.println("computing entropy B...");
		double eB = Entropy.entropy(automatonBadjusted);
		System.out.println(eB);

		if (canceller.isCancelled()) {
			return null;
		}

		System.out.println("computing conjunctive entropy...");
		double eC = conjunctiveEntropy(languageA, automatonB).doubleValue();
		System.out.println(eC);

		if (canceller.isCancelled()) {
			return null;
		}

		double gainRecall = eC / eA;
		double gainPrecision = eC / eB;

		return Pair.of(gainRecall, gainPrecision);
	}

	public static BigDecimal conjunctiveEntropy(StochasticLanguage languageA,
			StochasticDeterministicFiniteAutomatonMapped automatonB) {
		BigDecimal result = BigDecimal.ZERO;

		for (StochasticTraceIterator it = languageA.iterator(); it.hasNext();) {
			String[] trace = it.next();

			double pModel = getProbability(automatonB, trace).doubleValue();
			if (StochasticUtils.isLargerThanZero(pModel)) {
				//trace is in conjunction

				double entContribution = getTraceEntropy(it.getProbability(), pModel);

				result = result.add(BigDecimal.valueOf(entContribution));
			}
		}

		return result;
	}

	public static double getTraceEntropy(double pX, double pY) {
		double epsilon = StochasticUtils.getEpsilon();

		return Math.min(-pX * (1 - epsilon) * log2((pX * (1 - epsilon))), //
				-pY * (1 - epsilon) * log2(pY * (1 - epsilon))) //
				+ //
				Math.min(-pX * epsilon * log2(pX * epsilon), //
						-(pY * epsilon) * log2(pY * epsilon));
	}

	public static double log2(double value) {
		return Math.log(value) / Math.log(2);
	}

	private static BigDecimal getProbability(StochasticDeterministicFiniteAutomatonMapped automatonB, String[] trace) {
		int state = automatonB.getInitialState();
		EdgeIterableOutgoing it = automatonB.getOutgoingEdgesIterator(-1);
		BigDecimal probability = BigDecimal.ONE;

		for (String activity : trace) {
			short activityIndex = automatonB.transform(activity);

			//find the edge
			double activityProbability = Double.NaN;
			it.reset(state);
			while (it.hasNext()) {
				if (it.nextActivity() == activityIndex) {
					activityProbability = it.getProbability();
					state = it.getTarget();
					break;
				}
			}

			if (Double.isNaN(activityProbability)) {
				return BigDecimal.ZERO;
			}

			probability = probability.multiply(BigDecimal.valueOf(activityProbability));
		}

		//end trace
		probability = probability
				.multiply(BigDecimal.valueOf(StochasticUtils.getTerminationProbability(automatonB, state)));

		return probability;
	}

	public static double getActivityProbability(EdgeIterableOutgoing it, int state, short activity) {
		it.reset(state);
		while (it.hasNext()) {
			if (it.nextActivity() == activity) {
				return it.getProbability();
			}
		}
		return 0;
	}
}