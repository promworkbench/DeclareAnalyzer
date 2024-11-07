package utils;

import java.util.ArrayList;
import java.util.Vector;

public class DispositionsGenerator {

	public static String[][] generateDisp(String[] stringhe, int k) {

		

		ArrayList<ArrayList<NumberSet>> sets = getSets(stringhe.length, k);

		int count = 0;
		for (ArrayList<NumberSet> buffer : sets) {
			for (NumberSet num : buffer) {
				count++;
			}
		}
		String[][] output = new String[count][k];
		int l = 0;
		Vector vec = new Vector();
		int s = 0;
		l = 0;
		for (ArrayList<NumberSet> buffer : sets) {
			for (NumberSet num : buffer) {
				s = 0;
				for (Integer i : num.numbers) {
					output[l][s] = stringhe[i.intValue()];
					s++;
				}
				vec.add(output[l]);
				l++;
			}
		}

		// Uncomment if you want dispositions without repetitions:
		for (int m = 0; m < count; m++) {
			for (int n = 0; n < k; n++) {
				String temp = output[m][n];
				for (int z = n + 1; z < k; z++) {
					if (temp.equals(output[m][z])) {
						vec.remove(output[m]);
					}
				}
			}
		}

		String[][] newRes = new String[vec.size()][k];

		for (int m = 0; m < newRes.length; m++) {
			newRes[m] = (String[]) vec.elementAt(m);
		}
		return newRes;
	}

	public static ArrayList<ArrayList<NumberSet>> getSets(int n, int layer) {
		if (layer == 1) {
			ArrayList<ArrayList<NumberSet>> sets = new ArrayList<ArrayList<NumberSet>>(n);
			for (int i = 0; i < n; i++) {
				NumberSet simpleSet = new NumberSet(i);
				ArrayList<NumberSet> buffer = new ArrayList<NumberSet>();
				buffer.add(simpleSet);
				sets.add(buffer);
			}
			return sets;
		} else {
			ArrayList<ArrayList<NumberSet>> smallerSets = getSets(n, layer - 1);
			ArrayList<ArrayList<NumberSet>> newSet = new ArrayList<ArrayList<NumberSet>>();
			for (int i = 0; i < n; i++) {
				ArrayList<NumberSet> newList = new ArrayList<NumberSet>();
				newSet.add(newList);
			}
			for (int i = 0; i < n; i++) {
				ArrayList<NumberSet> buffer = smallerSets.get(i);
				for (int j = 0; j < buffer.size(); j++) {
					for (int k = 0; k < n; k++) {
						NumberSet newNumSet = new NumberSet(buffer.get(j).numbers, k);
						newSet.get(k).add(newNumSet);
					}
				}
			}
			return newSet;
		}
	}
}
