package org.processmining.stochasticawareconformancechecking.helperclasses;

public class StatePair {

	public static long pack(int a, int b) {
		return (((long) a) << 32) | (b & 0xffffffffL);
	}

	public static int unpackA(long l) {
		return (int) (l >> 32);
	}

	public static int unpackB(long l) {
		return (int) l;
	}
}
