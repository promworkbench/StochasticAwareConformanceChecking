package org.processmining.stochasticawareconformancechecking.automata;

import gnu.trove.iterator.TShortIterator;

public interface StochasticDeterministicFiniteAutomatonMapped extends StochasticDeterministicFiniteAutomaton {

	public short transform(String element);

	public void transform(String element, short index);

	public String transform(short index);

	public TShortIterator allMappedIndices();

	public StochasticDeterministicFiniteAutomatonMapped clone() throws CloneNotSupportedException;

}