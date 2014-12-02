package net.suteren.jcr.shell;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class SingleNodeIterator implements NodeIterator {

	private Node node = null;
	private boolean next = false;

	public SingleNodeIterator(Node n) {
		node = n;
		if (node != null)
			next = true;
	}

	public Node nextNode() {
		return node;
	}

	public long getPosition() {
		return next ? 0 : 1;
	}

	public long getSize() {
		return node == null ? 0 : 1;
	}

	public void skip(long skipNum) {
		if (skipNum > 1 || !next || node == null)
			throw new NoSuchElementException();
	}

	public boolean hasNext() {
		return next;
	}

	public Object next() {
		return nextNode();
	}

	public void remove() {
		node = null;
	}

}
