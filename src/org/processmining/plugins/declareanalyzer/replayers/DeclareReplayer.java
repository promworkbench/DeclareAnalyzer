package org.processmining.plugins.declareanalyzer.replayers;

import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareanalyzer.data.DataSnapshotListener;

public interface DeclareReplayer {
	
	public void process(int eventIndex, String event, XTrace trace, String traceId,DataSnapshotListener listener);

}
