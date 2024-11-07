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

import utils.XLogHelper;


public class ChainPrecedenceAnalyzer implements  DeclareReplayer{

	HashMap<String, String> conditionsAct = new HashMap<String, String>();
	HashMap<String, String> conditionsTarg = new HashMap<String, String>();
	HashMap<String, ConstraintConditions> cMap = new HashMap<String, ConstraintConditions>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> fulfillments = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> violations = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> targets = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	List<List<String>> dispositions;
	//
	HashMap<String, String> mappingConstrParam = new HashMap<String, String>();
	//
	public ChainPrecedenceAnalyzer(List<List<String>> dispositions){
		this.dispositions = dispositions;
		for(List<String> item : dispositions){
			Pair<String,String> pair = new Pair<String, String>(item.get(0), item.get(1)); 
			ConstraintConditions c = ConstraintConditions.build(item.get(3));
			mappingConstrParam.put(pair.getFirst()+item.get(3),pair.getFirst());
			cMap.put(pair.getFirst()+item.get(3)+pair.getSecond(), c);
			if(!c.containsActivationCondition()){
				conditionsAct.put(pair.getFirst()+item.get(3)+pair.getSecond(), "1");
			}else{
				conditionsAct.put(pair.getFirst()+item.get(3)+pair.getSecond(), c.getActivationCondition());
			}
			if(!c.containsConstraintCondition() && ! c.containsTimeCondition()){
				conditionsTarg.put(pair.getFirst()+item.get(3)+pair.getSecond(), "1");
			}else{
				conditionsTarg.put(pair.getFirst()+item.get(3)+pair.getSecond(), c.getConstraintCondition());
			}
		}
	}

