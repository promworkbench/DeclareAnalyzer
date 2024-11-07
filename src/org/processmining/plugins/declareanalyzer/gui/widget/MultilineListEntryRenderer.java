package org.processmining.plugins.declareanalyzer.gui.widget;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.processmining.framework.util.ui.widgets.WidgetColors;

public class MultilineListEntryRenderer extends JLabel implements ListCellRenderer {

	private static final long serialVersionUID = 5363852114973376808L;

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		MultilineListEntry entry = (MultilineListEntry) value;
		
		setText("<html><b style=\"color: #ffffff\">" + entry.getFirstLine() + "&nbsp;</b><br><small>" + entry.getSecondLine() + "&nbsp;</small></html>");
		
		if (isSelected) {
			setForeground(WidgetColors.COLOR_LIST_SELECTION_FG);
			setBackground(WidgetColors.COLOR_LIST_SELECTION_BG);
		} else {
			setForeground(WidgetColors.COLOR_LIST_FG);
			setBackground(WidgetColors.COLOR_LIST_BG);
		}
		
		setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
		setOpaque(true);
		
		return this;
	}

}
