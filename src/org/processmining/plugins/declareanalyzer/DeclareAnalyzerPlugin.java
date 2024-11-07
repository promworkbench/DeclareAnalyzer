package org.processmining.plugins.declareanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.declareanalyzer.data.DataSnapshotListener;
import org.processmining.plugins.declareanalyzer.data.TraceReaderAndReplayer;
import org.processmining.plugins.declareanalyzer.replayers.Absence2Analyzer;
import org.processmining.plugins.declareanalyzer.replayers.Absence3Analyzer;
import org.processmining.plugins.declareanalyzer.replayers.AbsenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.AlternatePrecedenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.AlternateResponseAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.ChainPrecedenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.ChainResponseAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.DeclareReplayer;
import org.processmining.plugins.declareanalyzer.replayers.Exactly1Analyzer;
import org.processmining.plugins.declareanalyzer.replayers.Exactly2Analyzer;
import org.processmining.plugins.declareanalyzer.replayers.Existence2Analyzer;
import org.processmining.plugins.declareanalyzer.replayers.Existence3Analyzer;
import org.processmining.plugins.declareanalyzer.replayers.ExistenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.InitAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.NotChainPrecedenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.NotChainResponseAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.NotPrecedenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.NotResponseAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.PrecedenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.RespondedExistenceAnalyzer;
import org.processmining.plugins.declareanalyzer.replayers.ResponseAnalyzer;
import org.processmining.plugins.declareminer.Watch;
import org.processmining.plugins.declareminer.visualizing.ActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.AssignmentModel;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.DeclareMinerOutput;
import org.processmining.plugins.declareminer.visualizing.Parameter;


/**
 * Class with the Declare Analyzer plugin
 * 
 * @author Andrea Burattin
 * @author Fabrizio Maggi
 */
public class DeclareAnalyzerPlugin {

	HashMap<String, DeclareReplayer> replayers = new HashMap<String, DeclareReplayer>();
	XAttributeMap eventAttributeMap;
	HashSet<String> templates = new HashSet<String>();

	List<List<String>> dispositionsresponse = new ArrayList<List<String>>();
	List<List<String>> dispositionsprecedence = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotresponse = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotprecedence = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotchainresponse = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotchainprecedence = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotrespondedexistence = new ArrayList<List<String>>();
	List<List<String>> dispositionssuccession = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotsuccession = new ArrayList<List<String>>();
	List<List<String>> dispositionschainresponse = new ArrayList<List<String>>();
	List<List<String>> dispositionschainprecedence = new ArrayList<List<String>>();
	List<List<String>> dispositionschainsuccession = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotchainsuccession = new ArrayList<List<String>>();
	List<List<String>> dispositionsalternateresponse = new ArrayList<List<String>>();
	List<List<String>> dispositionsalternateprecedence = new ArrayList<List<String>>();
	List<List<String>> dispositionsalternatesuccession = new ArrayList<List<String>>();
	List<List<String>> dispositionsrespondedexistence = new ArrayList<List<String>>();
	List<List<String>> dispositionscoexistence = new ArrayList<List<String>>();
	List<List<String>> dispositionsnotcoexistence = new ArrayList<List<String>>();
	List<List<String>> dispositionsexclusivechoice = new ArrayList<List<String>>();
	List<List<String>> dispositionschoice = new ArrayList<List<String>>();

	List<List<String>> invdispositionscoexistence = new ArrayList<List<String>>();
	List<List<String>> invdispositionsnotcoexistence = new ArrayList<List<String>>();
	List<List<String>> invdispositionsexclusivechoice = new ArrayList<List<String>>();
	List<List<String>> invdispositionschoice = new ArrayList<List<String>>();

	List<List<String>> dispositionsinit = new ArrayList<List<String>>();
	List<List<String>> dispositionsabsence = new ArrayList<List<String>>();
	List<List<String>> dispositionsabsence2 = new ArrayList<List<String>>();
	List<List<String>> dispositionsabsence3 = new ArrayList<List<String>>();
	List<List<String>> dispositionsexactly1 = new ArrayList<List<String>>();
	List<List<String>> dispositionsexactly2 = new ArrayList<List<String>>();
	List<List<String>> dispositionsexistence = new ArrayList<List<String>>();
	List<List<String>> dispositionsexistence2 = new ArrayList<List<String>>();
	List<List<String>> dispositionsexistence3 = new ArrayList<List<String>>();
	List<List<String>> dispositionsstronginit = new ArrayList<List<String>>();

