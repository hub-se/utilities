package se.de.hu_berlin.informatik.utils.processors;

import se.de.hu_berlin.informatik.utils.processors.sockets.ProcessorSocket;
import se.de.hu_berlin.informatik.utils.processors.sockets.eh.EHWithInput;
import se.de.hu_berlin.informatik.utils.processors.sockets.module.Module;
import se.de.hu_berlin.informatik.utils.processors.sockets.pipe.Pipe;
import se.de.hu_berlin.informatik.utils.threaded.disruptor.eventhandler.AbstractDisruptorEventHandler;

/**
 * A basic implementation of a {@link ConsumingProcessor} that implementing classes should
 * extend by implementing {@link #consumeItem(Object)}.
 * 
 * <p> Optionally, the methods {@link #getResultFromCollectedItems()}, {@link #resetAndInit()}, 
 * {@link #finalShutdown()} and {@link #newProcessorInstance()} can be overridden, if necessary.
 * 
 * <p> Other methods are generally not intended (and not safe) to be overridden by implementing
 * classes and overriding them may cause unintended behaviour when the Processor is used.
 * 
 * <p> For further details about the methods, take a look at the given comments.
 * 
 * @author Simon
 *
 * @param <A>
 * the type of input objects
 */
public abstract class AbstractConsumingProcessor<A> implements ConsumingProcessor<A> {

	private Pipe<A,Object> pipeView;
	private Module<A,Object> moduleView;
	private EHWithInput<A> ehView;
	private ProcessorSocket<A,Object> socket;
	private ClassLoader classLoader;
	
	public AbstractConsumingProcessor(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	
	public AbstractConsumingProcessor() {
		this(null);
	}

	/**
	 * Convenience method that calls {@link #asModule()} on this Processor
	 * and then submits the given item.
	 * @param item
	 * the item to process
	 * @return
	 * this Processor as a {@link Module}, for chaining
	 */
	public Module<A,Object> submit(Object item) {
		return asModule().submit(item);
	}
	
	@Override
	public Pipe<A,Object> asPipe() throws UnsupportedOperationException {
		return asPipe(8, this.classLoader);
	}
	
	@Override
	public Pipe<A,Object> asPipe(int bufferSize) throws UnsupportedOperationException {
		return asPipe(bufferSize, this.classLoader);
	}
	
	@Override
	public Pipe<A,Object> asPipe(int bufferSize, ClassLoader classLoader) throws UnsupportedOperationException {
		if (pipeView == null) {
			pipeView = new Pipe<>(this, bufferSize, true, classLoader);
		}
		return pipeView;
	}

	@Override
	public Module<A,Object> asModule() throws UnsupportedOperationException {
		if (moduleView == null) {
			moduleView = new Module<>(this);
		}
		return moduleView;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends AbstractDisruptorEventHandler<A> & ProcessorSocket<A,Object>> E asEH() throws UnsupportedOperationException {
		if (ehView == null) {
			ehView = new EHWithInput<>(this);
		}
		return (E) ehView;
	}
	
	@Override
	public void setSocket(ProcessorSocket<A, Object> socket) {
		if (socket == null) {
			throw new IllegalStateException("No socket given (null) for " + this.getClass() + ".");
		}
		this.socket = socket;
	}

	@Override
	public ProcessorSocket<A, Object> getSocket() {
		if (socket == null) {
			throw new IllegalStateException("No socket set for " + this.getClass() + ".");
		} else {
			return socket;
		}
	}
	
}
