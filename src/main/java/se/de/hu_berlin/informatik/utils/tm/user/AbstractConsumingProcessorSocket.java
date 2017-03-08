package se.de.hu_berlin.informatik.utils.tm.user;

import se.de.hu_berlin.informatik.utils.tm.ConsumingProcessor;
import se.de.hu_berlin.informatik.utils.tm.Processor;

public abstract class AbstractConsumingProcessorSocket<A> implements ConsumingProcessorSocket<A>, ConsumingProcessorSocketGenerator<A> {

	private Processor<A, Object> processor;
	
	public AbstractConsumingProcessorSocket(ConsumingProcessor<A> processor) {
		super();
		insert(processor);
	}

	@Override
	public Processor<A, Object> getProcessor() throws IllegalStateException {
		if (processor == null) {
			throw new IllegalStateException("No processor set for " + this.getClass().getSimpleName() + ".");
		} else {
			return processor;
		}
	}

	@Override
	public void setProcessor(Processor<A, Object> consumer) {
		this.processor = consumer;
	}

}