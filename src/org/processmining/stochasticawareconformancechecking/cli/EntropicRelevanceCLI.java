package org.processmining.stochasticawareconformancechecking.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.helperclasses.StochasticUtils;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;
import org.processmining.stochasticawareconformancechecking.plugins.StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin;
import org.processmining.xeslite.plugin.OpenLogFileLiteImplPlugin;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.strategy.HashingStrategy;

public class EntropicRelevanceCLI {
	
	//private static XEventClassifier eventClassifier = XLogInfoImpl.NAME_CLASSIFIER;

	public static void main(String[] args) throws Exception {
		if (args.length!=2) {
			System.out.println("Wrong command line argumetns");
			return;
		}
		
		System.out.println("PNML: " + args[0]);
		System.out.println("XES:  " + args[1]);
	
		// Load log
		File fileLog = new File(args[1]); 
		if (fileLog.isDirectory() ||!fileLog.canRead() || !fileLog.isFile()) {
			System.out.println("Cannot read log file."); 
			return; 
		} 
		PluginContext context = new FakeContext(); 
		XLog log = (XLog) new OpenLogFileLiteImplPlugin().importFile(context, fileLog);
		  
		// Load net 
		File fileNet = new File(args[0]); 
		if (fileNet.isDirectory() || !fileNet.canRead() || !fileNet.isFile()) {
			System.out.println("Cannot read model file."); 
			return;
		}
		Serializer serializer = new Persister(); 
		PNMLRoot pnml = serializer.read(PNMLRoot.class,fileNet);
		StochasticNetDeserializer converter = new StochasticNetDeserializer();
		Object[] objs = converter.convertToNet(context, pnml, fileNet.getName(), true); 
		StochasticNet model = (StochasticNet) objs[0];
		
		// Prepare log
		long start = System.nanoTime();
		//Collection<String> log = new ArrayList<String>();
		Map<XTrace,Integer> logFreq = new HashMap<XTrace,Integer>();
		int nTraces = 0;
		for (XTrace trace: log) {
			nTraces++;
			if (logFreq.containsKey(trace)) {
				logFreq.put(trace, Integer.valueOf(Integer.valueOf((logFreq.get(trace)))));
			}
			else {
				logFreq.put(trace, 1);
			}
		}
		
		// TODO: Compute entropic relevance
		double entRel   = 0.0;
		int coverage = 0;
		for (Map.Entry<XTrace,Integer> traceFreq: logFreq.entrySet()) {
			// Sander: Please implement the EntropicRelevanceCLI.getTraceProbability() method.
			// I started, but not sure how to finish :)
			// Once this is ready, I will debug if everything works together correctly. This should take couple of minutes (max 1 hour).
			Double p = EntropicRelevanceCLI.getTraceProbability(model, traceFreq.getKey());	
			if (!p.isNaN()) {
				coverage += traceFreq.getValue().intValue();
				entRel   += - ((double) traceFreq.getValue().intValue()/nTraces) * EntropicRelevanceCLI.log2(p.doubleValue());
			}
		}
		
		double p = (double) coverage/nTraces;
		System.out.println(p);
		entRel += - p * EntropicRelevanceCLI.log2(p) - (1-p) * EntropicRelevanceCLI.log2(1-p);
		long finish = System.nanoTime();
		
		System.out.println("Entropic relevance: " + entRel);
		System.out.println("Computation time: " + (finish-start) + " nanoseconds.");
	}

	// TODO: Sander: PLease finalize this method.
	public static Double getTraceProbability(StochasticNet net, XTrace trace) {
		Marking iniMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin.guessInitialMarking(net);
		EfficientStochasticNetSemanticsImpl s = new EfficientStochasticNetSemanticsImpl();
		s.initialize(net.getTransitions(), iniMarking);
		s.setCurrentState(iniMarking);
		short[] initialMarking = s.getCurrentInternalState().clone();

		//Map<Transition, Pair<short[], Double>> enabledTransitions = getEnabledTransitions(s, marking, result);
		
		short[] marking = initialMarking;
		double p = 1.0;
		for (XEvent e : trace) {
			String eName = e.getAttributes().get("concept:name").toString();
			Map<Transition, Pair<short[], Double>> enabledTransitions = getEnabledTransitions(s, marking, result);
		}
		
		return Double.NaN;
	}
	
