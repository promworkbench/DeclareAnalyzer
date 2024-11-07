package org.processmining.plugins.declareanalyzer.gui.widget;


public class MultilineListEntry<T> {
	
	private T object;
	private String firstLine;
	private String secondLine;

	public MultilineListEntry(T object, String firstLine, String secondLine) {
		this.object = object;
		this.firstLine = firstLine;
		this.secondLine = secondLine;
	}
	
	public MultilineListEntry(T object, String firstLine) {
		this(object, firstLine, "");
	}

	public String getFirstLine() {
		return this.firstLine;
	}

	public String getSecondLine() {
		return this.secondLine;
	}
	
	public T getObject() {
		return this.object;
	}

	@Override
	public String toString() {
		return this.firstLine + "\n" + this.secondLine;
	}
}
