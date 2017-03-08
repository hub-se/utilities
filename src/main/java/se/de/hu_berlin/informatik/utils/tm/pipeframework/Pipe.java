/**
 * 
 */
package se.de.hu_berlin.informatik.utils.tm.pipeframework;

import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.threaded.disruptor.DisruptorProvider;
import se.de.hu_berlin.informatik.utils.threaded.disruptor.eventhandler.AbstractDisruptorEventHandler;
import se.de.hu_berlin.informatik.utils.threaded.disruptor.eventhandler.DisruptorFCFSEventHandler;
import se.de.hu_berlin.informatik.utils.tm.Processor;
import se.de.hu_berlin.informatik.utils.tm.moduleframework.Module;
import se.de.hu_berlin.informatik.utils.tm.user.AbstractProcessorSocket;
import se.de.hu_berlin.informatik.utils.tm.user.ProcessorSocket;

/**
 * A {@link ProcessorSocket} implementation that provides basic functionalities of a pipe. 
 * Pipes have an input and an output, can be linked together such that one 
 * Pipe may provide the input to another Pipe. Each Pipe executes its calculations
 * inside of a thread which allows parallel execution of sequential tasks. After usage,
 * the Pipes have to be manually shutdown, though. (At least the first Pipe
 * has to be manually shutdown. All other linked Pipes are automatically
 * shutdown in a chain reaction after the shutdown of the first Pipe.
 * 
 * <br><br> For convenience, multiple (matching) Pipes may be linked together 
 * like this:
 * 
 * <br><br> {@code pipe1.linkTo(pipe2).linkTo(pipe3).linkTo(...)...;}
 * 
 * <br><br> which will link the output of {@code pipe1} to the input of 
 * {@code pipe2} and then link the output of {@code pipe2} to the input of
 * {@code pipe3}, etc.
 * 
 * <br><br> After linking, any matching item submitted to the first Pipe
 * will start the execution process. Non-matching items will abort the
 * application with an error message, throwing an exception. Submitted 
 * objects that equal {@code null} will simply be ignored.
 * 
 * <br><br> In general, Pipes should not be linked manually and should
 * preferably be linked together with a {@link PipeLinker} which provides
 * more general and easier access methods.
 * 
 * @author Simon Heiden
 *
 * @param <A>
 * is the type of the input objects
 * @param <B>
 * is the type of the output objects
 * 
 * @see PipeLinker
 */
public class Pipe<A,B> extends AbstractProcessorSocket<A,B> {
	
	final private DisruptorProvider<A> disruptorProvider;

	private boolean hasInput = false;
	private Pipe<B,?> output = null;

	private final boolean singleWriter;
	
	/**
	 * Creates a pipe object with a buffer size of 8.
	 * @param processor
	 * the processor 
	 * @param singleWriter
	 * whether this pipe writes to the output with only a single thread 
	 * (if not sure, set this to false)
	 */
	public Pipe(Processor<A,B> processor, boolean singleWriter) {
		this(processor, 8, singleWriter);
	}
	
	/**
	 * Creates a pipe object.
	 * @param processor
	 * the processor 
	 * @param bufferSize
	 * the size of the ring buffer, must be power of 2
	 * @param singleWriter
	 * whether this pipe writes to the output with only a single thread
	 * (if not sure, set this to false)
	 */
	@SuppressWarnings("unchecked")
	public Pipe(Processor<A,B> processor, int bufferSize, boolean singleWriter) {
		super(processor);
		disruptorProvider = new DisruptorProvider<>(bufferSize);
		//event handler used for transmitting items from one pipe to another
		disruptorProvider.connectHandlers(new DisruptorFCFSEventHandler<A>() {
			@Override
			public void resetAndInit() { /*not needed*/ }
			@Override
			public void processEvent(A item) {
				Pipe.this.initAndConsume(item);
			}
		});
		this.singleWriter = singleWriter;
	}
	
	@Override
	public void produce(B item) {
		if (output != null) {
			output.submit(item);
		}
	}
	
//	/**
//	 * Submits an object of type {@code B} to a connected output pipe.
//	 * If the pipe is not linked to any other pipe, then the item is 
//	 * discarded.
//	 * @param item
//	 * the object of type {@code B} to be submitted
//	 */
//	public void submitProcessedItem(B item) {
//		if (output != null) {
//			output.submit(item);
//		}
//	}
	
