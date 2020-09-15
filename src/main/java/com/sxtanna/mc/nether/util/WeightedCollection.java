package com.sxtanna.mc.nether.util;

import com.google.common.util.concurrent.AtomicDouble;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.concurrent.ThreadLocalRandom.current;

public final class WeightedCollection<T>
{

	private final AtomicDouble       total = new AtomicDouble();
	private final TreeMap<Double, T> value = new TreeMap<>();


	@NotNull
	public Optional<T> get()
	{
		if (value.isEmpty())
		{
			return Optional.empty();
		}

		final Map.Entry<Double, T> entry = value.higherEntry(current().nextDouble() * total.get());
		if (entry == null)
		{
			return Optional.empty();
		}

		return Optional.of(entry.getValue());
	}

	public void add(@NotNull final T value, @NotNull final Function<T, Number> weightFunction)
	{
		final double weight = weightFunction.apply(value).doubleValue();
		if (weight <= 0.0)
		{
			return;
		}

		this.value.put(this.total.addAndGet(weight), value);
	}


	public void clear()
	{
		value.clear();
		total.set(0.0);
	}

}
