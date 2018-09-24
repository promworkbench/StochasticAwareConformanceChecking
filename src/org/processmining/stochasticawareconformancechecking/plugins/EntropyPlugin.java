package org.processmining.stochasticawareconformancechecking.plugins;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.HTMLToString;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.helperclasses.Entropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedAutomatonException;

public class EntropyPlugin {
	@Plugin(name = "Compute entropy of stochastic deterministic finite automaton", returnLabels = {
			"Entropy" }, returnTypes = { HTMLToString.class }, parameterLabels = {
					"Stochastic deterministic finite automaton" }, userAccessible = true, help = "Compute entropy of stochastic deterministic finite automaton.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0 })
	public HTMLToString mineGuiProcessTree(final PluginContext context,
			StochasticDeterministicFiniteAutomaton automaton) throws UnsupportedAutomatonException {
		final double entropy = Entropy.entropy(automaton);
		return new HTMLToString() {
			public String toHTMLString(boolean includeHTMLTags) {
				return "entropy: " + entropy;
			}
		};
	}
}
