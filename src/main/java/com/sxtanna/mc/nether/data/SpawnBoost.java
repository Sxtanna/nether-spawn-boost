package com.sxtanna.mc.nether.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class SpawnBoost
{

	@NotNull
	private final Function<Location, Entity> entity;

	private final int weight;
	private final int minAmt;
	private final int maxAmt;


	public SpawnBoost(@NotNull final Function<Location, Entity> entity, final int weight, final int minAmt, final int maxAmt)
	{
		this.entity = entity;
		this.weight = weight;
		this.minAmt = minAmt;
		this.maxAmt = maxAmt;
	}


	@NotNull
	@Contract(pure = true)
	public Function<Location, Entity> getEntity()
	{
		return entity;
	}


	@Contract(pure = true)
	public int getWeight()
	{
		return weight;
	}

	@Contract(pure = true)
	public int getMinAmt()
	{
		return minAmt;
	}

	@Contract(pure = true)
	public int getMaxAmt()
	{
		return maxAmt;
	}

	@Contract(pure = true)
	public boolean amountIsRange()
	{
		return getMinAmt() != getMaxAmt();
	}


	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof SpawnBoost))
		{
			return false;
		}

		final SpawnBoost that = (SpawnBoost) o;
		return this.getWeight() == that.getWeight() &&
			   this.getMinAmt() == that.getMinAmt() &&
			   this.getMaxAmt() == that.getMaxAmt() &&
			   this.getEntity() == that.getEntity();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.getEntity(),
							this.getWeight(),
							this.getMinAmt(),
							this.getMaxAmt());
	}

	@Override
	public String toString()
	{
		return String.format("SpawnBoost[%s, weight=%d, amount=%d..%d]", entity, weight, minAmt, maxAmt);
	}

}
