package org.processmining.plugins.declare;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import org.processmining.plugins.declareminer.visualizing.ActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.AssignmentModel;
import org.processmining.plugins.declareminer.visualizing.AssignmentModelView;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.ConstraintTemplate;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.IItem;
import org.processmining.plugins.declareminer.visualizing.Language;
import org.processmining.plugins.declareminer.visualizing.LanguageGroup;
import org.processmining.plugins.declareminer.visualizing.Parameter;
import org.processmining.plugins.declareminer.visualizing.TemplateBroker;
import org.processmining.plugins.declareminer.visualizing.XMLBrokerFactory;

import utils.Triple;

/**
 * 
 * @author Andrea Burattin
 */
public class DeclareDesignerPlugin {

	private HashMap<DeclareTemplate, ConstraintTemplate> declareTemplateConstraintTemplateMap = new HashMap<DeclareTemplate, ConstraintTemplate>();
	private HashMap<String, DeclareTemplate> templateNameStringDeclareTemplateMap = new HashMap<String, DeclareTemplate>();
	private Language lang;

	@Plugin(
		name = "Simple Declare Designer",
		parameterLabels = { },
		returnLabels = { "Declare Model" },
		returnTypes = { DeclareMap.class },
		userAccessible = true
	)
	@UITopiaVariant(
		author = "A. Burattin, F.M. Maggi",
		email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
		affiliation = "University of Padua and Tartu"
	)
	public DeclareMap generateDeclareModel(UIPluginContext context) {
		readConstraintTemplates();
		ConstraintsConfigurator configuration = new ConstraintsConfigurator();
		context.showConfiguration("Configure Constraints", configuration);
		return getModel(context, configuration);
	}
	
	@Plugin(
		name = "Simple Declare Editor",
		parameterLabels = { "Source Model" },
		returnLabels = { "Declare Model" },
		returnTypes = { DeclareMap.class },
		userAccessible = true
	)
	@UITopiaVariant(
		author = "A. Burattin, F.M. Maggi",
		email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
		affiliation = "University of Padua and Tartu"
	)
	public DeclareMap editDeclareModel(UIPluginContext context, DeclareMap source) {
		readConstraintTemplates();
		ConstraintsConfigurator configuration = new ConstraintsConfigurator();
		Pattern p = Pattern.compile("\\[(.*?)\\]");
		
		for(ConstraintDefinition cd : source.getModel().getConstraintDefinitions()) {
			String name = cd.getName();
			
			String[] activities = {"", ""};
			Matcher m1 = p.matcher(cd.getCaption().replace(cd.getName() + ": ", ""));
			int i = 0;
			while(m1.find()) {
				activities[i++] = m1.group(1);
			}
			
			String[] conditions = {"", "", ""};
			Matcher m2 = p.matcher(cd.getCondition().toString());
			i = 0;
			while(m2.find()) {
				conditions[i++] = m2.group(1);
			}
			configuration.addConstraint(templateNameStringDeclareTemplateMap.get(name).name(), activities[0], activities[1], conditions[0], conditions[1], conditions[2]);
		}
		
		context.showConfiguration("Configure Constraints", configuration);
		return getModel(context, configuration);
	}
	
