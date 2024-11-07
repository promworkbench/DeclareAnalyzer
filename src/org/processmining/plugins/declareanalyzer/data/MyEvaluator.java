package org.processmining.plugins.declareanalyzer.data;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;
import utils.Triple;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class MyEvaluator {

	private static boolean USE_SMART_EVALUATION = true;
	private static int CACHE_SIZE = 1000;
	private static boolean RETURN_VALUE_IF_ERROR = false;
	
	private static Evaluator eval = new Evaluator();
	private static LoadingCache<Triple<Map<String,String>, Map<String, Class<?>>, String>, Boolean> cache = CacheBuilder
			.newBuilder()
			.maximumSize(CACHE_SIZE)
			.build(new CacheLoader<Triple<Map<String,String>, Map<String, Class<?>>, String>, Boolean>() {
				@Override
				public Boolean load(Triple<Map<String,String>, Map<String, Class<?>>, String> triple) throws Exception {
					Map<String,String> variables = triple.getFirst();
					Map<String, Class<?>> variableTypes = triple.getSecond();
					String expression = triple.getThird();
					try {
						StringBuilder variablesBuffer = new StringBuilder();
						for(String variableName : variables.keySet()) {
							variablesBuffer.append(variableName);
							variablesBuffer.append("|");
						}
						if (variablesBuffer.length() > 1) {
							variablesBuffer.insert(0, "(");
							variablesBuffer.setLength(variablesBuffer.length() - 1);
							variablesBuffer.append(")");
							expression = expression.replaceAll(variablesBuffer.toString(), "#{$1}");
						}
						
						for(String variableName : variables.keySet()) {
							String variableNameForTypeVerification = variableName;
							String prefix = (variableName.length() > 2)? variableName.substring(0, 2) : "";
							if (prefix.equals("A.") || prefix.equals("T.")) {
								variableNameForTypeVerification = variableName.substring(2);
							}
							if (expression.contains(variableName)) {
								String variableValue = variables.get(variableName);
								Class<?> variableType = variableTypes.get(variableNameForTypeVerification);
//								System.out.println("checking type for " + variableNameForTypeVerification + " - " + variableType);
								if (Boolean.class.equals(variableType)) {
									// boolean
									expression = expression.replace("#{" + variableName + "}", variableValue);
								} else if (Double.class.equals(variableType) || Long.class.equals(variableType)) {
									// numeric
									expression = expression.replace("#{" + variableName + "}", variableValue);
								} else if (Calendar.class.equals(variableType)) {
									// date
									if (variableValue.startsWith("'") && variableValue.endsWith("'")) {
										expression = expression.replace("#{" + variableName + "}", variableValue);
									} else {
										expression = expression.replace("#{" + variableName + "}", "'" + variableValue + "'");
									}
								} else {
									// string
									if (variableValue.startsWith("'") && variableValue.endsWith("'")) {
										expression = expression.replace("#{" + variableName + "}", variableValue);
									} else {
										expression = expression.replace("#{" + variableName + "}", "'" + variableValue + "'");
									}
								}
							}
						}
//						System.out.println("expr to evaluate: " + expression);
//						System.out.println("--------");
						return eval.getBooleanResult(expression);
					} catch (EvaluationException e) {
						e.printStackTrace();
					}
					return RETURN_VALUE_IF_ERROR;
				}
			});
	
	/*public static void main(String[] args) {
		Map<String,String> variables = new HashMap<String, String>();
		variables.put("A.org:role", "5");
		
		Map<String, Class<?>> variableTypes = new HashMap<String, Class<?>>();
		variableTypes.put("A.org:role", Long.class);
		
		String expression = "A.org:role <= 5";
		
		long t = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++) {
			evaluateExpression(variables, variableTypes, expression);
		}
		System.out.println("evaluation: " + (System.currentTimeMillis() - t) + "ms");
	}*/
	
	public static boolean evaluateExpression(Map<String,String> variables, Map<String, Class<?>> variableTypes, String expression) {
//		System.out.println(variables);
//		System.out.println(variableTypes);
//		System.out.println(expression);
		if (USE_SMART_EVALUATION) {
			return smartEvaluateExpression(variables, variableTypes, expression);
		}
		return lazyEvaluateExpression(variables, variableTypes, expression);
	}
	
	private static boolean lazyEvaluateExpression(Map<String,String> variables, Map<String, Class<?>> variableTypes, String expression) {
		StringBuilder bufferTypeStrings = new StringBuilder();
		StringBuilder bufferTypeOthers = new StringBuilder();
		for(String variableName : variables.keySet()) {
			if (String.class.equals(variableTypes.get(variableName))) {
				bufferTypeStrings.append(variableName);
				bufferTypeStrings.append("|");
			} else {
				bufferTypeOthers.append(variableName);
				bufferTypeOthers.append("|");
			}
		}
		if (bufferTypeStrings.length() > 1) {
			bufferTypeStrings.insert(0, "(");
			bufferTypeStrings.setLength(bufferTypeStrings.length() - 1);
			bufferTypeStrings.append(")");
			expression = expression.replaceAll(bufferTypeStrings.toString(), "'#{$1}'");
		}
		if (bufferTypeOthers.length() > 1) {
			bufferTypeOthers.insert(0, "(");
			bufferTypeOthers.setLength(bufferTypeOthers.length() - 1);
			bufferTypeOthers.append(")");
			expression = expression.replaceAll(bufferTypeOthers.toString(), "#{$1}");
		}
		
		boolean result = RETURN_VALUE_IF_ERROR;
		try {
			eval.setVariables(variables);
			result = eval.getBooleanResult(expression);
		} catch (EvaluationException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private static boolean smartEvaluateExpression(Map<String,String> variables, Map<String, Class<?>> variableTypes, String expression) {
		boolean result = RETURN_VALUE_IF_ERROR;
		try {
			result = cache.get(new Triple<Map<String,String>, Map<String, Class<?>>, String>(variables, variableTypes, expression));
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}
}
