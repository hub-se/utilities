/**
 * 
 */
package se.de.hu_berlin.informatik.utils.files.processors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.de.hu_berlin.informatik.utils.files.AFileWalker;
import se.de.hu_berlin.informatik.utils.files.AFileWalker.Builder;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;
import se.de.hu_berlin.informatik.utils.processors.sockets.ProcessorSocket;

/**
 * Starts a file walker that searches for files and directories that 
 * match a given pattern, starting from a submitted input path. Returns a
 * list of matched files and/or directories.
 * 
 * @author Simon Heiden
 */
public class SearchFileOrDirToListProcessor extends AbstractProcessor<Path,List<Path>> {

	final private String pattern;
	final private int depth;
	
	private boolean searchDirectories = false;
	private boolean searchFiles = false;
	private boolean includeRootDir = false;
	
	private boolean skipAfterFind = false;
	
	private boolean relative = false;
	
	/**
	 * Creates a new {@link SearchFileOrDirToListProcessor} object with the given parameter.
	 * @param pattern
	 * the pattern that the files are matched against
	 * (a value of null matches all files and dirs)
	 * @param recursive 
	 * whether files should be searched recursively
	 */
	public SearchFileOrDirToListProcessor(String pattern, boolean recursive) {
		this(pattern, recursive ? Integer.MAX_VALUE : 1);
	}
	
	/**
	 * Creates a new {@link SearchFileOrDirToListProcessor} object with the given parameter.
	 * @param pattern
	 * the pattern that the files are matched against
	 * (a value of null matches all files and dirs)
	 * @param depth
	 * maximum depth in which to search
	 */
	public SearchFileOrDirToListProcessor(String pattern, int depth) {
		super();
		this.pattern = pattern;
		if (depth < 0) {
			Log.abort(this, "Search depth has negative value.");
		}
		this.depth = depth;
	}
	
	/**
	 * Enables searching for files.
	 * @return
	 * this
	 */
	public SearchFileOrDirToListProcessor searchForFiles() {
		this.searchFiles = true;
		return this;
	}
	
	/**
	 * Puts out paths relative to the input path.
	 * @return
	 * this
	 */
	public SearchFileOrDirToListProcessor relative() {
		this.relative = true;
		return this;
	}
	
	/**
	 * Enables searching for directories.
	 * @return
	 * this
	 */
	public SearchFileOrDirToListProcessor searchForDirectories() {
		this.searchDirectories = true;
		return this;
	}
	
	/**
	 * Skips recursion to subtree after found items.
	 * @return
	 * this
	 */
	public SearchFileOrDirToListProcessor skipSubTreeAfterMatch() {
		this.skipAfterFind = true;
		return this;
	}
	
	/**
	 * Includes the root directory in the search.
	 * @return
	 * this
	 */
	public SearchFileOrDirToListProcessor includeRootDir() {
		includeRootDir = false;
		return this;
	}

	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
	 */
	@Override
	public List<Path> processItem(Path input, ProcessorSocket<Path, List<Path>> socket) {
		if (!input.toFile().exists()) {
			Log.abort(this, "Path '%s' doesn't exist.", input.toString());
		}
		
		SearchFileWalker.Builder builder = new Builder(pattern) {
			@Override
			public SearchFileWalker build() {
				return new SearchFileWalker(this);
			}
		};
		if (includeRootDir) {
			builder.includeRootDir();
		}
		if (searchDirectories) {
			builder.searchForDirectories();
		}
		if (searchFiles) {
			builder.searchForFiles();
		}
		if (skipAfterFind) {
			builder.skipSubTreeAfterMatch();
		}
		if (relative) {
			builder.relative();
		}
		
		//declare the file walker
		SearchFileWalker walker = (SearchFileWalker) builder.build();
		socket.delegateTrackingTo(walker);
		
		//traverse the file tree
		try {
			Files.walkFileTree(input, Collections.emptySet(), depth, walker);
		} catch (IOException e) {
			Log.abort(this, e, "IOException thrown.");
		}

		walker.delegateTrackingTo(socket);
		return walker.getMatchedPaths();
	}

	public class SearchFileWalker extends AFileWalker {

		final private List<Path> matchedPaths;
		
		protected SearchFileWalker(Builder builder) {
			super(builder);
			this.matchedPaths = new ArrayList<>();
		}

		@Override
		public void processMatchedFileOrDir(Path fileOrDir) {
			matchedPaths.add(fileOrDir);
		}
		
		public List<Path> getMatchedPaths() {
			return matchedPaths;
		}
		
	}
}
