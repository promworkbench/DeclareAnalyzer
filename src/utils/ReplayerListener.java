package utils;

import java.util.Set;
import java.util.Vector;

import org.deckfour.xes.model.XAttributeMap;

public interface ReplayerListener {
	void openTrace(XAttributeMap attribs, String traceId,Set<String> candidateActivations);
	void closeTrace(XAttributeMap attribs, String traceId);
	void processEvent(XAttributeMap attribs, int index);
}
