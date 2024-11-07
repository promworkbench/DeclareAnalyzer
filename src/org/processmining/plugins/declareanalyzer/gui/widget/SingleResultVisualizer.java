package org.processmining.plugins.declareanalyzer.gui.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

import javax.swing.JPanel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.widgets.WidgetColors;
import org.processmining.plugins.declareanalyzer.AnalysisSingleResult;

import utils.GUIUtils;

/**
 * This class contains the visualizer of a single analysis result
 * 
 * @author Andrea Burattin
 */
public class SingleResultVisualizer extends JPanel implements MouseMotionListener {

	private static final long serialVersionUID = 97011063951014094L;
	private static final int MAX_EVENTS_TO_SHOW = 50;
	private static final int MARGIN = 10;
	private static final int VERTICAL_SPACE_BETWEEN_BOXES = 20;
	private static final int BOX_PADDING = 10;
	private int BOX_HEIGHT = 50;
	private int BOX_WIDTH = 350;
	private static final int NUMBER_WIDTH = 20;
	private static final int CONNECTOR_WIDTH = 16;
	
	private static final Color NORMAL_BOX_BACKGROUND_1 = Color.GRAY;
	private static final Color NORMAL_BOX_BACKGROUND_2 = Color.LIGHT_GRAY;
	private static final Color NORMAL_BOX_FOREGROUND = Color.BLACK;
	private static final Color FULFILL_BOX_BACKGROUND_1 = GUIUtils.eventFulfilled.darker();
	private static final Color FULFILL_BOX_BACKGROUND_2 = GUIUtils.eventFulfilled.brighter();
	private static final Color FULFILL_BOX_FOREGROUND = Color.BLACK;
	private static final Color VIOLATION_BOX_BACKGROUND_1 = GUIUtils.eventViolated.darker();
	private static final Color VIOLATION_BOX_BACKGROUND_2 = GUIUtils.eventViolated.brighter();
	private static final Color VIOLATION_BOX_FOREGROUND = Color.WHITE;
	private static final Color NUMBER_COLOR_1 = WidgetColors.COLOR_LIST_SELECTION_BG;
	private static final Color NUMBER_COLOR_2 = WidgetColors.COLOR_LIST_SELECTION_BG.darker().darker();
	private static final Color CONNECTOR_COLOR = Color.DARK_GRAY;
	
	private XTrace trace;
//	private ConstraintDefinition constraint;
	private AnalysisSingleResult analysis;
	
	private SoftReference<BufferedImage> buffer = null;
	private Font activityNameFont = null;
	private FontMetrics activityNameFontMetrics = null;
	private Font eventIndexFont = null;
	private FontMetrics eventIndexFontMetrics = null;
	private int currentHoverEvent = -1;
	
