package org.processmining.stochasticawareconformancechecking.automata;

import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;

public class StochasticDeterministicFiniteAutomaton2Dot {
	public static Dot toDot(StochasticDeterministicFiniteAutomaton automaton) {
		Dot result = new Dot();
		result.setDirection(GraphDirection.leftRight);
		result.setNodeOption("shape", "circle");

		DotNode[] nodes = new DotNode[automaton.getNumberOfStates()];
		nodes[0] = result.addNode(automaton.getInitialState() + "");

		//edges
		EdgeIterable it = automaton.getEdgesIterator();
		while (it.hasNext()) {
			it.next();
			int source = it.getSource();
			if (nodes[source] == null) {
				nodes[source] = result.addNode(source + "");
			}

			int target = it.getTarget();
			if (nodes[target] == null) {
				nodes[target] = result.addNode(target + "");
			}

			String activity;
			if (automaton instanceof StochasticDeterministicFiniteAutomatonMapped) {
				activity = ((StochasticDeterministicFiniteAutomatonMapped) automaton).transform(it.getActivity());
			} else {
				activity = it.getActivity() + "";
			}
			result.addEdge(nodes[source], nodes[target], activity + " p" + it.getProbability());
		}

		//initial state
		DotNode initial = result.addNode("");
		initial.setOption("shape", "plaintext");
		result.addEdge(initial, nodes[0]);

		return result;
	}
}
