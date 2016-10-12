/**
 * 
 */
package se.de.hu_berlin.informatik.utils.tm.pipeframework;

import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.tm.ITransmitterProvider;
import se.de.hu_berlin.informatik.utils.tracking.ITrackable;
import se.de.hu_berlin.informatik.utils.tracking.ITrackingStrategy;
import se.de.hu_berlin.informatik.utils.tracking.TrackerDummy;

/**
 * Provides more general and easy access methods for the linking of pipes
 * and for the submission of items to a chain of pipes.
 * 
 * @author Simon Heiden
 *
 */
public class PipeLinker implements ITrackable {
	
	/**
	 * Creates a new pipe linker. Assumes that input items are
	 * submitted from a single thread. If multiple threads submit
	 * items to this linker, use the other constructor and set
	 * the parameter to false.
	 */
	public PipeLinker() {
		this(true);
	}

	/**
	 * Creates a new pipe linker.
	 * @param singleWriter
	 * whether input items are submitted from a single thread
	 */
	public PipeLinker(boolean singleWriter) {
		super();
		this.singleWriter = singleWriter;
	}

	private boolean singleWriter = true;
	private APipe<?,?> startPipe = null;
	private APipe<?,?> endPipe = null;

	/**
	 * Links the given transmitters together to a chain of pipes 
	 * and appends them to former appended pipes, if any. 
	 * If the transmitters don't match, then
	 * execution stops and the application aborts.
	 * @param transmitters
	 * transmitters (pipes or modules) to be linked together
	 * @return
	 * this pipe linker
	 */
	public PipeLinker append(ITransmitterProvider<?,?>... transmitters) {	
		if (transmitters.length != 0) {
			try {
				if (startPipe == null) {
					startPipe = transmitters[0].asPipe();
					//set whether input items are submitted with a single thread
					startPipe.setProducerType(singleWriter);
					if (isTracking()) {
						startPipe.enableTracking(getTracker());
					}
				} else {
					endPipe.linkTo(transmitters[0].asPipe());
				}

				for (int i = 0; i < transmitters.length-1; ++i) {
					transmitters[i].asPipe().linkTo(transmitters[i+1].asPipe());
				}

				endPipe = transmitters[transmitters.length-1].asPipe();
			} catch(UnsupportedOperationException e) {
				Log.abort(this, e, "Unable to get pipe from a given transmitter.");
			}
		}
		return this;
	}
	
	/**
	 * Retrieves the start pipe or aborts the application if none set.
	 * @return
	 * the start pipe
	 */
	private APipe<?, ?> getStartPipe() {
		if (startPipe == null) {
			Log.abort(this, "No start pipe available.");
		}
		return startPipe;
	}

	/**
	 * Submits a single or multiple items to the underlying chain
	 * of pipes.
	 * @param items
	 * items to be submitted
	 * @return
	 * this pipe linker
	 */
	public PipeLinker submit(Object... items) {
		for (int i = 0; i < items.length; ++i) {
			getStartPipe().submitObject(items[i]);
		}
		return this;
	}
	
	/**
	 * Shuts down the pipe chain. Has to be called to complete execution.
	 * Otherwise, the application won't stop. Will return when the pipe
	 * chain has completed all executions.
	 */
	public void shutdown() {
		getStartPipe().shutdown();
	}
	
	/**
	 * Submits a single or multiple items to the underlying chain
	 * of pipes and shuts down the pipe afterwards for convenience.
	 * @param items
	 * items to be submitted
	 */
	public void submitAndShutdown(Object... items) {
		for (int i = 0; i < items.length; ++i) {
			getStartPipe().submitObject(items[i]);
		}
		shutdown();
	}
	
	@Override
	public PipeLinker enableTracking() {
		if (startPipe != null) {
			startPipe.enableTracking();
		}
		return this;
	}
	
	@Override
	public PipeLinker enableTracking(int stepWidth) {
		if (startPipe != null) {
			startPipe.enableTracking(stepWidth);
		}
		return this;
	}

	@Override
	public PipeLinker disableTracking() {
		if (startPipe != null) {
			startPipe.disableTracking();
		}
		return this;
	}

	@Override
	public PipeLinker enableTracking(ITrackingStrategy tracker) {
		if (startPipe != null) {
			startPipe.enableTracking(tracker);
		}
		return this;
	}

	@Override
	public PipeLinker enableTracking(boolean useProgressBar) {
		if (startPipe != null) {
			startPipe.enableTracking(useProgressBar);
		}
		return this;
	}

	@Override
	public PipeLinker enableTracking(boolean useProgressBar, int stepWidth) {
		if (startPipe != null) {
			startPipe.enableTracking(useProgressBar, stepWidth);
		}
		return this;
	}

	@Override
	public boolean isTracking() {
		if (startPipe != null) {
			return startPipe.isTracking();
		}
		return false;
	}

	@Override
	public void track() {
		if (startPipe != null) {
			startPipe.track();
		}
	}

	@Override
	public void track(String msg) {
		if (startPipe != null) {
			startPipe.track(msg);
		}
	}

	@Override
	public void delegateTrackingTo(ITrackable target) {
		if (startPipe != null) {
			startPipe.delegateTrackingTo(target);
		}
	}

	@Override
	public ITrackingStrategy getTracker() {
		if (startPipe != null) {
			return startPipe.getTracker();
		}
		return TrackerDummy.getInstance();
	}

	@Override
	public void setTracker(ITrackingStrategy tracker) {
		if (startPipe != null) {
			startPipe.setTracker(tracker);
		}
	}
	
}
