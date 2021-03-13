package org.processmining.stochasticawareconformancechecking.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.HTMLToString;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.stochasticawareconformancechecking.automata.Log2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.helperclasses.GainEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.RelativeEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.StochasticPetriNet2StochasticDeterministicFiniteAutomaton2;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedAutomatonException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

public class RelativeEntropyPlugin {
	@Plugin(name = "Compute relative entropy of stochastic deterministic finite automata", returnLabels = {
			"Entropy" }, returnTypes = { HTMLToString.class }, parameterLabels = {
					"Stochastic deterministic finite automaton (log)",
					"Stochastic deterministic finite automaton (model)" }, userAccessible = true, help = "Compute relative entropy of stochastic deterministic finite automata.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0, 1 })
	public HTMLToString computeSDFASDFA(final PluginContext context,
			StochasticDeterministicFiniteAutomatonMapped automatonA,
			StochasticDeterministicFiniteAutomatonMapped automatonB)
			throws CloneNotSupportedException, UnsupportedAutomatonException {
		final Pair<Double, Double> entropyHalf = RelativeEntropy.relativeEntropyHalf(automatonA, automatonB);

		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "single-sided recall: " + entropyHalf.getA() + "<br>single-sided precision: "
						+ entropyHalf.getB();
			}
		};

	}

	@Plugin(name = "Compute relative entropy of a log and a stochastic Petri net", returnLabels = {
			"Entropy" }, returnTypes = { HTMLToString.class }, parameterLabels = {
					"Stochastic deterministic finite automaton",
					"Stochastic Petri net" }, userAccessible = true, help = "Compute relative entropy of stochastic deterministic finite automata.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0, 1 })
	public HTMLToString computeSPNSDFA(final PluginContext context, XLog log, StochasticNet pnB)
			throws IllegalTransitionException, UnsupportedPetriNetException, CloneNotSupportedException,
			UnsupportedLogException, UnsupportedAutomatonException {

		final Pair<Double, Double> entropyHalf = compute(log, pnB, new ProMCanceller() {
			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		});

		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "single-sided recall: " + entropyHalf.getA() + "<br>single-sided precision: "
						+ entropyHalf.getB();
			}
		};
	}

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
		StochasticDeterministicFiniteAutomatonMapped automatonA = Log2StochasticDeterministicFiniteAutomaton
				.convert(log, MiningParameters.getDefaultClassifier(), canceller);

		Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
				.guessInitialMarking(pnB);
		StochasticDeterministicFiniteAutomatonMapped automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
				.convert(pnB, initialMarking);
		//final Pair<Double, Double> entropy = RelativeEntropy.relativeEntropy(automatonA, automatonB);
		final Pair<Double, Double> entropyHalf = RelativeEntropy.relativeEntropyHalf(automatonA, automatonB);
		//final Pair<BigDecimal, BigDecimal> entropy = Pair.of(new BigDecimal("-100"), new BigDecimal("-100"));
		return entropyHalf;
	}

	/**
	 * 
	 * @param log
	 * @param pnB
	 * @param canceller
	 * @return (gain recall, gain precision)
	 * @throws CloneNotSupportedException
	 * @throws UnsupportedAutomatonException
	 * @throws UnsupportedPetriNetException
	 * @throws IllegalTransitionException
	 * @throws UnsupportedLogException
	 */
	public static Pair<Double, Double> computeGain(XLog log, StochasticNet pnB, ProMCanceller canceller)
			throws UnsupportedLogException, IllegalTransitionException, UnsupportedPetriNetException,
			UnsupportedAutomatonException, CloneNotSupportedException {
		Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
				.guessInitialMarking(pnB);

		final Pair<Double, Double> gainEntropy = GainEntropy.compute(log, pnB, initialMarking, canceller);
		return gainEntropy;
	}

	@Plugin(name = "Compute relative entropy of two stochastic Petri nets", returnLabels = {
			"Entropy" }, returnTypes = { HTMLToString.class }, parameterLabels = {
					"Stochastic Petri net" }, userAccessible = true, help = "Compute relative entropy of stochastic deterministic finite automata.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0, 0 })
	public HTMLToString compute(final PluginContext context, StochasticNet pnA, StochasticNet pnB)
			throws IllegalTransitionException, UnsupportedPetriNetException, CloneNotSupportedException,
			UnsupportedAutomatonException {

		StochasticDeterministicFiniteAutomatonMapped automatonA;
		{
			Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
					.guessInitialMarking(pnA);
			automatonA = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2.convert(pnA, initialMarking);
			//final Pair<Double, Double> entropy = RelativeEntropy.relativeEntropy(automatonA, automatonB);
		}

		StochasticDeterministicFiniteAutomatonMapped automatonB;
		{
			Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
					.guessInitialMarking(pnB);
			automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2.convert(pnB, initialMarking);
			//final Pair<Double, Double> entropy = RelativeEntropy.relativeEntropy(automatonA, automatonB);
		}
		final Pair<Double, Double> entropyHalf = RelativeEntropy.relativeEntropyHalf(automatonA, automatonB);
		//final Pair<BigDecimal, BigDecimal> entropy = Pair.of(new BigDecimal("-100"), new BigDecimal("-100"));

		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "single-sided recall: " + entropyHalf.getA() + "<br>single-sided precision: "
						+ entropyHalf.getB();
			}
		};
	}
}