	private DeclareMap getModel(UIPluginContext context, ConstraintsConfigurator configuration) {		
		// start the actual creation of the model
		AssignmentModel model = new AssignmentModel(lang);
		AssignmentModelView view = new AssignmentModelView(model);
		model.setName("hand made model");
		
		/* add activities */
		int actsCounter = 1;
		HashMap<String, Pair<Integer, ActivityDefinition>> acts = new HashMap<String, Pair<Integer, ActivityDefinition>>();
		for (Triple<String, Pair<String, String>, Triple<String, String, String>> cd : configuration.getConstraints()) {
			String actName = cd.getSecond().getFirst();
			if (!acts.containsKey(actName)) {
				ActivityDefinition ad = model.addActivityDefinition(actsCounter);
				ad.setName(actName);
				acts.put(actName, new Pair<Integer, ActivityDefinition>(actsCounter, ad));
				actsCounter++;
			}
			actName = cd.getSecond().getSecond();
			if (!acts.containsKey(actName)) {
				ActivityDefinition ad = model.addActivityDefinition(actsCounter);
				ad.setName(actName);
				acts.put(actName, new Pair<Integer, ActivityDefinition>(actsCounter, ad));
				actsCounter++;
			}
		}
		
		/* add constraints */
		int constraintId = 0;
		for (Triple<String, Pair<String, String>, Triple<String, String, String>> cd : configuration.getConstraints()) {
			try {
				DeclareTemplate t = DeclareTemplate.valueOf(cd.getFirst());
				ConstraintDefinition constraintdefinition = new ConstraintDefinition(constraintId, model, declareTemplateConstraintTemplateMap.get(t));
				
				Collection<Parameter> parameters = (declareTemplateConstraintTemplateMap.get(t)).getParameters();
				Iterator<Parameter> iter = parameters.iterator();
				
				constraintdefinition.addBranch(
						iter.next(),
						acts.get(cd.getSecond().getFirst()).getSecond());
				if (!cd.getSecond().getSecond().isEmpty()) {
					constraintdefinition.addBranch(
							iter.next(),
							acts.get(cd.getSecond().getSecond()).getSecond());
				}
				
				constraintdefinition.getCondition().setText("[" + cd.getThird().getFirst() + "][" + cd.getThird().getSecond() + "][" + cd.getThird().getThird() + "]");
				model.addConstraintDefiniton(constraintdefinition);
				constraintId++;
			} catch(Exception e) {
				e.printStackTrace();
				context.log("ERROR: parameter configuration error for constraint `" + cd.getFirst() + "'");
			}
		}
		
		view.updateUI();
		
		DeclareMap decModel = new DeclareMap(model, null, view, null, null, null);
		System.out.println("No. Constraints: "+model.constraintDefinitionsCount()+"-- No. Activities: "+model.activityDefinitionsCount());
		
		return decModel;
	}
	
	private void readConstraintTemplates() {
		DeclareTemplate declareTemplate = DeclareTemplate.Absence;
		DeclareTemplate[] declareTemplateNames = declareTemplate.getDeclaringClass().getEnumConstants();
		for(DeclareTemplate d : declareTemplateNames){
			String templateNameString = d.toString().replaceAll("_", " ").toLowerCase();
			templateNameStringDeclareTemplateMap.put(templateNameString, d);
		}
		
		InputStream templateInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("resources/template.xml");
		File languageFile = null;
		try {
			languageFile = File.createTempFile("template", ".xml");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(templateInputStream));
			String line = bufferedReader.readLine();
			PrintStream out = new PrintStream(languageFile);
			while (line != null) {
				out.println(line);
				line = bufferedReader.readLine();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		TemplateBroker templateBroker = XMLBrokerFactory.newTemplateBroker(languageFile.getAbsolutePath());
		List<Language> languagesList = templateBroker.readLanguages();

		//the first language in the list is the Condec language, which is what we need
		lang = languagesList.get(0);
		Language condecLanguage = languagesList.get(0);
		List<IItem> templateList = new ArrayList<IItem>();
		List<IItem> condecLanguageChildrenList = condecLanguage.getChildren();
		for (IItem condecLanguageChild : condecLanguageChildrenList) {
			if (condecLanguageChild instanceof LanguageGroup) {
				templateList.addAll(visit(condecLanguageChild));
			} else {
				templateList.add(condecLanguageChild);
			}
		}

		declareTemplateConstraintTemplateMap = new HashMap<DeclareTemplate, ConstraintTemplate>();

		for(IItem item : templateList){
			if(item instanceof ConstraintTemplate){
				ConstraintTemplate constraintTemplate = (ConstraintTemplate)item;
				//			System.out.println(constraintTemplate.getName()+" @ "+constraintTemplate.getDescription()+" @ "+constraintTemplate.getText());
				if(templateNameStringDeclareTemplateMap.containsKey(constraintTemplate.getName().replaceAll("-", "").toLowerCase())){
					declareTemplateConstraintTemplateMap.put(templateNameStringDeclareTemplateMap.get(constraintTemplate.getName().replaceAll("-", "").toLowerCase()), constraintTemplate);
					System.out.println(
							constraintTemplate.getName()+" @ "+
									templateNameStringDeclareTemplateMap.get(constraintTemplate.getName().replaceAll("-", "").toLowerCase()));
				}
			}
		}
	}
	
	private List<IItem> visit(IItem item){
		List<IItem> templateList = new ArrayList<IItem>();
		if (item instanceof LanguageGroup) {
			LanguageGroup languageGroup = (LanguageGroup) item;
			List<IItem> childrenList = languageGroup.getChildren();
			for (IItem child : childrenList) {
				if (child instanceof LanguageGroup) {
					templateList.addAll(visit(child));
				} else {
					templateList.add(child);
				}
			}
		}
		return templateList;
	}
}
