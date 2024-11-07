package utils;

import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;

public class ReverseTraceAttributesIntoEvents {

	@Plugin(
		name = "Reverse Trace Attributes into Events",
		parameterLabels = { "Source Log" },
		returnLabels = { "Log with Attributes on Events" },
		returnTypes = { XLog.class },
		userAccessible = true
	)
	@UITopiaVariant(
		author = "A. Burattin, F.M. Maggi",
		email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
		affiliation = "University of Padua and Tartu"
	)
	public XLog reverse(UIPluginContext context, XLog source) {
		XLog log = XLogHelper.generateNewXLog(XLogHelper.getName(source));
		
		for (XTrace t : source) {
			XAttributeMap am = t.getAttributes();
			XTrace tp = XLogHelper.insertTrace(log, XLogHelper.getName(t));
			for (XEvent e : t) {
				XEvent ep = XLogHelper.insertEvent(tp, XLogHelper.getName(e), XTimeExtension.instance().extractTimestamp(e));
				for (String a : e.getAttributes().keySet()) {
					if (!"concept:name".equals(a)) {
						XLogHelper.decorateElement(ep, a, e.getAttributes().get(a));
					}
				}
				if (!ep.getAttributes().keySet().contains("transition:type")) {
					XLogHelper.decorateElement(ep, "lifecycle:transition", "complete", XLifecycleExtension.instance().getName());
				}
				for (String a : am.keySet()) {
					if (!"concept:name".equals(a)) {
						XLogHelper.decorateElement(ep, a, am.get(a));
					}
				}
			}
		}
		
		context.getFutureResult(0).setLabel(XLogHelper.getName(source) + " (with trace attributes on events)");
		
		return log;
	}
}
