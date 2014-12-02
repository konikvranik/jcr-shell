package net.suteren.jcr.shell;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class ChildrenNodeIterator extends AppendingNodeIterator implements NodeIterator {

	NodeIterator parents = null;
	Node actualNode = null;
	NodeIterator childIterator;
	private int counter;

	public ChildrenNodeIterator(NodeIterator ni) {
		parents = ni;
	}

	public Node nextNode() {

		if (childIterator == null) {
			if (parents == null)
				throw new NoSuchElementException();
			if (actualNode == null)
				if (parents.hasNext())
					actualNode = parents.nextNode();
				else
					throw new NoSuchElementException();

			if (actualNode == null)
				throw new NoSuchElementException();
			else {
				try {
					childIterator = actualNode.getNodes();
				} catch (RepositoryException e) {
					throw new NoSuchElementException();
				}
			}
		}
		if (childIterator.hasNext())
			return childIterator.nextNode();
		else {
			while (parents.hasNext()) {
				actualNode = parents.nextNode();
				if (childIterator.hasNext()) {
					counter++;
					return childIterator.nextNode();
				}
			}
			throw new NoSuchElementException();
		}
	}

	public long getPosition() {
		return counter;
	}

	public long getSize() {
		throw new UnsupportedOperationException();
	}

	public void skip(long skipNum) {
		
	}

	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	public Object next() {
		// TODO Auto-generated method stub
		return null;
	}

	public void remove() {
		// TODO Auto-generated method stub

	}

}
