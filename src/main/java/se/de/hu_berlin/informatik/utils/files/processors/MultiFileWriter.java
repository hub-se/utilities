/**
 * 
 */
package se.de.hu_berlin.informatik.utils.files.processors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.miscellaneous.OutputPathGenerator;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;

/**
 * A file writer module for multiple files that gets for example a list 
 * of list of {@link String}s and writes their contents to specified
 * output paths. Output file names
 * may also be generated automatically with an included instance of
 * {@link OutputPathGenerator}. The given input is returned as it is
 * to the output in the end in case it has to be further processed.
 * 
 * @author Simon Heiden
 * 
 * @see OutputPathGenerator
 */
public class MultiFileWriter<A extends Iterable<? extends Iterable<? extends CharSequence>> > extends AbstractProcessor<A, A> {

	private boolean generateOutputPaths = false;
	private Path[] paths = null;
	private String extension;
	boolean overwrite = false;
	
	OutputPathGenerator generator;
	
	/**
	 * Creates a new {@link MultiFileWriter} with the given parameters.
	 * @param outputdir
	 * is either a directory or an output file path
	 * @param overwrite
	 * determines if files and directories should be overwritten
	 * @param extension
	 * is the extension of the automatically generated output paths
	 */
	public MultiFileWriter(Path outputdir, boolean overwrite, String extension) {
		super();
		this.extension = extension;
		this.generator = new OutputPathGenerator(outputdir, overwrite);
		this.generateOutputPaths = true;
	}
	
	/**
	 * Creates a new {@link MultiFileWriter} with the given parameters.
	 * @param overwrite
	 * determines if files and directories should be overwritten
	 * @param paths
	 * is a sequence of output paths that are used by the file writer
	 */
	public MultiFileWriter(boolean overwrite, Path... paths) {
		super();
		this.paths = paths;
		this.overwrite = overwrite;
	}
	
	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
	 */
	@Override
	public A processItem(A items) {
		Iterator<? extends Iterable<? extends CharSequence>>  iterator = items.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			Path outputPath = null;
			if (generateOutputPaths) {
				outputPath = generator.getNewOutputPath(extension);
			} else {
				try {
					outputPath = paths[i];
					++i;
					if (outputPath.toFile().isDirectory()) {
						Log.err(this, "Path \"%s\" is a directory and should be a file.", outputPath.toString());
						break;
					}
					if (!overwrite && outputPath.toFile().exists()) {
						Log.err(this, "File \"%s\" exists.", outputPath.toString());
						break;
					}
					outputPath.getParent().toFile().mkdirs();
				} catch (IndexOutOfBoundsException e) {
					Log.abort(this, "No output path for file %d given.", i+1);
				}
			}
			try {
				Files.write(outputPath, iterator.next(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				Log.abort(this, e, "Cannot write file \"" + outputPath.toString() + "\".");
			}
		}
		return items;
	}

}
