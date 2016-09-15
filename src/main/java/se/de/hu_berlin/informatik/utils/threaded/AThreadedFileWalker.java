/**
 * 
 */
package se.de.hu_berlin.informatik.utils.threaded;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import se.de.hu_berlin.informatik.utils.tracking.Trackable;

/**
 * FileVisitor implementation that is enriched with a blocking {@link ExecutorService}.
 * 
 * @author Simon Heiden
 * 
 * @see FileVisitor
 */
public abstract class AThreadedFileWalker extends Trackable implements FileVisitor<Path> {
	
	final private PathMatcher matcher;
	final private ExecutorServiceProvider executor;
	final private boolean searchDirectories;
	final private boolean searchFiles;
	final private boolean skipAfterFind;
	
	private boolean isFirst;
	
	protected AThreadedFileWalker(Builder builder) {
		executor = builder.executor;
		matcher = builder.matcher;
		searchDirectories = builder.searchDirectories;
		searchFiles = builder.searchFiles;
		skipAfterFind = builder.skipAfterFind;
		isFirst = builder.isFirst;
		
		if (executor == null) {
			throw new IllegalStateException("No executor service given.");
		}
		if (searchDirectories == false && searchFiles == false) {
			throw new IllegalStateException("Define whether files or directories shall be searched.");
		}
	}

	/**
	 * @return
	 * the executor service
	 */
	public ExecutorService getExecutorService() {
		return executor.getExecutorService();
	}

	/**
	 * @return
	 * the executor service provider
	 */
	public ExecutorServiceProvider getExecutorServiceProvider() {
		return executor;
	}
	
	/**
	 * @return 
	 * the path matcher
	 */
	protected PathMatcher getMatcher() {
		return matcher;
	}
	
	/**
	 * Convenience method to be used in actual implementations of this class
	 * @param path
	 * Path to be matched against the global pattern.
	 * @return
	 * true if path is not null and matches the global pattern.
	 */
	protected boolean match(Path path) {
		return path != null && matcher.matches(path);
	}
	
	
	abstract public void processMatchedFileOrDir(Path fileOrDir);
	
	/* (non-Javadoc)
	 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
	 */
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {		
		if (attrs.isDirectory()) {
			if (searchDirectories) {
				if (matcher == null) {
					track();
					processMatchedFileOrDir(file);
				} else if (match(file.toAbsolutePath())) {
					track();
					processMatchedFileOrDir(file);
//					Misc.out(file.toString());
					if (skipAfterFind) {
						return FileVisitResult.SKIP_SUBTREE;
					}
				}
			}
		} else {
			if (searchFiles) {
				if (matcher == null) {
					processMatchedFileOrDir(file);
				} else if (match(file.toAbsolutePath())) {
					track();
					processMatchedFileOrDir(file);
//					Misc.out(file.toString());
				}
			}
		}
		return CONTINUE;
	}

	/* (non-Javadoc)
	 * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
	 */
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (!isFirst) {
			if (searchDirectories) {
				if (matcher == null) {
					track();
					processMatchedFileOrDir(dir);
				} else if (match(dir.toAbsolutePath())) {
					track();
					processMatchedFileOrDir(dir);
//					Misc.out(dir.toString());
					if (skipAfterFind) {
						return FileVisitResult.SKIP_SUBTREE;
					}
				}
			}
		} else {
			isFirst = false;
		}
		return CONTINUE;
	}
	

    /**
     * Invoked for a file that could not be visited.
     *
     * <p> Unless overridden, this method re-throws the I/O exception that prevented
     * the file from being visited.
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
        throws IOException
    {
        Objects.requireNonNull(file);
        throw exc;
    }

    /**
     * Invoked for a directory after entries in the directory, and all of their
     * descendants, have been visited.
     *
     * <p> Unless overridden, this method returns {@link FileVisitResult#CONTINUE
     * CONTINUE} if the directory iteration completes without an I/O exception;
     * otherwise this method re-throws the I/O exception that caused the iteration
     * of the directory to terminate prematurely.
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
        throws IOException
    {
        Objects.requireNonNull(dir);
        if (exc != null)
            throw exc;
        return FileVisitResult.CONTINUE;
    }
    
    public static abstract class Builder implements se.de.hu_berlin.informatik.utils.miscellaneous.IBuilder<AThreadedFileWalker> {
		
		private final PathMatcher matcher;
		
		private ExecutorServiceProvider executor = null;
		private boolean searchDirectories = false;
		private boolean searchFiles = false;
		private boolean skipAfterFind = false;
		private boolean isFirst = true;
		
		/**
		 * Creates an {@link Builder} object with the given parameters.
		 * @param pattern
		 * holds a global pattern against which the visited files (more specific: their file names) should be matched
		 */
		public Builder(String pattern) {
			super();
			this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		}
		
		/**
		 * Enables searching for files.
		 * @return
		 * this
		 */
		public Builder searchForFiles() {
			this.searchFiles = true;
			return this;
		}
		
		/**
		 * Enables searching for directories.
		 * @return
		 * this
		 */
		public Builder searchForDirectories() {
			this.searchDirectories = true;
			return this;
		}
		
		/**
		 * Skips recursion to subtree after found items.
		 * @return
		 * this
		 */
		public Builder skipSubTreeAfterMatch() {
			this.skipAfterFind = true;
			return this;
		}
		
		/**
		 * Includes the root directory in the search.
		 * @return
		 * this
		 */
		public Builder includeRootDir() {
			isFirst = false;
			return this;
		}
		
		/**
		 * Sets the executor service.
		 * @param executor
		 * an executor service that shall be used
		 * @return
		 * this
		 */
		public Builder executor(ExecutorService executor) {
			this.executor = new ExecutorServiceProvider(executor);
			return this;
		}
		
		/**
		 * Sets the executor service.
		 * @param corePoolSize
		 * the number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set
		 * @param maximumPoolSize
		 * the maximum number of threads to allow in the pool
		 * @param keepAliveTime
		 * when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
		 * @param unit 
		 * the time unit for the keepAliveTime argument
		 * @return
		 * this
		 */
		public Builder executor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
			this.executor = new ExecutorServiceProvider(corePoolSize, maximumPoolSize, keepAliveTime, unit);
			return this;
		}
		
		/**
		 * Sets the executor service with {@code keepAliveTime=1L} and {@code unit=TimeUnit.SECONDS}.
		 * @param poolSize
		 * the number of threads to run in the pool
		 * @return
		 * this
		 */
		public Builder executor(int poolSize) {
			this.executor = new ExecutorServiceProvider(poolSize, poolSize, 1L, TimeUnit.SECONDS);
			return this;
		}
    	
    }
    
}
