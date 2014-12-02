package net.suteren.jcr.shell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class AppendingNodeIterator implements NodeIterator {

	List<NodeIterator> nodeIterators = new ArrayList<NodeIterator>();
	Iterator<NodeIterator> mainIterator = null;
	NodeIterator actualIterator = null;
	long counter = 0;

	public void addNodeIterator(NodeIterator ni) {
		nodeIterators.add(ni);
	}

	public Node nextNode() {
		if (actualIterator == null) {
			if (mainIterator == null) {
				mainIterator = nodeIterators.iterator();
				counter = 0;
			}
			if (mainIterator.hasNext()) {
				actualIterator = mainIterator.next();
			} else {
				return null;
			}
		}
		if (actualIterator.hasNext()) {
			counter = counter + 1L;
			return actualIterator.nextNode();
		} else
			while(mainIterator.hasNext()){
				actualIterator=mainIterator.next();
				if (actualIterator.hasNext())
					return actualIterator.nextNode();
			}
			return null;
	}

	public long getPosition() {
		return counter;
	}

	public long getSize() {
		long size = 0;
		for (NodeIterator ni : nodeIterators) {
			size = size + ni.getSize();
		}
		return size;
	}

	public void skip(long skipNum) {
		while (actualIterator.getSize() - actualIterator.getPosition() < skipNum) {
			if (actualIterator.hasNext()) {
				actualIterator = mainIterator.next();
				skipNum = skipNum - actualIterator.getSize()
						- actualIterator.getPosition();
			} else {
				actualIterator = null;
				mainIterator = null;
				return;
			}
		}
		actualIterator.skip(skipNum);
	}

	public boolean hasNext() {
		if (actualIterator == null) {
			if (mainIterator == null) {
				mainIterator = nodeIterators.iterator();
				counter = 0;
			}
			if (mainIterator.hasNext()) {
				actualIterator = mainIterator.next();
			} else {
				return false;
			}
		}
		if (actualIterator.hasNext()) {
			return true;
		} else
			while (mainIterator.hasNext()) {
				actualIterator = mainIterator.next();
				if (actualIterator.hasNext())
					return true;
			}
		return false;
	}

	public Object next() {
		return nextNode();
	}

	public void remove() {
		if (actualIterator != null) {
			actualIterator.remove();
		}
		throw new IllegalStateException();
	}

}