	public static double log2(double d)
    {
		if(d <= 0) throw new IllegalArgumentException();
		return Math.log(d) / Math.log(2);
    }
	
	/**
	 * Search through the enabled state spaces following tau transitions for
	 * non-silent transitions that are enabled.
	 * 
	 * @param mc
	 * @param automaton
	 * 
	 * @return
	 * @throws IllegalTransitionException
	 * @throws UnsupportedPetriNetException
	 */
	private static Map<Transition, Pair<short[], Double>> getEnabledTransitions(
			EfficientStochasticNetSemanticsImpl semantics, 
			short[] startMarking,
			StochasticDeterministicFiniteAutomaton automaton) throws IllegalTransitionException {

		Map<Transition[], short[]> path2marking = getPaths(semantics, startMarking);
		List<Transition[]> paths = new ArrayList<>(path2marking.keySet());

		//gather the probabilities for each transition
		double termination = 0;
		TObjectDoubleMap<Transition> probabilities = new TObjectDoubleHashMap<>(10, 0.5f, 0);
		Iterator<Transition[]> it = paths.iterator();
		for (int i = 0; i < paths.size(); i++) {
			Transition[] path = it.next();

			Transition t = path[path.length - 1];

			if (t.isInvisible()) {
				//the last transition on this path is silent, so this path adds to the termination probability. 
				termination = termination + getPathProbability(paths, i, automaton);
			} else {
				//the last transition on this path is an activity, so this path adds to the probability of that activity.
				double probability = getPathProbability(paths, i, automaton);

				probabilities.put(t, probabilities.get(t) + probability);
			}
		}

		//keep track of the shortest path for each transition (prevents concurrent silent transitions to interfere)
		THashMap<Transition, Transition[]> shortestPaths = new THashMap<>();
		{
			TObjectIntMap<Transition> shortestPathsLengths = new TObjectIntHashMap<>(10, 0.5f, Integer.MAX_VALUE);
			for (Transition[] path : paths) {
				Transition t = path[path.length - 1];

				if (!t.isInvisible()) {
					int shortestPath = shortestPathsLengths.get(t);
					if (path.length < shortestPath) {
						shortestPathsLengths.put(t, path.length);
						shortestPaths.put(t, path);
					}
				}
			}
		}

		//gather the result
		Map<Transition, Pair<short[], Double>> result = new THashMap<>();
		for (Transition t : shortestPaths.keySet()) {
			Transition[] path = shortestPaths.get(t);
			result.put(t, Pair.of(path2marking.get(path), probabilities.get(t)));
		}

		return result;
	}

