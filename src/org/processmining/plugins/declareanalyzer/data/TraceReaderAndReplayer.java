package org.processmining.plugins.declareanalyzer.data;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

import utils.ReplayerListener;

public class TraceReaderAndReplayer {
	XTrace trace;
	Map<String, Class<?>> dataTypes;
	Set<String> activityLabels;
	List<ReplayerListener> listeners;

	public TraceReaderAndReplayer(XTrace trace) throws Exception {
		this.trace = trace;
		init();
	}
	
	private void init() throws Exception {
		dataTypes = new HashMap<String, Class<?>>();
		activityLabels = new HashSet<String>();
		listeners = new LinkedList<ReplayerListener>();
		analyzeTrace();
	}
	
	public void addReplayerListener(ReplayerListener listener) {
		listeners.add(listener);
	}
	
	public void removeAllReplayerListeners() {
		listeners.clear();
	}
	
	public Map<String, Class<?>> getDataTypes() {
		return dataTypes;
	}

	public Set<String> getActivityLabels() {
		return activityLabels;
	}



	private void analyzeTrace() {
		analyzeDataAttributes(trace.getAttributes());
		for (XEvent event : trace) {
			String activityName = XConceptExtension.instance().extractName(event);//.getAttributes().get(XConceptExtension.KEY_NAME).toString();
			String eventType = XLifecycleExtension.instance().extractTransition(event);// event.getAttributes().get(XLifecycleExtension.KEY_TRANSITION).toString();
			analyzeDataAttributes(event.getAttributes());
			if (eventType != null && !eventType.isEmpty()) {
				activityLabels.add(activityName + "-" + eventType);
			} else {
				activityLabels.add(activityName);
			}
		}
	}

	protected void analyzeDataAttributes(XAttributeMap xAttributeMap) {
		for (XAttribute attr: xAttributeMap.values()) {
//			if (!attr.getKey().contains(":")) {
				String varName = attr.getKey();//.replaceAll("\\s", "_");
				Class<?> clazz = null;
				
				if (attr instanceof XAttributeBoolean) {
					clazz = Boolean.class;
				} else if (attr instanceof XAttributeContinuous) {
					clazz = Double.class;
				} else if (attr instanceof XAttributeDiscrete) {
					clazz = Long.class;
				} else if (attr instanceof XAttributeTimestamp) {
					clazz = Calendar.class;
				} else if (attr instanceof XAttributeLiteral) {
					clazz = String.class;
				}

				if (dataTypes.containsKey(varName)) {
					Class<?> oldClass = dataTypes.get(varName);
					if (!oldClass.equals(clazz)) {
						if (oldClass.equals(String.class) || clazz.equals(String.class)) {
							dataTypes.put(varName, String.class);
						} else if (oldClass.equals(Double.class) || clazz.equals(Double.class)) {
							dataTypes.put(varName, Double.class);
						}
					}
				} else {
					dataTypes.put(varName, clazz);
				}
//			}
		}
	}
	
	public void replayLog(Set<String> candidateActivations) {
		String traceId = XConceptExtension.instance().extractName(trace);
		for (ReplayerListener listener: listeners) {
			listener.openTrace(trace.getAttributes(), traceId, candidateActivations);
		}
		
		int index = 0;
		for (XEvent event : trace) {
			for (ReplayerListener listener: listeners) {
				listener.processEvent(event.getAttributes(), index);
			}
			index++;
		}
		for (ReplayerListener listener: listeners) {
			listener.closeTrace(trace.getAttributes(), traceId);
		}
	}
}
