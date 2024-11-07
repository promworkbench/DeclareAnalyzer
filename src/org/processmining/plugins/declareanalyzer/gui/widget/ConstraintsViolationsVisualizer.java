package org.processmining.plugins.declareanalyzer.gui.widget;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.collection.HashMultiSet;
import org.processmining.framework.util.collection.MultiSet;
import org.processmining.plugins.declareanalyzer.AnalysisSingleResult;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

import utils.GUIUtils;

public class ConstraintsViolationsVisualizer extends JPanel implements MouseMotionListener {

	private static final long serialVersionUID = 3287603101866616636L;
	private XTrace trace;
	private List<AnalysisSingleResult> constraints;
	private HashMap<String, Integer> constraintToPosition;
	
	private int constraintHeight = 20;
	private int resolutionDetailsHeight = 30;
	private int eventWidth = 8;
	private int eventHeight = -1;
	private int detailsRectangleHeight = -1;
	private int detailsVerticalMargin = -1;
	
	private Font defaultFont = null;
//	private Font traceFont = null;
	private Font detailsFont = null;
	private FontMetrics defaultFontMetric = null;
//	private FontMetrics traceFontMetric = null;
	private FontMetrics detailsFontMetric = null;
	private SoftReference<BufferedImage> buffer = null;
	
	private int mouseX = -1;
	private int mouseY = -1;
	
	private String traceName;
	private int totActivations = 0;
	private int totViolations = 0;
	private int totFulfilments = 0;
	private int totConflicts = 0;
	private HashMap<String, Double> activationSparsity;
	private HashMap<String, Double> fulfilmentRatio;
	private HashMap<String, Double> violationRatio;
	private HashMap<String, Double> conflictRatio;
	
