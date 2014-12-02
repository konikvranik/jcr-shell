package net.suteren.jcr.shell;

import gnu.getopt.Getopt;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefWriter;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.gnu.readline.Readline;

public class JackrabbitShell extends JcrShell {

	static final String JACKRABBIT_ARGUMENTS = "c:r:";
	private static final String JCR_CONFIG = "jcr.config";
	private static final String JCR_HOME = "jcr.home";
	private static final String UNIT_NAME = "JackrabbitShell";

	private JackrabbitShell() {
	}

	public JackrabbitShell(String config, String home) throws IOException {
		super(getRepository(getRepoConfig(config), getRepoHome(home)));
		JcrTool.log = LogFactory.getLog(getClass());
	}

	private static Repository getRepository(String repositroyConfig,
			String repositoryPath) throws IOException {
		return new TransientRepository(repositroyConfig, repositoryPath);
	}

	public int ntloadCommand(String[] args) {
		if (args.length > 1) {
			System.out.println("Too much arguments!");
			log.error("Too much arguments!");
			return 1;
		}
		try {
			InputStream is = null;
			if (args.length == 1)
				is = new FileInputStream(args[0]);
			else
				is = System.in;

			JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager) getWorkspace()
					.getNodeTypeManager();
			manager.registerNodeTypes(is, "text/x-jcr-cnd");
			commited = false;
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
		}
		return 1;
	}

	public int ntdumpCommand(String[] args) {
		if (args.length > 1) {
			System.out.println("Too much arguments!");
			log.error("Too much arguments!");
			return 1;
		}
		try {
			Writer writer = null;
			if (args.length == 1) {
				writer = new FileWriter(args[0]);
				log.debug("writing to " + args[0]);
			} else {
				writer = new OutputStreamWriter(System.out);
				log.debug("writing to STDOUT");
			}
			NodeTypeIterator nti = ((NodeTypeManagerImpl) getNodeTypeManager())
					.getAllNodeTypes();
			CompactNodeTypeDefWriter cntdw = new CompactNodeTypeDefWriter(
					writer, (NamespaceResolver) getSession(),
					(NamePathResolver) getSession());
			while (nti.hasNext()) {
				NodeTypeImpl ntim = (NodeTypeImpl) nti.next();
				cntdw.write(ntim.getDefinition());
				log.debug("Definition: " + ntim.getDefinition().toString());
			}
			if (writer instanceof OutputStreamWriter)
				writer.flush();
			else
				writer.close();

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
		}
		return 1;
	}

	private static String getRepoHome(String jcrHome) {
		File d = null;
		try {
			d = new File(jcrHome);
		} catch (NullPointerException e) {
			log.warn("JCR repository home not set in system property.");
		}
		if (d == null || !d.isDirectory()) {
			jcrHome = System.getProperty(JCR_HOME);
			try {
				d = new File(jcrHome);
			} catch (NullPointerException e) {
				log.warn("JCR repository home not set in system property.");
			}
		}
		String message = "Jackrabbit repository home: ";
		while (d == null || !d.isDirectory()) {
			try {
				jcrHome = Readline.readline(message);
			} catch (EOFException e) {
				log.debug(e);
				message = "EOF. Try again: ";
			} catch (UnsupportedEncodingException e) {
				log.debug(e);
				message = "Unsupported encoging. Try again: ";
			} catch (IOException e) {
				log.debug(e);
				message = "IO exceprion. Try again: ";
			}
			if ("".equals(jcrHome))
				System.exit(1);
			d = new File(jcrHome);
		}
		return jcrHome;
	}

	private static String getRepoConfig(String jcrConfig) {
		File d = null;
		try {
			d = new File(jcrConfig);
		} catch (NullPointerException e) {
			log
					.warn("JCR repository configuration not set in system property.");
		}
		if (d == null || !d.isFile()) {
			jcrConfig = System.getProperty(JCR_CONFIG);
			try {
				d = new File(jcrConfig);
			} catch (NullPointerException e) {
				log
						.warn("JCR repository configuration not set in system property.");
			}
		}
		String message = "Jackrabbit repository config: ";
		while (d == null || !d.isFile()) {
			try {
				jcrConfig = Readline.readline(message);
			} catch (EOFException e) {
				log.debug(e);
				message = "EOF. Try again: ";
			} catch (UnsupportedEncodingException e) {
				log.debug(e);
				message = "Unsupported encoging. Try again: ";
			} catch (IOException e) {
				log.debug(e);
				message = "IO exceprion. Try again: ";
			}
			if ("".equals(jcrConfig))
				System.exit(1);
			d = new File(jcrConfig);
		}
		return jcrConfig;
	}

	public static void main(String[] args) throws IOException {
		log.info(UNIT_NAME + " is starting.");
		Getopt getopt = new Getopt(PRINT_CMD, args, JCR_ARGUMENTS
				+ JACKRABBIT_ARGUMENTS);
		int c;
		String repoHome = null;
		String repoConfig = null;
		Vector<String> newArgs = new Vector<String>();
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 'c':
				repoConfig = getopt.getOptarg();
				break;
			case 'r':
				repoHome = getopt.getOptarg();
				break;
			case '?':
				String[] arg = { PRINT_CMD };
				new JackrabbitShell().helpCommand(arg);
				System.exit(2);
			default:
				newArgs.add("-" + (char) c);
				if (getopt.getOptarg() != null)
					newArgs.add(getopt.getOptarg());
			}
		}
		int firstarg = getopt.getOptind();
		for (int i = firstarg; i < args.length; i++)
			newArgs.add(args[i]);
		JackrabbitShell jShell = new JackrabbitShell(repoConfig, repoHome);
		log.debug("Args: " + newArgs);
		int returnStatus = jShell.runApp(newArgs.toArray(new String[newArgs
				.size()]));
		log.info("JacrrabbitTool is shutting down.");
		System.exit(returnStatus);
	}
}
