package org.processmining.stochasticawareconformancechecking.helperclasses;

import java.util.Iterator;

/**
 * Packs several markings (short[]) into a single short[], while keeping them
 * sorted for easy comparison. Idea: the marking set first contains the number
 * of shorts of the first marking (1 short), then the first marking, then the
 * number of shorts of the second marking (in 1 short), then the second marking,
 * etc.
 * 
 * @author sander
 *
 */
public class MarkingSet {

	private static final int headerLength = 1;

	public static short[] create(short[] marking) {
		short[] result = new short[headerLength + marking.length];
		//header
		setHeader(result, 0, marking.length);
		//marking
		System.arraycopy(marking, 0, result, headerLength, marking.length);
		return result;
	}

	/**
	 * Add the marking to the markingSet. If the marking was already in the
	 * markingSet, add the weight.
	 * 
	 * @param markingSet
	 * @param marking
	 * @return the new markingSet, or an updated one.
	 */
	public static short[] add(short[] markingSet, short[] marking) {
		int currentIndex = 0;
		while (currentIndex < markingSet.length) {
			//read the header
			int currentMarkingLength = getMarkingLength(markingSet, currentIndex);
			int compare = compare(markingSet, marking, currentIndex);
			if (compare < 0) {
				//the new marking should not come before this marking; continue
				currentIndex += headerLength + currentMarkingLength;
			} else if (compare == 0) {
				//the marking is equal and was already present; do nothing

				return markingSet;
			} else {
				//the current marking should come after the to-be inserted  
				break;
			}
		}

		short[] result = new short[markingSet.length + marking.length + headerLength];

		//the new marking should come before this marking; insert
		System.arraycopy(markingSet, 0, result, 0, currentIndex);
		//header
		setHeader(result, currentIndex, marking.length);
		System.arraycopy(marking, 0, result, currentIndex + headerLength, marking.length);
		//existing markings afterwards
		System.arraycopy(markingSet, currentIndex, result, currentIndex + headerLength + marking.length,
				markingSet.length - currentIndex);

		return result;
	}

	public static short[] addAll(short[] markingSetA, short[] markingSetB) {
		short[] result = markingSetA;
		for (MarkingIterator it = getMarkings(markingSetB); it.hasNext();) {
			short[] marking = it.next();
			result = add(result, marking);
		}
		return result;
	}

	public static class MarkingIterator implements Iterator<short[]> {

		int now = -1;
		int next = 0;
		short[] markingSet;

		public MarkingIterator(final short[] markingSet) {
			this.markingSet = markingSet;
		}

		public boolean hasNext() {
			return next < markingSet.length;
		}

		public short[] next() {
			now = next;
			int markingLength = getMarkingLength(markingSet, now);
			next += headerLength + markingLength;
			short[] result = new short[markingLength];
			System.arraycopy(markingSet, now + headerLength, result, 0, markingLength);
			return result;
		}
	}

	public static MarkingIterator getMarkings(final short[] markingSet) {
		return new MarkingIterator(markingSet);
	}

	public static boolean contains(short[] markingSet, short[] marking) {
		int currentIndex = 0;
		while (currentIndex < markingSet.length) {
			//read the header
			int currentMarkingLength = getMarkingLength(markingSet, currentIndex);
			int compare = compare(markingSet, marking, currentIndex);
			if (compare < 0) {
				//the new marking should not come before this marking; continue
				currentIndex += headerLength + currentMarkingLength;
			} else if (compare == 0) {
				//the marking is equal and was already present
				return true;
			} else {
				//we are past the point where this marking should occur
				return false;
			}
		}
		return false;
	}

	private static int compare(short[] markingSet, short[] markingA, int headerIndexB) {
		int lengthB = getMarkingLength(markingSet, headerIndexB);
		if (markingA.length < lengthB) {
			return 1;
		} else if (markingA.length > lengthB) {
			return -1;
		}

		int indexA = 0;
		for (int indexB = headerIndexB + headerLength; indexB < headerIndexB + headerLength + lengthB; indexB++) {
			if (markingA[indexA] < markingSet[indexB]) {
				return 1;
			} else if (markingA[indexA] > markingSet[indexB]) {
				return -1;
			}
			indexA++;
		}

		return 0;
	}

	private static short getMarkingLength(short[] markingSet, int headerIndex) {
		return markingSet[headerIndex];
	}

	private static void setHeader(short[] markingSet, int headerIndex, int length) {
		//first, the length of the marking
		markingSet[headerIndex] = (short) length;
	}

	/**
	 * 
	 * @param markingSet
	 * @param finalMarkings
	 * @return whether at least one of the markings in the markingSet is a final
	 *         marking.
	 */
	public static boolean containsFinalMarking(short[] markingSet, Iterable<short[]> finalMarkings) {
		for (short[] finalMarking : finalMarkings) {
			int currentIndex = 0;
			while (currentIndex < markingSet.length) {
				//read the header
				int currentMarkingLength = getMarkingLength(markingSet, currentIndex);
				int compare = compare(markingSet, finalMarking, currentIndex);
				if (compare < 0) {
					//the new marking should not come before this marking; continue
					currentIndex += headerLength + currentMarkingLength;
				} else if (compare == 0) {
					//the marking is equal and was already present
					return true;
				} else {
					break;
				}
			}
		}
		return false;
	}
}
