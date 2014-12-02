package net.suteren.jcr.shell;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gnu.readline.ReadlineCompleter;

public class JcrToolCompleter extends JcrTool implements ReadlineCompleter {

	private Vector<String> commands = new Vector<String>();
	private NodeIterator nodes = null;
	private Log log = LogFactory.getLog(this.getClass());
	private Node cwd = null;
	private JcrShell shell = null;
	private Vector<String> slectedCommands = null;

	public JcrToolCompleter(JcrShell shell) {
		super(shell.getRepository());
		this.shell = shell;
	}

	public void setCommands(Vector<String> commands) {
		this.commands = commands;
	}

	public void setNode(Node cwd) {
		this.cwd = cwd;
	}

	public String completer(String t, int s) {
		// TODO Predelat completer tak, aby doplnoval i do zanorenych urovni.
		// Veskery ziskavani nodename se presune do s=0 a naplni se vlastni pole
		// stringu, ktery bude obsahovat cesty vzhledem k aktualnimu umisteni
		log.debug("t: '" + t + "; s: " + s);
		slectedCommands = new Vector<String>();
		if (s == 0) {
			try {
				String path = "";
				try {
					path = t.substring(0, t
							.lastIndexOf(JcrShell.PATH_SEPARATOR));
				} catch (Exception e) {
				}
				nodes = shell.selectNodes(path, true);
				// nodes = cwd.getNodes(t + "*");
			} catch (Exception e) {
				log.error(e);
			}

			log.debug("all commands: " + commands);
			for (String c : commands) {
				log.debug("command " + c + "...");
				if (c.startsWith(t)) {
					slectedCommands.add(c);
					log.debug("selected command " + c);
				}
			}
			try {
				if (s < slectedCommands.size())
					return slectedCommands.elementAt(s);
			} catch (NullPointerException e) {
				log.error(e);
			}
		}
		try {
			while (nodes.hasNext())
				try {
					String n = nodes.nextNode().getName();
					String tl = t;
					if (tl.contains(JcrShell.PATH_SEPARATOR))
						tl = tl.substring(tl
								.lastIndexOf(JcrShell.PATH_SEPARATOR) + 1);
					log.debug("TL: " + tl);
					if (n.startsWith(tl))
						return n;
				} catch (RepositoryException e) {
					log.error(e);
					return null;
				}
		} catch (NullPointerException e) {
			log.debug(e);
		}
		return null;
	}
}
