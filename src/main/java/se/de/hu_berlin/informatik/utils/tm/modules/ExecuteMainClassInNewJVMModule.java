/**
 * 
 */
package se.de.hu_berlin.informatik.utils.tm.modules;

import java.io.File;
import java.io.IOException;

import se.de.hu_berlin.informatik.utils.miscellaneous.ClassPathParser;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.miscellaneous.Misc;
import se.de.hu_berlin.informatik.utils.tm.moduleframework.AModule;

/**
 * Starts the given Java Class in a new process.
 * 
 * @author Simon Heiden
 */
public class ExecuteMainClassInNewJVMModule extends AModule<String[],Integer> {

	private File executionDir;
	private String clazz;
	private String cp;
	private String[] properties;
	
	private String javaHome = null;
	
	/**
	 * Starts the given class as a new process with the standard JVM and the current class path.
	 * @param clazz
	 * the name of the Java class to execute. Must contain a main method
	 * @param executionDir
	 * the directory to execute the new process in (or null if the current directory should be used)
	 * @param properties
	 * other properties to give to the JVM
	 */
	public ExecuteMainClassInNewJVMModule(String clazz, File executionDir, String... properties) {
		this(null, clazz, null, executionDir, properties);
	}
	
	/**
	 * Starts the given class as a new process with the standard JVM.
	 * @param clazz
	 * the name of the Java class to execute. Must contain a main method
	 * @param cp
	 * the class path to use
	 * @param executionDir
	 * the directory to execute the new process in (or null if the current directory should be used)
	 * @param properties
	 * other properties to give to the JVM
	 */
	public ExecuteMainClassInNewJVMModule(String clazz, String cp, File executionDir, String... properties) {
		this(null, clazz, cp, executionDir, properties);
	}
	
	/**
	 * Starts the given class as a new process.
	 * @param javaHome
	 * a path to a Java installation directory (or null if the standard Java installation should be used)
	 * @param clazz
	 * the name of the Java class to execute. Must contain a main method
	 * @param cp
	 * the class path to use
	 * @param executionDir
	 * the directory to execute the new process in (or null if the current directory should be used)
	 * @param properties
	 * other properties to give to the JVM
	 */
	public ExecuteMainClassInNewJVMModule(String javaHome,  
			String clazz, String cp, File executionDir, String... properties) {
		super(true);
		this.executionDir = executionDir;
		this.clazz = clazz;
		if (cp != null) {
			this.cp = cp;
		} else {
			this.cp = new ClassPathParser()
					.parseSystemClasspath()
					.getClasspath();
		}
		this.properties = properties;
		
		this.javaHome = javaHome;
	}

	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
	 */
	public Integer processItem(String[] args) {
		return run(args);
	}

	private int run(String... args) {
		String tool = "java";
		if (javaHome != null) {
			tool = javaHome + File.separator + "bin" + File.separator + "java";
		}
		String[] fullArgs = {tool, "-server", "-cp", cp};
		String[] clazzWrapper = { clazz };
		fullArgs = Misc.joinArrays(fullArgs, properties);
		fullArgs = Misc.joinArrays(fullArgs, clazzWrapper);
		fullArgs = Misc.joinArrays(fullArgs, args);

        ProcessBuilder pb = new ProcessBuilder(fullArgs);
        pb.directory(executionDir);
        pb.inheritIO();
        Process p = null;
		try {
			p = pb.start();
		} catch (IOException e) {
			Log.err(this, e, "IOException thrown.");
			return 1;
		}
//        InputStreamConsumer consumer = new InputStreamConsumer(p.getInputStream(), System.out);
//        consumer.start();
//        InputStreamConsumer errconsumer = new InputStreamConsumer(p.getErrorStream(), System.err);
//        errconsumer.start();

        //obtain result and wait for the process to finish execution
        int result = 1;
        boolean isFirst = true;
        while (p.isAlive() || isFirst) {
        	isFirst = false;
        	try {
        		result = p.waitFor();
        	} catch (InterruptedException e) {
        	}
        }

//        while (consumer.isAlive()) {
//        	try {
//        		consumer.join();
//        	} catch (InterruptedException e) {
//        	}
//        }
//        while (errconsumer.isAlive()) {
//        	try {
//        		errconsumer.join();
//        	} catch (InterruptedException e) {
//        	}
//        }

        return result;
    }	
}