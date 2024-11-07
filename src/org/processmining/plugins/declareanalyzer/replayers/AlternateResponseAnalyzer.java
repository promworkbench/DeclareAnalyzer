package org.processmining.plugins.declareanalyzer.replayers;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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


public class AlternateResponseAnalyzer implements  DeclareReplayer{

	HashMap<String, String> conditionsAct = new HashMap<String, String>();
	HashMap<String, String> conditionsTarg = new HashMap<String, String>();
	HashMap<String, ConstraintConditions> cMap = new HashMap<String, ConstraintConditions>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> fulfillments = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> violations = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> pendingActivations = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> targets = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	List<List<String>> dispositions;

	//
	HashMap<String, String> mappingConstrParam = new HashMap<String, String>();
	//

	public AlternateResponseAnalyzer(List<List<String>> dispositions){
		this.dispositions = dispositions;
		for(List<String> item : dispositions){
			Pair<String,String> pair = new Pair<String, String>(item.get(0), item.get(1)); 
			ConstraintConditions c = ConstraintConditions.build(item.get(3));
			//
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
			//
		}
	}

	public void process(int eventIndex, String event, XTrace trace, String traceId, DataSnapshotListener listener) {
		HashMap<String, HashMap<String, Set<Integer>>> pendingPerTrace;
		if(pendingActivations.containsKey(traceId)){
			pendingPerTrace = pendingActivations.get(traceId);
		}else{
			pendingPerTrace = new HashMap<String, HashMap<String,Set<Integer>>>();
			for(List<String> pair : dispositions){
				HashMap<String, Set<Integer>> pend;
				//
				if(pendingPerTrace.containsKey(pair.get(0)+pair.get(3))){
					pend = pendingPerTrace.get(pair.get(0)+pair.get(3));
				}else{
					pend = new HashMap<String, Set<Integer>>();
				}
				pend.put(pair.get(1), new HashSet<Integer>());
				pendingPerTrace.put(pair.get(0)+pair.get(3),pend);
				//
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


		for(String key : pendingPerTrace.keySet()){
			if(mappingConstrParam.get(key).equals(event)){
				HashMap<String, Set<Integer>> pend = pendingPerTrace.get(key);
				for(String param2 : pend.keySet()){

					if(targetsPerTrace.get(key).get(param2).size() >=1  && pendingPerTrace.get(key).get(param2).size()==1){
						Pair<String,String> params = new Pair<String, String>(key, param2);
						String conditionTarget = conditionsTarg.get(params);
						if(conditionTarget.equals("1")){
							HashMap<String, Set<Integer>> fulfill = fulfillmentPerTrace.get(key);
							Set<Integer> indexesFulfill = fulfill.get(param2);
							indexesFulfill.addAll(pendingPerTrace.get(key).get(param2));
							fulfill.put(param2, indexesFulfill);
							fulfillmentPerTrace.put(key, fulfill);
							pend.put(param2, new HashSet<Integer>());
							pendingPerTrace.put(key, pend);
							targetsPerTrace.get(key).put(param2, new HashSet<Integer>());
						}else{

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
							for(Integer targetIndex : targetsPerTrace.get(event).get(param2)){
								boolean	targetFound = false;
								//String activation = (trace.get(p).getAttributes().get(XConceptExtension.KEY_NAME)+"-"+trace.get(p).getAttributes().get(XLifecycleExtension.KEY_TRANSITION)).toLowerCase();
								String targ = XLogHelper.getCompleteName(trace.get(targetIndex).getAttributes());
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

								List<Pair<Integer, Map<String, String>>> snapshotsTarget = listener.getInstances().get(targ).get(traceId);
								Map<String, String> snapshotTarget = null;
								//Map<String,String> datavalues = new HashMap<String, String>();
								for(Pair pair : snapshotsTarget){
									if(((Integer)pair.getFirst()).intValue()==eventIndex){
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
								if(targetFound){
									HashMap<String, Set<Integer>> fulfill = fulfillmentPerTrace.get(key);
									Set<Integer> indexesFulfill = fulfill.get(param2);
									indexesFulfill.addAll(pendingPerTrace.get(key).get(param2));
									fulfill.put(param2, indexesFulfill);
									fulfillmentPerTrace.put(key, fulfill);
									pend.put(param2, new HashSet<Integer>());
									pendingPerTrace.put(key, pend);
									targetsPerTrace.get(key).put(param2, new HashSet<Integer>());
								}else{
									HashMap<String, Set<Integer>> viol = violationsPerTrace.get(key);
									Set<Integer> indexesViol = viol.get(param2);
									indexesViol.addAll(pendingPerTrace.get(key).get(param2));
									viol.put(param2, indexesViol);
									violationsPerTrace.put(key, viol);
									pend.put(param2, new HashSet<Integer>());
									pendingPerTrace.put(key, pend);
								}
							}
						}
					}else if(targetsPerTrace.get(key).get(param2).size() ==0 && pendingPerTrace.get(key).get(param2).size()==1){
						boolean isActivation = true;
						Pair<String,String> params = new Pair<String, String>(key, param2);
						String conditionActivation = conditionsAct.get(params.getFirst()+params.getSecond());	
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
								isActivation = MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), conditionActivation);
							}catch(Exception e){
								e.printStackTrace();
							}

						}
						if(isActivation){
							HashMap<String, Set<Integer>> viol = violationsPerTrace.get(key);
							Set<Integer> indexesViol = viol.get(param2);
							indexesViol.addAll(pendingPerTrace.get(key).get(param2));
							viol.put(param2, indexesViol);
							violationsPerTrace.put(key, viol);
							pend.put(param2, new HashSet<Integer>());
							pendingPerTrace.put(key, pend);
						}					
					}
					boolean isActivation = true;
					Pair<String,String> params = new Pair<String, String>(key, param2);
					String conditionActivation = conditionsAct.get(params.getFirst()+params.getSecond());	
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
							isActivation = MyEvaluator.evaluateExpression(datavalues, listener.getDataTypes(), conditionActivation);
						}catch(Exception e){
							e.printStackTrace();
						}

					}
					if(isActivation){
						Set<Integer> indexesForPending = pend.get(param2);
						indexesForPending.add(eventIndex);
						pend.put(param2, indexesForPending);
					}
				}
			}
		}
		for(String param1 : targetsPerTrace.keySet()){
			HashMap<String, Set<Integer>> target = targetsPerTrace.get(param1);	
			if(target.containsKey(event)){
				Set<Integer> indexesTarg = target.get(event);
				indexesTarg.add(eventIndex);
				target.put(event, indexesTarg);
				targetsPerTrace.put(param1, target);			
			}
		}

		for(String param1 : fulfillmentPerTrace.keySet()){
			HashMap<String, Set<Integer>> fulfill = fulfillmentPerTrace.get(param1);	
			if(fulfill.containsKey(event)){
				if(targetsPerTrace.get(param1).get(event).size() >=1 && pendingPerTrace.get(param1).get(event).size()==1){			
					Pair<String,String> params = new Pair<String, String>(param1, event);
					String conditionTarget = conditionsTarg.get(params.getFirst()+params.getSecond());
					if(conditionTarget.equals("1")){
						Set<Integer> indexesFulfill = fulfill.get(event);
						indexesFulfill.addAll(pendingPerTrace.get(param1).get(event));
						fulfill.put(event, indexesFulfill);
						fulfillmentPerTrace.put(param1, fulfill);
						pendingPerTrace.get(param1).get(event).clear();	
					}else{
						List<Pair<Integer, Map<String, String>>> snapshotsActivation = listener.getInstances().get(param1.substring(0, param1.indexOf("["))).get(traceId);
						Map<String, String> snapshotActivation = null;
						Map<String,String> datavalues = new HashMap<String, String>();
						for(Pair pair : snapshotsActivation){
							if(((Integer)pair.getFirst()).intValue()==pendingPerTrace.get(param1).get(event).iterator().next()){
								snapshotActivation = (Map<String, String>)pair.getSecond();
								for(String attribute : snapshotActivation.keySet()){
									datavalues.put("A."+attribute, snapshotActivation.get(attribute));
									//datavalues.put(attribute, snapshotActivation.get(attribute)); // TODO: help (mega search and replace :O)
								}
								break;
							}
						}
						//for(Integer targetIndex : targetsPerTrace.get(param1).get(event)){
						boolean	targetFound = false;
						//String activation = (trace.get(p).getAttributes().get(XConceptExtension.KEY_NAME)+"-"+trace.get(p).getAttributes().get(XLifecycleExtension.KEY_TRANSITION)).toLowerCase();
						//	String targ = (trace.get(eventIndex).getAttributes().get(XConceptExtension.KEY_NAME)+"-"+trace.get(eventIndex).getAttributes().get(XLifecycleExtension.KEY_TRANSITION)).toLowerCase();
						Format timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						Date date1 = null;
						Date date2 = null;
						try {
							date1 = (Date) timestampFormat.parseObject(trace.get(pendingPerTrace.get(param1).get(event).iterator().next()).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
							date2 = (Date) timestampFormat.parseObject(trace.get(eventIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
						} catch (ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						List<Pair<Integer, Map<String, String>>> snapshotsTarget = listener.getInstances().get(event).get(traceId);
						Map<String, String> snapshotTarget = null;
						//Map<String,String> datavalues = new HashMap<String, String>();
						for(Pair pair : snapshotsTarget){
							if(((Integer)pair.getFirst()).intValue()==eventIndex){
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
						if(targetFound){
							Set<Integer> indexesFulfill = fulfill.get(event);
							indexesFulfill.addAll(pendingPerTrace.get(param1).get(event));
							fulfill.put(event, indexesFulfill);
							fulfillmentPerTrace.put(param1, fulfill);
							pendingPerTrace.get(param1).get(event).clear();
						}
					}
				}
			}
		}


		fulfillments.put(traceId, fulfillmentPerTrace);
		violations.put(traceId, violationsPerTrace);
		pendingActivations.put(traceId, pendingPerTrace);
	}

	public Set<Integer> getFulfillments(String traceId, String param1, String param2) {
		return fulfillments.get(traceId).get(param1).get(param2);
	}


	public Set<Integer> getViolations(String traceId, String param1, String param2) {
		return violations.get(traceId).get(param1).get(param2);
	}

	public Set<Integer> getPendingActivations(String traceId, String param1, String param2) {
		return pendingActivations.get(traceId).get(param1).get(param2);
	}

	public Set<Integer> getTBViolations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		for(Integer i : violations.get(traceId).get(param1).get(param2)){
			if(violations.get(traceId).get(param1).get(param3).contains(i)){
				output.add(i);
			}
		}
		return output;
	}


	public Set<Integer> getTBFulfillments(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(fulfillments.get(traceId).get(param1).get(param3));
		output.addAll(fulfillments.get(traceId).get(param1).get(param2));
		return output;
	}


	public Set<Integer> getTBPendingActivations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		for(Integer i : pendingActivations.get(traceId).get(param1).get(param2)){
			if(pendingActivations.get(traceId).get(param1).get(param3).contains(i)){
				output.add(i);
			}
		}
		return output;
	}

	public Set<Integer> getSBPendingActivations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(pendingActivations.get(traceId).get(param1).get(param3));
		output.addAll(pendingActivations.get(traceId).get(param2).get(param3));
		return output;
	}

	public Set<Integer> getSBFulfillments(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		Set<Integer> activParam1 = new HashSet<Integer>();
		Set<Integer> activParam2 = new HashSet<Integer>();
		Set<Integer> activParam3 = new HashSet<Integer>();

		for(Integer i : pendingActivations.get(traceId).get(param1).get(param2)){
			activParam1.add(i);
		}
		for(Integer i : violations.get(traceId).get(param1).get(param2)){
			activParam1.add(i);
		}
		for(Integer i : fulfillments.get(traceId).get(param1).get(param2)){
			activParam1.add(i);
		}

		for(Integer i : pendingActivations.get(traceId).get(param2).get(param3)){
			activParam2.add(i);
		}
		for(Integer i : violations.get(traceId).get(param2).get(param3)){
			activParam2.add(i);
		}
		for(Integer i : fulfillments.get(traceId).get(param2).get(param3)){
			activParam2.add(i);
		}

		for(Integer i : pendingActivations.get(traceId).get(param3).get(param1)){
			activParam3.add(i);
		}
		for(Integer i : violations.get(traceId).get(param3).get(param1)){
			activParam3.add(i);
		}
		for(Integer i : fulfillments.get(traceId).get(param3).get(param1)){
			activParam3.add(i);
		}

		ArrayList<String> verificationList = new ArrayList<String>();

		int prev = 0;
		for(Integer i : activParam1){
			for(int f = prev; f<i; f++){
				verificationList.add("x");
			}
			verificationList.add("a");
			prev = i+1;
		}
		prev = 0;
		for(Integer i : activParam2){
			for(int f = prev; f<i; f++){
				if(verificationList.size()>f && !verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"x");
				}
				if(verificationList.size()>f && verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"a");
				}
				if(verificationList.size()<=f)
				{				
					verificationList.add("x");
				}
			}
			if(verificationList.size()>i && !verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"a");
			}
			if(verificationList.size()>i && verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"a");
			}
			if(verificationList.size()<=i)
			{				
				verificationList.add("a");
			}
			prev = i+1;		
		}
		prev = 0;
		for(Integer i : activParam3){
			for(int f = prev; f<i; f++){
				if(verificationList.size()>f && !verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"x");
				}
				if(verificationList.size()>f && verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"a");
				}
				if(verificationList.size()<=f)
				{				
					verificationList.add("x");
				}
			}
			if(verificationList.size()>i && !verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"t");
			}
			if(verificationList.size()>i && verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"a");
			}
			if(verificationList.size()<=i)
			{				
				verificationList.add("t");
			}
			prev = i+1;		
		}		
		boolean alreadyActivated = false;
		int previousActivatingIndex  = -1;
		int counter =0;
		for(String element : verificationList){
			
			if(element!=null && element.equals("a")){
				previousActivatingIndex = counter;
				alreadyActivated = true;
			}
			
			if(element!=null && alreadyActivated && element.equals("t")){
				output.add(previousActivatingIndex);
			}
			counter ++;
		}		
		return output;
	}

	public Set<Integer> getSBViolations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(violations.get(traceId).get(param1).get(param3));
		output.addAll(violations.get(traceId).get(param2).get(param3));
		Set<Integer> activParam1 = new HashSet<Integer>();
		Set<Integer> activParam2 = new HashSet<Integer>();
		Set<Integer> activParam3 = new HashSet<Integer>();

		for(Integer i : pendingActivations.get(traceId).get(param1).get(param3)){
			activParam1.add(i);
		}
		for(Integer i : violations.get(traceId).get(param1).get(param3)){
			activParam1.add(i);
		}
		for(Integer i : fulfillments.get(traceId).get(param1).get(param3)){
			activParam1.add(i);
		}

		for(Integer i : pendingActivations.get(traceId).get(param2).get(param3)){
			activParam2.add(i);
		}
		for(Integer i : violations.get(traceId).get(param2).get(param3)){
			activParam2.add(i);
		}
		for(Integer i : fulfillments.get(traceId).get(param2).get(param3)){
			activParam2.add(i);
		}

		for(Integer i : pendingActivations.get(traceId).get(param3).get(param1)){
			activParam3.add(i);
		}
		for(Integer i : violations.get(traceId).get(param3).get(param1)){
			activParam3.add(i);
		}
		for(Integer i : fulfillments.get(traceId).get(param3).get(param1)){
			activParam3.add(i);
		}

		ArrayList<String> verificationList = new ArrayList<String>();

		int prev = 0;
		for(Integer i : activParam1){
			for(int f = prev; f<i; f++){
				verificationList.add("x");
			}
			verificationList.add("a");
			prev = i+1;
		}
		prev = 0;
		for(Integer i : activParam2){
			for(int f = prev; f<i; f++){
				if(verificationList.size()>f && !verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"x");
				}
				if(verificationList.size()>f && verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"a");
				}
				if(verificationList.size()<=f)
				{				
					verificationList.add("x");
				}
			}
			if(verificationList.size()>i && !verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"a");
			}
			if(verificationList.size()>i && verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"a");
			}
			if(verificationList.size()<=i)
			{				
				verificationList.add("a");
			}
			prev = i+1;		
		}
		prev = 0;
		for(Integer i : activParam3){
			for(int f = prev; f<i; f++){
				if(verificationList.size()>f && !verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"x");
				}
				if(verificationList.size()>f && verificationList.get(f).equals("a"))
				{				
					verificationList.set(f,"a");
				}
				if(verificationList.size()<=f)
				{				
					verificationList.add("x");
				}
			}
			if(verificationList.size()>i && !verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"t");
			}
			if(verificationList.size()>i && verificationList.get(i).equals("a"))
			{				
				verificationList.set(i,"a");
			}
			if(verificationList.size()<=i)
			{				
				verificationList.add("t");
			}
			prev = i+1;		
		}		//boolean alreadyActiva
		
		boolean alreadyActivated = false;
		int previousActivatingIndex  = -1;
		int counter = 0;
		for(String element : verificationList){
			
			
			
			if(element!=null && alreadyActivated && element.equals("a")){
				output.add(previousActivatingIndex);
				previousActivatingIndex = counter;
			}
			
			if(element!=null && !alreadyActivated && element.equals("a")){
				alreadyActivated = true;
				previousActivatingIndex = counter;
			}
			
			if(element!=null && element.equals("t")){
				alreadyActivated = false;
			}
			counter++;
		}
		return output;
	}
}
