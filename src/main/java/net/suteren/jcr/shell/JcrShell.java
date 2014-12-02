// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   JackrabbitTool.java

package net.suteren.jcr.shell;

import gnu.getopt.Getopt;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.ReadlineLibrary;

// Referenced classes of package cz.empire.common.jcr.tools:
//            JcrTool

public class JcrShell extends JcrTool {

	protected static final String JCR_ARGUMENTS = "hU:W:uw";

	public class CtrlCHook extends Thread {
		@Override
		public void run() {
			log.debug("Shutdown catched.");
			while (true) {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					log.debug("Shutdown hook interupded.");
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
					}
					break;

				}
			}
		}
	}

	private static final String COMMAND_SUFFIX = "Command";
	protected static final String ACTUAL_NODE = ".";
	protected static final String UNIT_NAME = "JcrShell";
	protected static final String HISTORY_FILE = ".jackrabbittool.history";
	protected static final String MKNOD_CMD = "mknod";
	protected static final String IMPORT_CMD = "import";
	protected static final String DUMP_CMD = "dump";
	protected static final String PRINT_CMD = "print";
	protected static final String RMPROP_CMD = "rmprop";
	protected static final String LS_CMD = "ls";
	protected static final String CD_CMD = "cd";
	protected static final String CMDLINE_HELP = "help";
	protected static final String PATH_SEPARATOR = "/";
	private static final String ROOT_NODE = "/jcr:root";
	private static final String SETPROP_CMD = "setprop";
	private static final String ADDPROP_CMD = "addprop";
	private static final int MAX_PATH_LENGTH = 40;
	private static final String PATH_STRIP_PREFIX = "...";
	private static final String COUNT_CMD = null;

	protected static Log log = LogFactory.getLog(UNIT_NAME);
	protected Node cwd = null;
	protected Node oldcwd = null;
	boolean commited = true;
	protected int returnStatement = 0;
	File history = new File(HISTORY_FILE);

	ReadlineLibrary[] readlineLibraries = { ReadlineLibrary.GnuReadline,
			ReadlineLibrary.Getline, ReadlineLibrary.Editline,
			ReadlineLibrary.PureJava };
	protected JcrToolCompleter completer = null;
	protected Credentials credentials;

	protected JcrShell() {
		super(null);
	}

	public JcrShell(Repository newRepository) {
		super(newRepository);
		log = LogFactory.getLog(this.getClass());
	}

	protected String[] tokenize(String params, int i) {
		int c = 1;
		Vector<String> out = new Vector<String>();
		String buf = "";
		boolean quote = false;
		boolean vacuum = true;
		for (int j = 0; j < params.length(); j++) {
			log.trace("cahr: '" + params.charAt(j) + "'");
			switch (params.charAt(j)) {
			case ' ':
			case '\n':
			case '\t':
				log.debug("empty character");
				if (!vacuum) {
					vacuum = true;
					if (quote) {
						buf = buf + params.charAt(j);
					} else {
						out.add(buf);
						buf = "";
						c++;
						if (i > 0 && c >= i) {
							out.add(params.substring(j + 1));
							log.debug("Out length: " + out.size());
							log.debug("Out: " + out);
							return out.toArray(new String[out.size()]);
						}
					}
				}
				break;
			case '"':
			case '\'':
				log.debug("apostrophe");
				vacuum = false;
				if (quote)
					quote = false;
				else
					quote = true;
				break;
			case '\\':
				log.debug("backslash");
				j++;
			default:
				log.debug("character '" + params.charAt(j) + "'");
				vacuum = false;
				buf = buf + params.charAt(j);
			}
		}
		if (buf != null && !"".equals(buf))
			out.add(buf);
		log.debug("out length: " + out.size());
		log.debug("out: " + out);
		if (out.size() == 0)
			return new String[0];
		return out.toArray(new String[out.size()]);
	}

	protected String propertiesToString(PropertyIterator pi, boolean verbose)
			throws RepositoryException {
		String out = "";
		while (pi.hasNext()) {
			Property p = pi.nextProperty();
			String name = p.getName();
			if (verbose) {
				String value = null;
				PropertyDefinition pd = p.getDefinition();
				if (pd.isMultiple()) {
					value = "[";
					Value[] v = p.getValues();
					for (int i = 0; i < v.length; i++) {
						if (i > 0)
							value = value + ", ";
						value = value + v[i].getString();
					}
					value = value + "]";
				} else {
					value = p.getValue().getString();
				}
				out = out + "@" + name + "=\"" + value + "\""
						+ (pi.hasNext() ? "\t" : "");
			} else {
				out = out + "@" + name + (pi.hasNext() ? ", " : "");
			}
		}
		return out;
	}

	protected void initReadLine(String unitName, ReadlineCompleter completer)
			throws UnableToInitializeReadlineException, EOFException,
			UnsupportedEncodingException {
		boolean success = false;
		for (int i = 0; i < readlineLibraries.length; i++) {
			try {
				Readline.load(readlineLibraries[i]);
				success = true;
				log.info("Using " + readlineLibraries[i].getClass().getName()
						+ " (" + i + ")");
				break;
			} catch (UnsatisfiedLinkError e) {
				log.error("Unable to get satisfying readline library: " + e);
			}
		}

		if (!success) {
			log.fatal("Unable to initialize readline!");
			throw new UnableToInitializeReadlineException();
		}
		Readline.initReadline(unitName);
		Readline.readHistoryFile(history.getName());
		Readline.parseAndBind("\"\\e[18~\": \"Function key F7\"");
		Readline.parseAndBind("\"\\e[19~\": \"Function key F8\"");
		Readline.setWordBreakCharacters(" \t;");

	}

	protected int handleException(Exception e) {
		log.error(e);
		log.debug(stackTrace(e));
		System.out.println();
		System.out.println("ERROR: " + e.getMessage() != null ? e.getMessage()
				: e.getClass().getName() + ".");
		System.out.println();
		return 1;
	}

	private Object stackTrace(Exception e) {
		Writer w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		e.printStackTrace(pw);
		return w.toString();
	}

	protected String absolutePath(String relativePath)
			throws RepositoryException {
		log.debug("Relative path: " + relativePath);
		String currentPath = cwd.getPath();
		log.debug("Current path: " + cwd.getPath());
		if (ACTUAL_NODE.equals(relativePath))
			relativePath = cwd.getPath();
		if (!relativePath.startsWith(PATH_SEPARATOR))
			relativePath = currentPath
					+ (currentPath.endsWith(PATH_SEPARATOR) ? ""
							: PATH_SEPARATOR) + relativePath;

		log.debug("Absolute path: " + relativePath);
		return ROOT_NODE + relativePath;
	}

	protected NodeIterator selectNodes(String string, boolean children)
			throws RepositoryException, IOException {
		log.debug("Requested path: " + string);
		boolean localChildren = !(string.contains("*") || string.contains("//"));

		String query = "";
		if (string == null)
			string = ACTUAL_NODE;
		// while (string.endsWith(PATH_SEPARATOR))
		// string = string.substring(0, string.length() - 1);
		if ("".equals(string))
			string = ACTUAL_NODE;

		query = absolutePath(string.trim());

		if (children && localChildren) {
			query = query
					+ (query.endsWith(PATH_SEPARATOR) ? "" : PATH_SEPARATOR)
					+ "*";
		}

		log.debug("Query: " + query);
		return executeXpathQuery(query).getNodes();
	}

	public int helpCommand(String[] args) {
		String cmd = null;
		if (args.length > 0)
			cmd = args[0];
		// TODO doplnit helpy a udelat je lokalizovany.
		if (cmd != null) {
			if (LS_CMD.equals(cmd)) {
				System.out.println("Parametry jsou: -l -t -r -1 -d");
			} else if (CMDLINE_HELP.equals(cmd)) {
				System.out
						.println("Pouziti: java -Djcr.home=... -Djcr.config=.. "
								+ this.getClass().getName() + " AKCE");
				System.out.println("Prikazy AKCE jsou: " + getCommandNames());
			} else {
				System.out.println("Prikazy AKCE jsou: " + getCommandNames());
			}
		} else {
			System.out.println("Pouziti: java -Djcr.home=... -Djcr.config=.. "
					+ this.getClass().getName() + " AKCE");
			System.out.println("Prikazy AKCE jsou: " + getCommandNames());
		}
		return 0;
	}

	public int loginCommand(String[] args) {
		Getopt getopt = new Getopt(IMPORT_CMD, args, "U:W:wu");
		int c;
		String userName = null;
		char[] password = null;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'U':
				userName = getopt.getOptarg();
				break;
			case 'W':
				password = getopt.getOptarg().toCharArray();
				break;
			case 'w':
				try {
					password = Readline.readline("Password: ", false)
							.toCharArray();
				} catch (Exception e) {
					handleException(e);
				}
				break;
			case 'u':
				try {
					userName = Readline.readline("User: ", false);
				} catch (Exception e) {
					handleException(e);
				}
				break;
			case '?':
				String[] arg = {};
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		Credentials cred = new SimpleCredentials(userName, password);
		boolean logged = false;
		while (!logged) {
			try {
				try {
					log.debug(getSession());
				} catch (Exception e) {
				}
				login(cred);
				logged = true;
			} catch (LoginException e) {
				System.out.println("Lognin failed: "
						+ (e.getLocalizedMessage() != null ? e
								.getLocalizedMessage() : e.getMessage()));
				handleException(e);
			} catch (Exception e) {
				System.out.println("Lognin failed: " + e);
				handleException(e);
				return 1;
			}
		}
		return 0;
	}

	public int pwdCommand(String[] args) {
		System.out.println();
		try {
			System.out.println(cwd.getPath());
		} catch (Exception e) {
			System.out.println("ERROR: "
					+ (e.getLocalizedMessage() != null ? e
							.getLocalizedMessage() : e.getMessage()));
			handleException(e);
			return 1;
		} finally {
			System.out.println();
		}
		return 0;
	}

	public int cdCommand(String[] args) {
		if (args.length > 1) {
			System.out.println("Too many parameters.");
			return 2;
		}
		try {
			if (PATH_SEPARATOR.equals(args[0])) {
				oldcwd = cwd;
				cwd = getRootNode();
				((JcrToolCompleter) Readline.getCompleter()).setNode(cwd);
				return 0;
			} else if ("-".equals(args[0])) {
				Node tmp = cwd;
				cwd = oldcwd;
				oldcwd = tmp;
				((JcrToolCompleter) Readline.getCompleter()).setNode(cwd);
				return 0;
			} else if (ACTUAL_NODE.equals(args[0])) {
				return 0;
			} else {
				Node n = cwd.getNode(args[0]);
				oldcwd = cwd;
				cwd = n;
				((JcrToolCompleter) Readline.getCompleter()).setNode(cwd);
				return 0;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out
					.println("Command cd requires exactly one parameter - nodename.");
			return 2;
		} catch (PathNotFoundException e) {
			System.out.println("Node " + args[0] + " does not exists.");
			return 1;
		} catch (NullPointerException e) {
			System.out
					.println("Command cd requires exactly one parameter - nodename.");
			return 2;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	@SuppressWarnings("unused")
	public int lsCommand(String[] args) {
		Getopt getopt = new Getopt(LS_CMD, args, "ltr1d");

		int c;
		boolean detail = false;
		int sort = 0;
		boolean descending = false;
		boolean collumns = false;
		boolean children = true;

		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'l':
				detail = true;
				collumns = true;
				break;
			case 't':
				sort = 1;
				break;
			case 'r':
				descending = true;
				break;
			case '1':
				collumns = true;
				break;
			case 'd':
				children = false;
				break;
			case '?':
				String[] arg = { LS_CMD };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}

		int firstarg = getopt.getOptind();
		NodeIterator ani = new AppendingNodeIterator();
		try {
			if (args.length - firstarg > 0) {
				for (int i = firstarg; i < args.length; i++) {
					((AppendingNodeIterator) ani).addNodeIterator(selectNodes(
							args[i], children));
				}
			} else {
				ani = selectNodes(ACTUAL_NODE, children);
			}
			long count = ani.getSize();
			while (ani.hasNext()) {
				Node n = ani.nextNode();
				if (detail) {
					System.out.println(n.getPath() + "\t"
							+ propertiesToString(n.getProperties(), false));

				} else {
					if (collumns)
						System.out.println(n.getName());
					else
						System.out.print(n.getName()
								+ (ani.hasNext() ? "\t" : "\n"));
				}
			}
			System.out.println("\nFound " + count + " nodes\n");
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	@SuppressWarnings("unused")
	public int countCommand(String[] args) {
		Getopt getopt = new Getopt(LS_CMD, args, "r");

		int c;
		int sort = 0;
		boolean children = false;

		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'r':
				children = true;
				break;
			case '?':
				String[] arg = { COUNT_CMD };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}

		int firstarg = getopt.getOptind();
		NodeIterator ani = new AppendingNodeIterator();
		try {
			if (args.length - firstarg > 0) {
				for (int i = firstarg; i < args.length; i++) {
					((AppendingNodeIterator) ani).addNodeIterator(selectNodes(
							args[i], children));
				}
			} else {
				ani = selectNodes(ACTUAL_NODE, children);
			}
			long count = ani.getSize();
			System.out.println("\nFound " + count + " nodes\n");
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	@SuppressWarnings("unused")
	public int rmnodCommand(String[] args) {
		Getopt getopt = new Getopt(MKNOD_CMD, args, "f");
		int c;
		boolean force = false;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'f':
				force = true;
				break;
			case '?':
				String[] arg = { MKNOD_CMD };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		NodeIterator ani = new AppendingNodeIterator();
		try {
			if (args.length - firstarg > 0) {
				for (int i = firstarg; i < args.length; i++) {
					((AppendingNodeIterator) ani).addNodeIterator(selectNodes(
							args[i], false));
				}
			} else {
				System.out.println("Unsupported number of arguments.");
				return 2;
			}
			while (ani.hasNext()) {
				Node n = ani.nextNode();
				n.remove();
				commited = false;
			}
			return 0;
		} catch (Exception e) {
			handleException(e);
			return 1;
		}
	}

	public int printCommand(String[] args) {
		Getopt getopt = new Getopt(PRINT_CMD, args, "v");
		int c;
		boolean verbose = false;
		boolean children = false;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'v':
				verbose = true;
				break;
			case 'r':
				children = true;
				break;
			case '?':
				String[] arg = { PRINT_CMD };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		NodeIterator ani = new AppendingNodeIterator();
		try {
			System.out.println();
			if (args.length - firstarg > 0) {
				for (int i = firstarg; i < args.length; i++) {
					((AppendingNodeIterator) ani).addNodeIterator(selectNodes(
							args[i], children));
				}
			} else {
				System.out
						.println("+"
								+ cwd.getName()
								+ "\n  "
								+ propertiesToString(cwd.getProperties(),
										verbose) + "");
				System.out.println();
				return 0;
			}
			while (ani.hasNext()) {
				Node n = ani.nextNode();
				System.out.println("+" + n.getName() + "\n  "
						+ propertiesToString(n.getProperties(), verbose) + "");
				System.out.println();
			}
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}

	}

	public int dumpCommand(String[] args) {
		Getopt getopt = new Getopt(DUMP_CMD, args, "rf");
		int c;
		boolean children = false;
		boolean full = false;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'r':
				children = true;
				break;
			case 'f':
				full = true;
				break;
			case '?':
				String[] arg = { DUMP_CMD };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		try {
			String path = ROOT_NODE + cwd.getPath();
			OutputStream out = System.out;
			if (args.length - firstarg > 0)
				path = args[firstarg];
			if (args.length - firstarg > 1)
				out = new FileOutputStream(args[firstarg + 1]);
			getSession().exportDocumentView(path, out, !full, !children);
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	public int importCommand(String[] args) {
		Getopt getopt = new Getopt(IMPORT_CMD, args, "b:");
		int c;
		int behavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
		while ((c = getopt.getopt()) != -1) {
			log.trace("c: " + (char) c);
			switch (c) {
			case 'b':
				try {
					Field field = ImportUUIDBehavior.class.getField(getopt
							.getOptarg());
					behavior = field.getInt(null);
				} catch (NoSuchFieldException e) {
					System.out.println("Unsupported behavior: "
							+ getopt.getOptarg() + ".");
					return 2;
				} catch (Exception e) {
					return handleException(e);
				}
				break;
			case '?':
				String[] arg = {};
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		try {
			String path = ROOT_NODE;
			if (cwd != null)
				path = ROOT_NODE + cwd.getPath();
			InputStream is = System.in;
			if (args.length - firstarg > 0)
				path = args[firstarg];
			log.debug("Path: " + path);
			if (args.length - firstarg > 1) {
				is = new FileInputStream(args[firstarg + 1]);
				log.debug("Input: " + args[firstarg + 1]);
			} else {
				log.debug("Input: STDIN");
			}
			getSession().importXML(path, is, behavior);
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	public int commitCommand(String[] args) {
		try {
			commit();
			commited = true;
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	public int rollbackCommand(String[] args) {
		try {
			login(credentials);
			commited = true;
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	@SuppressWarnings("unused")
	public int mknodCommand(String[] args) {
		Getopt getopt = new Getopt("mknod", args, "t:");
		int c;
		String type = null;
		boolean verbose = false;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 't':
				type = getopt.getOptarg();
				break;
			case '?':
				String[] arg = { "" };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		if (args.length - firstarg > 1) {
			System.out.println("Too many arguments.");
			return 2;
		}
		try {
			if (type != null) {
				cwd.addNode(args[firstarg], type);
			} else {
				cwd.addNode(args[firstarg]);
			}
			commited = false;
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	public int rmnsCommand(String[] args) {
		int status = 0;
		for (String i : args) {
			try {
				getNamespaceRegistry().unregisterNamespace(i);
				commited = false;
			} catch (NamespaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status = 1;
			} catch (UnsupportedRepositoryOperationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status = 1;
			} catch (AccessDeniedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status = 1;
			} catch (LoginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status = 1;
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status = 1;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status = 1;
			}
		}
		return status;
	}

	public int lsnsCommand(String[] args) {
		NamespaceRegistry nsr;
		try {
			System.out.println();
			nsr = getNamespaceRegistry();
			for (String i : nsr.getPrefixes()) {
				System.out.println(i + ":" + nsr.getURI(i));
			}
			return 0;
		} catch (LoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.out.println();
		}

		return 1;
	}

	public int addnsCommand(String[] args) {
		if (args.length != 2) {
			System.out.println("addns requres exactly 2 parameters!");
			log.error("addns requres exactly 2 parameters!");
			return 2;
		}
		try {
			getNamespaceRegistry().registerNamespace(args[0], args[1]);
			commited = false;
			return 0;
		} catch (NamespaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedRepositoryOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AccessDeniedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1;
	}

	public int setpropCommand(String[] args) {
		Getopt getopt = new Getopt(SETPROP_CMD, args, "p:");
		int c;
		String path = null;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'p':
				path = getopt.getOptarg();
				break;
			case '?':
				String[] arg = { "" };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		if (args.length - firstarg < 2) {
			System.out.println("Too few arguments.");
			return 2;
		}

		NodeIterator ni = null;
		try {
			if (path == null)
				ni = selectNodes(ACTUAL_NODE, false);
			else
				ni = selectNodes(path, false);
		} catch (RepositoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String[] params = new String[args.length - firstarg - 1];
		log.debug("args.length: " + args.length);
		log.debug("params.length: " + params.length);
		log.debug("firstarg: " + firstarg);
		log.debug("path: " + path);
		for (int i = 0; i < params.length; i++) {
			params[i] = args[firstarg + i + 1];
		}
		log.debug("params: " + params);
		log.debug("name: " + args[firstarg]);
		int state = 0;
		while (ni.hasNext()) {
			Node n = ni.nextNode();
			try {
				try {
					if (n.getProperty(args[firstarg]).getDefinition()
							.isMultiple())
						n.setProperty(args[firstarg], params);
					else {
						String out = "";
						for (String i : params)
							out = out + " " + i;
						n.setProperty(args[firstarg], out.trim());
					}
				} catch (PathNotFoundException e) {
					try {
						n.setProperty(args[firstarg], params);
					} catch (Exception e1) {
						String out = "";
						for (String i : params)
							out = out + " " + i;
						n.setProperty(args[firstarg], out.trim());
					}
				}
				commited = false;
			} catch (Exception e) {
				state = handleException(e);
			}

		}
		return state;
	}

	@SuppressWarnings("unused")
	public int rmpropCommand(String[] args) {
		Getopt getopt = new Getopt(RMPROP_CMD, args, "f");
		int c;
		boolean force = false;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'f':
				force = true;
				break;
			case '?':
				String[] arg = { RMPROP_CMD };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		String propName = args[firstarg];
		NodeIterator ani = new AppendingNodeIterator();
		try {
			if (args.length - firstarg > 1) {
				for (int i = firstarg + 1; i < args.length; i++) {
					((AppendingNodeIterator) ani).addNodeIterator(selectNodes(
							args[i], false));
				}
			} else {
				System.out.println("Unsupported number of arguments.");
				return 2;
			}
			while (ani.hasNext()) {
				Node n = ani.nextNode();
				try {
					n.getProperty(propName).remove();
				} catch (PathNotFoundException e) {
					System.out.println("Property " + propName
							+ " does not exists in node " + n.getPath()
							+ ACTUAL_NODE);
				}
				commited = false;
			}
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	public int addpropCommand(String[] args) {
		Getopt getopt = new Getopt(ADDPROP_CMD, args, "");
		int c;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case '?':
				String[] arg = { "" };
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		int firstarg = getopt.getOptind();
		if (args.length - firstarg != 2) {
			System.out.println(ADDPROP_CMD
					+ " requires exactly two parameters.");
			return 2;
		}
		try {
			String out = "";
			for (int i = firstarg; i < args.length; i++)
				out = out + args[i];
			try {
				Property p = cwd.getProperty(args[firstarg]);
				String[] v = new String[p.getValues().length + 1];
				for (int i = 0; i < v.length - 1; i++)
					v[i] = p.getValues()[i].getString();
				v[v.length] = out;
				cwd.setProperty(args[firstarg], v);
			} catch (PathNotFoundException e) {
				Property p = cwd.setProperty(args[firstarg], out);
			}
			commited = false;
			return 0;
		} catch (Exception e) {
			return handleException(e);
		}
	}

	private Vector<String> getCommandNames() {
		Method[] met = this.getClass().getMethods();
		Vector<String> commands = new Vector<String>();
		for (int i = 0; i < met.length; i++) {
			if (met[i].getName().endsWith(COMMAND_SUFFIX)) {
				String command = met[i].getName().substring(0,
						met[i].getName().length() - COMMAND_SUFFIX.length());
				commands.add(command);
				log.debug("Command " + command + " added to completer.");
			}
		}
		commands.add("quit");
		return commands;
	}

	@SuppressWarnings("unchecked")
	public int console() {

		completer = new JcrToolCompleter(this);
		Readline.setCompleter(completer);
		String line = null;
		log.debug("Command exit added to completer.");
		completer.setCommands(getCommandNames());
		try {
			cwd = getRootNode();
			completer.setNode(cwd);
		} catch (Exception e) {
			return handleException(e);
		}
		Thread shutdown = new CtrlCHook();
		Runtime.getRuntime().addShutdownHook(shutdown);
		while (true) {
			try {
				String path = null;
				try {
					path = cwd.getPath();
				} catch (Exception e) {
					path = "<N/A>";
				}
				if (path.length() > MAX_PATH_LENGTH)
					path = PATH_STRIP_PREFIX
							+ path.substring(path.length()
									+ PATH_STRIP_PREFIX.length()
									- MAX_PATH_LENGTH);
				line = Readline.readline("jcr:" + path + "> ");
				String[] tokens = null;
				if (line != null)
					tokens = tokenize(line, 0);
				else
					continue;
				String cmd = null;
				String[] params = new String[tokens.length - 1];
				try {
					cmd = tokens[0];
					for (int i = 0; i < params.length; i++)
						params[i] = tokens[i + 1];
				} catch (IndexOutOfBoundsException e) {
				} catch (NullPointerException e) {
				}

				if ("exit".equals(cmd) || "quit".equals(cmd)) {
					if (!commited)
						System.out
								.println("Not commited. Use commit or rollback.");
					else {
						returnStatement = 0;
						break;
					}
				} else {
					try {
						Class[] paramTypes = { params.getClass() };
						Method command = getClass().getMethod(
								cmd + COMMAND_SUFFIX, paramTypes);
						returnStatement = ((Integer) command.invoke(this,
								(Object) params)).intValue();
					} catch (NoSuchMethodException e) {
						handleException(e);
						Method[] methods = getClass().getMethods();
						for (int i = 0; i < methods.length; i++) {
							Type[] t = methods[i].getGenericParameterTypes();
							String ty = "";
							for (int j = 0; j < t.length; j++) {
								ty = ty + (j < t.length - 1 ? ", " : "")
										+ t.toString();
							}
							log.debug(methods[i].getName() + "(" + ty + ")");
						}
						System.out.println("Unknown command: " + cmd
								+ ".\nTry to get help");
						returnStatement = 2;
					} catch (IllegalArgumentException e) {
						handleException(e);
						Method[] methods = getClass().getMethods();
						for (int i = 0; i < methods.length; i++) {
							Type[] t = methods[i].getGenericParameterTypes();
							String ty = "";
							for (int j = 0; j < t.length; j++) {
								ty = ty + (j < t.length - 1 ? ", " : "")
										+ t.toString();
							}
							log.debug(methods[i].getName() + "(" + ty + ")");
						}
					}
				}

			} catch (Exception e) {
				handleException(e);
			}
		}
		try {
			Readline.writeHistoryFile(history.getName());
		} catch (Exception e) {
			returnStatement = handleException(e);
		}
		Readline.cleanup();
		if (shutdown.isAlive())
			shutdown.interrupt();
		else
			Runtime.getRuntime().removeShutdownHook(shutdown);
		return returnStatement;
	}

	public int runApp(String[] args) {
		log.debug("java.library.path: "
				+ System.getProperty("java.library.path"));
		log.debug("Args: " + args);
		try {
			initReadLine(UNIT_NAME, completer);
		} catch (UnableToInitializeReadlineException e) {
			return handleException(e);
		} catch (Exception e) {
			handleException(e);
		}
		Getopt getopt = new Getopt(UNIT_NAME, args, JCR_ARGUMENTS);
		int c;
		String command = "console";
		String userName = null;
		char[] password = null;
		boolean login = false;
		boolean logged = false;
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'h':
				command = "help";
				break;
			case 'w':
				login = true;
				password = readPassword();
				break;
			case 'u':
				login = true;
				userName = readUserName();
				break;
			case 'U':
				login = true;
				userName = getopt.getOptarg();
				break;
			case 'W':
				login = true;
				password = getopt.getOptarg().toCharArray();
				break;
			case '?':
				String[] arg = {};
				helpCommand(arg);
				return 2;
			default:
				System.out.print("getopt() returned " + c + "\n");
			}
		}
		if (login) {
			while (true) {
				if (password == null)
					password = readPassword();
				if (userName == null)
					userName = readUserName();
				try {
					login(new SimpleCredentials(userName, password));
					System.out.println("Successfully logged in as " + userName
							+ ".");
					logged = true;
					break;
				} catch (LoginException e) {
					System.out.println("Lognin failed: "
							+ (e.getLocalizedMessage() != null ? e
									.getLocalizedMessage() : e.getMessage()));
					handleException(e);
				} catch (Exception e) {
					System.out.println("Login failed: "
							+ (e.getLocalizedMessage() != null ? e
									.getLocalizedMessage() : e.getMessage()));
					handleException(e);
					break;
				}
			}
		}

		int firstarg = getopt.getOptind();
		log.debug(args.length + " args.");
		log.debug("Firstarg: " + firstarg);
		if (args.length - firstarg > 1) {
			command = args[firstarg];
		}
		String[] params = {};
		if (args.length > firstarg) {
			log.trace("Args length: " + args.length);
			log.trace("Firstarg: " + firstarg);
			params = new String[args.length - firstarg - 1];
			for (int i = 0; i < params.length && i < args.length + firstarg + 1; i++) {
				params[i] = args[firstarg + i + 1];
			}
		}
		log.debug("Command " + command);
		if ("console".equals(command)) {
			console();
		} else if ("help".equals(command)) {
			helpCommand(params);
		} else {
			if (!logged) {
				log.warn("Not logged in => no write permission");
				System.out.println("Not logged in => no write permission");
			}
			try {
				log.debug("params: " + params);
				log.debug("paramsl: " + params.length);
				log.debug("paramsc: " + params.getClass());
				Method commandM = this.getClass().getMethod(
						command + COMMAND_SUFFIX, params.getClass());
				Object[] arg = new Object[1];
				arg[0] = params;
				returnStatement = ((Integer) commandM.invoke(this, arg))
						.intValue();
				commit();
			} catch (NoSuchMethodException e) {
				System.out.println("Unknown command: " + command
						+ ".\nTry to get help");
				returnStatement = 2;
			} catch (Exception e) {
				return handleException(e);
			}

		}
		return 0;
	}

	private String readUserName() {
		try {
			return Readline.readline("Username: ", false);
		} catch (Exception e) {
			handleException(e);
			return null;
		}
	}

	private char[] readPassword() {
		try {
			return Readline.readline("Password: ", false).toCharArray();
		} catch (Exception e) {
			handleException(e);
			return null;
		}
	}
}
