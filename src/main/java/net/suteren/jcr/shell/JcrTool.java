// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   JcrTool.java

package net.suteren.jcr.shell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Credentials;
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
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JcrTool {
	private static final String NODE_PROPERTY_NAME = "name";
	private static final char UNDERLINE = '_';
	private static final String NUMBER_SHORTCUT = "n";

	public JcrTool(Repository newRepository) {
		session = null;
		workspace = null;
		queryManager = null;
		namespaceRegistry = null;
		nodeTypeManager = null;
		log = LogFactory.getLog(getClass());
		repository = newRepository;
	}

	public Repository getRepository() {
		return repository;
	}

	public Session login(Credentials credentials) throws LoginException,
			RepositoryException, IOException {
		if (credentials == null)
			session = getRepository().login();
		else {
			if (getSession() != null)
				session = getSession().impersonate(credentials);
			else
				session = getRepository().login(credentials);
		}
		return session;
	}

	public Session login() throws LoginException, RepositoryException, IOException {
		return login(null);
	}

	public Session getSession() throws LoginException, IOException,
			RepositoryException {
		if (session == null) {
			session = login();
		}
		return session;
	}

	public Workspace getWorkspace() throws LoginException, IOException,
			RepositoryException {
		if (workspace == null)
			workspace = getSession().getWorkspace();
		return workspace;
	}

	public QueryManager getQueryManager() throws LoginException,
			RepositoryException, IOException {
		if (queryManager == null)
			queryManager = getWorkspace().getQueryManager();
		return queryManager;
	}

	public NamespaceRegistry getNamespaceRegistry() throws LoginException,
			RepositoryException, IOException {
		if (namespaceRegistry == null)
			namespaceRegistry = getWorkspace().getNamespaceRegistry();
		return namespaceRegistry;
	}

	public NodeTypeManager getNodeTypeManager() throws LoginException,
			RepositoryException, IOException {
		if (nodeTypeManager == null)
			nodeTypeManager = getWorkspace().getNodeTypeManager();
		return nodeTypeManager;
	}

	public Query createXpathQuery(String query) throws InvalidQueryException,
			LoginException, RepositoryException, IOException {
		return getQueryManager().createQuery(query, "xpath");
	}

	public Query createSqlQuery(String query) throws InvalidQueryException,
			LoginException, RepositoryException, IOException {
		return getQueryManager().createQuery(query, "sql");
	}

	public QueryResult executeXpathQuery(String query)
			throws InvalidQueryException, LoginException, RepositoryException,
			IOException {
		return createXpathQuery(query).execute();
	}

	public QueryResult executeSqlQuery(String query)
			throws InvalidQueryException, LoginException, RepositoryException,
			IOException {
		return createSqlQuery(query).execute();
	}

	public void commit() throws RepositoryException, IOException {
		Session session = getSession();
		session.save();
	}

	public void logout() throws RepositoryException, IOException {
		Session session = getSession();
		session.logout();
	}

	public Node getRootNode() throws RepositoryException, IOException {
		Session session = getSession();
		Node rootNode = session.getRootNode();
		return rootNode;
	}

	public Node getRepositoryRoot() throws PathNotFoundException,
			RepositoryException, IOException {
		return getRootNode().getNode("jcr:root");
	}

	public static Node addNodeIfNotExists(Node root, String node)
			throws RepositoryException {
		NodeIterator ni = root.getNodes(node);
		if (ni.hasNext())
			return null;
		else
			return root.addNode(node);
	}

	public static Node addNodeIfNotExists(Node root, String node, String type)
			throws RepositoryException {
		try {
			NodeIterator ni = root.getNodes(node);
			// String typeName = root.getPrimaryNodeType().getName();
			if (ni.hasNext())
				return ni.nextNode();
			else {
				Node oNode = root.addNode(node, type);
				oNode.setProperty(NODE_PROPERTY_NAME, node);
				return oNode;
			}
		} catch (Exception e) {
			Node oNode = root.addNode(node, type);
			oNode.setProperty(NODE_PROPERTY_NAME, node);
			return oNode;
		}
	}

	/**
	 * Vrati node s rel.path, ak neexistuje, tak ho vytvori. Kontroluje sa
	 * parameter node a nie name.
	 * 
	 * @param root
	 *            - parent node
	 * @param node
	 *            - rel.path (jcr nazov)
	 * @param name
	 *            - nazov nodu (property name)
	 * @param type
	 *            - jcr type
	 * @return - node s rel.path
	 * @throws RepositoryException
	 */
	public static Node addNodeIfNotExists(Node root, String node, String name,
			String type) throws RepositoryException {
		try {
			NodeIterator ni = root.getNodes(node);
			// String typeName = root.getPrimaryNodeType().getName();
			if (ni.hasNext())
				return ni.nextNode();
			else {
				Node oNode = root.addNode(node, type);
				oNode.setProperty(NODE_PROPERTY_NAME, name);
				return oNode;
			}
		} catch (Exception e) {
			Node oNode = root.addNode(node, type);
			oNode.setProperty(NODE_PROPERTY_NAME, node);
			return oNode;
		}
	}

	public void registerNamespace(String namespace, String uri)
			throws RepositoryException, IOException {
		NamespaceRegistry nsr = getNamespaceRegistry();
		String euri = null;
		try {
			euri = nsr.getURI(namespace);
		} catch (NamespaceException e) {
			log.debug("Namespace " + namespace + " not registered yet.");
		}
		if (uri.equals(euri))
			log.warn("Namespace " + namespace + " already registered " + " as "
					+ euri + ". New request is " + uri);
		else
			try {
				nsr.registerNamespace(namespace, uri);
				log.info("Namespace registered: " + namespace + " = " + uri);
			} catch (NamespaceException e) {
			}
	}

	public static String dump(Node node) throws RepositoryException {
		return dump(node, true);
	}

	public static String printTree(Node node) throws RepositoryException {
		return dump(node, false);
	}

	private static String dump(Node node, boolean detail)
			throws RepositoryException {
		String depth = "";
		if ("jcr:system".equals(node.getName()))
			return "";
		for (int i = 0; i < node.getDepth(); i++)
			depth = depth + " ";

		StringBuilder sb = new StringBuilder();
		String sep = ",";
		sb.append(depth);
		sb.append("[" + node.getPath());
		for (PropertyIterator propIterator = node.getProperties(); propIterator
				.hasNext();) {
			Property prop = propIterator.nextProperty();
			sb.append(sep);
			sb.append("@" + prop.getName() + "=");
			try {
				String value = prop.getString();
				String firstLine = null;
				if (value.indexOf('\n') > -1) {
					firstLine = value.substring(0, value.indexOf('\n'));
					if (firstLine.length() < value.length())
						value = firstLine + "...";
				}
				if (value.length() > DUMP_LINE_LENGTH)
					value = value.substring(0, DUMP_LINE_LENGTH) + "...";
				sb.append("\"" + value + "\"");
			} catch (RepositoryException e) {
				sb.append("UNPRINTABLE");
			}
		}

		sb.append("]");
		sb.append("\n");
		for (NodeIterator nodeIterator = node.getNodes(); nodeIterator
				.hasNext(); sb.append(dump(nodeIterator.nextNode())))
			;
		return sb.toString();
	}

	public static String printout(Node n) throws RepositoryException {
		String out = "NODE: >" + n.getPath() + "<\n";
		for (PropertyIterator pi = n.getProperties(); pi.hasNext();) {
			Property p = pi.nextProperty();
			out = out + " * " + p.getName() + " = ";
			try {
				out = out + p.getString();
			} catch (Exception e) {
				out = out + "UNPRINTABLE";
			}
			out = out + "\n";
		}

		return out;
	}

	public void measureQuery(String query) throws LoginException,
			RepositoryException, IOException {
		long tsStartQuery = System.currentTimeMillis();
		QueryResult qr = executeXpathQuery(query);
		long tsExecutedQuery = System.currentTimeMillis();
		log.debug("Query " + query + " executed in "
				+ (tsExecutedQuery - tsStartQuery) + "ms.");
		NodeIterator ni = qr.getNodes();
		long size = ni.getSize();
		long tsGotNodes = System.currentTimeMillis();
		log.debug(ni.getSize() + " nodes for query " + query + " got in "
				+ (tsGotNodes - tsExecutedQuery) + "ms.");
		boolean firstTime = true;
		long tsFirstNode = 0L;
		while (ni.hasNext()) {
			if (firstTime) {
				tsFirstNode = System.currentTimeMillis();
				log.debug("First node for " + query + " got in "
						+ (tsFirstNode - tsGotNodes) + "ms.");
				firstTime = false;
			}
			Node node = ni.nextNode();
		}
		long tsAllNodes = System.currentTimeMillis();
		log.debug("All " + size + " nodes for " + query + " got in "
				+ (tsAllNodes - tsGotNodes) + "ms.");
		log.info("query: \"" + query + "\" [" + size + "] ("
				+ (tsAllNodes - tsStartQuery) + " ["
				+ (tsExecutedQuery - tsStartQuery) + "|"
				+ (tsGotNodes - tsStartQuery) + "|"
				+ (tsFirstNode - tsStartQuery) + "])");
	}

	public void cleanUpRepository() throws PathNotFoundException,
			RepositoryException, IOException {
		NodeIterator ni = getRootNode().getNodes();
		do {
			if (!ni.hasNext())
				break;
			Node n = ni.nextNode();
			if (!"jcr:system".equals(n.getName())) {
				log.debug("removing node " + n.getName());
				n.remove();
			}
		} while (true);
	}

	public String exportDocumentView(String path) throws PathNotFoundException,
			LoginException, IOException, RepositoryException {
		OutputStream ostr = new ByteArrayOutputStream();
		getSession().exportDocumentView(path, ostr, false, false);
		return ((ByteArrayOutputStream) ostr).toString();
	}

	public String exportSystemView(String path) throws PathNotFoundException,
			LoginException, IOException, RepositoryException {
		OutputStream ostr = new ByteArrayOutputStream();
		getSession().exportSystemView(path, ostr, false, false);
		return ((ByteArrayOutputStream) ostr).toString();
	}

	private static final int DUMP_LINE_LENGTH = 80;
	protected static Log log = LogFactory
			.getLog("cz.empire.common.jct.tools.AbstractJcrTool");
	protected static Repository repository = null;
	private Session session;
	private Workspace workspace;
	private QueryManager queryManager;
	private NamespaceRegistry namespaceRegistry;
	private NodeTypeManager nodeTypeManager;

	public Node removeNodeFromJcr(Node oNode) throws RepositoryException,
			IOException {
		log.info("removing node " + oNode.getName());
		Node oParent = oNode.getParent();
		log.info("removing node " + oParent.getName());
		oNode.remove();
		commit();
		return oParent;
	}

	public static String cleanCharFromString(String sPart) {
		if (sPart == null || "".equals(sPart))
			return "";
		else if (Character.isDigit(sPart.charAt(0)))
			sPart = NUMBER_SHORTCUT + sPart; // aby cislo nebylo
		// prvnim znakem jmena
		for (int i = 0; i < sPart.length(); i++) {
			char ch = sPart.charAt(i);
			if (!Character.isLetterOrDigit(ch))
				sPart = sPart.replace(ch, UNDERLINE);
		}
		return sPart;
	}

}
