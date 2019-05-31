package org.processmining.stochasticawareconformancechecking.plugins;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.helperclasses.StochasticPetriNet2StochasticDeterministicFiniteAutomaton2;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

public class StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin {
	@Plugin(name = "Convert stochastic Petri net to stochastic deterministic finite automaton", returnLabels = {
			"Stochastic deterministic finite automaton" }, returnTypes = {
					StochasticDeterministicFiniteAutomatonMapped.class }, parameterLabels = {
							"Stochastic Petri net" }, userAccessible = true, help = "Convert stochastic Petri net to stochastic deterministic finite automaton. The net must be a workflow net, bounded and stochastically regular.", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Mine a sdfa, dialog", requiredParameterLabels = { 0 })
	public StochasticDeterministicFiniteAutomatonMapped convert(final PluginContext context, StochasticNet net)
			throws IllegalTransitionException, UnsupportedPetriNetException {
		Marking initialMarking = guessInitialMarking(net);
		return StochasticPetriNet2StochasticDeterministicFiniteAutomaton2.convert(net, initialMarking);
	}

	public static Marking guessInitialMarking(Petrinet net) {
		Marking result = new Marking();
		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty()) {
				result.add(p);
			}
		}

		if (result.isEmpty()) {
			for (Place p : net.getPlaces()) {
				if (p.getLabel().equals("source 1")) {
					result.add(p);
				}
			}
		}

		if (result.isEmpty()) {
			System.out.println("Could not find initial marking");
		}

		return result;
	}
}
