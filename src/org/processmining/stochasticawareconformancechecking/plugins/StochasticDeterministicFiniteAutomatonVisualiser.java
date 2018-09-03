package org.processmining.stochasticawareconformancechecking.plugins;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton2Dot;

public class StochasticDeterministicFiniteAutomatonVisualiser {
	@Plugin(name = "Stochastic Deterministic Finite Automaton visualisation (GraphViz)", returnLabels = {
			"Dot visualization" }, returnTypes = {
					JComponent.class }, parameterLabels = { "SDFA" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Visualise SDFA", requiredParameterLabels = { 0 })
	public DotPanel visualise(PluginContext context, StochasticDeterministicFiniteAutomaton automaton)
			throws UnknownTreeNodeException {
		Dot dot = StochasticDeterministicFiniteAutomaton2Dot.toDot(automaton);
		return new DotPanel(dot);
	}

}
