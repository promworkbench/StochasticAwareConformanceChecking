package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

public class CheckDeadPaths {

	public static boolean hasDeathPaths(StochasticDeterministicFiniteAutomaton automaton) {
		//apply Tarjan's algorithm to find strongly connected components
		AtomicInteger index = new AtomicInteger(0);
		TIntStack S = new TIntArrayStack();
		TIntIntMap indices = new TIntIntHashMap(10, 0.5f, -1, -1);
		TIntIntMap lowlink = new TIntIntHashMap(10, 0.5f, -1, -1);
		EdgeIterableOutgoing it = automaton.getOutgoingEdgesIterator(automaton.getInitialState());
		BitSet onStack = new BitSet(automaton.getNumberOfStates());
		for (int state = 0; state < automaton.getNumberOfStates(); state++) {
			if (!indices.containsKey(state)) {
				if (strongconnect(state, index, S, indices, lowlink, onStack, it)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean strongconnect(int v, AtomicInteger index, TIntStack S, TIntIntMap indices,
			TIntIntMap lowlink, BitSet onStack, EdgeIterableOutgoing it) {
		//set the depth index for state to the smalles unused index
		indices.put(v, index.get());
		lowlink.put(v, index.get());
		index.incrementAndGet();
		S.push(v);
		onStack.set(v);

		//consider successors of state
		it.reset(v);
		while (it.hasNext()) {
			int w = it.nextTarget();
			if (!indices.containsKey(w)) {
				strongconnect(w, index, S, indices, lowlink, onStack, it);
				lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
			} else if (onStack.get(w)) {
				lowlink.put(v, Math.min(lowlink.get(v), indices.get(w)));
			}
		}

		//if v is a root node, pop the stack and generate an SCC
		if (lowlink.get(v) == indices.get(v)) {
			//start a new strongly connected component
			TIntList scc = new TIntArrayList();
			int w;
			do {
				w = S.pop();
				onStack.clear(w);
				//add w to current strongly connected component
				scc.add(w);
			} while (w != v);
			//output the current strongly connected component
			if (check(it, scc)) {
				return true;
			}
		}

		return false;
	}

	private static boolean check(EdgeIterableOutgoing it, TIntList component) {
		//check whether the strongly connected component has an outgoing edge or can terminate
		TIntIterator stateIt = component.iterator();
		while (stateIt.hasNext()) {
			int state = stateIt.next();
			it.reset(state);
			double sum = 0;

			while (it.hasNext()) {
				if (!component.contains(it.nextTarget())) {
					//we can leave the component
					return false;
				}

				sum += it.getProbability();
			}

			if (sum < 1) {
				//we can terminate from this state
				return false;
			}
		}
		return true;
	}
}
