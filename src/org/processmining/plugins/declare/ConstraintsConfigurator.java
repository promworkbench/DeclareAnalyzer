package org.processmining.plugins.declare;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.processmining.framework.util.Pair;
import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.plugins.declareanalyzer.gui.widget.MultilineListEntry;
import org.processmining.plugins.declareanalyzer.gui.widget.MultilineListEntryRenderer;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;

import utils.Triple;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * 
 * @author Andrea Burattin
 */
public class ConstraintsConfigurator extends JPanel {

	private String[] constraintNames = {
		DeclareTemplate.Absence.name(),
		DeclareTemplate.Absence2.name(),
		DeclareTemplate.Absence3.name(),
		DeclareTemplate.Alternate_Precedence.name(),
		DeclareTemplate.Alternate_Response.name(),
		DeclareTemplate.Chain_Precedence.name(),
		DeclareTemplate.Chain_Response.name(),
		DeclareTemplate.Exactly1.name(),
		DeclareTemplate.Exactly2.name(),
		DeclareTemplate.Existence.name(),
//		DeclareTemplate.Not_Chain_Response.name(),
//		DeclareTemplate.Not_Chain_Precedence.name(),
//		DeclareTemplate.Not_Precedence.name(),
//		DeclareTemplate.Not_Responded_Existence.name(),
//		DeclareTemplate.Not_Response.name(),
		DeclareTemplate.Precedence.name(),
		DeclareTemplate.Responded_Existence.name(),
		DeclareTemplate.Response.name(),
	};
	private static final long serialVersionUID = 6886195107070978119L;
	private DefaultListModel constraintsListModel = new DefaultListModel();
	private ProMList<MultilineListEntry<?>> constraintsList;
	private JButton addConstraint;
	private JButton removeConstraint;
	
