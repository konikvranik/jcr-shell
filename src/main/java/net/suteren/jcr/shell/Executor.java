package net.suteren.jcr.shell;

import gnu.getopt.Getopt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Executor {

	private static final String BASIC_ARGUMENTS = "t:";
	protected static Log log = LogFactory.getLog(Executor.class);

	public static void main(String[] args) throws IOException {
		log.info(JcrShell.UNIT_NAME + " is starting.");
		Getopt getopt = new Getopt(JcrShell.PRINT_CMD, args, BASIC_ARGUMENTS
				+ JcrShell.JCR_ARGUMENTS + JackrabbitShell.JACKRABBIT_ARGUMENTS);
		int c;
		Class type = null;
		String t = null;
		Vector<String> newArgs = new Vector<String>();
		while ((c = getopt.getopt()) != -1) {
			switch (c) {
			case 't':
				t = getopt.getOptarg();
				break;
			case '?':
				String[] arg = { JcrShell.PRINT_CMD };
				helpCommand(arg);
				System.exit(2);
			default:
				newArgs.add("-" + (char) c);
				if (getopt.getOptarg() != null)
					newArgs.add(getopt.getOptarg());
			}
		}

		try {
			type = Class.forName(t);
			if (!JcrShell.class.isAssignableFrom(type))
				throw new ClassNotFoundException();
		} catch (ClassNotFoundException e1) {
			type = null;
		}

		if (type == null) {
			if ("jackrabbit".equals(t))
				type = JackrabbitShell.class;
			else if ("".equals(t))
				type = JackrabbitShell.class;

			if (type == null)
				type = JackrabbitShell.class;

		}
		int firstarg = getopt.getOptind();
		for (int i = firstarg; i < args.length; i++)
			newArgs.add(args[i]);

		log.debug("Args: " + newArgs);

		try {
			Class[] a = { String[].class };
			Method main = type.getMethod("main", a);
			main.invoke(null, newArgs.toArray(new String[newArgs.size()]));
		} catch (Exception e) {
			log.fatal(e);
			System.err.println("ERROR: " + e);
		}
	}

	private static void helpCommand(String[] arg) {
		new JcrShell().helpCommand(arg);
	}
}