	/**
	 * Visualizer constructor
	 * 
	 * @param analysis
	 */
	public SingleResultVisualizer(AnalysisSingleResult analysis) {
		this.trace = analysis.getTrace();
//		this.constraint = analysis.getConstraint();
		this.analysis = analysis;
		
		addMouseMotionListener(this);
		setOpaque(true);
		setBackground(WidgetColors.COLOR_ENCLOSURE_BG);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		// create new back buffer
		buffer = new SoftReference<BufferedImage>(new BufferedImage(getWidth(), getHeight(), BufferedImage.TRANSLUCENT));
		Graphics2D g2d = buffer.get().createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// setting up the "one-time font stuff"
		if (activityNameFont == null) {
			activityNameFont = g2d.getFont();
			activityNameFont = activityNameFont.deriveFont(12f);
			activityNameFontMetrics = g2d.getFontMetrics(activityNameFont);
		}
		if (eventIndexFont == null) {
			eventIndexFont = g2d.getFont();
			eventIndexFont = eventIndexFont.deriveFont(11f);
			eventIndexFontMetrics = g2d.getFontMetrics(eventIndexFont);
		}
		
		// set box dimensions
		BOX_WIDTH = 0;
		for (XEvent e : trace) {
			BOX_WIDTH = Math.max(BOX_WIDTH, activityNameFontMetrics.stringWidth(XConceptExtension.instance().extractName(e)));
		}
		BOX_WIDTH += BOX_PADDING * 2;
		BOX_HEIGHT = activityNameFontMetrics.getHeight() + BOX_PADDING * 2;
		
		// paint activities
		int eventIndex = 0;
		for (XEvent e : trace) {
			int currentY = MARGIN + eventIndex * (BOX_HEIGHT + VERTICAL_SPACE_BETWEEN_BOXES);
			boolean isViolation = analysis.getViolations().contains(eventIndex);
			boolean isFulfillment = analysis.getFulfilments().contains(eventIndex);
			boolean mouseHover = currentHoverEvent >= 0;
			
			// draw event number
			g2d.setPaint(new GradientPaint(
					MARGIN + NUMBER_WIDTH - 15, 0, NUMBER_COLOR_1,
					MARGIN + NUMBER_WIDTH, 0, NUMBER_COLOR_2));
			g2d.fillRoundRect(MARGIN, currentY + BOX_PADDING, NUMBER_WIDTH + 5, eventIndexFontMetrics.getHeight(), 5, 5);
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.setFont(eventIndexFont.deriveFont(9f));
			g2d.drawString(Integer.toString(eventIndex + 1), MARGIN + 2, currentY + BOX_PADDING + eventIndexFontMetrics.getAscent());
			
			// draw activity box
			int activityNameWidth = activityNameFontMetrics.stringWidth(XConceptExtension.instance().extractName(e));
			if (isViolation) {
				g2d.setPaint(new GradientPaint(0, currentY, VIOLATION_BOX_BACKGROUND_1, 0, currentY + BOX_HEIGHT, VIOLATION_BOX_BACKGROUND_2));
			} else if (isFulfillment) {
				g2d.setPaint(new GradientPaint(0, currentY, FULFILL_BOX_BACKGROUND_1, 0, currentY + BOX_HEIGHT, FULFILL_BOX_BACKGROUND_2));
			} else {
				g2d.setPaint(new GradientPaint(0, currentY, NORMAL_BOX_BACKGROUND_1, 0, currentY + BOX_HEIGHT, NORMAL_BOX_BACKGROUND_2));
			}
			g2d.fillRoundRect(NUMBER_WIDTH + MARGIN, currentY, BOX_WIDTH, BOX_HEIGHT, 10, 10);
			if (isViolation) {
				g2d.setColor(VIOLATION_BOX_FOREGROUND);
			} else if (isFulfillment) {
				g2d.setColor(FULFILL_BOX_FOREGROUND);
			} else {
				g2d.setColor(NORMAL_BOX_FOREGROUND);
			}
			g2d.setFont(activityNameFont);
//			g2d.drawLine(NUMBER_WIDTH + MARGIN, currentY, NUMBER_WIDTH + MARGIN + BOX_WIDTH, currentY + BOX_HEIGHT);
//			g2d.drawLine(NUMBER_WIDTH + MARGIN, currentY + BOX_HEIGHT, NUMBER_WIDTH + MARGIN + BOX_WIDTH, currentY);
			g2d.drawString(XConceptExtension.instance().extractName(e),
					MARGIN + NUMBER_WIDTH + (BOX_WIDTH / 2 - activityNameWidth / 2),
					currentY + activityNameFontMetrics.getAscent() + BOX_PADDING);
			
			// draw connector
			if (eventIndex + 1 < trace.size()) {
				g2d.setColor(CONNECTOR_COLOR);
				g2d.fillRect(MARGIN + NUMBER_WIDTH + (BOX_WIDTH / 2) - (CONNECTOR_WIDTH / 2), currentY + BOX_HEIGHT, CONNECTOR_WIDTH, VERTICAL_SPACE_BETWEEN_BOXES);
			}
			
			// draw mouse hover
			if (mouseHover && currentHoverEvent == eventIndex) {
				// draw border around activity
				float dash1[] = { 10f, 3f };
				BasicStroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
				g2d.setStroke(dashed);
				g2d.setColor(Color.LIGHT_GRAY);
				g2d.drawRoundRect(NUMBER_WIDTH + MARGIN - 3, currentY - 3, BOX_WIDTH + 6, BOX_HEIGHT + 6, 10, 10);
				
				int boxHeight = BOX_PADDING + ((trace.getAttributes().size() + 3 + e.getAttributes().size()) * eventIndexFontMetrics.getHeight()) + BOX_PADDING;
				int offsetY = 0;
				if (isFulfillment || isViolation) {
					offsetY = activityNameFontMetrics.getHeight() + 10;
					boxHeight += offsetY;
				}
				
				int boxWidth = 300; //getWidth() - (MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + MARGIN);
				for (String attributeName : e.getAttributes().keySet()) {
					boxWidth = Math.max(boxWidth, BOX_PADDING * 2 + eventIndexFontMetrics.stringWidth(attributeName + " = " + e.getAttributes().get(attributeName)));
				}
				for (String attributeName : trace.getAttributes().keySet()) {
					boxWidth = Math.max(boxWidth, BOX_PADDING * 2 + eventIndexFontMetrics.stringWidth(attributeName + " = " + e.getAttributes().get(attributeName)));
				}
				boxWidth = Math.min(boxWidth, getWidth() - (MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15));
				
				int boxY = Math.max(currentY + BOX_HEIGHT / 2 - boxHeight / 2, MARGIN - 3);
				boxY = Math.min(boxY, getHeight() - boxHeight - MARGIN);
				
				// draw popup
				g2d.setColor(Color.LIGHT_GRAY);
				g2d.fillRect(
						MARGIN + NUMBER_WIDTH + BOX_WIDTH + 5,
						currentY + BOX_HEIGHT / 2 - 1,
						15, 2);
				g2d.fillRoundRect(
						MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15,
						boxY,
						boxWidth, boxHeight, 10, 10);
				
				// draw semaphore
				if (isFulfillment) {
					g2d.setColor(FULFILL_BOX_BACKGROUND_1);
				} else if (isViolation) {
					g2d.setColor(VIOLATION_BOX_BACKGROUND_1);
				}
				g2d.fillOval(
						MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + boxWidth - 15 - BOX_PADDING,
						boxY + BOX_PADDING,
						15, 15);
				
				// draw popup text
				g2d.setFont(activityNameFont);
				g2d.setColor(Color.BLACK);
				if (isFulfillment) {
					g2d.drawString("This activity is fullfillment!",
							MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + BOX_PADDING,
							boxY + BOX_PADDING + activityNameFontMetrics.getHeight());
				} else if (isViolation) {
					g2d.drawString("This activity is a violation!",
							MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + BOX_PADDING,
							boxY + BOX_PADDING + activityNameFontMetrics.getAscent());
				}
				g2d.setFont(eventIndexFont);
				g2d.setColor(Color.DARK_GRAY.darker());
				g2d.setFont(eventIndexFont.deriveFont(Font.BOLD));
				int attributeIndex = 0;
				g2d.drawString("Event Attributes",
						MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + BOX_PADDING,
						boxY + BOX_PADDING + offsetY + eventIndexFontMetrics.getAscent() + eventIndexFontMetrics.getHeight() * attributeIndex);
				g2d.setFont(eventIndexFont);
				attributeIndex++;
				for (String attributeName : e.getAttributes().keySet()) {
					g2d.drawString(attributeName + " = " + e.getAttributes().get(attributeName),
							MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + BOX_PADDING,
							boxY + BOX_PADDING + offsetY + eventIndexFontMetrics.getAscent() + eventIndexFontMetrics.getHeight() * attributeIndex);
					attributeIndex++;
				}
				offsetY += eventIndexFontMetrics.getHeight();
				g2d.setFont(eventIndexFont.deriveFont(Font.BOLD));
				g2d.drawString("Trace Attributes",
						MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + BOX_PADDING,
						boxY + BOX_PADDING + offsetY + eventIndexFontMetrics.getAscent() + eventIndexFontMetrics.getHeight() * attributeIndex);
				offsetY += eventIndexFontMetrics.getHeight();
				g2d.setFont(eventIndexFont);
				for (String attributeName : trace.getAttributes().keySet()) {
					g2d.drawString(attributeName + " = " + trace.getAttributes().get(attributeName),
							MARGIN + NUMBER_WIDTH + BOX_WIDTH + 15 + BOX_PADDING,
							boxY + BOX_PADDING + offsetY + eventIndexFontMetrics.getAscent() + eventIndexFontMetrics.getHeight() * attributeIndex);
					attributeIndex++;
				}
			}
			eventIndex++;
			if (eventIndex == MAX_EVENTS_TO_SHOW) {
				if (eventIndex < trace.size()) {
					String warning = "Trace too log, truncated";
					g2d.setFont(eventIndexFont);
					g2d.setColor(Color.WHITE);
					g2d.drawString(warning,
							MARGIN + NUMBER_WIDTH + BOX_WIDTH - eventIndexFontMetrics.stringWidth(warning),
							currentY + BOX_HEIGHT + VERTICAL_SPACE_BETWEEN_BOXES - eventIndexFontMetrics.getDescent());
				}
				break;
			}
		}
		
		// final paint stuff
		g2d.dispose();
		Rectangle clip = g.getClipBounds();
		g.drawImage(buffer.get(), clip.x, clip.y, clip.x + clip.width, clip.y + clip.height,
				clip.x, clip.y, clip.x + clip.width, clip.y + clip.height, null);
	}
	