	public void process(int eventIndex, String event, XTrace trace, String traceId, DataSnapshotListener listener) {

		HashMap<String, HashMap<String, Set<Integer>>> targetsPerTrace;
		if(targets.containsKey(traceId)){
			targetsPerTrace = targets.get(traceId);
		}else{
			targetsPerTrace = new HashMap<String, HashMap<String,Set<Integer>>>();
			for(List<String> pair : dispositions){
				HashMap<String, Set<Integer>> target;
				if(targetsPerTrace.containsKey(pair.get(0)+pair.get(3))){
					target = targetsPerTrace.get(pair.get(0)+pair.get(3));
				}else{
					target = new HashMap<String, Set<Integer>>();
				}
				target.put(pair.get(1), new HashSet<Integer>());
				targetsPerTrace.put(pair.get(0)+pair.get(3),target);
			}	
		}


		HashMap<String, HashMap<String, Set<Integer>>> violationsPerTrace;
		if(violations.containsKey(traceId)){
			violationsPerTrace = violations.get(traceId);
		}else{
			violationsPerTrace = new HashMap<String, HashMap<String,Set<Integer>>>();
			for(List<String> pair : dispositions){
				HashMap<String, Set<Integer>> viol;
				if(violationsPerTrace.containsKey(pair.get(0)+pair.get(3))){
					viol = violationsPerTrace.get(pair.get(0)+pair.get(3));
				}else{
					viol = new HashMap<String, Set<Integer>>();
				}
				viol.put(pair.get(1), new HashSet<Integer>());
				violationsPerTrace.put(pair.get(0)+pair.get(3),viol);
			}	
		}

		HashMap<String, HashMap<String, Set<Integer>>> fulfillmentPerTrace;
		if(fulfillments.containsKey(traceId)){
			fulfillmentPerTrace = fulfillments.get(traceId);
		}else{
			fulfillmentPerTrace = new HashMap<String, HashMap<String,Set<Integer>>>();
			for(List<String> pair : dispositions){
				HashMap<String, Set<Integer>> fulfill;
				if(fulfillmentPerTrace.containsKey(pair.get(0)+pair.get(3))){
					fulfill = fulfillmentPerTrace.get(pair.get(0)+pair.get(3));
				}else{
					fulfill = new HashMap<String, Set<Integer>>();
				}
				fulfill.put(pair.get(1), new HashSet<Integer>());
				fulfillmentPerTrace.put(pair.get(0)+pair.get(3),fulfill);
			}	
		}

		for(String param1 : targetsPerTrace.keySet()){
			HashMap<String, Set<Integer>> target = targetsPerTrace.get(param1);
			for(String param2 : target.keySet()){	
				HashMap<String, Set<Integer>> fulfill = fulfillmentPerTrace.get(param1);
				Set<Integer> indexesTarg = target.get(param2);
				Set<Integer> indexesFulfill = fulfill.get(param2);
				HashMap<String, Set<Integer>> viol = violationsPerTrace.get(param1);
				Set<Integer> indexesViol = viol.get(param2);
				Pair<String,String> params = new Pair<String, String>(param1, param2);
				if(param2.equals(event)){
					String conditionActivation = conditionsAct.get(params.getFirst()+params.getSecond());		
					if(conditionActivation.equals("1")){
						String conditionTarget = conditionsTarg.get(params.getFirst()+params.getSecond());
						if(conditionTarget.equals("1")){
							if(target.get(param2).size()==1){
								if(param2.equals(event)){
									indexesFulfill.add(eventIndex);
									fulfill.put(param2, indexesFulfill);
									fulfillmentPerTrace.put(param1, fulfill);
								}
							}else{
								if(param2.equals(event)){
									indexesViol.add(eventIndex);
									viol.put(param2, indexesViol);
									violationsPerTrace.put(param1, viol);
								}
							}
						}else{
							Map<String,String> datavalues = new HashMap<String, String>();
							Set<Integer> targetsList = targetsPerTrace.get(param1).get(event);
							Integer targetIndex = targetsList.iterator().next();
							Format timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							Date date1 = null;
							Date date2 = null;
							try {
								date1 = (Date) timestampFormat.parseObject(trace.get(targetIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
								date2 = (Date) timestampFormat.parseObject(trace.get(eventIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
							} catch (ParseException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							String activation = XLogHelper.getCompleteName(trace.get(eventIndex).getAttributes());
							List<Pair<Integer, Map<String, String>>> snapshotsActivation = listener.getInstances().get(activation).get(traceId);
							Map<String, String> snapshotActivation = null;
							//Map<String,String> datavalues = new HashMap<String, String>();
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
							boolean targetFound = false;
							String targ = XLogHelper.getCompleteName(trace.get(targetIndex).getAttributes());
							List<Pair<Integer, Map<String, String>>> snapshotsTarget = listener.getInstances().get(targ).get(traceId);
							Map<String, String> snapshotTarget = null;
							for(Pair pair : snapshotsTarget){
								if(((Integer)pair.getFirst()).intValue()==targetIndex){
									snapshotTarget = (Map<String, String>)pair.getSecond();
									for(String attribute : snapshotTarget.keySet()){
										datavalues.put("T."+attribute, snapshotTarget.get(attribute));
										//	datavalues.put(attribute, snapshotTarget.get(attribute));
									}
									break;
								}
							}
							try{
								long milliseconds = 0;
								long m1 = date1.getTime();
								long m2 = date2.getTime();
								if(m1>m2){
									milliseconds = m1-m2;
								}else{
									milliseconds = m2-m1;
								}
								targetFound =
										MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), conditionTarget) &&
										MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), cMap.get(params.getFirst()+params.getSecond()).getTimeCondition(milliseconds));

							}catch(Exception e){
								e.printStackTrace();
							}	
							if(!targetFound){
								//Set<Integer> indexesViol = viol.get(param2);
								indexesViol.add(eventIndex);
								viol.put(event, indexesViol);
								violationsPerTrace.put(param1, viol);
							}else{
								//Set<Integer> indexesFulfill = fulfill.get(event);
								indexesFulfill.add(eventIndex);
								fulfill.put(event, indexesFulfill);
								fulfillmentPerTrace.put(param1, fulfill);
							}

						}
					}else{
						boolean isActivation = false;
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
							isActivation = MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), conditionActivation);
						}catch(Exception e){
							e.printStackTrace();
						}
						if(isActivation){
							boolean targetFound = false;
							String conditionTarget = conditionsTarg.get(params.getFirst()+params.getSecond());
							Set<Integer> targetsList = targetsPerTrace.get(param1).get(event);
							if(conditionTarget.equals("1")&& targetsList.size()>0){	
								targetFound = true;
							}else{
								//Set<Integer> indexesTar = pending.get(event);
								if(targetsList.size()>0){
									Integer targetIndex = targetsList.iterator().next();

									Format timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
									Date date1 = null;
									Date date2 = null;
									try {
										date1 = (Date) timestampFormat.parseObject(trace.get(targetIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
										date2 = (Date) timestampFormat.parseObject(trace.get(eventIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
									} catch (ParseException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}


									String targ = XLogHelper.getCompleteName(trace.get(targetIndex).getAttributes());
									List<Pair<Integer, Map<String, String>>> snapshotsTarget = listener.getInstances().get(targ).get(traceId);
									Map<String, String> snapshotTarget = null;
									for(Pair pair : snapshotsTarget){
										if(((Integer)pair.getFirst()).intValue()==targetIndex){
											snapshotTarget = (Map<String, String>)pair.getSecond();
											for(String attribute : snapshotTarget.keySet()){
												datavalues.put("T."+attribute, snapshotTarget.get(attribute));
												//	datavalues.put(attribute, snapshotTarget.get(attribute));
											}
											break;
										}
									}
									try{
										long milliseconds = 0;
										long m1 = date1.getTime();
										long m2 = date2.getTime();
										if(m1>m2){
											milliseconds = m1-m2;
										}else{
											milliseconds = m2-m1;
										}
										targetFound =
												MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), conditionTarget) &&
												MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), cMap.get(params.getFirst()+params.getSecond()).getTimeCondition(milliseconds));
										if(targetFound){
											break;
										}
									}catch(Exception e){
										e.printStackTrace();
									}	
								}
							}
							if(!targetFound){
								//Set<Integer> indexesViol = viol.get(param2);
								indexesViol.add(eventIndex);
								viol.put(event, indexesViol);
								violationsPerTrace.put(param1, viol);
							}else{
								//Set<Integer> indexesFulfill = fulfill.get(event);
								indexesFulfill.add(eventIndex);
								fulfill.put(event, indexesFulfill);
								fulfillmentPerTrace.put(param1, fulfill);
							}
						}
					}
				}
				indexesTarg.clear();
				target.put(param2, indexesTarg);
				targetsPerTrace.put(param1, target);
			}
		}
		for(String key : targetsPerTrace.keySet()){
			if(mappingConstrParam.get(key).equals(event)){
				//if(targetsPerTrace.containsKey(event)){
				HashMap<String, Set<Integer>> target = targetsPerTrace.get(key);
				for(String param2 : target.keySet()){
					Set<Integer> indexesForTarg = target.get(param2);
					indexesForTarg.add(eventIndex);
					target.put(param2, indexesForTarg);
				}
			}
		}
		fulfillments.put(traceId, fulfillmentPerTrace);
		violations.put(traceId, violationsPerTrace);
		targets.put(traceId, targetsPerTrace);
	}

	public Set<Integer> getFulfillments(String traceId, String param1, String param2) {
		return fulfillments.get(traceId).get(param1).get(param2);
	}


	public Set<Integer> getViolations(String traceId, String param1, String param2) {
		return violations.get(traceId).get(param1).get(param2);
	}
	

	public Set<Integer> getSBFulfillments(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(fulfillments.get(traceId).get(param1).get(param2));
		output.addAll(fulfillments.get(traceId).get(param1).get(param3));
		return output;
	}


	public Set<Integer> getSBViolations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(violations.get(traceId).get(param1).get(param2));
		output.addAll(violations.get(traceId).get(param1).get(param3));
		return output;
	}
	
	
	public Set<Integer> getTBFulfillments(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		for(Integer i : violations.get(traceId).get(param1).get(param3)){
			if(fulfillments.get(traceId).get(param2).get(param3).contains(i)){
				output.add(i);
			}
		}
		for(Integer i : fulfillments.get(traceId).get(param1).get(param3)){
				output.add(i);
		}
		return output;
	}


	public Set<Integer> getTBViolations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		for(Integer i : violations.get(traceId).get(param1).get(param3)){
			if(violations.get(traceId).get(param2).get(param3).contains(i)){
				output.add(i);
			}
		}
		return output;
	}
}