	public ConstraintsConfigurator() {
		super(new BorderLayout());
		
		constraintsList = new ProMList<MultilineListEntry<?>>("Constraints List", constraintsListModel);
		constraintsList.setPreferredSize(new Dimension(150, 150));
		constraintsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		((JList)((JViewport)((ProMScrollPane) constraintsList.getComponent(2)).getComponent(0)).getComponent(0)).setCellRenderer(new MultilineListEntryRenderer());
		constraintsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (constraintsList.getSelectedValuesList().size() > 0) {
					removeConstraint.setEnabled(true);
				}
			}
		});
		constraintsList.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			public void mouseClicked(MouseEvent evt) {
				JList list = (JList) evt.getSource();
				if (evt.getClickCount() == 2) {
					int index = list.locationToIndex(evt.getPoint());
					editConstraint((MultilineListEntry<Triple<String, Pair<String, String>, Triple<String, String, String>>>) constraintsListModel.get(index));
				}
			}
		});
		
		addConstraint = SlickerFactory.instance().createButton("Add new");
		addConstraint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addNewConstraint();
			}
		});
		
		removeConstraint = SlickerFactory.instance().createButton("Remove");
		removeConstraint.setEnabled(false);
		removeConstraint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (MultilineListEntry<?> mle : constraintsList.getSelectedValuesList()) {
					constraintsListModel.removeElement(mle);
				}
				removeConstraint.setEnabled(false);
			}
		});
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonsPanel.setOpaque(false);
		buttonsPanel.add(addConstraint);
		buttonsPanel.add(removeConstraint);
		buttonsPanel.add(new JLabel("To edit a constraint, double click it."));
		
		add(constraintsList, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.SOUTH);
	}
	
	public Set<Triple<String, Pair<String, String>, Triple<String, String, String>>> getConstraints() {
		HashSet<Triple<String, Pair<String, String>, Triple<String, String, String>>> set = new HashSet<Triple<String, Pair<String, String>, Triple<String, String, String>>>();
		for (int i = 0; i < constraintsListModel.size(); i++) {
			@SuppressWarnings("unchecked")
			MultilineListEntry<Triple<String, Pair<String, String>, Triple<String, String, String>>> mle = (MultilineListEntry<Triple<String, Pair<String, String>, Triple<String, String, String>>>) constraintsListModel.get(i);
			set.add(mle.getObject());
		}
		return set;
	}
	
	public void addConstraint(String name, String activity1, String activity2, String condition1, String condition2, String condition3) {
		constraintsListModel.addElement(new MultilineListEntry<Triple<String, Pair<String, String>, Triple<String, String, String>>>(
				new Triple<String, Pair<String, String>, Triple<String, String, String>>(
						name,
						new Pair<String, String>(activity1, activity2),
						new Triple<String, String, String>(condition1, condition2, condition3)),
				name,
				"[" + activity1.replace("<", "&lt;") + "], [" + activity2.replace("<", "&lt;") + "]<br>"
						+ "[" + condition1.replace("<", "&lt;") + "][" + condition2.replace("<", "&lt;") + "][" + condition3.replace("<", "&lt;") + "]"));
		constraintsList.updateUI();
	}
	
	private void addNewConstraint() {
		JComboBox constraintsField = new JComboBox(constraintNames);
		JTextField activitiesField1 = new JTextField("", 15);
		JTextField activitiesField2 = new JTextField("", 15);
		JTextField conditionsField1 = new JTextField("", 15);
		JTextField conditionsField2 = new JTextField("", 15);
		JTextField conditionsField3 = new JTextField("", 15);
		JPanel newPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		newPanel.add(new JLabel("Constraint:", JLabel.RIGHT));
		newPanel.add(constraintsField);
		newPanel.add(new JLabel("First activity:", JLabel.RIGHT));
		newPanel.add(activitiesField1);
		newPanel.add(new JLabel("Second activity (optional):", JLabel.RIGHT));
		newPanel.add(activitiesField2);
		newPanel.add(new JLabel("Activation condition (optional):", JLabel.RIGHT));
		newPanel.add(conditionsField1);
		newPanel.add(new JLabel("Constraint condition (optional):", JLabel.RIGHT));
		newPanel.add(conditionsField2);
		newPanel.add(new JLabel("Time condition (optional):", JLabel.RIGHT));
		newPanel.add(conditionsField3);
		
		int result = JOptionPane.showConfirmDialog(this, newPanel, "Please Enter Constraint Details", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			String name = constraintsField.getSelectedItem().toString();
			String activity1 = activitiesField1.getText();
			String activity2 = activitiesField2.getText();
			String condition1 = conditionsField1.getText();
			String condition2 = conditionsField2.getText();
			String condition3 = conditionsField3.getText();
			
			addConstraint(name, activity1, activity2, condition1, condition2, condition3);
		}
	}
	
	private void editConstraint(MultilineListEntry<Triple<String, Pair<String, String>, Triple<String, String, String>>> constraint) {
		JComboBox constraintsField = new JComboBox(constraintNames);
		constraintsField.setSelectedItem(constraint.getObject().getFirst());
		JTextField activitiesField1 = new JTextField(constraint.getObject().getSecond().getFirst(), 15);
		JTextField activitiesField2 = new JTextField(constraint.getObject().getSecond().getSecond(), 15);
		JTextField conditionsField1 = new JTextField(constraint.getObject().getThird().getFirst(), 15);
		JTextField conditionsField2 = new JTextField(constraint.getObject().getThird().getSecond(), 15);
		JTextField conditionsField3 = new JTextField(constraint.getObject().getThird().getThird(), 15);
		JPanel newPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		newPanel.add(new JLabel("Constraint:", JLabel.RIGHT));
		newPanel.add(constraintsField);
		newPanel.add(new JLabel("First activity:", JLabel.RIGHT));
		newPanel.add(activitiesField1);
		newPanel.add(new JLabel("Second activity (optional):", JLabel.RIGHT));
		newPanel.add(activitiesField2);
		newPanel.add(new JLabel("Activation condition (optional):", JLabel.RIGHT));
		newPanel.add(conditionsField1);
		newPanel.add(new JLabel("Constraint condition (optional):", JLabel.RIGHT));
		newPanel.add(conditionsField2);
		newPanel.add(new JLabel("Time condition (optional):", JLabel.RIGHT));
		newPanel.add(conditionsField3);
		
		int result = JOptionPane.showConfirmDialog(this, newPanel, "Please Enter Constraint Details", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			String name = constraintsField.getSelectedItem().toString();
			String activity1 = activitiesField1.getText();
			String activity2 = activitiesField2.getText();
			String condition1 = conditionsField1.getText();
			String condition2 = conditionsField2.getText();
			String condition3 = conditionsField3.getText();
			
			addConstraint(name, activity1, activity2, condition1, condition2, condition3);
			constraintsListModel.removeElement(constraint);
		}
	}
}
