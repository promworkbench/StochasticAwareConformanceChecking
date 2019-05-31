package org.processmining.stochasticawareconformancechecking.automata;

import java.util.ArrayList;

import gnu.trove.iterator.TShortIterator;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class StochasticDeterministicFiniteAutomatonMappedImpl extends StochasticDeterministicFiniteAutomatonImpl
		implements StochasticDeterministicFiniteAutomatonMapped {

	TObjectShortMap<String> activity2index = new TObjectShortHashMap<>(10, 0.6f, (short) -1);
	ArrayList<String> index2activity = new ArrayList<>();
	short maxIndex = -1;

	public short transform(String element) {
		short index = activity2index.putIfAbsent(element, (short) (maxIndex + 1));
		if (index == activity2index.getNoEntryValue()) {
			if (maxIndex == Short.MAX_VALUE) {
				throw new RuntimeException("too many activities");
			}
			index2activity.add(element);
			index = (short) (maxIndex + 1);
			maxIndex++;
		}
		return index;
	}

	public void transform(String element, short index) {
		activity2index.put(element, index);
		while (index2activity.size() <= index) {
			index2activity.add(null);
		}
		index2activity.set(index, element);
		maxIndex = (short) Math.max(index, maxIndex);
	}

	public String transform(short index) {
		return index2activity.get(index);
	}

	public TShortIterator allMappedIndices() {
		return new TShortIterator() {
			short now = -1;

			public void remove() {
				throw new NotImplementedException();
			}

			public boolean hasNext() {
				return now < index2activity.size() - 1;
			}

			public short next() {
				now++;
				return now;
			}
		};
	}

	public StochasticDeterministicFiniteAutomatonMappedImpl clone() throws CloneNotSupportedException {
		StochasticDeterministicFiniteAutomatonMappedImpl result = (StochasticDeterministicFiniteAutomatonMappedImpl) super.clone();

		result.activity2index = new TObjectShortHashMap<>(this.activity2index);
		result.index2activity = new ArrayList<>(this.index2activity);
		result.maxIndex = this.maxIndex;

		return result;
	}
}
