package org.processmining.plugins.declareanalyzer;

import java.io.File;

import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.plugins.declareminer.Watch;
import org.processmining.plugins.declareminer.visualizing.AssignmentModel;
import org.processmining.plugins.declareminer.visualizing.AssignmentModelView;
import org.processmining.plugins.declareminer.visualizing.AssignmentViewBroker;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.DeclareModel;
import org.processmining.plugins.declareminer.visualizing.XMLBrokerFactory;

public class Tester {

	public static void main(String args[]) throws Exception {
//		String logFile = "/home/delas/work-tmp/papers-related/2014-journal-declare-analyzer/benchmarks/logs/30-acts-100000-traces.xes.gz";
//		String modelFile = "/home/delas/work-tmp/papers-related/2014-journal-declare-analyzer/benchmarks/models/model-10-constraints.xml";
		
		if (args.length != 2) {
			System.err.println("Use: java -jar file.jar LOG_FILE MODEL_FILE");
			System.exit(1);
		}
		String logFile = args[0];
		String modelFile = args[1];
		
		Watch w = new Watch();
		
		System.out.println("CONFIGURATION");
		System.out.println("Log: " + logFile);
		System.out.println("Model: " + modelFile);
		System.out.println("");
		
		System.out.println("SIMULATION");
		w.start();
		System.out.print("Generating plugin context... ");
		CLIPluginContext context = new CLIPluginContext(new CLIContext(), "test");
		System.out.println("done! " + w.secs() + " (sec)");
		
		w.start();
		System.out.print("Loading log... ");
		XParser parser = new XesXmlGZIPParser();
		XLog log = parser.parse(new File(logFile)).get(0);
		System.out.println("done! " + w.secs() + " (sec)");
		
		w.start();
		System.out.print("Loading model... ");
		DeclareMap model = getModel(modelFile);
		System.out.println("done! " + w.secs() + " (sec)");
		
		System.out.println("Running simulation... ");
		DeclareAnalyzerPlugin analysis = new DeclareAnalyzerPlugin();
		analysis.analyze(context, log, model);
	}
	
	@SuppressWarnings("unused")
	private static DeclareMap getModel(String fileName) {
		AssignmentViewBroker broker = XMLBrokerFactory.newAssignmentBroker(fileName);
		AssignmentModel model = broker.readAssignment();
		AssignmentModelView view = new AssignmentModelView(model);
		broker.readAssignmentGraphical(model, view);
		DeclareModel decModel = new DeclareModel(model, view);
		DeclareMap map = new DeclareMap(model, null, view, null, broker, null);
		return map;
	}
}
