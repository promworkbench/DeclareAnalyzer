package org.processmining.plugins.declareanalyzer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

/**
 * 
 * @author Andrea Burattin
 * @author Fabrizio Maggi
 */
public class AnalysisSingleResult implements Comparable<AnalysisSingleResult> {

	private final ConstraintDefinition constraint;
	private final XTrace trace;
	private Set<Integer> activations = new HashSet<Integer>();
	private Set<Integer> violations = new HashSet<Integer>();
	private Set<Integer> fulfilments = new HashSet<Integer>();
	private Set<Integer> conflicts = null;
	private ArrayList<List<Integer>> resolutions;

	/**
	 * 
	 * @param constraint
	 * @param trace
	 * @param activations
	 * @param violations
	 * @param fulfilments
	 */
	public AnalysisSingleResult(ConstraintDefinition constraint, XTrace trace, Set<Integer> activations,
			Set<Integer> violations, Set<Integer> fulfilments,
			ArrayList<List<Integer>> resolutions) {
		this.constraint = constraint;
		this.trace = trace;
		this.activations = activations;
		this.violations = violations;
		this.fulfilments = fulfilments;
		this.resolutions = resolutions;
	}

	/**
	 * 
	 * @return the constraint
	 */
	public ConstraintDefinition getConstraint() {
		return constraint;
	}

	/**
	 * 
	 * @return the trace
	 */
	public XTrace getTrace() {
		return trace;
	}
	
	public ConstraintDefinition getConstraintDefinition() {
		return constraint;
	}

	/**
	 * 
	 * @return the activations
	 */
	public Set<Integer> getActivations() {
		return activations;
	}

	/**
	 * 
	 * @return the violations
	 */
	public Set<Integer> getViolations() {
		return violations;
	}

	/**
	 * 
	 * @return the fulfillments
	 */
	public Set<Integer> getFulfilments() {
		return fulfilments;
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<Integer> getConflicts() {
		if (conflicts == null) {
			conflicts = new HashSet<Integer>();
			
			for (Integer a : activations) {
				int observations = 0;
				for (int j = 0; j < resolutions.size(); j++) {
					List<Integer> et = resolutions.get(j);
					if (et.contains(a)) {
						observations++;
					}
				}
				if (observations != 0 && observations != resolutions.size()) {
					conflicts.add(a);
				}
			}
		}
		return conflicts;
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<List<Integer>> getResolutions() {
		return resolutions;
	}

	@Override
	public int compareTo(AnalysisSingleResult o) {
		return constraint.getCaption().compareTo(o.getConstraint().getCaption());
	}
}
