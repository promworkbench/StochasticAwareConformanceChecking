package org.processmining.stochasticawareconformancechecking.plugins;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class StochasticFlowerMiner {
	@Plugin(name = "Mine stochastic flower model", returnLabels = { "Stochastic Petri net" }, returnTypes = {
			StochasticNet.class }, parameterLabels = {
					"Event log" }, userAccessible = true, help = "", level = PluginLevel.Regular)
	@UITopiaVariant(affiliation = IMMiningDialog.affiliation, author = IMMiningDialog.author, email = IMMiningDialog.email)
	@PluginVariant(variantLabel = "Compute entropy of sdfa, dialog", requiredParameterLabels = { 0 })
	public StochasticNet mine(final PluginContext context, XLog log) {
		TObjectIntMap<String> probabilities = new TObjectIntHashMap<>(10, 0.5f, 0);
		{
			for (XTrace trace : log) {
				for (XEvent event : trace) {
					String activity = XConceptExtension.instance().extractName(event);
					probabilities.adjustOrPutValue(activity, 1, 1);
				}
			}
		}

		StochasticNet net = new StochasticNetImpl("stochastic flower net");
		net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		net.setTimeUnit(TimeUnit.HOURS);
		Place source = net.addPlace("source");
		Marking marking = new Marking();
		marking.add(source);
		TimedTransition start = net.addTimedTransition("tau start", 1, DistributionType.UNIFORM, 0.0, 200.0);
		start.setInvisible(true);
		net.addArc(source, start);
		Place heart = net.addPlace("heart");
		net.addArc(start, heart);

		for (String activity : probabilities.keySet()) {
			double probability = probabilities.get(activity);
			Transition transition = net.addTimedTransition(activity, probability, DistributionType.UNIFORM, 0.0, 200.0);
			net.addArc(heart, transition);
			net.addArc(transition, heart);
		}

		TimedTransition stop = net.addTimedTransition("tau stop", log.size(), DistributionType.UNIFORM, 0.0, 200.0);
		stop.setInvisible(true);
		net.addArc(heart, stop);
		Place sink = net.addPlace("sink");
		net.addArc(stop, sink);

		return net;
	}
}
