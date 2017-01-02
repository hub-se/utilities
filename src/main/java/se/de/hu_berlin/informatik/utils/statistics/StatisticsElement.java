package se.de.hu_berlin.informatik.utils.statistics;

import se.de.hu_berlin.informatik.utils.statistics.StatisticsAPI.StatisticType;

public interface StatisticsElement<T> {

	public StatisticType getType();
	
	public T getValue();

	public void mergeWith(StatisticsElement<?> element);
	
}
