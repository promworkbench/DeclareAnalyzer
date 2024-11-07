package utils;

import java.util.LinkedList;

/**
 * A trace element that can be extended with new elements
 * 
 * @author Andrea Burattin
 * @author Fabrizio Maggi
 */
public class ExtendibleTrace implements Comparable<ExtendibleTrace> {

	private LinkedList<Integer> trace;
	private LinkedList<Integer> activations;
	private boolean isViolation = false;
	
	/**
	 * Creates a new empty extendible trace
	 */
	public ExtendibleTrace() {
		this.trace = new LinkedList<Integer>();
		this.activations = new LinkedList<Integer>();
	}
	
	/**
	 * Creates a new extendible trace with the given element
	 * 
	 * @param event
	 */
	public ExtendibleTrace(Integer event) {
		this();
		this.trace.add(event);
	}
	
	/**
	 * Creates a new extendible trace as a deep copy of the given one
	 * 
	 * @param trace
	 */
	@SuppressWarnings("unchecked")
	public ExtendibleTrace(ExtendibleTrace trace) {
		this.trace = (LinkedList<Integer>) trace.trace.clone();
		this.activations = (LinkedList<Integer>) trace.activations.clone();
	}
	
	/**
	 * Append the given element to the current string
	 * 
	 * @param a
	 * @return
	 */
	public ExtendibleTrace appendToNew(Integer a, boolean activation) {
		ExtendibleTrace at = new ExtendibleTrace(this);
		at.trace.add(a);
		if (activation) {
			at.activations.add(a);
		}
		return at;
	}

	/**
	 * 
	 * @return
	 */
	public LinkedList<Integer> getTrace() {
		return trace;
	}
	
	/**
	 * 
	 * @return
	 */
	public LinkedList<Integer> getActivations() {
		return activations;
	}
	
	@Override
	public String toString() {
		String complete = "";
		for(Integer s : trace) {
			complete += "[" + s + "]" + ";";
		}
		return complete;
	}

	@Override
	public int compareTo(ExtendibleTrace o) {
		LinkedList<Integer> trace = o.trace;
		
		if (this.trace.size() < trace.size()) {
			return -1;
		} else if (this.trace.size() > trace.size()) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * @return the isViolation
	 */
	public boolean isViolation() {
		return isViolation;
	}

	/**
	 * @param isViolation the isViolation to set
	 */
	public void setViolation(boolean isViolation) {
		this.isViolation = isViolation;
	}
	
	/**
	 * Method to check if the current trace is contained into the other one
	 * 
	 * @param larger
	 * @return
	 */
	public boolean isContained(ExtendibleTrace larger) {
		return larger.getTrace().containsAll(getTrace());
	}
}
