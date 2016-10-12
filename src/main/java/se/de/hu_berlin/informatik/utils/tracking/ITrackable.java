package se.de.hu_berlin.informatik.utils.tracking;

public interface ITrackable {

	/**
	 * This method should always return a valid
	 * tracker object, otherwise a NullPointerException will be thrown
	 * if track() is called. If tracking shall be disabled, set this to a
	 * TrackerDummy instance. 
	 * @return
	 * the tracker object
	 */
	public ITrackingStrategy getTracker();
	
	/**
	 * @param tracker
	 * sets a tracker
	 */
	public void setTracker(ITrackingStrategy tracker);
	
	/**
	 * Enables tracking of progress. Doesn't use a progress bar.
	 * @return
	 * this object for chaining
	 */
	default public ITrackable enableTracking() {
		return enableTracking(false);
	}
	
	/**
	 * Enables tracking of progress.
	 * @param useProgressBar
	 * whether to use a progress bar to show the progress
	 * @return
	 * this object for chaining
	 */
	default public ITrackable enableTracking(boolean useProgressBar) {
		setTracker(new ProgressTracker(useProgressBar));
		return this;
	}
	
	/**
	 * Enables tracking of progress. Uses the given
	 * step width to only produce output after the
	 * given number of tracked elements. Doesn't use 
	 * a progress bar.
	 * @param stepWidth
	 * sets the step width for producing outputs
	 * @return
	 * this object for chaining
	 */
	default public ITrackable enableTracking(int stepWidth) {
		return enableTracking(false, stepWidth);
	}
	
	/**
	 * Enables tracking of progress. Uses the given
	 * step width to only produce output after the
	 * given number of tracked elements.
	 * @param useProgressBar
	 * whether to use a progress bar to show the progress
	 * @param stepWidth
	 * sets the step width for producing outputs
	 * @return
	 * this object for chaining
	 */
	default public ITrackable enableTracking(boolean useProgressBar, int stepWidth) {
		setTracker(new ProgressTracker(useProgressBar, stepWidth));
		return this;
	}
	
	/**
	 * Disables tracking of progress.
	 * @return
	 * this object for chaining
	 */
	default public ITrackable disableTracking() {
		setTracker(TrackerDummy.getInstance());
		return this;
	}
	
	/**
	 * Enables tracking of progress, while using the given 
	 * tracker object. If the given tracker is null, then a
	 * new tracker is created that doesn't use a progress bar.
	 * @param tracker
	 * a tracker object to use
	 * @return
	 * this object for chaining
	 */
	default public ITrackable enableTracking(ITrackingStrategy tracker) {
		if (tracker != null) {
			setTracker(tracker);
		} else {
			setTracker(new ProgressTracker(false));
		}
		return this;
	}
	
	/**
	 * @return
	 * whether this transmitter's progress tracking is enabled
	 */
	default public boolean isTracking() {
		return (getTracker() != null && getTracker() != TrackerDummy.getInstance());
	}
	
	/**
	 * Tracks the progress for a processed element if tracking
	 * has been enabled.
	 * @throws NullPointerException
	 * if getTracker() returns null
	 */
	default public void track() throws NullPointerException {
//		if (isTracking()) {
			getTracker().track();
//		}
	}
	
	/**
	 * Tracks the progress for a processed element if tracking
	 * has been enabled.
	 * @param msg
	 * a message to display
	 * @throws NullPointerException
	 * if getTracker() returns null
	 */
	default public void track(String msg) throws NullPointerException {
//		if (isTracking()) {
			getTracker().track(msg);
//		}
	}
	
	/**
	 * Delegates the tracking tasks from this object to the given
	 * target trackable object. After delegation to another object,
	 * tracking will not work any more for this object and all
	 * properties will be moved to the target object. Additionally,
	 * the tracker will be reset.
	 * @param target
	 * the Trackable object to delegate the tracking to
	 */
	default public void delegateTrackingTo(ITrackable target) {
		if (isTracking()) {
			this.getTracker().reset();
			target.enableTracking(this.getTracker());
			this.disableTracking();
		}
	}
	
}