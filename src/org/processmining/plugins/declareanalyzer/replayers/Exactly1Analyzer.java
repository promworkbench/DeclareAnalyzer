package org.processmining.plugins.declareanalyzer.replayers;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.declareanalyzer.ConstraintConditions;
import org.processmining.plugins.declareanalyzer.data.DataSnapshotListener;
import org.processmining.plugins.declareanalyzer.data.MyEvaluator;




public class Exactly1Analyzer implements DeclareReplayer{


	HashMap<String, HashMap<String, Set<Integer>>> activations = new HashMap<String, HashMap<String, Set<Integer>>>();
	HashMap<String, HashMap<String, Set<Integer>>> fulfillments = new HashMap<String, HashMap<String, Set<Integer>>>();
	HashMap<String, HashMap<String, Set<Integer>>> violations = new HashMap<String, HashMap<String, Set<Integer>>>();
	List<List<String>> dispositions;

	//
		HashMap<String, String> mappingConstrParam = new HashMap<String, String>();
		//

	HashMap<String, String> conditionsAct = new HashMap<String, String>();
	HashMap<String, String> conditionsTarg = new HashMap<String, String>();
	HashMap<String, ConstraintConditions> cMap = new HashMap<String, ConstraintConditions>();
	public Exactly1Analyzer(List<List<String>> dispositions){
		this.dispositions = dispositions;
		for(List<String> item : dispositions){ 
			ConstraintConditions c = ConstraintConditions.build(item.get(2));
			mappingConstrParam.put(item.get(0)+item.get(2), item.get(0));
			cMap.put(item.get(0)+item.get(2), c);
			if(!c.containsActivationCondition()){
				conditionsAct.put(item.get(0)+item.get(2), "1");
			}else{
				conditionsAct.put(item.get(0)+item.get(2), c.getActivationCondition());
			}
			if(!c.containsConstraintCondition() && ! c.containsTimeCondition()){
				conditionsTarg.put(item.get(0)+item.get(2), "1");
			}else{
				conditionsTarg.put(item.get(0)+item.get(2), c.getConstraintCondition());
			}
			//
		}
	}

	public void process(int eventIndex, String event, XTrace trace, String traceId, DataSnapshotListener listener) {
		HashMap<String, Set<Integer>> activationsPerTrace;
		if(activations.containsKey(traceId)){
			activationsPerTrace = activations.get(traceId);
		}else{
			activationsPerTrace = new HashMap<String, Set<Integer>>();
			for(List<String> pair : dispositions){
				HashSet<Integer> set = new HashSet<Integer>();
				activationsPerTrace.put(pair.get(0)+pair.get(2), set);
			}	
		}

		HashMap<String, Set<Integer>> violationsPerTrace;
		if(violations.containsKey(traceId)){
			violationsPerTrace = violations.get(traceId);
		}else{
			violationsPerTrace = new HashMap<String, Set<Integer>>();
			for(List<String> pair : dispositions){
				HashSet<Integer> set = new HashSet<Integer>();
				violationsPerTrace.put(pair.get(0)+pair.get(2), set);
			}	
		}

		HashMap<String, Set<Integer>> fulfillmentsPerTrace;
		if(fulfillments.containsKey(traceId)){
			fulfillmentsPerTrace = fulfillments.get(traceId);
		}else{
			fulfillmentsPerTrace = new HashMap<String, Set<Integer>>();
			for(List<String> pair : dispositions){
				HashSet<Integer> set = new HashSet<Integer>();
				fulfillmentsPerTrace.put(pair.get(0)+pair.get(2), set);
			}	
		}
	

		for(String param : activationsPerTrace.keySet()){
			boolean isActivation = true;
			//Pair<String,String> params = new Pair<String, String>(event, param2);
			String conditionActivation = conditionsAct.get(param);		
			if(!conditionActivation.equals("1")){
				isActivation = false;
				List<Pair<Integer, Map<String, String>>> snapshotsActivation = listener.getInstances().get(event).get(traceId);
				Map<String, String> snapshotActivation = null;
				Map<String,String> datavalues = new HashMap<String, String>();
				for(Pair pair : snapshotsActivation){
					if(((Integer)pair.getFirst()).intValue()==eventIndex){
						snapshotActivation = (Map<String, String>)pair.getSecond();
						for(String attribute : snapshotActivation.keySet()){
							datavalues.put("A."+attribute, snapshotActivation.get(attribute));
							//datavalues.put(attribute, snapshotActivation.get(attribute)); // TODO: help (mega search and replace :O)
						}
						break;
					}
				}
				try{
					Format timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					Date date1 = null;
					Date date2 = null;
					try {
						date1 = (Date) timestampFormat.parseObject(trace.get(0).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
						date2 = (Date) timestampFormat.parseObject(trace.get(eventIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
					} catch (ParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					long milliseconds = 0;
					long m1 = date1.getTime();
					long m2 = date2.getTime();
					if(m1>m2){
						milliseconds = m1-m2;
					}else{
						milliseconds = m2-m1;
					}
					isActivation =
							MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), conditionActivation) &&
							MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), cMap.get(param).getTimeCondition(milliseconds));
				}catch(Exception e){
					e.printStackTrace();
				}

			}

			if(event.equals(mappingConstrParam.get(param)) && isActivation){
				Set<Integer> acts = activationsPerTrace.get(param);
				acts.add(eventIndex);
				activationsPerTrace.put(param, acts);
				if(acts.size()!=1){
					fulfillmentsPerTrace.put(param, new HashSet<Integer>());
					Set<Integer> viols = violationsPerTrace.get(param);
					viols.addAll(acts);
					violationsPerTrace.put(param, viols);
				}else{
					violationsPerTrace.put(param, new HashSet<Integer>());
					Set<Integer> fulfills = fulfillmentsPerTrace.get(param);
					fulfills.addAll(acts);
					fulfillmentsPerTrace.put(param, fulfills);
				}
			}
		}
		activations.put(traceId, activationsPerTrace);
		fulfillments.put(traceId, fulfillmentsPerTrace);
		violations.put(traceId, violationsPerTrace);
	}


