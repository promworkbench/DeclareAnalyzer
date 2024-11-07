package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.processmining.ltl2automaton.plugins.automaton.Automaton;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.ltl2automaton.plugins.formula.DefaultParser;
import org.processmining.ltl2automaton.plugins.formula.Formula;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeLeaf;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeNode;
import org.processmining.ltl2automaton.plugins.formula.conjunction.DefaultTreeFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.GroupedTreeConjunction;
import org.processmining.ltl2automaton.plugins.formula.conjunction.TreeFactory;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;

public class ActivationGenerator {
	private Vector visited = new Vector();
	private Automaton pAut;
	ExecutableAutomaton automaton;
	ArrayList<String> language;
	ArrayList<String> activations;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ActivationGenerator ag = new ActivationGenerator();
		//String[] parameters = {"ask","reply"};
		//String formulaName = "response";
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Insert formula");
		String formulaName;
		try {
			formulaName = in.readLine();
			System.out.println("Insert parameters separated by commas");
			String[] parameters= in.readLine().split(",");
			ArrayList<String> result = ag.generate(formulaName,parameters);
			for(String activation: result){
				if(!activation.equals("&end;"))
					System.out.println(activation);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> generate(String formulaName, String[] parameters){

		String formula = "";
		language = new ArrayList<String>();
		activations = new ArrayList<String>();
		for(int i = 0; i<parameters.length; i++){
			language.add(parameters[i]);
		}
		if (formulaName.startsWith("strong init")) {
			formula = "(\""+parameters[0]+"\")";
		}else if (formulaName.startsWith("init")) {
			formula = "(\""+parameters[0]+"\")";
		}else if (formulaName.startsWith("absence")) {
			formula = "!( <> ( \""+parameters[0]+"\" ) )";
		}else if (formulaName.startsWith("existence")) {
			formula = "( <> ( \""+parameters[0]+"\" ) )";
		}else if (formulaName.startsWith("co-existence")) {
			formula = "(( ( <>( \""+parameters[0]+"\" ) <-> (<>( \""+parameters[1]+"\" ) )) ))";
		}else if (formulaName.startsWith("responded existence")) {
			formula = "(( ( <>( \""+parameters[0]+"\" ) -> (<>( \""+parameters[1]+"\" ) )) ))";
		}else if (formulaName.startsWith("precedence")) {
			formula = "( ! (\""+parameters[1]+"\" ) U \""+parameters[0]+"\" ) \\/ ([](!(\""+parameters[1]+"\"))) /\\ ! (\""+parameters[1]+"\" )";
		}else if (formulaName.startsWith("response")) {
			formula = "( []( ( \""+parameters[0]+"\" -> <>( \""+parameters[1]+"\" ) ) ))";// /\\ (( ! (\""+parameters[1]+"\" ) U \""+parameters[0]+"\" ) \\/ ([](!(\""+parameters[1]+"\"))) /\\ ! (\""+parameters[1]+"\" ))"; 
		}else if (formulaName.startsWith("succession")) {
			formula = "(( []( ( \""+parameters[0]+"\" -> <>( \""+parameters[1]+"\" ) ) ))) /\\ (( ! (\""+parameters[1]+"\" ) U \""+parameters[0]+"\" ) \\/ ([](!(\""+parameters[1]+"\"))) /\\ ! (\""+parameters[1]+"\" ))";
		}else if (formulaName.startsWith("alternate precedence")) {
			formula = "(( ( (! ( \""+parameters[1]+"\" ) U \""+parameters[0]+"\") \\/ ([](!(\""+parameters[1]+"\"))) ) /\\ [] ( ( \""+parameters[1]+"\" -> X( ( ( ! ( \""+parameters[1]+"\" ) U \""+parameters[0]+"\" )\\/([](!(\""+parameters[1]+"\"))) )) ) ) ) /\\ (! (\""+parameters[1]+"\" )))";
		}else if (formulaName.startsWith("alternate response")) {
			formula = "( []( ( \""+parameters[0]+"\" -> X(( ! ( \""+parameters[0]+"\" ) U \""+parameters[1]+"\" ) )) ) )";// /\\ ( ( (! ( \""+parameters[1]+"\" ) U \""+parameters[0]+"\") \\/ ([](!(\""+parameters[1]+"\"))) ) /\\ [] ( ( \""+parameters[1]+"\" -> X( ( ( ! ( \""+parameters[1]+"\" ) U \""+parameters[0]+"\" )\\/([](!(\""+parameters[1]+"\"))) )) ) ) )" ;
		}else if (formulaName.startsWith("alternate succession")) {
			formula = "(( []( ( \""+parameters[0]+"\" -> X(( ! ( \""+parameters[0]+"\" ) U \""+parameters[1]+"\" ) )) ) )) /\\ ( ( ( (! ( \""+parameters[1]+"\" ) U \""+parameters[0]+"\") \\/ ([](!(\""+parameters[1]+"\"))) ) /\\ [] ( ( \""+parameters[1]+"\" -> X( ( ( ! ( \""+parameters[1]+"\" ) U \""+parameters[0]+"\" )\\/([](!(\""+parameters[1]+"\"))) )) ) ) )  /\\ (! (\""+parameters[1]+"\" )))";
		}else if (formulaName.startsWith("alternate")) {
			formula = "[]( ( \""+parameters[0]+"\" -> X(( (!(\""+parameters[0]+"\") U \""+parameters[1]+"\") \\/ ([](!(\""+parameters[0]+"\"))) ))))";
		}else if (formulaName.startsWith("chain response")) {
			formula = "[] ( ( \""+parameters[0]+"\" -> X( \""+parameters[1]+"\" ) ) )";
		}else if (formulaName.startsWith("chain precedence")) {
			formula = "[]( ( X( \""+parameters[1]+"\" ) -> \""+parameters[0]+"\") )/\\ ! (\""+parameters[1]+"\" )";
		}else if (formulaName.startsWith("chain succession")) {
			formula = "([]( ( \""+parameters[0]+"\" -> X( \""+parameters[1]+"\" ) ) )) /\\ ([]( ( X( \""+parameters[1]+"\" ) ->  \""+parameters[0]+"\") ) /\\ ! (\""+parameters[1]+"\" ))";
		}else if (formulaName.startsWith("not co-existence")) {
			formula = "(( ( <>( \""+parameters[0]+"\" ) -> (!<>( \""+parameters[1]+"\" ) )) )) /\\ (( ( <>( \""+parameters[1]+"\" ) -> (!<>( \""+parameters[0]+"\" ) )) ))";
		}else if (formulaName.startsWith("not succession")) {
			formula = "[]( ( \""+parameters[0]+"\" -> !(<>( \""+parameters[1]+"\" ) ) ))";
		}else if (formulaName.startsWith("not chain succession")) {
			formula = "[]( ( \""+parameters[0]+"\" -> !(X( \""+parameters[1]+"\" ) ) ))";
		}else if (formulaName.startsWith("not response")) {
			formula = "[]( ( \""+parameters[0]+"\" -> !(<>( \""+parameters[1]+"\" ) ) ))";
		}else if (formulaName.startsWith("not chain response")) {
			formula = "[]( ( \""+parameters[0]+"\" -> !(X( \""+parameters[1]+"\" ) ) ))";
		}else if (formulaName.startsWith("not precedence")) {
			formula = "[]( ( \""+parameters[0]+"\" -> !(<>( \""+parameters[1]+"\" ) ) ))";
		}else if (formulaName.startsWith("not chain precedence")) {
			formula = "[]( ( \""+parameters[0]+"\" -> !(X( \""+parameters[1]+"\" ) ) ))";
		}else{
			formula = formulaName;
		}

		String currentF = formula;
		currentF = currentF.replace("/\\ event==COMPLETE", "\"");
		currentF = currentF.replace("/\\ event==complete", "\"");
		currentF = currentF.replace("activity==", "\"");
		currentF = currentF.replace("_O", "X");
		currentF = currentF.replace("U_", "U");
		currentF = currentF.replace("<->", "=");

		List<Formula> formulaeParsed = new ArrayList<Formula>();
		try {
			formulaeParsed.add(new DefaultParser(currentF).parse());
		} catch (SyntaxParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
		ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction
				.getFactory(treeFactory);
		GroupedTreeConjunction conjunction = conjunctionFactory.instance(formulaeParsed);
		pAut = conjunction.getAutomaton().op.reduce();
		automaton = new ExecutableAutomaton(pAut);
		Vector trace = new Vector();
		visit(trace);
		ArrayList<String> output = new ArrayList<String>();  
		for(String act : activations){
			if(!output.contains(act)){
				output.add(act);
			}
		}
		return output;
	}

	private void visit(Vector trace){
		automaton.ini();
		boolean isInitial = true;
		for(Object o: trace){
			automaton.next((String)o);
			isInitial = false;
		}
		if(isInitial){
			ArrayList<String> initialObligations = getObligations();
			if(initialObligations.size()>0){
				activations.addAll(initialObligations);
			}
		}
		PossibleNodes state = automaton.currentState();
		if(!visited.contains(state)){
			visited.add(state);
			ArrayList<String> oldObligations = getObligations();
			for(Transition t: state.output()){
				Vector currentTrace = new Vector();
				for(Object o: trace){
					currentTrace.add(o);
				}
				if(t.isAll()&&state.isAccepting()){
					//return;
				}
				else if(t.isPositive()){
					String pos = t.getPositiveLabel();
					automaton.next(pos);
					ArrayList<String> currentObligations = getObligations();
					currentObligations.removeAll(oldObligations);
					if(currentObligations.size()>0){
						activations.add(pos);
						activations.addAll(currentObligations);
					}
					currentTrace.add(pos);
					visit(currentTrace);
					automaton.ini();
					for(Object o: trace){
						automaton.next((String)o);
					}
				}
				else if(t.isNegative()){
					for(String neg: t.getNegativeLabels()){
						currentTrace = new Vector();
						for(Object o: trace){
							currentTrace.add(o);
						}
						oldObligations = getObligations();
						automaton.next("!"+neg);
						ArrayList<String> currentObligations = getObligations();
						currentObligations.removeAll(oldObligations);
						if(currentObligations.size()>0){
						//	activations.add(neg);
							activations.addAll(currentObligations);
						}
						currentTrace.add("!"+neg);
						visit(currentTrace);
						automaton.ini();
						for(Object o: trace){
							automaton.next((String)o);
						}
					}
				}
			}
		}
		return;
	}


	private ArrayList<String> getObligations(){
		ArrayList<String> output = new ArrayList<String>(); 
		for(String word : language){
			if(!automaton.currentState().parses(word)){
				output.add(word);
			}
		}
		if(!automaton.currentState().isAccepting()){
			output.add("&end;");
		}
		return output;
	}

}