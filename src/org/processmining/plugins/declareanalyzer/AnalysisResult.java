package org.processmining.plugins.declareanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.annotations.AuthoredType;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

/**
 * Result type of the Declare Analyzer
 * 
 * @author Andrea Burattin
 * @author Fabrizio Maggi
 */
@AuthoredType(
	typeName = "Declare Analysis Result",
	author = "A. Burattin, F.M. Maggi",
	email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
	affiliation = UITopiaVariant.EHV
)
public class AnalysisResult {
	
	/* FIXME: it doesn't seem that a ConstraintDefinition can be used as the key
	 *        for a map. In the meantime, let's use a string (getCaption method)
	 */
	/* counters with values summed up per constraint */
	private Set<String> constraints = new HashSet<String>();
	private Map<String, Integer> constraintActivations = new HashMap<String, Integer>();
	private Map<String, Integer> constraintViolations = new HashMap<String, Integer>();
	private Map<String, Integer> constraintFulfilments = new HashMap<String, Integer>();
	private Map<String, Integer> constraintConflicts = new HashMap<String, Integer>();
	
	private Map<String, Double> totActSparsity = new HashMap<String, Double>();
	private Map<String, Double> totFulfilRatio = new HashMap<String, Double>();
	private Map<String, Double> totViolationRatio = new HashMap<String, Double>();
	private Map<String, Double> totConflictRatio = new HashMap<String, Double>();
	
	/* counters detailed per trace */
	private Map<XTrace, Set<AnalysisSingleResult>> detailedResults = new HashMap<XTrace, Set<AnalysisSingleResult>>();
	
	/**
	 * 
	 * @param constraint
	 * @param trace
	 * @param activations
	 * @param violations
	 * @param fulfilments
	 */
	public void addResult(
			XTrace trace,
			ConstraintDefinition constraint,
			Set<Integer> activations,
			Set<Integer> violations,
			Set<Integer> fulfilments) {
		ArrayList<List<Integer>> listResolutions = new ArrayList<List<Integer>>();
		addResult(new AnalysisSingleResult(constraint, trace, activations, violations, fulfilments, listResolutions));
	}
	
	/**
	 * 
	 * @param analysis
	 */
	public void addResult(AnalysisSingleResult analysis) {
		
		String constraintName = analysis.getConstraint().getCaption();
		XTrace trace = analysis.getTrace();
		
		/* update detailed results */
		Set<AnalysisSingleResult> constraintsDetails = detailedResults.get(trace);
		if (constraintsDetails == null) {
			constraintsDetails = new HashSet<AnalysisSingleResult>();
		}
		constraintsDetails.add(analysis);
		detailedResults.put(trace, constraintsDetails);
		
		/* update set of constraints */
		if (!constraints.contains(constraintName)) {
			constraints.add(constraintName);
		}
		
		/* update values for activations */
		Integer val = constraintActivations.get(constraintName);
		if (val == null) {
			val = 0;
		}
		val += analysis.getActivations().size();
		constraintActivations.put(constraintName, val);
		
		/* update values for violations */
		val = constraintViolations.get(constraintName);
		if (val == null) {
			val = 0;
		}
		val += analysis.getViolations().size();
		constraintViolations.put(constraintName, val);
		
		/* update values for fulfillments */
		val = constraintFulfilments.get(constraintName);
		if (val == null) {
			val = 0;
		}
		val += analysis.getFulfilments().size();
		constraintFulfilments.put(constraintName, val);
		
		/* update values for conflicts */
		val = constraintConflicts.get(constraintName);
		if (val == null) {
			val = 0;
		}
		val += analysis.getConflicts().size();
		constraintConflicts.put(constraintName, val);
		
		/* update statistics */
		Double v = totActSparsity.get(constraintName); if (v == null) v = 0.0;
		v += 1.0 - ((double)analysis.getActivations().size() / (double)trace.size());
		totActSparsity.put(constraintName, v);
		
		v = totFulfilRatio.get(constraintName); if (v == null) v = 0.0;
		v += ((double)constraintFulfilments.get(constraintName) / (double)constraintActivations.get(constraintName));
		totFulfilRatio.put(constraintName, v);
		
		v = totViolationRatio.get(constraintName); if (v == null) v = 0.0;
		v += ((double)constraintViolations.get(constraintName) / (double)constraintActivations.get(constraintName));
		totViolationRatio.put(constraintName, v);
		
		v = totConflictRatio.get(constraintName); if (v == null) v = 0.0;
		v += ((double)constraintConflicts.get(constraintName) / (double)constraintActivations.get(constraintName));
		totConflictRatio.put(constraintName, v);
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<String> getConstraints() {
		return constraints;
	}
	
	/**
	 * 
	 * @param constraint
	 * @return
	 */
	public Integer getActivations(String constraint) {
		return constraintActivations.get(constraint);
	}
	
	/**
	 * 
	 * @param constraint
	 * @return
	 */
	public Integer getViolations(String constraint) {
		return constraintViolations.get(constraint);
	}
	
	/**
	 * 
	 * @param constraint
	 * @return
	 */
	public Integer getFulfilments(String constraint) {
		return constraintFulfilments.get(constraint);
	}
	
	/**
	 * 
	 * @param constraint
	 * @return
	 */
	public Integer getConflicts(String constraint) {
		return constraintConflicts.get(constraint);
	}
	
	public Double getAvgActivationSparsity(String constraint) {
		Double v = totActSparsity.get(constraint);
		if (v.isNaN()) {
			return 0.0;
		}
		return v / detailedResults.keySet().size();
	}
	
	public Double getAvgFulfilmentRatio(String constraint) {
		Double v = totFulfilRatio.get(constraint);;
		if (v.isNaN()) {
			return 0.0;
		}
		return v / detailedResults.keySet().size();
	}
	
	public Double getAvgViolationRatio(String constraint) {
		Double v = totViolationRatio.get(constraint);
		if (v.isNaN()) {
			return 0.0;
		}
		return v / detailedResults.keySet().size();
	}
	
	public Double getAvgConflictRatio(String constraint) {
		Double v = totConflictRatio.get(constraint);
		if (v.isNaN()) {
			return 0.0;
		}
		return v / detailedResults.keySet().size();
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<XTrace> getTraces() {
		return detailedResults.keySet();
	}
	
	/**
	 * 
	 * @param trace
	 * @return
	 */
	public Set<AnalysisSingleResult> getResults(XTrace trace) {
		return detailedResults.get(trace);
	}
}
