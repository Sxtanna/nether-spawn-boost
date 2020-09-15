package com.sxtanna.mc.nether.mods;

import com.google.common.primitives.Ints;
import com.sxtanna.mc.nether.base.State;
import com.sxtanna.mc.nether.data.SpawnBoost;
import com.sxtanna.mc.nether.util.WeightedCollection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.ThreadLocalRandom.current;

public final class SpawnBoostModule implements State, Listener
{

	private static final int VALID_SPAWN_ATTEMPTS = 4;


	private       int                            every;
	private       int                            range;
	private       int                            limitWithinRange;
	private       int                            limitWithinSpawn;
	private final WeightedCollection<SpawnBoost> boost = new WeightedCollection<>();

	@NotNull
	private final Plugin                      plugin;
	@NotNull
	private final AtomicReference<BukkitTask> update = new AtomicReference<>();


	public SpawnBoostModule(@NotNull final Plugin plugin)
	{
		this.plugin = plugin;
	}


	@Override
	public void load()
	{
		loadSpawnValues();
		loadEntityTypes();

		plugin.getServer()
			  .getPluginManager()
			  .registerEvents(this, plugin);

		this.update.set(plugin.getServer()
							  .getScheduler()
							  .runTaskTimerAsynchronously(plugin, this::spawn, 0L, every));
	}

	@Override
	public void kill()
	{
		HandlerList.unregisterAll(this);

		final BukkitTask task = this.update.getAndSet(null);
		if (task != null)
		{
			task.cancel();
		}

		boost.clear();
	}


	public void spawn()
	{
		final Set<UUID>                ignored = new HashSet<>();
		final Set<Player>              targets = new HashSet<>();
		final Map<World, List<Player>> players = retrievePlayersInNetherWorlds();
		if (players.isEmpty())
		{
			return;
		}

		final int max = players.values().stream().mapToInt(Collection::size).sum();
		if (max == 0)
		{
			return;
		}


		players.values().stream().flatMap(Collection::stream).forEach(player -> {
			if (ignored.contains(player.getUniqueId()))
			{
				return;
			}
			if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL)
			{
				return;
			}

			final Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), range, range, range);
			nearby.removeIf(entity -> !(entity instanceof LivingEntity) || (entity instanceof Player));

			if (nearby.size() >= limitWithinRange)
			{
				nearby.stream()
					  .filter(Player.class::isInstance)
					  .map(Entity::getUniqueId)
					  .forEach(ignored::add);
				return;
			}

			final int compare = current().nextInt(0, 100);
			final int chances = Math.min(100, max + current().nextInt(0, 5));

			// the more players there are, the more likely this check is to pass
			if (compare >= chances)
			{
				return;
			}

			targets.add(player);
		});


		ignored.clear();
		players.clear();

		targets.stream()
			   .sorted(Comparator.comparingInt(player -> player.getWorld().getEntities().size()))
			   .limit(limitWithinSpawn)
			   .forEach(player -> {
				   final Optional<SpawnBoost> optionalSpawnBoost = this.boost.get();
				   if (!optionalSpawnBoost.isPresent())
				   {
					   return;
				   }

				   final SpawnBoost     boost     = optionalSpawnBoost.get();
				   final List<Location> locations = new ArrayList<>();


				   final Location origin = player.getLocation();
				   final int      amount = !boost.amountIsRange() ? boost.getMinAmt() : current().nextInt(boost.getMinAmt(), boost.getMaxAmt());

				   for (int $ = 0; $ < amount; $++)
				   {
					   findValidSpawnLocation(origin).ifPresent(locations::add);
				   }

				   if (locations.isEmpty())
				   {
					   return;
				   }

				   plugin.getServer()
						 .getScheduler()
						 .runTask(plugin, () -> locations.forEach(location -> boost.getEntity().apply(location)));
			   });
	}


	@NotNull
	private Map<World, List<Player>> retrievePlayersInNetherWorlds()
	{
		return plugin.getServer()
					 .getWorlds()
					 .stream()
					 .filter(world -> world.getEnvironment() == World.Environment.NETHER)
					 .collect(Collectors.toMap(Function.identity(), World::getPlayers));
	}

	@NotNull
	private Optional<Location> findValidSpawnLocation(@NotNull final Location origin)
	{
		int attempts = 0;
		while (attempts++ <= VALID_SPAWN_ATTEMPTS)
		{
			final Location offset = origin.clone().add(current().nextDouble(-range, +range),
													   0.0,
													   current().nextDouble(-range, +range));


			final Block block = offset.getBlock();

			final Block above = block.getRelative(BlockFace.UP);
			final Block below = block.getRelative(BlockFace.DOWN);

			if (!below.getType().isSolid() || !block.getType().isTransparent() || !above.getType().isTransparent())
			{
				continue;
			}

			return Optional.of(offset);
		}


		return Optional.empty();
	}


	private void loadSpawnValues()
	{
		final int every = plugin.getConfig().getInt("spawn.every", 2);
		final int range = plugin.getConfig().getInt("spawn.range", 20);

		final int limitWithinRange = plugin.getConfig().getInt("spawn.limit.within_range", 50);
		final int limitWithinSpawn = plugin.getConfig().getInt("spawn.limit.within_spawn", 50);

		this.every = every;
		this.range = range;

		this.limitWithinRange = limitWithinRange;
		this.limitWithinSpawn = limitWithinSpawn;
	}

	@SuppressWarnings("UnstableApiUsage")
	private void loadEntityTypes()
	{
		final ConfigurationSection section = plugin.getConfig().getConfigurationSection("types");
		if (section == null)
		{
			return;
		}

		for (final String type : section.getKeys(false))
		{
			EntityType entity;
			boolean    wither = false;

			try
			{
				entity = EntityType.valueOf(type.toUpperCase());
			}
			catch (final IllegalArgumentException ex)
			{
				if (!type.equalsIgnoreCase("wither_skeleton"))
				{
					continue; // maybe log this?
				}

				wither = true;
				entity = EntityType.SKELETON;
			}

			final int    weight = section.getInt(type + ".weight", 1);
			final String amount = section.getString(type + ".amount", "1");

			final int minAmt;
			final int maxAmt;

			final String[] amounts = amount.split("\\.\\.");
			if (amounts.length == 0)
			{
				continue; // maybe log this?
			}

			if (amounts.length == 1)
			{
				final Integer parse = Ints.tryParse(amounts[0]);
				if (parse == null)
				{
					continue; // maybe log this?
				}

				minAmt = parse;
				maxAmt = parse;
			}
			else
			{
				final Integer minParse = Ints.tryParse(amounts[0]);
				if (minParse == null)
				{
					continue; // maybe log this?
				}

				final Integer maxParse = Ints.tryParse(amounts[1]);
				if (maxParse == null)
				{
					continue; // maybe log this?
				}

				minAmt = minParse;
				maxAmt = maxParse;
			}

			final EntityType finalEntity = entity;
			final boolean    finalWither = wither;

			boost.add(new SpawnBoost(location -> {
				final Entity spawned = location.getWorld().spawnEntity(location, finalEntity);
				if (finalWither && spawned instanceof Skeleton)
				{
					((Skeleton) spawned).setSkeletonType(Skeleton.SkeletonType.WITHER);
				}

				return spawned;
			}, weight, minAmt, maxAmt), SpawnBoost::getWeight);
		}
	}

}