	Set<String> candidateActivations = new HashSet<String>();
	HashMap<String,HashSet<String>> occurredParametersByTemplate = new HashMap<String,HashSet<String>>();
	AssignmentModel assignmentModel = null;
	boolean data = false;
	
	
	@Plugin(
			name = "Declare Analyzer",
			parameterLabels = { "An event log", "A Declare model" },
			returnLabels = { "Declare Analyzer result" },
			returnTypes = { AnalysisResult.class },
			userAccessible = true)
	@UITopiaVariant(
			author = "A. Burattin, F.M. Maggi",
			email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
			affiliation = "University of Padua and Tartu")
	public AnalysisResult analyze(PluginContext context, XLog log, DeclareMap model) {
		if (model.getModel() == null) {
			return null;
		}
		
		Watch w = new Watch();
		Watch w2 = new Watch();
		w.start();
		w2.start();
	if(context!= null){
		context.getProgress().setCaption("Preparing model");
	}
		AnalysisResult analysis = new AnalysisResult();
		assignmentModel = model.getModel();
	if(context!=null){
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(assignmentModel.constraintDefinitionsCount() * log.size());
	}
//		System.out.println("STEP 1");
//		System.out.println("=============================================");
		prepareDataStructure();
		System.out.println("Time required for step 1: " + w.secs() + " (sec)");
		w.start();

//		System.out.println("STEP 2");
//		System.out.println("=============================================");
		for (XTrace t : log) {
			
			TraceReaderAndReplayer replay = null;
			DataSnapshotListener listener = null;
			if(data) {
				try {
					replay = new TraceReaderAndReplayer(t);
					listener = new DataSnapshotListener(replay.getDataTypes(), replay.getActivityLabels());
					replay.addReplayerListener(listener);
					replay.replayLog(candidateActivations);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
			String traceId = XConceptExtension.instance().extractName(t);
			int i = 0;
			for (XEvent e : t) {
				String event = XConceptExtension.instance().extractName(e);
				String transitionType = XLifecycleExtension.instance().extractTransition(e);
				if (transitionType == null) {
					event += "-complete";
				} else {
					event += "-" + transitionType;
				}
				for(DeclareReplayer replayer : replayers.values()){
					replayer.process(i, event.toLowerCase(), t, traceId, listener);
				}
				i++;
			}
		}
		System.out.println("Time required for step 2: " + w.secs() + " (sec)");
		w.start();
		
//		System.out.println("STEP 3");
//		System.out.println("=============================================");

		for (XTrace t : log) {
			XAttributeMap tracetAttributeMap = t.getAttributes();
			String traceId = tracetAttributeMap.get(XConceptExtension.KEY_NAME).toString();
			for (ConstraintDefinition cd : assignmentModel.getConstraintDefinitions()) {
				ArrayList<String> disposs = new ArrayList<String>();
				for (Parameter p : cd.getParameters()) {
					for (ActivityDefinition b : cd.getBranches(p)) {
						String bName = b.getName().toLowerCase();
						
						if((!bName.endsWith("-assign")&&!bName.endsWith("-ate_abort")&&!bName.endsWith("-suspend")&&!bName.endsWith("-complete")&&!bName.endsWith("-autoskip")&&!bName.endsWith("-manualskip")&&!bName.contains("pi_abort")&&!bName.endsWith("-reassign")&&!bName.endsWith("-resume")&&!bName.endsWith("-schedule")&&!bName.endsWith("-start")&&!bName.endsWith("-unknown")&&!bName.endsWith("-withdraw"))&&(!bName.contains("<center>assign")&&!bName.contains("<center>ate_abort")&&!bName.contains("<center>suspend")&&!bName.contains("<center>complete")&&!bName.contains("<center>autoskip")&&!bName.contains("<center>manualskip")&&!bName.contains("<center>pi_abort")&&!bName.contains("<center>reassign")&&!bName.contains("<center>resume")&&!bName.contains("<center>schedule")&&!bName.contains("<center>start")&&!bName.contains("<center>unknown")&&!bName.contains("<center>withdraw"))){
							disposs.add(bName + "-complete");
						}else{

//							bName = bName.replace("-COMPLETE", "-complete");
//							bName = bName.replace("-ASSIGN", "-assign");
//							bName = bName.replace("-ATE_ABORT", "-ate_abort");
//							bName = bName.replace("-SUSPEND", "-suspend");
//							bName = bName.replace("-AUTOSKIP", "-autoskip");
//							bName = bName.replace("-MANUALSKIP", "-manualskip");
//							bName = bName.replace("PI_ABORT", "pi_abort");
//							bName = bName.replace("-REASSIGN", "-reassign");
//							bName = bName.replace("-RESUME", "-resume");
//							bName = bName.replace("-SCHEDULE", "-schedule");
//							bName = bName.replace("-START", "-start");
//							bName = bName.replace("-UNKNOWN", "-unknown");
//							bName = bName.replace("-WITHDRAW", "-withdraw");
							
							disposs.add(bName);
						}
					}

				}
				String cdName = cd.getName().toLowerCase();
				String condi = "";
				if(!cd.getCondition().toString().equals("[][][]")){
					condi = condi + cd.getCondition().toString();
				}
				String param0 = disposs.get(0);
				if(cdName.equals("response")){
					ResponseAnalyzer replayer = (ResponseAnalyzer)replayers.get("response");
					Set<Integer> violations = replayer.getPendingActivations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not response")){
					NotResponseAnalyzer replayer = (NotResponseAnalyzer)replayers.get("not response");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getPendingActivations(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("precedence")){
					PrecedenceAnalyzer replayer = (PrecedenceAnalyzer)replayers.get("precedence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not precedence")){
					NotPrecedenceAnalyzer replayer = (NotPrecedenceAnalyzer)replayers.get("not precedence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("init")){
					InitAnalyzer replayer = (InitAnalyzer)replayers.get("init");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("strong init")){
					InitAnalyzer replayer = (InitAnalyzer)replayers.get("strong init");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("absence")){
					AbsenceAnalyzer replayer = (AbsenceAnalyzer)replayers.get("absence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = new HashSet<Integer>();
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("absence2")){
					Absence2Analyzer replayer = (Absence2Analyzer)replayers.get("absence2");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("absence3")){
					Absence3Analyzer replayer = (Absence3Analyzer)replayers.get("absence3");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("existence")){
					ExistenceAnalyzer replayer = (ExistenceAnalyzer)replayers.get("existence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("existence2")){
					Existence2Analyzer replayer = (Existence2Analyzer)replayers.get("existence2");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("existence3")){
					Existence3Analyzer replayer = (Existence3Analyzer)replayers.get("existence3");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("exactly1")){
					Exactly1Analyzer replayer = (Exactly1Analyzer)replayers.get("exactly1");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("exactly2")){
					Exactly2Analyzer replayer = (Exactly2Analyzer)replayers.get("exactly2");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi);
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("responded existence")){
					RespondedExistenceAnalyzer replayer = (RespondedExistenceAnalyzer)replayers.get("responded existence");
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					Set<Integer> violations = replayer.getActivations(traceId, param0+condi, disposs.get(1));
					activations.addAll(violations);
					activations.addAll(fulfillments);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not responded existence")){
					RespondedExistenceAnalyzer replayer = (RespondedExistenceAnalyzer)replayers.get("not responded existence");
					Set<Integer> fulfillments = replayer.getActivations(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					Set<Integer> violations = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					activations.addAll(violations);
					activations.addAll(fulfillments);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("succession")){
					ResponseAnalyzer replayerResponse = (ResponseAnalyzer)replayers.get("succession r");
					PrecedenceAnalyzer replayerPrecedence = (PrecedenceAnalyzer)replayers.get("succession p");
					Set<Integer> violations = replayerResponse.getPendingActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(replayerPrecedence.getViolations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> fulfillments = replayerResponse.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(replayerPrecedence.getFulfillments(traceId, param0+condi, disposs.get(1)));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not succession")){
					NotResponseAnalyzer replayerResponse = (NotResponseAnalyzer)replayers.get("not succession r");
					NotPrecedenceAnalyzer replayerPrecedence = (NotPrecedenceAnalyzer)replayers.get("not succession p");
					Set<Integer> violations = replayerResponse.getViolations(traceId, param0+condi, disposs.get(1));
					violations.addAll(replayerPrecedence.getViolations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> fulfillments = replayerResponse.getPendingActivations(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(replayerPrecedence.getFulfillments(traceId, param0+condi, disposs.get(1)));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("chain response")){
					ChainResponseAnalyzer replayer = (ChainResponseAnalyzer)replayers.get("chain response");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					violations.addAll(replayer.getPendingActivations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not chain response")){
					NotChainResponseAnalyzer replayer = (NotChainResponseAnalyzer)replayers.get("not chain response");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(replayer.getPendingActivations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("chain precedence")){
					ChainPrecedenceAnalyzer replayer = (ChainPrecedenceAnalyzer)replayers.get("chain precedence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not chain precedence")){
					NotChainPrecedenceAnalyzer replayer = (NotChainPrecedenceAnalyzer)replayers.get("not chain precedence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("not chain succession")){
					NotChainResponseAnalyzer replayerResponse = (NotChainResponseAnalyzer)replayers.get("not chain succession r");
					NotChainPrecedenceAnalyzer replayerPrecedence = (NotChainPrecedenceAnalyzer)replayers.get("not chain succession p");
					//Set<Integer> violations = replayerResponse.getPendingActivations(traceId, param0, disposs.get(1));
					Set<Integer> violations = replayerPrecedence.getViolations(traceId, param0+condi, disposs.get(1));
					violations.addAll(replayerResponse.getViolations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> fulfillments = replayerResponse.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(replayerPrecedence.getFulfillments(traceId, param0+condi, disposs.get(1)));
					fulfillments.addAll(replayerResponse.getPendingActivations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("chain succession")){
					ChainResponseAnalyzer replayerResponse = (ChainResponseAnalyzer)replayers.get("chain succession r");
					ChainPrecedenceAnalyzer replayerPrecedence = (ChainPrecedenceAnalyzer)replayers.get("chain succession p");
					Set<Integer> violations = replayerResponse.getPendingActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(replayerResponse.getViolations(traceId, param0+condi, disposs.get(1)));
					violations.addAll(replayerPrecedence.getViolations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> fulfillments = replayerResponse.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(replayerPrecedence.getFulfillments(traceId, param0+condi, disposs.get(1)));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("alternate response")){
					AlternateResponseAnalyzer replayer = (AlternateResponseAnalyzer)replayers.get("alternate response");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> pending = replayer.getPendingActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(pending);
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("alternate precedence")){
					AlternatePrecedenceAnalyzer replayer = (AlternatePrecedenceAnalyzer)replayers.get("alternate precedence");
					Set<Integer> violations = replayer.getViolations(traceId, param0+condi, disposs.get(1));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("alternate succession")){
					AlternateResponseAnalyzer replayerResponse = (AlternateResponseAnalyzer)replayers.get("alternate succession r");
					AlternatePrecedenceAnalyzer replayerPrecedence = (AlternatePrecedenceAnalyzer)replayers.get("alternate succession p");
					Set<Integer> violations = replayerResponse.getViolations(traceId, param0+condi, disposs.get(1));
					violations.addAll(replayerPrecedence.getViolations(traceId, param0+condi, disposs.get(1)));
					Set<Integer> fulfillments = replayerResponse.getFulfillments(traceId, param0+condi, disposs.get(1));
					Set<Integer> pending = replayerResponse.getPendingActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(pending);
					fulfillments.addAll(replayerPrecedence.getFulfillments(traceId, param0+condi, disposs.get(1)));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("co-existence")){
					RespondedExistenceAnalyzer replayer = (RespondedExistenceAnalyzer)replayers.get("co-existence");
					RespondedExistenceAnalyzer invreplayer = (RespondedExistenceAnalyzer)replayers.get("inv co-existence");
					Set<Integer> violations = replayer.getActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(invreplayer.getActivations(traceId, disposs.get(1), param0+condi));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(invreplayer.getFulfillments(traceId, disposs.get(1), param0+condi));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, violations, fulfillments);
				}else if(cdName.equals("choice")){
					RespondedExistenceAnalyzer replayer = (RespondedExistenceAnalyzer)replayers.get("choice");
					RespondedExistenceAnalyzer invreplayer = (RespondedExistenceAnalyzer)replayers.get("inv choice");
					Set<Integer> fulfillments = replayer.getActivations(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(invreplayer.getActivations(traceId, disposs.get(1), param0+condi));
					fulfillments.addAll(replayer.getFulfillments(traceId, param0+condi, disposs.get(1)));
					fulfillments.addAll(invreplayer.getFulfillments(traceId, disposs.get(1), param0+condi));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					//activations.addAll(violations);
					analysis.addResult(t, cd, activations, new HashSet<Integer>(), fulfillments);
				}else if(cdName.equals("not co-existence")){
					RespondedExistenceAnalyzer replayer = (RespondedExistenceAnalyzer)replayers.get("not co-existence");
					RespondedExistenceAnalyzer invreplayer = (RespondedExistenceAnalyzer)replayers.get("inv not co-existence");
					Set<Integer> violations = replayer.getActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(invreplayer.getActivations(traceId, disposs.get(1), param0+condi));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(invreplayer.getFulfillments(traceId, disposs.get(1), param0+condi));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, fulfillments, violations);
				}else if(cdName.equals("exclusive choice")){
					RespondedExistenceAnalyzer replayer = (RespondedExistenceAnalyzer)replayers.get("exclusive choice");
					RespondedExistenceAnalyzer invreplayer = (RespondedExistenceAnalyzer)replayers.get("inv exclusive choice");
					Set<Integer> violations = replayer.getActivations(traceId, param0+condi, disposs.get(1));
					violations.addAll(invreplayer.getActivations(traceId, disposs.get(1), param0+condi));
					Set<Integer> fulfillments = replayer.getFulfillments(traceId, param0+condi, disposs.get(1));
					fulfillments.addAll(invreplayer.getFulfillments(traceId, disposs.get(1), param0+condi));
					Set<Integer> activations = new HashSet<Integer>();
					activations.addAll(fulfillments);
					activations.addAll(violations);
					analysis.addResult(t, cd, activations, fulfillments, violations);
				}
				if(context!=null && context.getProgress() != null){
					context.getProgress().inc();
					if (context.getResult() != null && context.getProgress().isCancelled()) {
						return null;
					}
				}
			}
		}
		System.out.println("Time required for step 3: " + w.secs() + " (sec)");
		System.out.println("Total time required: " + w2.msecs() + " (msec)");
		System.gc();
		
		if (context != null && context.getResult() != null) {
			context.getFutureResult(0).setLabel("Declare Analysis [log: "+ log.getAttributes().get("concept:name") +", model: "+ model.getModel().getName() +"]");
		}
		return analysis;
	}

	@Plugin(
			name = "Declare Analyzer from miner output",
			parameterLabels = { "An event log", "A Declare miner output" },
			returnLabels = { "Declare Analyzer result" },
			returnTypes = { AnalysisResult.class },
			userAccessible = true)
	@UITopiaVariant(
			author = "A. Burattin, F.M. Maggi",
			email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
			affiliation = UITopiaVariant.EHV)
	public AnalysisResult analyze(PluginContext context, XLog log, DeclareMinerOutput model) {
		return analyze(context, log, model.getModel());
	}
	
	private void prepareDataStructure() {
		for (ConstraintDefinition cd : assignmentModel.getConstraintDefinitions()) {
			boolean occurred = false;
			HashSet<String> occurredParameters = new HashSet<String>();
			if(occurredParametersByTemplate.containsKey(cd.getDisplay())){
				occurredParameters = occurredParametersByTemplate.get(cd.getDisplay());
			}
		
			templates.add(cd.getName());
			String temp = cd.getName();
			ArrayList<String> disposs = new ArrayList<String>();
			for (Parameter p : cd.getParameters()) {
				for (ActivityDefinition b : cd.getBranches(p)) {
					if((!b.getName().endsWith("-assign")&&!b.getName().endsWith("-ate_abort")&&!b.getName().endsWith("-suspend")&&!b.getName().endsWith("-complete")&&!b.getName().endsWith("-autoskip")&&!b.getName().endsWith("-manualskip")&&!b.getName().contains("pi_abort")&&!b.getName().endsWith("-reassign")&&!b.getName().endsWith("-resume")&&!b.getName().endsWith("-schedule")&&!b.getName().endsWith("-start")&&!b.getName().endsWith("-unknown")&&!b.getName().endsWith("-withdraw"))&&(!b.getName().contains("<center>assign")&&!b.getName().contains("<center>ate_abort")&&!b.getName().contains("<center>suspend")&&!b.getName().contains("<center>complete")&&!b.getName().contains("<center>autoskip")&&!b.getName().contains("<center>manualskip")&&!b.getName().contains("<center>pi_abort")&&!b.getName().contains("<center>reassign")&&!b.getName().contains("<center>resume")&&!b.getName().contains("<center>schedule")&&!b.getName().contains("<center>start")&&!b.getName().contains("<center>unknown")&&!b.getName().contains("<center>withdraw"))){
						disposs.add(b.getName().toLowerCase()+"-complete");
						if(cd.getCondition().toString().contains("[")){
							candidateActivations.add(b.getName().toLowerCase()+"-complete");
							if(occurredParameters.contains(b.getName().toLowerCase()+"-complete")){
								occurred = true;
							}
							occurredParameters.add(b.getName().toLowerCase()+"-complete");
							data = true;
						}
					}else{
						disposs.add(b.getName().toLowerCase());
						if(cd.getCondition().toString().contains("[")){
							candidateActivations.add(b.getName().toLowerCase());
							if(occurredParameters.contains(b.getName().toLowerCase())){
								occurred = true;
							}
							occurredParameters.add(b.getName().toLowerCase());
							data = true;
						}
					}
				}
			}
			String condi = "";
			if(!cd.getCondition().toString().equals("[][][]")){
				condi = condi + cd.getCondition().toString();
			}
			occurredParametersByTemplate.put(cd.getDisplay(),occurredParameters);
			String tempLower = temp.toLowerCase();
			if(tempLower.equals("response")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsresponse.add(disposs);
			}else if(tempLower.equals("not response")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsnotresponse.add(disposs);
			}else if(tempLower.equals("exclusive choice")){
				disposs.add("responded existence");
				disposs.add(condi);
				ArrayList<String> inverse = new ArrayList<String>();
				inverse.add(disposs.get(1));
				inverse.add(disposs.get(0));
				inverse.add(disposs.get(2));
				inverse.add(disposs.get(3));
				dispositionsexclusivechoice.add(disposs);
				invdispositionsexclusivechoice.add(inverse);
			}else if(tempLower.equals("choice")){
				disposs.add("responded existence");
				disposs.add(condi);
				ArrayList<String> inverse = new ArrayList<String>();
				inverse.add(disposs.get(1));
				inverse.add(disposs.get(0));
				inverse.add(disposs.get(2));
				inverse.add(disposs.get(3));
				dispositionschoice.add(disposs);
				invdispositionschoice.add(inverse);
			}else if(tempLower.equals("precedence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsprecedence.add(disposs);
			}else if(tempLower.equals("not precedence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsnotprecedence.add(disposs);
			}else if(tempLower.equals("init")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsinit.add(disposs);
			}else if(tempLower.equals("strong init")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsinit.add(disposs);
			}else if(tempLower.equals("absence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsabsence.add(disposs);
			}else if(tempLower.equals("absence2")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsabsence2.add(disposs);
			}else if(tempLower.equals("absence3")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsabsence3.add(disposs);
			}else if(tempLower.equals("existence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsexistence.add(disposs);
			}else if(tempLower.equals("existence2")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsexistence2.add(disposs);
			}else if(tempLower.equals("existence3")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsexistence3.add(disposs);
			}else if(tempLower.equals("exactly1")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsexactly1.add(disposs);
			}else if(tempLower.equals("exactly2")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsexactly2.add(disposs);
			}else if(tempLower.equals("responded existence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsrespondedexistence.add(disposs);
			}else if(tempLower.equals("not responded existence")){
				disposs.add("responded existence");
				disposs.add(condi);
				dispositionsnotrespondedexistence.add(disposs);
			}else if(tempLower.equals("co-existence")){
				disposs.add("responded existence");
				disposs.add(condi);
				ArrayList<String> inverse = new ArrayList<String>();
				inverse.add(disposs.get(1));
				inverse.add(disposs.get(0));
				inverse.add(disposs.get(2));
				inverse.add(disposs.get(3));
				dispositionscoexistence.add(disposs);
				invdispositionscoexistence.add(inverse);
			}else if(tempLower.equals("not co-existence")){
				disposs.add("responded existence");
				disposs.add(condi);
				//.add(disposs);
				ArrayList<String> inverse = new ArrayList<String>();
				inverse.add(disposs.get(1));
				inverse.add(disposs.get(0));
				inverse.add(disposs.get(2));
				inverse.add(disposs.get(3));
				dispositionsnotcoexistence.add(disposs);
				invdispositionsnotcoexistence.add(inverse);
			}else if(tempLower.equals("succession")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionssuccession.add(disposs);
			}else if(tempLower.equals("not succession")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsnotsuccession.add(disposs);
			}else if(tempLower.equals("chain response")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionschainresponse.add(disposs);
			}else if(tempLower.equals("not chain response")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsnotchainresponse.add(disposs);
			}else if(tempLower.equals("chain precedence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionschainprecedence.add(disposs);
			}else if(tempLower.equals("not chain precedence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsnotchainprecedence.add(disposs);
			}else if(tempLower.equals("chain succession")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionschainsuccession.add(disposs);
			}else if(tempLower.equals("not chain succession")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsnotchainsuccession.add(disposs);
			}else if(tempLower.equals("alternate response")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsalternateresponse.add(disposs);
			}else if(tempLower.equals("alternate precedence")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsalternateprecedence.add(disposs);
			}else if(tempLower.equals("alternate succession")){
				disposs.add(cd.getDisplay());
				disposs.add(condi);
				dispositionsalternatesuccession.add(disposs);
			}
			//}
		}

		for(String temp : templates){
			String tempLower = temp.toLowerCase();
			if(tempLower.equals("response")){
				ResponseAnalyzer replayer = new ResponseAnalyzer(dispositionsresponse);
				replayers.put("response", replayer);
			}else if(tempLower.equals("not response")){
				NotResponseAnalyzer replayer = new NotResponseAnalyzer(dispositionsnotresponse);
				replayers.put("not response", replayer);
			}else if(tempLower.equals("precedence")){
				PrecedenceAnalyzer replayer = new PrecedenceAnalyzer(dispositionsprecedence);
				replayers.put("precedence", replayer);
			}else if(tempLower.equals("not precedence")){
				NotPrecedenceAnalyzer replayer = new NotPrecedenceAnalyzer(dispositionsnotprecedence);
				replayers.put("not precedence", replayer);
			}else if(tempLower.equals("exclusive choice")){
				RespondedExistenceAnalyzer replayer = new RespondedExistenceAnalyzer(dispositionsexclusivechoice);
				replayers.put("exclusive choice", replayer);
				RespondedExistenceAnalyzer replayerinv = new RespondedExistenceAnalyzer(invdispositionsexclusivechoice);
				replayers.put("inv exclusive choice", replayerinv);
			}else if(tempLower.equals("choice")){
				RespondedExistenceAnalyzer replayer = new RespondedExistenceAnalyzer(dispositionschoice);
				replayers.put("choice", replayer);
				RespondedExistenceAnalyzer replayerinv = new RespondedExistenceAnalyzer(invdispositionschoice);
				replayers.put("inv choice", replayerinv);
			}else if(tempLower.equals("init")){
				InitAnalyzer replayer = new InitAnalyzer(dispositionsinit);
				replayers.put("init", replayer);
			}else if(tempLower.equals("strong init")){
				InitAnalyzer replayer = new InitAnalyzer(dispositionsstronginit);
				replayers.put("strong init", replayer);
			}else if(tempLower.equals("absence")){
				AbsenceAnalyzer replayer = new AbsenceAnalyzer(dispositionsabsence);
				replayers.put("absence", replayer);
			}else if(tempLower.equals("absence2")){
				Absence2Analyzer replayer = new Absence2Analyzer(dispositionsabsence2);
				replayers.put("absence2", replayer);
			}else if(tempLower.equals("absence3")){
				Absence3Analyzer replayer = new Absence3Analyzer(dispositionsabsence3);
				replayers.put("absence3", replayer);
			}else if(tempLower.equals("existence")){
				ExistenceAnalyzer replayer = new ExistenceAnalyzer(dispositionsexistence);
				replayers.put("existence", replayer);
			}else if(tempLower.equals("existence2")){
				Existence2Analyzer replayer = new Existence2Analyzer(dispositionsexistence2);
				replayers.put("existence2", replayer);
			}else if(tempLower.equals("existence3")){
				Existence3Analyzer replayer = new Existence3Analyzer(dispositionsexistence3);
				replayers.put("existence3", replayer);
			}else if(tempLower.equals("exactly1")){
				Exactly1Analyzer replayer = new Exactly1Analyzer(dispositionsexactly1);
				replayers.put("exactly1", replayer);
			}else if(tempLower.equals("exactly2")){
				Exactly2Analyzer replayer = new Exactly2Analyzer(dispositionsexactly2);
				replayers.put("exactly2", replayer);
			}else if(tempLower.equals("responded existence")){
				RespondedExistenceAnalyzer replayer = new RespondedExistenceAnalyzer(dispositionsrespondedexistence);
				replayers.put("responded existence", replayer);
			}else if(tempLower.equals("not responded existence")){
				RespondedExistenceAnalyzer replayer = new RespondedExistenceAnalyzer(dispositionsnotrespondedexistence);
				replayers.put("not responded existence", replayer);
			}else if(tempLower.equals("co-existence")){
				RespondedExistenceAnalyzer replayer = new RespondedExistenceAnalyzer(dispositionscoexistence);
				replayers.put("co-existence", replayer);
				RespondedExistenceAnalyzer replayerinv = new RespondedExistenceAnalyzer(invdispositionscoexistence);
				replayers.put("inv co-existence", replayerinv);
			}else if(tempLower.equals("not co-existence")){
				RespondedExistenceAnalyzer replayer = new RespondedExistenceAnalyzer(dispositionsnotcoexistence);
				replayers.put("not co-existence", replayer);
				RespondedExistenceAnalyzer replayerinv = new RespondedExistenceAnalyzer(invdispositionsnotcoexistence);
				replayers.put("inv not co-existence", replayerinv);
			}else if(tempLower.equals("succession")){
				ResponseAnalyzer replayerResponse = new ResponseAnalyzer(dispositionssuccession);
				replayers.put("succession r", replayerResponse);
				PrecedenceAnalyzer replayerPrecedence = new PrecedenceAnalyzer(dispositionssuccession);
				replayers.put("succession p", replayerPrecedence);
			}else if(tempLower.equals("not succession")){
				NotResponseAnalyzer replayerNotResponse = new NotResponseAnalyzer(dispositionsnotsuccession);
				replayers.put("not succession r", replayerNotResponse);
				NotPrecedenceAnalyzer replayerNotPrecedence = new NotPrecedenceAnalyzer(dispositionsnotsuccession);
				replayers.put("not succession p", replayerNotPrecedence);
			}else if(tempLower.equals("chain response")){
				ChainResponseAnalyzer replayer = new ChainResponseAnalyzer(dispositionschainresponse);
				replayers.put("chain response", replayer);
			}else if(tempLower.equals("not chain response")){
				NotChainResponseAnalyzer replayer = new NotChainResponseAnalyzer(dispositionsnotchainresponse);
				replayers.put("not chain response", replayer);
			}else if(tempLower.equals("chain precedence")){
				ChainPrecedenceAnalyzer replayer = new ChainPrecedenceAnalyzer(dispositionschainprecedence);
				replayers.put("chain precedence", replayer);
			}else if(tempLower.equals("not chain precedence")){
				NotChainPrecedenceAnalyzer replayer = new NotChainPrecedenceAnalyzer(dispositionsnotchainprecedence);
				replayers.put("not chain precedence", replayer);
			}else if(tempLower.equals("chain succession")){
				ChainResponseAnalyzer replayerResponse = new ChainResponseAnalyzer(dispositionschainsuccession);
				replayers.put("chain succession r", replayerResponse);
				ChainPrecedenceAnalyzer replayerPrecedence = new ChainPrecedenceAnalyzer(dispositionschainsuccession);
				replayers.put("chain succession p", replayerPrecedence);
			}else if(tempLower.equals("alternate response")){
				AlternateResponseAnalyzer replayer = new AlternateResponseAnalyzer(dispositionsalternateresponse);
				replayers.put("alternate response", replayer); 
			}else if(tempLower.equals("alternate precedence")){
				AlternatePrecedenceAnalyzer replayer = new AlternatePrecedenceAnalyzer(dispositionsalternateprecedence);
				replayers.put("alternate precedence", replayer);
			}else if(tempLower.equals("alternate succession")){
				AlternateResponseAnalyzer replayerResponse = new AlternateResponseAnalyzer(dispositionsalternatesuccession);
				replayers.put("alternate succession r", replayerResponse);
				AlternatePrecedenceAnalyzer replayerPrecedence = new AlternatePrecedenceAnalyzer(dispositionsalternatesuccession);
				replayers.put("alternate succession p", replayerPrecedence);
			}else if(tempLower.equals("not chain succession")){
				NotChainResponseAnalyzer replayerNotResponse = new NotChainResponseAnalyzer(dispositionsnotchainsuccession);
				replayers.put("not chain succession r", replayerNotResponse);
				NotChainPrecedenceAnalyzer replayerNotPrecedence = new NotChainPrecedenceAnalyzer(dispositionsnotchainsuccession);
				replayers.put("not chain succession p", replayerNotPrecedence);
			}
		}
	}
}