	/**
	 * Sets the producer type of the associated disruptor. 
	 * @param singleWriter
	 * whether the input pipe writes to this pipe with only a single thread 
	 */
	protected void setInput(boolean singleWriter) {
		setProducerType(singleWriter);
		hasInput = true;
	}
	
	/**
	 * Sets this pipe to have an input pipe. 
	 * @param singleWriter
	 * whether the input pipe writes to this pipe with only a single thread 
	 */
	public void setProducerType(boolean singleWriter) {
		disruptorProvider.setProducerType(singleWriter);
	}
	
	/**
	 * @return
	 * whether an input pipe exists
	 */
	private boolean hasInput() {
		return hasInput;
	}

	/**
	 * @param pipe
	 * the output pipe
	 */
	private void setOutput(Pipe<B,?> pipe) {
		output = pipe;
	}
	
	protected DisruptorProvider<A> getDisruptorProvider() {
		return disruptorProvider;
	}

	@Override
	public <C> ProcessorSocket<C,?> linkTo(ProcessorSocket<C,?> consumer) throws IllegalArgumentException, IllegalStateException {
		if (consumer instanceof Pipe) {
			return linkPipeTo((Pipe<C, ?>)consumer, singleWriter);
		} else {
			throw new IllegalStateException("Can only link to other pipes.");
		}
	}

	/**
	 * Links a matching pipe to the output of this pipe.
	 * @param <C>
	 * the input type of the pipe to be linked to
	 * @param <D>
	 * the output type of the pipe to be linked to
	 * @param pipe
	 * the pipe to be linked to
	 * @param singleWriter
	 * whether this pipe writes to the output only with a single thread
	 * @return
	 * the pipe to be linked to
	 * @throws IllegalArgumentException
	 * if the input type C of the given pipe does not match the output type B of this pipe
	 * @throws IllegalStateException
	 * if the pipes can't be linked due to other reasons
	 */
	@SuppressWarnings("unchecked")
	private <C,D> Pipe<C, D> linkPipeTo(Pipe<C, D> pipe, boolean singleWriter) throws IllegalArgumentException, IllegalStateException {
		if (!pipe.hasInput()) {
			//output pipe has no input yet
			try {				
				setOutput((Pipe<B, ?>) pipe);
				pipe.setInput(singleWriter);
			} catch (ClassCastException e) {
				throw new IllegalArgumentException("Type mismatch while linking to other pipe.", e);
			}
		} else {
			throw new IllegalStateException("No linking to already used pipes allowed!");
		}
		return pipe;
	}

	/**
	 * Shuts down the pipe. Waits for all executions to terminate.
	 */
	public void shutdown() {
//		Log.out(this, "Shutting down..., %s", Thread.currentThread());
		//shut down the disruptor
		disruptorProvider.shutdown();
		
		//check whether there are collected items to submit
		B result;
		if ((result = getProcessor().getResultFromCollectedItems()) != null) {
			//submit the collected result
			produce(result);
		}
		
		finalShutdown();

		//initiate shut down of the pipe linked to this pipe's output (if any)
		if (output != null) {
			output.shutdown();
		}
	}

	/**
	 * Submits an item to this pipe.
	 * @param item
	 * the item to be submitted
	 */
	public void submit(A item) {
		if (item != null) {
			disruptorProvider.submit(item);
		}
	}
	
	/**
	 * Submits an item of some kind to this pipe. Will abort the
	 * application if the type does not match the pipe's input type.
	 * More specificially, it will abort if the item can't be cast
	 * to the pipe's input type.
	 * @param item
	 * the item to be submitted
	 */
	@SuppressWarnings("unchecked")
	public void submitObject(Object item) {
		try {
			submit((A)item);
		} catch (ClassCastException e) {
			Log.abort(this, e, "Type mismatch while submitting item.");
		}
	}

	@Override
	public Pipe<A, B> asPipe() throws UnsupportedOperationException {
		return this;
	}

	@Override
	public Module<A, B> asModule() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public <E extends AbstractDisruptorEventHandler<A> & ProcessorSocket<A,B>> E asEH() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("not supported");
	}
	
}