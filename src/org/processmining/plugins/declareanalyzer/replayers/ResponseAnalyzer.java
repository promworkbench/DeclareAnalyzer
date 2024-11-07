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



public class ResponseAnalyzer implements DeclareReplayer{

	HashMap<String, String> conditionsAct = new HashMap<String, String>();
	HashMap<String, String> conditionsTarg = new HashMap<String, String>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> fulfillments = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>> pendingActivations = new HashMap<String, HashMap<String, HashMap<String, Set<Integer>>>>();
	List<List<String>> dispositions;
	//
	HashMap<String, String> mappingConstrParam = new HashMap<String, String>();
	//
	HashMap<String, ConstraintConditions> cMap = new HashMap<String, ConstraintConditions>();

	public ResponseAnalyzer(List<List<String>> dispositions){
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
		//
		for(String key : pendingPerTrace.keySet()){
			if(mappingConstrParam.get(key).equals(event)){
				HashMap<String, Set<Integer>> pend = pendingPerTrace.get(key);
				for(String param2 : pend.keySet()){
					boolean isActivation = true;
					Pair<String,String> params = new Pair<String, String>(key, param2);
					
					//
					
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
		HashMap<String, HashMap<String, Set<Integer>>> fulfillmentPerTrace;
		if(fulfillments.containsKey(traceId)){
			fulfillmentPerTrace = fulfillments.get(traceId);
		}else{
			fulfillmentPerTrace = new HashMap<String, HashMap<String,Set<Integer>>>();
			for(List<String> pair : dispositions){
				HashMap<String, Set<Integer>> fulfill;
				//
				if(fulfillmentPerTrace.containsKey(pair.get(0)+pair.get(3))){
					fulfill = fulfillmentPerTrace.get(pair.get(0)+pair.get(3));
				}else{
					fulfill = new HashMap<String, Set<Integer>>();
				}
				fulfill.put(pair.get(1), new HashSet<Integer>());
				fulfillmentPerTrace.put(pair.get(0)+pair.get(3),fulfill);
				//
			}	
		}
		for(String param1 : pendingPerTrace.keySet()){
			HashMap<String, Set<Integer>> pending = pendingPerTrace.get(param1);
			HashMap<String, Set<Integer>> fulfill = fulfillmentPerTrace.get(param1);
			Pair<String,String> params = new Pair<String, String>(param1, event);
			String condition = param1.replace(params.getFirst(),"");
			String conditionTarget = conditionsTarg.get(params.getFirst()+params.getSecond()+condition);
			if(pending.containsKey(event)){
				if(conditionTarget.equals("1")){
					Set<Integer> indexesPend = pending.get(event);
					Set<Integer> indexesFulfill = fulfill.get(event);
					indexesFulfill.addAll(indexesPend);
					indexesPend.clear();
					pending.put(event, indexesPend);
					fulfill.put(event, indexesFulfill);
					fulfillmentPerTrace.put(param1, fulfill);
					pendingPerTrace.put(param1, pending);
				}else{
					Set<Integer> indexesPend = pending.get(event);
					Set<Integer> indexesToremove = new HashSet<Integer>();
					for(Integer p : indexesPend){
						boolean	targetFound = false;
						String activation = XLogHelper.getCompleteName(trace.get(p).getAttributes());
						Format timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						//timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						Date date1 = null;
						Date date2 = null;
						try {
							date1 = (Date) timestampFormat.parseObject(trace.get(p).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
							date2 = (Date) timestampFormat.parseObject(trace.get(eventIndex).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
						} catch (ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						//Date date1 = format1.p
						//Date date1 = DateFormat.ptrace.get(p).getAttributes().get(XTimeExtension.KEY_TIMESTAMP).toString());
						List<Pair<Integer, Map<String, String>>> snapshotsActivation = listener.getInstances().get(activation).get(traceId);
						Map<String, String> snapshotActivation = null;
						Map<String,String> datavalues = new HashMap<String, String>();
						for(Pair pair : snapshotsActivation){
							if(((Integer)pair.getFirst()).intValue()==p){
								snapshotActivation = (Map<String, String>)pair.getSecond();
								for(String attribute : snapshotActivation.keySet()){
									datavalues.put("A."+attribute, snapshotActivation.get(attribute));
									//datavalues.put(attribute, snapshotActivation.get(attribute)); // TODO: help (mega search and replace :O)
								}
								break;
							}
						}
						//String activation = (trace.get(p).getAttributes().get(XConceptExtension.KEY_NAME)+"-"+trace.get(p).getAttributes().get(XLifecycleExtension.KEY_TRANSITION));
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
							//						System.out.println(conditionTarget);
						}catch(Exception e){
							e.printStackTrace();
						}
						if(targetFound){
							Set<Integer> indexesFulfill = fulfill.get(event);
							indexesFulfill.add(p);
							//indexesPend.clear();
							indexesToremove.add(p);

							pending.put(event, indexesPend);
							fulfill.put(event, indexesFulfill);
							fulfillmentPerTrace.put(param1, fulfill);
							pendingPerTrace.put(param1, pending);
						}
					}
					indexesPend.removeAll(indexesToremove);
				}
			}
		}
		fulfillments.put(traceId, fulfillmentPerTrace);
		pendingActivations.put(traceId, pendingPerTrace);
	}


	
	
	public Set<Integer> getSBFulfillments(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(fulfillments.get(traceId).get(param1).get(param3));
		output.addAll(fulfillments.get(traceId).get(param2).get(param3));
		return output;
	}


	public Set<Integer> getSBPendingActivations(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		output.addAll(pendingActivations.get(traceId).get(param1).get(param3));
		output.addAll(pendingActivations.get(traceId).get(param2).get(param3));
		return output;
	}
	
	
	public Set<Integer> getTBFulfillments(String traceId, String param1, String param2, String param3) {
		Set<Integer> output = new HashSet<Integer>();
		for(Integer i : pendingActivations.get(traceId).get(param1).get(param2)){
			if(fulfillments.get(traceId).get(param1).get(param3).contains(i)){
				output.add(i);
			}
		}
		for(Integer i : fulfillments.get(traceId).get(param1).get(param2)){
				output.add(i);
		}
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
	
	
	public Set<Integer> getFulfillments(String traceId, String param1, String param2) {
		return fulfillments.get(traceId).get(param1).get(param2);
	}


	public Set<Integer> getPendingActivations(String traceId, String param1, String param2) {
		return pendingActivations.get(traceId).get(param1).get(param2);
	}
}
