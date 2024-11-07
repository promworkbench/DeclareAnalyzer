package org.processmining.plugins.declareanalyzer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.framework.util.ui.widgets.WidgetColors;
import org.processmining.plugins.declareanalyzer.AnalysisResult;
import org.processmining.plugins.declareanalyzer.AnalysisSingleResult;
import org.processmining.plugins.declareanalyzer.gui.widget.ConstraintsViolationsVisualizer;
import org.processmining.plugins.declareanalyzer.gui.widget.MultilineListEntry;
import org.processmining.plugins.declareanalyzer.gui.widget.MultilineListEntryRenderer;
import org.processmining.plugins.declareanalyzer.gui.widget.SingleResultVisualizer;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

import utils.GUIUtils;
import utils.Triple;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.components.SlickerSearchField;
import com.fluxicon.slickerbox.components.SlickerTabbedPane;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.fluxicon.slickerbox.ui.SlickerScrollBarUI;
import com.lowagie.text.Font;

/**
 * Plugin for the visualization of the result of the analysis
 * 
 * @author Andrea Burattin
 * @author Fabrizio Maggi
 */
public class AnalysisResultVisualizer extends SlickerTabbedPane {

	private static final long serialVersionUID = 321944298882508667L;
	private static HashMap<String, Comparator<ConstraintsViolationsVisualizer>> comparators = new HashMap<String, Comparator<ConstraintsViolationsVisualizer>>();
	
