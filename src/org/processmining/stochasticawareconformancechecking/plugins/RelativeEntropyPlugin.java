package org.processmining.stochasticawareconformancechecking.plugins;

import java.math.BigDecimal;

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
import org.processmining.stochasticawareconformancechecking.automata.StochasticPetriNet2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.helperclasses.RelativeEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedAutomatonException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

public class RelativeEntropyPlugin {
	@Plugin(name = "Compute relative entropy of stochastic deterministic finite automata", returnLabels = {
			"Entropy" }, returnTypes = { HTMLToString.class }, parameterLabels = {
					"Stochastic deterministic finite automaton" }, userAccessible = true, help = "Compute relative entropy of stochastic deterministic finite automata.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0, 1 })
	public <X> HTMLToString computeSDFASDFA(final PluginContext context,
			StochasticDeterministicFiniteAutomatonMapped<String> automatonA,
			StochasticDeterministicFiniteAutomatonMapped<String> automatonB)
			throws CloneNotSupportedException, UnsupportedAutomatonException {
		final Pair<BigDecimal, BigDecimal> entropy = RelativeEntropy.relativeEntropy(automatonA, automatonB);
		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "recall: " + entropy.getA() + "<br>precision: " + entropy.getB();
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

		StochasticDeterministicFiniteAutomatonMapped<String> automatonA = Log2StochasticDeterministicFiniteAutomaton
				.convert(log, MiningParameters.getDefaultClassifier(), new ProMCanceller() {
					public boolean isCancelled() {
						return context.getProgress().isCancelled();
					}
				});

		Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
				.guessInitialMarking(pnB);
		StochasticDeterministicFiniteAutomatonMapped<String> automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton
				.convert(pnB, initialMarking);
		final Pair<BigDecimal, BigDecimal> entropy = RelativeEntropy.relativeEntropy(automatonA, automatonB);
		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "recall: " + entropy.getA() + "<br>precision: " + entropy.getB();
			}
		};
	}
}