	@Override
	public Dimension getPreferredSize() {
		int width = MARGIN * 2 + BOX_WIDTH + NUMBER_WIDTH;
		int height = MARGIN * 2 + Math.min(trace.size(), MAX_EVENTS_TO_SHOW) * (BOX_HEIGHT + VERTICAL_SPACE_BETWEEN_BOXES);
		return new Dimension(width, height);
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	public void mouseDragged(MouseEvent e) { }
	
	@Override
	public void mouseMoved(MouseEvent me) {
		int mouseX = me.getX();
		int mouseY = me.getY();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
		int newCurrentHoverEvent = -1;
		for(int eventIndex = 0; eventIndex < trace.size(); eventIndex++) {
			int currentY = MARGIN + eventIndex * (BOX_HEIGHT + VERTICAL_SPACE_BETWEEN_BOXES);
			if ((mouseX >= MARGIN + NUMBER_WIDTH && mouseX < MARGIN + NUMBER_WIDTH + BOX_WIDTH) &&
				(mouseY >= currentY && mouseY < currentY + BOX_HEIGHT)) {
				newCurrentHoverEvent = eventIndex;
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				break;
			}
			if (eventIndex == MAX_EVENTS_TO_SHOW) {
				break;
			}
		}
		
		if (newCurrentHoverEvent != currentHoverEvent) {
			currentHoverEvent = newCurrentHoverEvent;
			repaint();
		}
	}
}