	static {
		comparators.put("Trace name", new Comparator<ConstraintsViolationsVisualizer>() {
			public int compare(ConstraintsViolationsVisualizer o1, ConstraintsViolationsVisualizer o2) {
				return o1.getTraceName().compareTo(o2.getTraceName());
			}
		});
		comparators.put("Activations number", new Comparator<ConstraintsViolationsVisualizer>() {
			public int compare(ConstraintsViolationsVisualizer o1, ConstraintsViolationsVisualizer o2) {
				if (o1.getTotActivations() < o2.getTotActivations()) {
					return 1;
				} else if (o1.getTotActivations() > o2.getTotActivations()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		comparators.put("Violations number", new Comparator<ConstraintsViolationsVisualizer>() {
			public int compare(ConstraintsViolationsVisualizer o1, ConstraintsViolationsVisualizer o2) {
				if (o1.getTotViolations() < o2.getTotViolations()) {
					return 1;
				} else if (o1.getTotViolations() > o2.getTotViolations()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		comparators.put("Fulfilments number", new Comparator<ConstraintsViolationsVisualizer>() {
			public int compare(ConstraintsViolationsVisualizer o1, ConstraintsViolationsVisualizer o2) {
				if (o1.getTotFulfilments() < o2.getTotFulfilments()) {
					return 1;
				} else if (o1.getTotFulfilments() > o2.getTotFulfilments()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}
	
	private AnalysisResult result;
	private String filter;
	
	private DefaultListModel caseIdsListModel = new DefaultListModel();
	private DefaultListModel constraintsListModel = new DefaultListModel();
	private ProMList<String> caseIdsList;
	private ProMList<String> constraintsList;
	private JPanel caseDetails;
	private JPanel tracesContainer;
	private Set<Triple<String, String, ConstraintDefinition>> constraints = new HashSet<Triple<String, String, ConstraintDefinition>>();
	private List<ConstraintsViolationsVisualizer> constraintsVisualizerList;
	private Comparator<ConstraintsViolationsVisualizer> currentSorter = comparators.get("Activations number");
	

	public AnalysisResultVisualizer() {
		super("Declare Analyzer result", GUIUtils.panelBackground, Color.lightGray, Color.gray);
		setBackground(Color.black);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setOpaque(true);
	}
	
	@Plugin(
		name = "Analysis Result Visualizer",
		parameterLabels = { "The result of an analysis" },
		returnLabels = { "Analysis visualization" },
		returnTypes = { JComponent.class },
		userAccessible = true
	)
	@UITopiaVariant(
		author = "A. Burattin, F.M. Maggi",
		email = "burattin@math.unipd.it, f.m.maggi@tue.nl",
		affiliation = "UniPD, UniTartu"
	)
	@Visualizer(name = "Analysis Result Visualizer")
	public JComponent visualize(UIPluginContext context, AnalysisResult result) {
		this.result = result;
		this.filter = "";
		
		populateConstraints();
		initComponents();
		
		return this;
	}
	
	private void populateConstraints() {
		Set<XTrace> traces = result.getTraces();
		constraintsVisualizerList = new LinkedList<ConstraintsViolationsVisualizer>();
		
		for (XTrace trace : traces) {
			ConstraintsViolationsVisualizer visualizer = new ConstraintsViolationsVisualizer(trace, result.getResults(trace));
			constraintsVisualizerList.add(visualizer);
		}
	}
	
	private void initComponents() {
		/* add everything to the gui */
		addTab("Overall Details", prepareGeneralDetails());
		addTab("Trace/Constraints Details", prepareDetailedView());
		addTab("Trace/Constraints Overview", prepareOverallView());
		
	}
	
	@SuppressWarnings("cast")
	private JPanel prepareGeneralDetails() {
		GridBagConstraints c;
		
		/* overall details */
		/* ================================================================== */
		RoundedPanel overallDetails = new RoundedPanel(15, 5, 3);
		overallDetails.setLayout(new BorderLayout());
		overallDetails.setBackground(GUIUtils.panelBackground);
		overallDetails.add(GUIUtils.prepareTitle("Constraints details"), BorderLayout.NORTH);
		
		JPanel detailsContainer = new JPanel();
		detailsContainer.setOpaque(false);
		detailsContainer.setLayout(new GridBagLayout());
		
		JScrollPane detailsScrollerContainer = new JScrollPane(detailsContainer);
		detailsScrollerContainer.setOpaque(false);
		detailsScrollerContainer.getViewport().setOpaque(false);
		detailsScrollerContainer.getVerticalScrollBar().setUI(new SlickerScrollBarUI(detailsScrollerContainer.getVerticalScrollBar(), GUIUtils.panelBackground, GUIUtils.panelTextColor, GUIUtils.panelTextColor.brighter(), 4, 11));
		detailsScrollerContainer.getHorizontalScrollBar().setUI(new SlickerScrollBarUI(detailsScrollerContainer.getHorizontalScrollBar(), GUIUtils.panelBackground, GUIUtils.panelTextColor, GUIUtils.panelTextColor.brighter(), 4, 11));
		detailsScrollerContainer.setBorder(BorderFactory.createEmptyBorder());
		overallDetails.add(detailsScrollerContainer, BorderLayout.CENTER);
		
		/* header */
		c = new GridBagConstraints();
		c.insets = new Insets(5, 10, 5, 10);
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.weightx = 1;
		GUIUtils.addToGridBagLayout(0, 0, detailsContainer, GUIUtils.prepareTitleLabel("Constraint"), c);
		c.weightx = 0;
		c.insets = new Insets(5, 10, 5, 10);
		c.anchor = GridBagConstraints.SOUTHEAST;
		GUIUtils.addToGridBagLayout(1, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Activations no."), c);
		GUIUtils.addToGridBagLayout(2, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Violations no."), c);
		GUIUtils.addToGridBagLayout(3, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Fulfilments no."), c);
		GUIUtils.addToGridBagLayout(4, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Conflicts no."), c);
		
		GUIUtils.addToGridBagLayout(5, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Avg. act. sparsity"), c);
		GUIUtils.addToGridBagLayout(6, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Avg. violation ratio"), c);
		GUIUtils.addToGridBagLayout(7, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Avg. fulfilment ratio"), c);
		GUIUtils.addToGridBagLayout(8, 0, detailsContainer, GUIUtils.prepareVericalTitleLabel("Avg. conflict ratio"), c);
		
		/* details */
		Integer totActivations = 0;
		Integer totViolations = 0;
		Integer totFulfilments = 0;
		Integer totConflicts = 0;
		
		Double totActSparsity = 0.0;
		Double totVioRatio = 0.0;
		Double totFulfilRatio = 0.0;
		Double totConfRatio = 0.0;
		
		int i = 0;
		c = new GridBagConstraints();
		for(String constraint : result.getConstraints()) {
			
			totActivations += result.getActivations(constraint);
			totViolations += result.getViolations(constraint);
			totFulfilments += result.getFulfilments(constraint);
			totConflicts += result.getConflicts(constraint);
			
			totActSparsity += result.getAvgActivationSparsity(constraint);
			totVioRatio += (double)result.getViolations(constraint)/(double)result.getActivations(constraint);
			totFulfilRatio += (double)result.getFulfilments(constraint)/(double)result.getActivations(constraint);
			totConfRatio += (double)result.getConflicts(constraint)/(double)result.getActivations(constraint);
			
			c.insets = new Insets(5, 10, 5, 10);
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 1;
			GUIUtils.addToGridBagLayout(0, i+1, detailsContainer, GUIUtils.prepareLabel(constraint), c);
			
			double avgVioRatio = (double)result.getViolations(constraint)/(double)result.getActivations(constraint);
			double avgFulfillRatio = (double)result.getFulfilments(constraint)/(double)result.getActivations(constraint);
			double avgConflictRatio = (double)result.getConflicts(constraint)/(double)result.getActivations(constraint);
			
			c.anchor = GridBagConstraints.EAST;
			GUIUtils.addToGridBagLayout(1, i + 1, detailsContainer, GUIUtils.prepareLabel(result.getActivations(constraint)), c);
			GUIUtils.addToGridBagLayout(2, i + 1, detailsContainer, GUIUtils.prepareLabel(result.getViolations(constraint)), c);
			GUIUtils.addToGridBagLayout(3, i + 1, detailsContainer, GUIUtils.prepareLabel(result.getFulfilments(constraint)), c);
			GUIUtils.addToGridBagLayout(4, i + 1, detailsContainer, GUIUtils.prepareLabel(result.getConflicts(constraint)), c);
			GUIUtils.addToGridBagLayout(5, i + 1, detailsContainer, GUIUtils.prepareLabel(result.getAvgActivationSparsity(constraint), GUIUtils.df2), c);
			GUIUtils.addToGridBagLayout(6, i + 1, detailsContainer, GUIUtils.prepareLabel(avgVioRatio, GUIUtils.df2), c);
			GUIUtils.addToGridBagLayout(7, i + 1, detailsContainer, GUIUtils.prepareLabel(avgFulfillRatio, GUIUtils.df2), c);
			GUIUtils.addToGridBagLayout(8, i + 1, detailsContainer, GUIUtils.prepareLabel(avgConflictRatio, GUIUtils.df2), c);
			
			
			JPanel line = new JPanel();
			line.setBackground(GUIUtils.panelBackground.darker());
			line.setPreferredSize(new Dimension(10, 1));
			c.gridwidth = 9;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			GUIUtils.addToGridBagLayout(0, i + 2, detailsContainer, line, c);
			
			i += 2;
		}
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 10, 0, 10);
		GUIUtils.addToGridBagLayout(0, i + 1, detailsContainer, GUIUtils.prepareTitleLabel("Totals"), c);
		GUIUtils.addToGridBagLayout(0, i + 2, detailsContainer, GUIUtils.prepareTitleLabel("Averages"), c);
		
		c.anchor = GridBagConstraints.EAST;
		GUIUtils.addToGridBagLayout(1, i + 1, detailsContainer, GUIUtils.prepareLabel(totActivations), c);
		GUIUtils.addToGridBagLayout(2, i + 1, detailsContainer, GUIUtils.prepareLabel(totViolations), c);
		GUIUtils.addToGridBagLayout(3, i + 1, detailsContainer, GUIUtils.prepareLabel(totFulfilments), c);
		GUIUtils.addToGridBagLayout(4, i + 1, detailsContainer, GUIUtils.prepareLabel(totConflicts), c);
		
		GUIUtils.addToGridBagLayout(1, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totActivations / ((double)i/2.), GUIUtils.df2), c);
		GUIUtils.addToGridBagLayout(2, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totViolations  / ((double)i/2.), GUIUtils.df2), c);
		GUIUtils.addToGridBagLayout(3, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totFulfilments / ((double)i/2.), GUIUtils.df2), c);
		GUIUtils.addToGridBagLayout(4, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totConflicts / ((double)i/2.), GUIUtils.df2), c);
		
		GUIUtils.addToGridBagLayout(5, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totActSparsity / ((double)i/2.), GUIUtils.df2), c);
		GUIUtils.addToGridBagLayout(6, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totVioRatio / ((double)i/2.), GUIUtils.df2), c);
		GUIUtils.addToGridBagLayout(7, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totFulfilRatio / ((double)i/2.), GUIUtils.df2), c);
		GUIUtils.addToGridBagLayout(8, i + 2, detailsContainer, GUIUtils.prepareLabel((double)totConfRatio / ((double)i/2.), GUIUtils.df2), c);
		
		GUIUtils.addToGridBagLayout(0, i + 3, detailsContainer, Box.createVerticalGlue(), 0, 1);
		
		return overallDetails;
	}
	
	private JPanel prepareOverallView() {
		
		/* specific details per trace */
		/* ================================================================== */
		// sort field
		final ProMComboBox<String> sorter = new ProMComboBox<String>(comparators.keySet());
		sorter.setPreferredSize(new Dimension(200, 25));
		sorter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				currentSorter = comparators.get(sorter.getSelectedItem());
				refreshTraces(tracesContainer, currentSorter);
			}
		});
		
		// search field
		final SlickerSearchField search = new SlickerSearchField(200, 25, WidgetColors.COLOR_LIST_BG, Color.lightGray, Color.lightGray.brighter(), Color.gray);
		search.addSearchListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filter = search.getSearchText();
				if (filter.length() >= 2 || filter.length() == 0) {
					refreshTraces(tracesContainer, currentSorter);
				}
			}
		});
		JPanel searchContainer = new JPanel(new FlowLayout());
		searchContainer.setOpaque(false);
		searchContainer.add(GUIUtils.prepareLabel("Trace filter", SwingConstants.LEFT, Color.lightGray));
		searchContainer.add(search);
		searchContainer.add(GUIUtils.prepareLabel("Sort trace by:", SwingConstants.LEFT, Color.lightGray));
		searchContainer.add(sorter);
		
		JPanel titleContainer = new JPanel(new BorderLayout());
		titleContainer.setOpaque(false);
		titleContainer.add(GUIUtils.prepareTitle("Trace/constraints details"), BorderLayout.WEST);
		titleContainer.add(searchContainer, BorderLayout.EAST);
		
		RoundedPanel traceDetails = new RoundedPanel(15, 5, 3);
		traceDetails.setLayout(new BorderLayout());
		traceDetails.setBackground(GUIUtils.panelBackground);
		traceDetails.add(titleContainer, BorderLayout.NORTH);
		
		tracesContainer = new JPanel();
		tracesContainer.setOpaque(false);
		tracesContainer.setLayout(new GridBagLayout());
		
		JScrollPane tracesScrollerContainer = new JScrollPane(tracesContainer);
		tracesScrollerContainer.setOpaque(false);
		tracesScrollerContainer.getViewport().setOpaque(false);
		tracesScrollerContainer.getVerticalScrollBar().setUI(new SlickerScrollBarUI(tracesScrollerContainer.getVerticalScrollBar(), GUIUtils.panelBackground, GUIUtils.panelTextColor, GUIUtils.panelTextColor.brighter(), 4, 11));
		tracesScrollerContainer.getHorizontalScrollBar().setUI(new SlickerScrollBarUI(tracesScrollerContainer.getHorizontalScrollBar(), GUIUtils.panelBackground, GUIUtils.panelTextColor, GUIUtils.panelTextColor.brighter(), 4, 11));
		tracesScrollerContainer.setBorder(BorderFactory.createEmptyBorder());
		tracesScrollerContainer.getVerticalScrollBar().setUnitIncrement(30);
		traceDetails.add(tracesScrollerContainer, BorderLayout.CENTER);
		
		refreshTraces(tracesContainer, currentSorter);
		
		return traceDetails;
	}
	
	private JPanel prepareDetailedView() {
		/* specific details per trace */
		/* ================================================================== */
		// sort field
		final ProMComboBox<String> sorter = new ProMComboBox<String>(comparators.keySet());
		sorter.setPreferredSize(new Dimension(200, 25));
		sorter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				currentSorter = comparators.get(sorter.getSelectedItem());
				new Thread(new Runnable() {
					public void run() {
						refreshTraces(currentSorter);
					}
				}).start();
				
			}
		});
		
		// search field
		final SlickerSearchField search = new SlickerSearchField(200, 25, WidgetColors.COLOR_LIST_BG, Color.lightGray, Color.lightGray.brighter(), Color.gray);
		final Timer timer = new Timer(true);
		search.addSearchListener(new ActionListener() {
			private TimerTask task;
			public void actionPerformed(ActionEvent e) {
				if(task != null) {
					task.cancel();
				}
				task = new TimerTask() {
					@Override
					public void run() {
						filter = search.getSearchText();
						if (filter.length() >= 2 || filter.length() == 0) {
							refreshTraces(currentSorter);
						}
					}
				};
				timer.schedule(task, 1000);
			}
		});
		JPanel searchContainer = new JPanel(new FlowLayout());
		searchContainer.setOpaque(false);
		searchContainer.add(GUIUtils.prepareLabel("Trace filter", SwingConstants.LEFT, Color.lightGray));
		searchContainer.add(search);
		searchContainer.add(GUIUtils.prepareLabel("Sort trace by:", SwingConstants.LEFT, Color.lightGray));
		searchContainer.add(sorter);
		
		JPanel titleContainer = new JPanel(new BorderLayout());
		titleContainer.setOpaque(false);
		titleContainer.add(GUIUtils.prepareTitle("Details per Constraint/Trace"), BorderLayout.WEST);
		titleContainer.add(searchContainer, BorderLayout.EAST);
		
		RoundedPanel traceDetails = new RoundedPanel(5, 5, 5);
		traceDetails.setLayout(new BorderLayout());
		traceDetails.setBackground(GUIUtils.panelBackground);
		traceDetails.add(titleContainer, BorderLayout.NORTH);
		
		
		JPanel boxContainer = new JPanel();
		boxContainer.setOpaque(false);
		
		caseIdsList = new ProMList<String>("Process Instances", caseIdsListModel);
		caseIdsList.setPreferredSize(new Dimension(250, 10000));
		caseIdsList.setMinimumSize(new Dimension(250, 100));
		caseIdsList.setMaximumSize(new Dimension(250, 10000));
		caseIdsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		caseIdsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				showTraceDetails();
			}
		});
		((JList)((JViewport)((ProMScrollPane) caseIdsList.getComponent(2)).getComponent(0)).getComponent(0)).setCellRenderer(new MultilineListEntryRenderer());
		