	/**
	 * 
	 * @param constraints
	 */
	public ConstraintsViolationsVisualizer(XTrace trace, Set<AnalysisSingleResult> constraints) {
		this.trace = trace;
		this.constraints = new ArrayList<AnalysisSingleResult>(constraints.size());
		this.constraintToPosition = new HashMap<String, Integer>();
		this.activationSparsity = new HashMap<String, Double>();
		this.fulfilmentRatio = new HashMap<String, Double>();
		this.violationRatio = new HashMap<String, Double>();
		this.conflictRatio = new HashMap<String, Double>();
		
		int i = 0;
		for (AnalysisSingleResult asr : constraints) {
			this.constraints.add(i++, asr);
		}
		Collections.sort(this.constraints);
		
		// general information
		this.traceName = trace.getAttributes().get("concept:name").toString();
		
		addMouseMotionListener(this);
		
		// general configuration
		int width = trace.size() * (eventWidth + 2);
		int height = (constraints.size() + 1) * constraintHeight + 50;
		int maxLetterConstraint = 0;
		
		for (AnalysisSingleResult ar : constraints) {
			if (ar.getConflicts().size() > 0) {
				height += ar.getResolutions().size() * (constraintHeight + resolutionDetailsHeight);
			}
			totActivations += ar.getActivations().size();
			totViolations += ar.getViolations().size();
			totFulfilments += ar.getFulfilments().size();
			totConflicts += ar.getConflicts().size();
			
			// statistics
			activationSparsity.put(ar.getConstraint().getCaption(), 1.0 - (double)(ar.getActivations().size()) / (double)(trace.size()));
			fulfilmentRatio.put(ar.getConstraint().getCaption(), (double)(ar.getFulfilments().size()) / (double)(ar.getActivations().size()));
			violationRatio.put(ar.getConstraint().getCaption(), (double)(ar.getViolations().size()) / (double)(ar.getActivations().size()));
			conflictRatio.put(ar.getConstraint().getCaption(), (double)(ar.getConflicts().size()) / (double)(ar.getActivations().size()));
			
			// longest string
			maxLetterConstraint = Math.max(ar.getConstraint().getCaption().length(), maxLetterConstraint);
		}
		
		// Heuristics on letter width... we don't yet have the font metrics,
		// let's assume an average of 10 pixel per letter
		width += (maxLetterConstraint * 10);
		
		setMinimumSize(new Dimension(width, height));
		setPreferredSize(new Dimension(width, height));
		setOpaque(false);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		int width = this.getWidth();
		int height = this.getHeight();
		
		// create new back buffer
		buffer = new SoftReference<BufferedImage>(new BufferedImage(width, height, BufferedImage.TRANSLUCENT));
		Graphics2D g2d = buffer.get().createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// setting up the "one-time font stuff"
		if (defaultFont == null) {
			defaultFont = g2d.getFont();
			defaultFont = defaultFont.deriveFont(12f);
			defaultFontMetric = g2d.getFontMetrics(defaultFont);
		}
//		if (traceFont == null) {
//			traceFont = defaultFont.deriveFont(Font.BOLD);
//			traceFontMetric = g2d.getFontMetrics(traceFont);
//		}
		if (detailsFont == null) {
			detailsFont = defaultFont.deriveFont(10f);
			detailsFontMetric = g2d.getFontMetrics(detailsFont);
		}
		if (eventHeight == -1) {
			eventHeight = defaultFontMetric.getAscent() + defaultFontMetric.getDescent();
		}
		if (detailsRectangleHeight == -1) {
			detailsRectangleHeight = detailsFontMetric.getAscent() + detailsFontMetric.getDescent();
		}
		if (detailsVerticalMargin == -1) {
			detailsVerticalMargin = (defaultFontMetric.getAscent() + defaultFontMetric.getDescent() - detailsRectangleHeight) / 2;
		}
		
		// constraints name
		int maxSpace = 0;
		for (AnalysisSingleResult ar : constraints) {
			maxSpace = Math.max(maxSpace, defaultFontMetric.stringWidth(ar.getConstraint().getCaption()));
		}
		
		// header
		int headerWidth = 100;
		
		g2d.setColor(GUIUtils.panelTextTitleColor);
		g2d.setFont(detailsFont);
		g2d.drawString("Activations: " + getTotActivations(),
				headerWidth * 0, detailsFontMetric.getAscent() + defaultFontMetric.getDescent());
		g2d.drawString("Violations: " + getTotViolations(),
				headerWidth * 1, detailsFontMetric.getAscent() + defaultFontMetric.getDescent());
		g2d.drawString("Fulfilment: " + getTotFulfilments(),
				headerWidth * 2, detailsFontMetric.getAscent() + defaultFontMetric.getDescent());
		g2d.drawString("Conflicts: " + getTotConflicts(),
				headerWidth * 3, detailsFontMetric.getAscent() + defaultFontMetric.getDescent());
		g2d.drawString("Act. sparsity: " + GUIUtils.df2.format(getAvgActivationSparsity()),
				headerWidth * 0, 2 * (detailsFontMetric.getAscent() + defaultFontMetric.getDescent()));
		g2d.drawString("Vio. ratio: " + GUIUtils.df2.format((double)getTotViolations()/(double)getTotActivations()),
				headerWidth * 1, 2 * (detailsFontMetric.getAscent() + defaultFontMetric.getDescent()));
		g2d.drawString("Ful. ratio: " + GUIUtils.df2.format((double)getTotFulfilments()/(double)getTotActivations()),
				headerWidth * 2, 2 * (detailsFontMetric.getAscent() + defaultFontMetric.getDescent()));
		g2d.drawString("Conf. ratio: " + GUIUtils.df2.format((double)getTotConflicts()/(double)getTotActivations()),
				headerWidth * 3, 2 * (detailsFontMetric.getAscent() + defaultFontMetric.getDescent()));
		
		// trace
		int k = 0;
		int extraHeight = 20;
		for (AnalysisSingleResult ar : constraints) {
			
			int positionY = extraHeight + defaultFontMetric.getAscent() + 6 + (k * constraintHeight);
			
			// trace name
			g2d.setFont(defaultFont);
			g2d.setColor(GUIUtils.panelTextColor);
			g2d.drawString(
					ar.getConstraint().getCaption(),
					maxSpace - defaultFontMetric.stringWidth(ar.getConstraint().getCaption()) + 5,
					positionY + defaultFontMetric.getAscent());
			constraintToPosition.put(ar.getConstraint().getCaption(), positionY);
			
			// the actual trace
			LinkedList<String> eventNames = new LinkedList<String>();
			for (XEvent e : trace) {
				eventNames.add(e.getAttributes().get("concept:name").toString());
			}
			paintTrace(g2d, maxSpace + 15, positionY, width, mouseX, mouseY, eventNames,
					ar.getActivations(), ar.getViolations(), ar.getFulfilments(), ar.getConflicts(), false);
			
			/* resolutions */
			ArrayList<List<Integer>> resolutions = ar.getResolutions();
			if (resolutions.size() > 0 && ar.getConflicts().size() > 0) {
				
				String resolutionText = "Resolutions:";
				g2d.setColor(GUIUtils.panelBackground.darker());
				g2d.setFont(defaultFont);
				g2d.drawString(resolutionText,
						maxSpace + 5 - defaultFontMetric.stringWidth(resolutionText),
						positionY + constraintHeight + defaultFontMetric.getAscent());
				
				Set<Integer> conflicts = ar.getConflicts();
				int resolutionsCounter = 0;
				for (List<Integer> et : resolutions) {
					int positionX = maxSpace + 15;
					
					Set<Integer> violations = new HashSet<Integer>();
					Set<Integer> fulfilments = new HashSet<Integer>();
					
					for (Integer c : conflicts) {
						if (et.contains(c)) {
							fulfilments.add(c);
						} else {
							violations.add(c);
						}
					}
					
					// resolution trace
					paintTrace(g2d,
							positionX, positionY + constraintHeight + resolutionsCounter * (constraintHeight + resolutionDetailsHeight),
							width,
							mouseX, mouseY, eventNames,
							null, violations, fulfilments, null, true);
					
					// resolution local likelihood
					double localLikelihood = (double) fulfilments.size() / ar.getConflicts().size();
					
					// resolution global likelihood
					double globalLikelihood = 0.0;
					MultiSet<Double> likelihoods = new HashMultiSet<Double>();
					
					for (Integer c : conflicts) {
						double violationsLikelihood = 0.0;
						double fulfillmentsLikelihood = 0.0;
						for (AnalysisSingleResult constr : constraints) {
							if (violations.contains(c)) {
								if (constr.getViolations().contains(c)) {
									violationsLikelihood++;
								}
							} else {
								if (constr.getFulfilments().contains(c)) {
									fulfillmentsLikelihood++;
								}
							}
						}
						if (violationsLikelihood > 0) {
							violationsLikelihood /= constraints.size();
							likelihoods.add(violationsLikelihood);
						}
						if (fulfillmentsLikelihood > 0) {
							fulfillmentsLikelihood /= constraints.size();
							likelihoods.add(fulfillmentsLikelihood);
						}
					}

					if (likelihoods.size() > 0) {
						for (Double l : likelihoods) {
							globalLikelihood += l;
						}
						globalLikelihood /= likelihoods.size();
					}
					
					int yPositionLikelihood = positionY + (resolutionsCounter + 1) * (constraintHeight + resolutionDetailsHeight);
					g2d.setFont(detailsFont);
					g2d.setColor(GUIUtils.eventDetailsColor);
					g2d.drawString(GUIUtils.df2.format(localLikelihood) + "",
							positionX,
							yPositionLikelihood);
					g2d.drawString(GUIUtils.df2.format(globalLikelihood),
							positionX,
							yPositionLikelihood + detailsFontMetric.getAscent());
					g2d.drawString(GUIUtils.df2.format((localLikelihood + globalLikelihood) / 2),
							positionX + 130,
							yPositionLikelihood + detailsFontMetric.getAscent() / 2);
					
					g2d.setColor(GUIUtils.panelBackground.darker());
					g2d.drawString("Local likelihood",
							positionX + 35,
							yPositionLikelihood);
					g2d.drawString("Global likelihood",
							positionX + 35,
							yPositionLikelihood + detailsFontMetric.getAscent());
					g2d.drawString("Avg of likelihoods",
							positionX + 165,
							yPositionLikelihood + detailsFontMetric.getAscent() / 2);
					g2d.drawLine(
							positionX + 120,
							yPositionLikelihood - detailsFontMetric.getAscent(),
							positionX + 120,
							yPositionLikelihood + detailsFontMetric.getAscent());
					g2d.drawLine(
							positionX + 117,
							yPositionLikelihood - detailsFontMetric.getAscent(),
							positionX + 120,
							yPositionLikelihood - detailsFontMetric.getAscent());
					g2d.drawLine(
							positionX + 117,
							yPositionLikelihood + detailsFontMetric.getAscent(),
							positionX + 120,
							yPositionLikelihood + detailsFontMetric.getAscent());
					g2d.drawLine(
							positionX + 120,
							yPositionLikelihood,
							positionX + 123,
							yPositionLikelihood);
					
					resolutionsCounter++;
				}
				extraHeight += resolutionsCounter * (constraintHeight + resolutionDetailsHeight);
			}
			k++;
		}
		
		// constraint details
		for (AnalysisSingleResult ar : constraints) {
			paintConstraintDetails(g2d, ar,
					0, constraintToPosition.get(ar.getConstraint().getCaption()),
					maxSpace, defaultFontMetric.getAscent() + defaultFontMetric.getDescent() + 1, mouseX, mouseY, height);
		}
		
		// final paint stuff
		g2d.dispose();
		Rectangle clip = g.getClipBounds();
		g.drawImage(buffer.get(), clip.x, clip.y, clip.x + clip.width, clip.y + clip.height,
				clip.x, clip.y, clip.x + clip.width, clip.y + clip.height, null);
	}
	
