package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Binary tree with support for operations on right and left leaves
 * 
 * @author Andrea Burattin
 * @author Fabrizio Maggi
 */
public class BinaryTree {
	
	/**
	 * Internal node of the binary tree
	 */
	class Node {
		int id = 0;
		ExtendibleTrace value;
		Node left;
		Node right;
		
		/**
		 * Creates a new empty node
		 */
		public Node() {
			value = new ExtendibleTrace();
			id = idCounter++;
		}
		
		/**
		 * Method to get the DOT representation of the current node (and its
		 * subgraph)
		 * 
		 * @return the DOT representation
		 */
		private String getDot() {
			
			String left = "";
			String right = "";
			String isViolation = (value.isViolation())? "fillcolor=red, style=\"filled\", " : "";
			String activations = ""; 
			if (value.getActivations().size() > 0) {
				activations += "\\nacts: " + value.getActivations();
			}
			String dot = "node" + this.id + "["+ isViolation +"label=\"<f0> | <f1> trace: "+ this.value.toString() + activations  + " | <f2>\"];\n";
			
			if (this.left != null) {
				dot += this.left.getDot();
				left = "node" + this.id +":f0 -> node" + this.left.id +":f1;\n";
			}
			
			if (this.right != null) {
				dot += this.right.getDot();
				right = "node" + this.id +":f2 -> node" + this.right.id +":f1;\n";
			}
			
			dot = dot + left + right;
			return dot;
		}
		
		@Override
		public String toString() {
			String left = (this.left == null)? "NULL" : this.left.toString();
			String right = (this.right == null)? "NULL" : this.right.toString();
			return value.getTrace() + " ("+ left +" ; "+ right +")";
		}
	};
	
	private static int idCounter = 0;
	private Node root;
	private Set<Node> leftLeaves;
	private Set<Node> rightLeaves;
	
	/**
	 * Tree constructor
	 */
	public BinaryTree() {
		this.leftLeaves = new HashSet<Node>();
		this.rightLeaves = new HashSet<Node>();
		this.root = new Node();
		this.leftLeaves.add(root);
		this.rightLeaves.add(root);
	}
	
	/**
	 * Get only the leaves that are "left children"
	 * 
	 * @return
	 */
	protected Set<Node> getLeftLeaves() {
		Set<Node> leaves = new HashSet<Node>();

		for(Node n : leftLeaves) {
			if (!n.value.isViolation()) {
				leaves.add(n);
			}
		}
		
		return leaves;
	}
	
	/**
	 * Get only the leaves that are "right children"
	 * 
	 * @return
	 */
	protected Set<Node> getRightLeaves() {
		Set<Node> leaves = new HashSet<Node>();

		for(Node n : rightLeaves) {
			if (!n.value.isViolation()) {
				leaves.add(n);
			}
		}
		
		return leaves;
	}
	
	/**
	 * Get all the leaves of the tree
	 * 
	 * @return
	 */
	protected Set<Node> getLeaves() {
		Set<Node> leaves = new HashSet<Node>();

		for(Node n : leftLeaves) {
			if (!n.value.isViolation()) {
				leaves.add(n);
			}
		}
		for(Node n : rightLeaves) {
			if (!n.value.isViolation()) {
				leaves.add(n);
			}
		}
		
		return leaves;
	}
	
	/**
	 * 
	 * @param elements
	 * @param element
	 */
	protected void addLeftLeaf(Set<Node> leaves, Integer newLeafValue) {
		for(Node n : leaves) {
			if (!n.value.isViolation()) {
				addLeftLeaf(n, n.value.appendToNew(newLeafValue, false));
			}
		}
	}
	
	/**
	 * 
	 * @param elements
	 * @param element
	 */
	protected void addRightLeaf(Set<Node> leaves, Integer newLeafValue) {
		for(Node n : leaves) {
			if (!n.value.isViolation()) {
				addRightLeaf(n, n.value.appendToNew(newLeafValue, true));
			}
		}
	}
	
	/**
	 * 
	 * @param leaves
	 * @param newLeafValue
	 */
	protected void addLeftRightLeaf(Set<Node> leaves, Integer newLeafValue) {
		addLeftLeaf(leaves, newLeafValue);
		addRightLeaf(leaves, newLeafValue);
	}
	
	/**
	 * Method to get the DOT representation of the current tree
	 * 
	 * @param file 
	 * @throws IOException 
	 */
	public void toDotFile(File file) throws IOException {
		String dot = "digraph G {\n";
		dot += "node[shape=record];\n";
		dot += root.getDot();
		dot += "}";
		
		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(dot);
		out.close();
	}
	
	@Override
	public String toString() {
		return root.toString();
	}
	
	/**
	 * Add a left leaf to the node
	 * 
	 * @param tree
	 * @param element
	 */
	protected Node addLeftLeaf(Node tree, ExtendibleTrace element) {
		return addLeaf(tree, element, true);
	}
	
	/**
	 * Add a right leaf to the node
	 * 
	 * @param tree
	 * @param element
	 */
	protected Node addRightLeaf(Node tree, ExtendibleTrace element) {
		return addLeaf(tree, element, false);
	}

	private Node addLeaf(Node tree, ExtendibleTrace element, boolean addAsLeft) {
		if (leftLeaves.contains(tree)) {
			leftLeaves.remove(tree);
		}
		if (rightLeaves.contains(tree)) {
			rightLeaves.remove(tree);
		}
		
		Node n = new Node();
		n.value = element;
		
		if (addAsLeft) {
			tree.left = n;
			leftLeaves.add(n);
		} else {
			tree.right = n;
			rightLeaves.add(n);
		}
		
		return n;
	}
}