		constraintsList = new ProMList<String>("Constraints", constraintsListModel);
		constraintsList.setPreferredSize(new Dimension(400, 10000));
		constraintsList.setMinimumSize(new Dimension(400, 100));
		constraintsList.setMaximumSize(new Dimension(400, 10000));
		constraintsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		constraintsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				showTraceDetails();
			}
		});
		((JList)((JViewport)((ProMScrollPane) constraintsList.getComponent(2)).getComponent(0)).getComponent(0)).setCellRenderer(new MultilineListEntryRenderer());
		refreshConstraints();
		
		JLabel tmpLabel = GUIUtils.prepareLabel("Please select a trace and a constraint...");
		tmpLabel.setFont(tmpLabel.getFont().deriveFont(14f).deriveFont(Font.ITALIC));
		tmpLabel.setForeground(GUIUtils.tabForeground);
		tmpLabel.setHorizontalAlignment(JLabel.CENTER);
		
		caseDetails = SlickerFactory.instance().createRoundedPanel(15, WidgetColors.COLOR_ENCLOSURE_BG);
		caseDetails.setLayout(new BorderLayout(0, 15));
		caseDetails.add(tmpLabel);
		
		boxContainer.setLayout(new BoxLayout(boxContainer, BoxLayout.X_AXIS));
		boxContainer.add(caseIdsList);
		boxContainer.add(Box.createHorizontalStrut(5));
		boxContainer.add(constraintsList);
		boxContainer.add(Box.createHorizontalStrut(5));
		boxContainer.add(caseDetails);
		
		// populate case ids
		refreshTraces(currentSorter);
		
		// populate constraints
		
		traceDetails.add(boxContainer);
		
		return traceDetails;
	}
	
	private void refreshTraces(Comparator<ConstraintsViolationsVisualizer> comparator) {
		caseIdsListModel.removeAllElements();
		Collections.sort(constraintsVisualizerList, comparator);
		
		for (ConstraintsViolationsVisualizer visualizer : constraintsVisualizerList) {
			String traceName = visualizer.getTraceName();
			
			if (traceName.contains(filter)) {
				caseIdsListModel.addElement(new MultilineListEntry<XTrace>(visualizer.getTrace(), traceName,
						visualizer.getTotActivations() + " activ.&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
						visualizer.getTotViolations() + " viol.&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
						visualizer.getTotFulfilments() + " fulfil."));
			}
		}
		caseIdsList.updateUI();
	}
	
	private void refreshTraces(JPanel tracesContainer, Comparator<ConstraintsViolationsVisualizer> comparator) {
		
		tracesContainer.removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		GUIUtils.addToGridBagLayout(0, 0, tracesContainer, Box.createVerticalStrut(10));
		int i = 1;
		
		Collections.sort(constraintsVisualizerList, comparator);
		
		for (ConstraintsViolationsVisualizer visualizer : constraintsVisualizerList) {
			String traceName = visualizer.getTraceName();
			
			if (traceName.contains(filter)) {
				c = new GridBagConstraints();
				c.anchor = GridBagConstraints.WEST;
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				
				GUIUtils.addToGridBagLayout(0, i, tracesContainer, GUIUtils.prepareTitleBordered(traceName), c);
				GUIUtils.addToGridBagLayout(0, i + 1, tracesContainer, visualizer, c);
				GUIUtils.addToGridBagLayout(0, i + 2, tracesContainer, Box.createVerticalStrut(10));
				
				i += 2;
			}
		}
		
		GUIUtils.addToGridBagLayout(0, i + 2, tracesContainer, Box.createVerticalGlue(), 0, 1);
		
		tracesContainer.updateUI();
	}
	
	private void refreshConstraints() {
		for (ConstraintsViolationsVisualizer visualizer : constraintsVisualizerList) {
			List<AnalysisSingleResult> currentConstraints = visualizer.getConstraints();
			for (AnalysisSingleResult analysis : currentConstraints) {
				String name = analysis.getConstraint().getName();
				String description = analysis.getConstraint().getCaption();
				description = description.substring(description.indexOf("["));
				
				description += "<br>" + analysis.getConstraint().getCondition().toString();
				constraints.add(new Triple<String, String, ConstraintDefinition>(name, description, analysis.getConstraint()));

			}
		}
		for(Triple<String, String, ConstraintDefinition> cn : constraints) {
			constraintsListModel.addElement(new MultilineListEntry<ConstraintDefinition>(cn.getThird(), cn.getFirst(), cn.getSecond()));
		}
		constraintsList.updateUI();
	}
	
	/**
	 * This method shows the actual trace for the selected trace and constraint.
	 */
	@SuppressWarnings({ "cast", "unchecked" })
	private void showTraceDetails() {
		int selectedTraceIndex = ((JList)((JViewport)((ProMScrollPane) caseIdsList.getComponent(2)).getComponent(0)).getComponent(0)).getSelectedIndex();
		int selectedConstraintIndex = ((JList)((JViewport)((ProMScrollPane) constraintsList.getComponent(2)).getComponent(0)).getComponent(0)).getSelectedIndex();
		if (selectedTraceIndex == -1 || selectedConstraintIndex == -1) {
			return;
		}
		
		// extract the trace and constraint objects from the list models
		XTrace selectedTrace = (XTrace) ((MultilineListEntry<XTrace>) caseIdsListModel.get(selectedTraceIndex)).getObject();
		ConstraintDefinition constraint = (ConstraintDefinition) ((MultilineListEntry<ConstraintDefinition>) constraintsListModel.get(selectedConstraintIndex)).getObject();
		
		// extract the actual single result to show
		Set<AnalysisSingleResult> analysisResults = result.getResults(selectedTrace);
		AnalysisSingleResult analysisResult = null;
		for(AnalysisSingleResult result : analysisResults) {
			if (result.getConstraint().equals(constraint)) {
				analysisResult = result;
				break;
			}
		}
		
		JPanel statistics = SlickerFactory.instance().createRoundedPanel(15, Color.GRAY);
		statistics.setLayout(new GridLayout(0, 2));
		statistics.add(GUIUtils.prepareLabel("Activations: " + analysisResult.getActivations().size()));
		statistics.add(GUIUtils.prepareLabel("Act. sparsity: " + parseNicely(getActivationSparsity(analysisResult, selectedTrace))));
		statistics.add(GUIUtils.prepareLabel("Fulfilments: " + analysisResult.getFulfilments().size()));
		statistics.add(GUIUtils.prepareLabel("Fulfilment ratio: " + parseNicely((double)analysisResult.getFulfilments().size()/(double)analysisResult.getActivations().size())));
		statistics.add(GUIUtils.prepareLabel("Violations: " + analysisResult.getViolations().size()));
		statistics.add(GUIUtils.prepareLabel("Violation ratio: " + parseNicely((double)analysisResult.getViolations().size()/(double)analysisResult.getActivations().size())));
//		statistics.add(GUIUtils.prepareLabel("Conflicts: " + analysisResult.getConflicts().size()));
//		statistics.add(GUIUtils.prepareLabel("Conflict ratio: " + GUIUtils.df2.format((double)analysisResult.getConflicts().size()/(double)analysisResult.getActivations().size())));
		
		
		SingleResultVisualizer visualizer = new SingleResultVisualizer(analysisResult);
		JScrollPane caseDetailsContainer = new ProMScrollPane(visualizer);
		caseDetails.removeAll();
		caseDetails.add(statistics, BorderLayout.NORTH);
		caseDetails.add(caseDetailsContainer, BorderLayout.CENTER);
		caseDetails.revalidate();
	}
	
	private Double getActivationSparsity(AnalysisSingleResult ar, XTrace trace) {
		return 1.0 - (double)(ar.getActivations().size()) / (double)(trace.size());
	}
	
	private String parseNicely(Double value) {
		if (value.isNaN()) {
			return "/";
		}
		return GUIUtils.df2.format(value);
	}
}