	/**
	 * 
	 * @param paths
	 * @param pathIndex
	 * @param automaton
	 * @return the probability of the path
	 */
	private static double getPathProbability(List<Transition[]> paths, int pathIndex,
			StochasticDeterministicFiniteAutomaton automaton) {
		BitSet prefixMatches = new BitSet(paths.size());
		prefixMatches.set(0, paths.size());
		Transition[] path = paths.get(pathIndex);

		double result = 1;

		for (int round = 0; round < path.length; round++) {

			//compute the total sum of this round
			double sumWeightRound = 0;
			int numberOfEnabledTransitions = 0;
			{
				THashSet<Transition> enabledTransitions = new THashSet<>();
				for (int pathIndex2 = prefixMatches.nextSetBit(0); pathIndex2 >= 0; pathIndex2 = prefixMatches
						.nextSetBit(pathIndex2 + 1)) {
					enabledTransitions.add(paths.get(pathIndex2)[round]);
				}
				for (Transition t : enabledTransitions) {
					sumWeightRound += ((TimedTransition) t).getWeight();
					numberOfEnabledTransitions++;
				}
			}

			//add the probability of this step to the final result
			{
				Transition t = path[round];
				if (StochasticUtils.isLargerThanZero(sumWeightRound)) {
					double probabilityThisStep = (((TimedTransition) t).getWeight()) / sumWeightRound;
					result = result * probabilityThisStep;
				} else {
					//if the sum weight is zero, then all transitions have no probabilities. Distribute evenly.
					double probabilityThisStep = 1.0 / numberOfEnabledTransitions;
					result = result * probabilityThisStep;
				}
			}

			//next, limit the paths that are not prefixes of pathIndex
			for (int pathIndex2 = prefixMatches.nextSetBit(0); pathIndex2 >= 0; pathIndex2 = prefixMatches
					.nextSetBit(pathIndex2 + 1)) {
				if (!paths.get(pathIndex2)[round].equals(path[round]) || paths.get(pathIndex2).length == round + 1) {
					//if this round's transition is not the same, or the path is too short for a next round, exclude it.
					prefixMatches.clear(pathIndex2);
				}
			}
		}
		return result;
	}

	/**
	 * Get all paths ending in a non-silent transition and the marking they end
	 * up in.
	 * 
	 * @param semantics
	 * @param startMarking
	 * @return
	 * @throws IllegalTransitionException
	 */
	private static Map<Transition[], short[]> getPaths(EfficientStochasticNetSemanticsImpl semantics,
			short[] startMarking) throws IllegalTransitionException {
		Map<Transition[], short[]> result = getPathsSet();
		Transition[] pathUpTillNow = new Transition[0];
		Set<short[]> visited = getMarkingSet();
		visited.add(startMarking);
		getPaths(semantics, startMarking, result, visited, pathUpTillNow);
		return result;
	}

	/**
	 * Explore the startMarking for paths towards non-silent transitions.
	 * 
	 * @param semantics
	 * @param startMarking
	 * @param result
	 * @param pathUpTillNow
	 * @throws IllegalTransitionException
	 */
	private static void getPaths(EfficientStochasticNetSemanticsImpl semantics, short[] marking,
			Map<Transition[], short[]> result, Set<short[]> visited, Transition[] pathUpTillNow)
			throws IllegalTransitionException {
		semantics.setCurrentState(marking);
		Collection<Transition> enabledTransitions = semantics.getExecutableTransitions();

		//if we can terminate in this marking, add a corresponding path, but not an empty one
		if (enabledTransitions.isEmpty() && pathUpTillNow.length != 0) {
			result.put(pathUpTillNow, marking);
		}

		for (Transition t : enabledTransitions) {
			semantics.setCurrentState(marking);
			semantics.executeExecutableTransition(t);

			Transition[] newPath = Arrays.copyOf(pathUpTillNow, pathUpTillNow.length + 1);
			newPath[newPath.length - 1] = t;

			short[] newMarking = semantics.getCurrentInternalState().clone();

			if (t.isInvisible()) {
				//we encountered a new invisible transition; follow
				if (!visited.contains(newMarking)) {
					visited.add(newMarking);
					getPaths(semantics, newMarking, result, visited, newPath);
				}
			} else {
				//we reached the end of a path; add the path to the set
				result.put(newPath, newMarking);
			}
		}
	}

	private static Map<Transition[], short[]> getPathsSet() {
		return new TCustomHashMap<>(new HashingStrategy<Transition[]>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public int computeHashCode(Transition[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(Transition[] o1, Transition[] o2) {
				return Arrays.equals(o1, o2);
			}
		});
	}

	private static Set<short[]> getMarkingSet() {
		return new TCustomHashSet<>(new HashingStrategy<short[]>() {
			private static final long serialVersionUID = 9085136431842993102L;

			public int computeHashCode(short[] object) {
				return Arrays.hashCode(object);
			}

			public boolean equals(short[] o1, short[] o2) {
				return Arrays.equals(o1, o2);
			}
		});
	}
}
