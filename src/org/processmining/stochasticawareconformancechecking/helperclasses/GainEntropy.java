package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.commons.io.IOUtils;
import org.deckfour.xes.model.XLog;
import org.processmining.earthmoversstochasticconformancechecking.algorithms.XLog2StochasticLanguage;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.Activity2IndexKey;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.StochasticLanguage;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.StochasticTraceIterator;
import org.processmining.earthmoversstochasticconformancechecking.stochasticlanguage.TotalOrder;
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

		try {
			IOUtils.write(automatonA.toString(), new FileWriter(
					"/home/sander/Documents/svn/20 - stochastic conformance checking - Artem/05 - IS paper invited/experiment/automatonA.dot"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		RelativeEntropy.prepareAndCheckAutomaton(automatonA);

		if (canceller.isCancelled()) {
			return null;
		}

		StochasticDeterministicFiniteAutomatonMapped automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
				.convert(pnB, initialMarking);

		try {
			IOUtils.write(automatonB.toString(), new FileWriter(
					"/home/sander/Documents/svn/20 - stochastic conformance checking - Artem/05 - IS paper invited/experiment/automatonB-before.dot"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		StochasticDeterministicFiniteAutomatonMapped automatonBadjusted = automatonB.clone();
		RelativeEntropy.prepareAndCheckAutomaton(automatonBadjusted);

		try {
			IOUtils.write(automatonBadjusted.toString(), new FileWriter(
					"/home/sander/Documents/svn/20 - stochastic conformance checking - Artem/05 - IS paper invited/experiment/automatonB.dot"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (canceller.isCancelled()) {
			return null;
		}

		Activity2IndexKey activityKey = new Activity2IndexKey();
		StochasticLanguage<TotalOrder> languageA = XLog2StochasticLanguage.convert(log,
				MiningParameters.getDefaultClassifier(), activityKey, canceller);

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

	public static BigDecimal conjunctiveEntropy(StochasticLanguage<TotalOrder> languageA,
			StochasticDeterministicFiniteAutomatonMapped automatonB) {
		BigDecimal result = BigDecimal.ZERO;

		for (StochasticTraceIterator<TotalOrder> it = languageA.iterator(); it.hasNext();) {
			String[] trace = languageA.getActivityKey().toTraceString(it.next());

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
		double epsilon = StochasticUtils.getMeaningfulepsilon();
		double min1 = -pX * (1 - epsilon) * log2((pX * (1 - epsilon)));
		double min2 = -pY * (1 - epsilon) * log2(pY * (1 - epsilon));
		double min3 = -pX * epsilon * log2(pX * epsilon);
		double min4 = -(pY * epsilon) * log2(pY * epsilon);

		//		System.out.println("min1 " + min1);
		//		System.out.println("min2 " + min2);
		//		System.out.println("min3 " + min3);
		//		System.out.println("min4 " + min4);
		//
		//		System.out.println("sumB " + (min2 + min4));

		//return Math.min(min1 + min3, min2 + min4);
		return Math.min(min1, min2) + Math.min(min3, min4);
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