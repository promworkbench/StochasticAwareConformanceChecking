package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.math.BigDecimal;

import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterableOutgoing;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonImpl;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMappedImpl;

import gnu.trove.iterator.TShortIterator;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TShortShortMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TShortShortHashMap;
import gnu.trove.stack.array.TLongArrayStack;

public class Projection {

	public static enum ChooseProbability {
		A, B, Minimum;

		public double getProbability(double probabilityA, double probabilityB) {
			switch (this) {
				case A :
					return probabilityA;
				case B :
					return probabilityB;
				case Minimum :
					return Math.min(probabilityA, probabilityB);
				default :
					return Double.MIN_VALUE;
			}
		}
	}

	public static <X> StochasticDeterministicFiniteAutomaton project(StochasticDeterministicFiniteAutomatonMapped<X> a,
			StochasticDeterministicFiniteAutomatonMapped<X> b, ChooseProbability chooseProbability) {
		StochasticDeterministicFiniteAutomatonMappedImpl<X> result = new StochasticDeterministicFiniteAutomatonMappedImpl<>();

		//first, relate the activities of the two automata
		TShortShortMap AToB = new TShortShortHashMap(10, 0.5f, (short) -1, (short) -1);
		for (TShortIterator it = a.allMappedIndices(); it.hasNext();) {
			short indexA = it.next();
			X elementA = a.transform(indexA);
			short indexB = b.transform(elementA);
			if (indexB >= 0) {
				AToB.put(indexA, indexB);
				result.transform(elementA, indexB);
			}
		}

		TLongArrayStack worklist = new TLongArrayStack(10, -1);
		TLongIntMap statePair2conjunctionState = new TLongIntHashMap(10, 0.5f, -1, -1);
		{
			long statePair = StatePair.pack(a.getInitialState(), b.getInitialState());
			worklist.push(statePair);
			statePair2conjunctionState.put(statePair, result.getInitialState());
		}

		EdgeIterableOutgoing itA = a.getOutgoingEdgesIterator(a.getInitialState());
		EdgeIterableOutgoing itB = b.getOutgoingEdgesIterator(b.getInitialState());
		while (worklist.size() > 0) {
			long sourcePair = worklist.pop();
			int projectionSourceState = statePair2conjunctionState.get(sourcePair);
			int stateA = StatePair.unpackA(sourcePair);
			int stateB = StatePair.unpackB(sourcePair);

			//			//count the termination probability, being the minimum of A and B
			//			BigDecimal termination;
			//			{
			//				BigDecimal terminationA = BigDecimal.ONE;
			//				itA.reset(stateA);
			//				while (itA.hasNext()) {
			//					terminationA = terminationA.subtract(itA.nextProbability());
			//				}
			//				BigDecimal terminationB = BigDecimal.ONE;
			//				itB.reset(stateB);
			//				while (itB.hasNext()) {
			//					terminationB = terminationB.subtract(itB.nextProbability());
			//				}
			//				//System.out.println("  add " + terminationA + ", " + terminationB + ", termination");
			//				termination = chooseProbability.getProbability(terminationA, terminationB);
			//			}
			//
			//			//count the sum probability
			//			BigDecimal totalProbability = termination;
			//			{
			//				for (itA.reset(stateA); itA.hasNext();) {
			//					short tA = AToB.get(itA.nextActivity());
			//					if (tA != -1) {
			//						for (itB.reset(stateB); itB.hasNext();) {
			//							short tB = itB.nextActivity();
			//							if (tA == tB) {
			//								//for all outgoing edges with the same activities
			//								//System.out.println("  add " + itA.getProbability() + ", " + itB.getProbability() + ", activity " + itA.getActivity());
			//								totalProbability = totalProbability.add(
			//										chooseProbability.getProbability(itA.getProbability(), itB.getProbability()));
			//							}
			//						}
			//					}
			//				}
			//			}
			//
			//			//System.out.println(" total probability: " + totalProbability);
			//			assert (totalProbability.compareTo(BigDecimal.ONE) <= 0);

			//add the edges to the automaton
			{
				for (itA.reset(stateA); itA.hasNext();) {
					short tA = AToB.get(itA.nextActivity());
					if (tA != -1) {
						for (itB.reset(stateB); itB.hasNext();) {
							short tB = itB.nextActivity();
							if (tA == tB) {
								//for all outgoing edges with the same activities
								//								process(result, worklist, statePair2conjunctionState, itA, itB, projectionSourceState,
								//										AToB, totalProbability, chooseProbability);
								process(result, worklist, statePair2conjunctionState, itA, itB, projectionSourceState,
										AToB, BigDecimal.ZERO, chooseProbability);
							}
						}
					}
				}
			}
		}

		return result;
	}

	public static void process(StochasticDeterministicFiniteAutomatonImpl result, TLongArrayStack worklist,
			TLongIntMap statePair2conjunctionState, EdgeIterableOutgoing itA, EdgeIterableOutgoing itB,
			int conjunctionSourceState, TShortShortMap AToB, BigDecimal totalProbability,
			ChooseProbability chooseProbability) {
		short tA = AToB.get(itA.getActivity());
		short tB = itB.getActivity();
		if (tA != -1 && tA == tB) {
			long statePairTarget = StatePair.pack(itA.getTarget(), itB.getTarget());
			int conjunctionTargetState = statePair2conjunctionState.get(statePairTarget);

			//			BigDecimal probability = chooseProbability.getProbability(itA.getProbability(), itB.getProbability())
			//					.divide(totalProbability, result.getRoundingMathContext());
			//			assert (totalProbability.compareTo(BigDecimal.ONE) <= 0);
			double probability = chooseProbability.getProbability(itA.getProbability(), itB.getProbability());

			if (conjunctionTargetState == statePair2conjunctionState.getNoEntryValue()) {
				//this state pair did not exist yet

				conjunctionTargetState = result.addEdge(conjunctionSourceState, tA, probability);
				statePair2conjunctionState.put(statePairTarget, conjunctionTargetState);

				worklist.push(statePairTarget);
			} else {
				//this state pair did not exist yet
				result.addEdge(conjunctionSourceState, tA, conjunctionTargetState, probability);
			}
		}
	}
}