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
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.stochasticawareconformancechecking.helperclasses.GainEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedAutomatonException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

public class GainEntropyPlugin {

	@Plugin(name = "Compute gain entropy of a log and a stochastic Petri net", returnLabels = {
			"Entropy" }, returnTypes = { HTMLToString.class }, parameterLabels = {
					"Stochastic deterministic finite automaton",
					"Stochastic Petri net" }, userAccessible = true, help = "Compute relative entropy of stochastic deterministic finite automata.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0, 1 })
	public HTMLToString computeSPNSDFA(final PluginContext context, XLog log, StochasticNet pnB)
			throws IllegalTransitionException, UnsupportedPetriNetException, CloneNotSupportedException,
			UnsupportedLogException, UnsupportedAutomatonException {

		final Pair<Double, Double> entropy = compute(log, pnB, new ProMCanceller() {
			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		});

		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "gain recall: " + entropy.getA() + "<br>gain precision: " + entropy.getB();
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
		Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
				.guessInitialMarking(pnB);
		final Pair<Double, Double> entropyHalf = GainEntropy.compute(log, pnB, initialMarking, canceller);
		return entropyHalf;
	}
}
