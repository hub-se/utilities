package se.de.hu_berlin.informatik.utils.threaded.disruptor;

/**
 * Abstract multiplexer that collects output generated by multiple
 * threads and processes it. Automatically uses the handlers connected
 * to the given disruptor as its input.
 * 
 * @author Simon Heiden
 * @param <B>
 * the type of objects that are processed
 */
public abstract class AbstractDisruptorMultiplexer<B> extends AbstractMultiplexer<B> {

	private DisruptorProvider<?> disruptor;
	
	public AbstractDisruptorMultiplexer(DisruptorProvider<?> disruptor) {
		super();
		this.disruptor = disruptor;
	}
	
	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.threaded.IMultiplexer#start()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void start() {
		if (!isRunning()) {
			connectHandlers((MultiplexerInput<B>[]) disruptor.getHandlers());
		}
		super.start();
	}
	
}