	//
		public Set<Integer> getFulfillments(String traceId, String param) {
			Set<Integer> output = new HashSet<Integer>();
				if(fulfillments.containsKey(traceId) && fulfillments.get(traceId).containsKey(param)){
					output.addAll(fulfillments.get(traceId).get(param));
				}
			return output;
		}
		//
		
		//
		public Set<Integer> getViolations(String traceId, String param) {
			Set<Integer> output = new HashSet<Integer>();
				if(violations.containsKey(traceId) && violations.get(traceId).containsKey(param)){
					output.addAll(violations.get(traceId).get(param));
				}
			return output;
		}
		//
		
		//
		public Set<Integer> getActivations(String traceId, String param) {
			Set<Integer> output = new HashSet<Integer>();
				if(activations.containsKey(traceId) && activations.get(traceId).containsKey(param)){
					output.addAll(activations.get(traceId).get(param));
				}
			return output;
		}
		//

		//
		public Set<Integer> getBExactly1Fulfillments(String traceId, String param1, String param2) {
			Set<Integer> output = new HashSet<Integer>();
			//int numberOfActivations = 0;
			if(activations.containsKey(traceId) && activations.get(traceId).containsKey(param1)){
				output.addAll(activations.get(traceId).get(param1));
			}
			if(activations.containsKey(traceId) && activations.get(traceId).containsKey(param2)){
				output.addAll(activations.get(traceId).get(param2));
			}
			if(output.size() == 1){
				return output;
			}else{
				return new HashSet<Integer>();
			}
		}
		//
		
		//
			public Set<Integer> getBExactly1Violations(String traceId, String param1, String param2) {
				Set<Integer> output = new HashSet<Integer>();
				//int numberOfActivations = 0;
				if(activations.containsKey(traceId) && activations.get(traceId).containsKey(param1)){
					output.addAll(activations.get(traceId).get(param1));
				}
				if(activations.containsKey(traceId) && activations.get(traceId).containsKey(param2)){
					output.addAll(activations.get(traceId).get(param2));
				}	
				if(output.size() != 1){
					return output;
				}else{
					return new HashSet<Integer>();
				}
			}
			//


}
