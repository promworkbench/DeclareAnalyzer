package utils;

import java.util.ArrayList;

public class NumberSet {

	public ArrayList<Integer> numbers = new ArrayList<Integer>();

	public NumberSet(int i) {
		numbers.add(i);
	}

	public NumberSet(ArrayList<Integer> num, int newInt) {
		numbers.addAll(num);
		numbers.add(newInt);
	}

}
