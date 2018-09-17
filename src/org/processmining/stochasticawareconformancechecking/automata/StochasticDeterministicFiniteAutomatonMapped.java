package org.processmining.stochasticawareconformancechecking.automata;

import gnu.trove.iterator.TShortIterator;

public interface StochasticDeterministicFiniteAutomatonMapped<X> extends StochasticDeterministicFiniteAutomaton {

	public short transform(X element);

	public void transform(X element, short index);

	public X transform(short index);

	public TShortIterator allMappedIndices();

	public StochasticDeterministicFiniteAutomatonMapped<X> clone() throws CloneNotSupportedException;

}