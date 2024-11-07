package org.processmining.plugins.declareanalyzer.data;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.processmining.framework.util.Pair;

import utils.ReplayerListener;
import utils.XLogHelper;


public class DataSnapshotListener implements ReplayerListener {
	Map<String, Map<String, List<Pair<Integer, Map<String, String>>>>> instances;
	Map<String, Class<?>> dataTypes;

	Set<String> activityLabels;
	List<Pair<String,Pair<Integer, Map<String,String>>>> records;
	Map<String, Set<String>> stringDomains;

	Map<String, String> assignment = null;
	String traceId = null;
	private Set<String> frequentActivations;

	public DataSnapshotListener(Map<String, Class<?>> dataTypes, Set<String> activityLabels) {
		this.instances = new HashMap<String, Map<String,List<Pair<Integer,Map<String,String>>>>>();
		this.activityLabels = activityLabels;
		this.dataTypes = dataTypes;

		this.stringDomains = new HashMap<String, Set<String>>();
		for (String varname: dataTypes.keySet()) {
			if (dataTypes.get(varname).equals(String.class) || dataTypes.get(varname).equals(Boolean.class)) {
				stringDomains.put(varname, new HashSet<String>());
			}
		}
	}

	@Override
	public void openTrace(XAttributeMap attribs, String traceId,Set<String> frequentActivations) {
		this.frequentActivations = frequentActivations;
		this.traceId = traceId;
		this.assignment = new HashMap<String, String>();
		this.records = new LinkedList<Pair<String, Pair<Integer, Map<String,String>>>>();

		for (String varname: dataTypes.keySet()) {
			assignment.put(varname, null);
		}
		updateSnapshot(attribs, assignment);
	}

	@Override
	public void closeTrace(XAttributeMap attribs, String traceId) {
		copyRecords(instances, traceId, records);
	}

	@Override
	public void processEvent(XAttributeMap attribs, int index) {
		String activityName = XLogHelper.getCompleteName(attribs);
		updateSnapshot(attribs, assignment);
		records.add(new Pair<String, Pair<Integer,Map<String,String>>>(activityName.toLowerCase(), new Pair<Integer, Map<String,String>>(index++, new HashMap<String, String>(assignment))));		
	}

	private void updateSnapshot(XAttributeMap attribs,
			Map<String, String> assignment) {
		for (XAttribute attr: attribs.values()) {
//			if (!attr.getKey().contains(":")) {
				String varName = attr.getKey();//.replaceAll("\\s", "_");
//				String token = attr.toString();
				Object value = null;

				if (dataTypes.get(varName).equals(Boolean.class)) {
					value = ((XAttributeBoolean) attr).getValue();
					
				} else if (dataTypes.get(varName).equals(Double.class)) {
					value = ((XAttributeContinuous) attr).getValue();
					
				} else if (dataTypes.get(varName).equals(Long.class)) {
					value = ((XAttributeDiscrete) attr).getValue();
					
				} else if (dataTypes.get(varName).equals(Calendar.class)) {
					value = ((XAttributeTimestamp) attr).getValue();
				
				} else if (dataTypes.get(varName).equals(String.class)) {
					String val = "'" + attr.toString() + "'"; //((XAttributeLiteral) attr).getValue();
					value = val.replace("(\r\n|\n\r|\r|\n)", " ");
					stringDomains.get(varName).add((String) value);
					
				}
				assignment.put(varName, value.toString());
//			}
		}
	}

	private void copyRecords(Map<String, Map<String, List<Pair<Integer, Map<String, String>>>>> instances2, String traceId, List<Pair<String, Pair<Integer, Map<String, String>>>> records) {
		for (Pair<String, Pair<Integer, Map<String, String>>> record: records) {
			Map<String, List<Pair<Integer, Map<String, String>>>> map = instances2.get(record.getFirst());
			if (map == null)
				instances2.put(record.getFirst(), map = new HashMap<String,List<Pair<Integer,Map<String,String>>>>());
			if (!record.getSecond().getSecond().isEmpty()) {
				List<Pair<Integer,Map<String, String>>> list = map.get(traceId);
				if (list == null)
					map.put(traceId, list = new LinkedList<Pair<Integer, Map<String,String>>>());
				list.add(record.getSecond());
			}
		}
	}
//traceId;    activityName;   position;   
	public Map<String, Map<String, List<Pair<Integer, Map<String, String>>>>> getInstances() {
		return instances;
	}
	
	public Map<String, Set<String>> getDomains() {
		return stringDomains;
	}
	
	public Map<String, Class<?>> getDataTypes() {
		return dataTypes;
	}
}