	private void paintTrace(
			Graphics2D g2d,
			int x, int y,
			int width,
			int mouseX, int mouseY,
			List<String> trace,
			Set<Integer> activations,
			Set<Integer> violations,
			Set<Integer> fulfilments,
			Set<Integer> conflicts,
			boolean isResolution) {
		
		int textWidth = 0;
		
		for (int j = 0; j < trace.size(); j++) {
			
			int positionX = x + (j * (eventWidth + 2));
			
			if (isResolution) {
				g2d.setColor(GUIUtils.resolutionEvent);
			} else {
				g2d.setColor(GUIUtils.event);
			}
			
			if (violations != null && violations.contains(j)) {
				if (isResolution) {
					g2d.setColor(GUIUtils.resolutionEventViolated);
				} else {
					g2d.setColor(GUIUtils.eventViolated);
				}
			}
			if (fulfilments != null && fulfilments.contains(j)) {
				if (isResolution) {
					g2d.setColor(GUIUtils.resolutionEventFulfilled);
				} else {
					g2d.setColor(GUIUtils.eventFulfilled);
				}
			}
			if (conflicts != null && conflicts.contains(j)) {
				g2d.setColor(GUIUtils.eventConflict);
			}
			
			g2d.fillRoundRect(positionX, y, eventWidth, eventHeight, 4, 4);
		}
		
		/* details part */
		if (mouseX >= x && mouseX <= x + (trace.size() * (eventWidth + 2))) {
			if (mouseY >= y && mouseY <= y + defaultFontMetric.getAscent() + defaultFontMetric.getDescent()) {
				
				int j = (mouseX - x) / (eventWidth + 2);
				
				if (j >= 0 && j < trace.size()) {

					String is = "";
					
					g2d.setColor(GUIUtils.event);
					if (violations!= null && violations.contains(j)) {
						if (is == "") {
							is += " is ";
						} else {
							is += ", ";
						}
						is += "violation";
					}
					if (fulfilments != null && fulfilments.contains(j)) {
						if (is == "") {
							is += " is ";
						} else {
							is += ", ";
						}
						is += "fulfilment";
					}
					if (conflicts != null && conflicts.contains(j)) {
						if (is == "") {
							is += " is ";
						} else {
							is += ", ";
						}
						is += "conflict";
					}
					
					is += " (ev. no. " + (j+1) + ")";
					
					int positionX = x + (j * (eventWidth + 2));
					
					/* name of the event and constraint status */
					String text = "\"" + trace.get(j) + "\"" + is;
					textWidth = detailsFontMetric.stringWidth(text);
					
					boolean flip = false;
					if (positionX + textWidth + 11 > width) {
						flip = true;
						positionX -= (textWidth + (eventWidth + 2)*2 + 5);
					}
					
					g2d.setColor(GUIUtils.eventDetailsBackground);
					g2d.fillRoundRect(positionX + eventWidth + 5, y + detailsVerticalMargin, textWidth + 6, detailsRectangleHeight, 5, 5);
					if (!flip) {
						g2d.fillPolygon(new int[] {
								positionX + eventWidth + 1,
								positionX + eventWidth + 5,
								positionX + eventWidth + 5},
							new int[]{
								y + (detailsRectangleHeight / 2) + detailsVerticalMargin,
								y + (detailsRectangleHeight / 2) + detailsVerticalMargin - 3,
								y + (detailsRectangleHeight / 2) + detailsVerticalMargin + 3}, 3);
					} else {
						g2d.fillPolygon(new int[] {
								positionX + textWidth + (eventWidth + 2)*2 + 5,
								positionX + textWidth + (eventWidth + 2)*2 - 1,
								positionX + textWidth + (eventWidth + 2)*2 - 1},
							new int[]{
								y + (detailsRectangleHeight / 2) + detailsVerticalMargin,
								y + (detailsRectangleHeight / 2) + detailsVerticalMargin - 3,
								y + (detailsRectangleHeight / 2) + detailsVerticalMargin + 3}, 3);
					}
					g2d.setColor(GUIUtils.eventDetailsColor);
					g2d.setFont(detailsFont);
					g2d.drawString(text, positionX + eventWidth + 8, y + detailsVerticalMargin + detailsFontMetric.getAscent());
				
				}
			}
		}
	}
	
