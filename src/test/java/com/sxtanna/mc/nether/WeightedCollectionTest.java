package com.sxtanna.mc.nether;

import com.sxtanna.mc.nether.util.WeightedCollection;
import org.junit.jupiter.api.Test;

public final class WeightedCollectionTest
{

	@Test
	void testGet()
	{
		final WeightedCollection<String> strings = new WeightedCollection<>();

		strings.add("Hello", String::length);
		strings.add("World", String::length);

		strings.add("Much Higher Weight", String::length);


		for (int i = 0; i < 20; i++)
		{
			System.out.println(strings.get());
		}
	}

}
