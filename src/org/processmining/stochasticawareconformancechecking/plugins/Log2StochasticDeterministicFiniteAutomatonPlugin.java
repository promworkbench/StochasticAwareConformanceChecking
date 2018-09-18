package org.processmining.stochasticawareconformancechecking.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.inductiveminer2.mining.MiningParameters;
import org.processmining.stochasticawareconformancechecking.automata.Log2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedLogException;

public class Log2StochasticDeterministicFiniteAutomatonPlugin {
	@Plugin(name = "Convert log to stochastic deterministic finite automaton", returnLabels = {
			"Stochastic deterministic finite automaton" }, returnTypes = {
					StochasticDeterministicFiniteAutomatonMapped.class }, parameterLabels = {
							"Event log" }, userAccessible = true, help = "Convert log to stochastic deterministic finite automaton.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Mine a sdfa, dialog", requiredParameterLabels = { 0 })
	public StochasticDeterministicFiniteAutomatonMapped<String> convert(final PluginContext context, XLog log)
			throws UnsupportedLogException {
		return Log2StochasticDeterministicFiniteAutomaton.convert(log, MiningParameters.defaultClassifier,
				new ProMCanceller() {
					public boolean isCancelled() {
						return context.getProgress().isCancelled();
					}
				});
	}
}