	private void paintConstraintDetails(
			Graphics2D g2d,
			AnalysisSingleResult ar,
			int x, int y,
			int width, int height,
			int mouseX, int mouseY, int maxYPosition) {
		
		if (mouseX >= x && mouseX <= x + width) {
			if (mouseY >= y && mouseY <= y + height) {
				
				int panelHeight = 90;
				int panelWidth = detailsFontMetric.stringWidth(ar.getConstraint().getCaption()) + 20;
				if (panelWidth < 230) {
					panelWidth = 230;
				}
				
				int positionY = y - 5;
				int positionX = x + width + 10;
				int stringWidth = defaultFontMetric.stringWidth(ar.getConstraint().getCaption());
				
				if (positionY + panelHeight > maxYPosition) {
					positionY = maxYPosition - panelHeight - 5;
				}
				
				g2d.setColor(GUIUtils.eventDetailsBackground);
				g2d.fillRoundRect(positionX, positionY, panelWidth, panelHeight, 10, 10);
				g2d.setColor(GUIUtils.panelBackground.darker());
				g2d.fillRoundRect(width - stringWidth, y, stringWidth + 7, height, 7, 7);
				
				g2d.setColor(GUIUtils.panelTextColor);
				g2d.drawRoundRect(width - stringWidth, y, stringWidth + 7, height, 7, 7);
				g2d.drawLine(
						positionX - 3, y + 7,
						positionX, y + 7);
				
				g2d.setFont(defaultFont);
//				g2d.setColor(GUIUtils.eventDetailsColor.darker());
				g2d.setColor(GUIUtils.eventDetailsColor);
				g2d.drawString(
						ar.getConstraint().getCaption(),
						width - defaultFontMetric.stringWidth(ar.getConstraint().getCaption()) + 5,
						y + defaultFontMetric.getAscent());
				
				g2d.setFont(detailsFont);
				g2d.setColor(GUIUtils.eventDetailsColor);
				g2d.drawString(ar.getConstraint().getCaption(),
						positionX + 10,
						positionY + detailsFontMetric.getDescent() + detailsFontMetric.getAscent());
				g2d.drawLine(positionX + 10, positionY + detailsFontMetric.getDescent() + detailsFontMetric.getAscent() + 5,
						positionX + panelWidth - 10, positionY + detailsFontMetric.getDescent() + detailsFontMetric.getAscent() + 5);

				int row = 1;
				g2d.drawString("Activations:", positionX + 10, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + ar.getActivations().size(), positionX + 70, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("Fulfilments:", positionX + 10, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + ar.getFulfilments().size(), positionX + 70, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("Violations:", positionX + 10, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + ar.getViolations().size(), positionX + 70, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("Conflicts:", positionX + 10, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + ar.getConflicts().size(), positionX + 70, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				
				row = 1;
				g2d.drawString("Act. sparsity:", positionX + 100, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + GUIUtils.df2.format(getActivationSparsity(ar.getConstraint())), positionX + 180, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("Fulfilment ratio:", positionX + 100, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + GUIUtils.df2.format((double)ar.getFulfilments().size()/(double)ar.getActivations().size()), positionX + 180, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("Violation ratio:", positionX + 100, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + GUIUtils.df2.format((double)ar.getViolations().size()/(double)ar.getActivations().size()), positionX + 180, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("Conflict ratio:", positionX + 100, positionY + 5 + ++row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
				g2d.drawString("" + GUIUtils.df2.format((double)ar.getConflicts().size()/(double)ar.getActivations().size()), positionX + 180, positionY + 5 + row * (detailsFontMetric.getAscent() + detailsFontMetric.getDescent() + 2));
			}
		}
	}
	
	public XTrace getTrace() {
		return trace;
	}
	
	public String getTraceName() {
		return traceName;
	}
	
	public int getTotActivations() {
		return totActivations;
	}
	
	public int getTotViolations() {
		return totViolations;
	}
	
	public int getTotFulfilments() {
		return totFulfilments;
	}
	
	public int getTotConflicts() {
		return totConflicts;
	}
	
	public Double getActivationSparsity(ConstraintDefinition c) {
		Double d = activationSparsity.get(c.getCaption());
		if (d == null || d.isNaN()) {
			return 0.0;
		}
		return d;
	}
	
	public Double getAvgActivationSparsity() {
		Double tot = 0.0;
		for (AnalysisSingleResult asr : constraints) {
			tot += getActivationSparsity(asr.getConstraint());
		}
		return tot / constraints.size();
	}
	
	public Double getFulfilmentRatio(ConstraintDefinition c) {
		Double d = fulfilmentRatio.get(c.getCaption());
		if (d == null || d.isNaN()) {
			return 0.0;
		}
		return d;
	}
	
	public Double getAvgFulfilmentRatio() {
		Double tot = 0.0;
		for (AnalysisSingleResult asr : constraints) {
			tot += getFulfilmentRatio(asr.getConstraint());
		}
		return tot / constraints.size();
	}
	
	public Double getViolationRatio(ConstraintDefinition c) {
		Double d = violationRatio.get(c.getCaption());
		if (d == null || d.isNaN()) {
			return 0.0;
		}
		return d;
	}
	
	public Double getAvgViolationRatio() {
		Double tot = 0.0;
		for (AnalysisSingleResult asr : constraints) {
			tot += getViolationRatio(asr.getConstraint());
		}
		return tot / constraints.size();
	}
	
	public Double getConflictRatio(ConstraintDefinition c) {
		Double d = conflictRatio.get(c.getCaption());
		if (d == null || d.isNaN()) {
			return 0.0;
		}
		return d;
	}
	
	public Double getAvgConflictRatio() {
		Double tot = 0.0;
		for (AnalysisSingleResult asr : constraints) {
			tot += getConflictRatio(asr.getConstraint());
		}
		return tot / constraints.size();
	}
	
	public List<AnalysisSingleResult> getConstraints() {
		return constraints;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		mouseX = arg0.getX();
		mouseY = arg0.getY();
		repaint();
	}
}
