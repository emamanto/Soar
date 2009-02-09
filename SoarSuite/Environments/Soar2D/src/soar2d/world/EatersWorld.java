package soar2d.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import soar2d.Direction;
import soar2d.Names;
import soar2d.Soar2D;
import soar2d.map.CellObject;
import soar2d.map.EatersMap;
import soar2d.map.GridMap;
import soar2d.players.MoveInfo;
import soar2d.players.Player;

public class EatersWorld implements IWorld {
	private static Logger logger = Logger.getLogger(EatersWorld.class);

	public boolean postLoad(GridMap newMap) {
		return true;
	}
	
	String restartMessage;

	public String update(GridMap _map, PlayersManager players) {
		restartMessage = null;
		
		EatersMap map = (EatersMap)_map;
		moveEaters(map, players);
		if (Soar2D.control.isShuttingDown()) {
			return null;
		}
		updateMapAndEatFood(map, players);
		handleEatersCollisions(map, players, findCollisions(players));	
		updatePlayers(false, map, players);
		map.updateObjects(null);
		
		return restartMessage;
	}
	
	public void fragPlayer(Player player, GridMap map, PlayersManager players, int [] location) {
		
	}
	
	public void putInStartingLocation(Player player, GridMap map, PlayersManager players, int [] location) {
		// remove food from it
		map.removeAllByProperty(players.getLocation(player), Names.kPropertyEdible);
	}
	
	public void reset(GridMap map) {
		this.restartMessage = null;
	}
	
	public void updatePlayers(boolean playersChanged, GridMap map, PlayersManager players) {
		Iterator<Player> iter = players.iterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			player.update(players.getLocation(player));
		}
	}

	private void moveEaters(EatersMap map, PlayersManager players) {
		Iterator<Player> iter = players.iterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			MoveInfo move = players.getMove(player);			

			if (!move.move) {
				continue;
			}

			// Calculate new location
			int [] oldLocation = players.getLocation(player);
			int [] newLocation = Arrays.copyOf(oldLocation, oldLocation.length);
			Direction.translate(newLocation, move.moveDirection);
			if (move.jump) {
				Direction.translate(newLocation, move.moveDirection);
			}
			
			// Verify legal move and commit move
			if (map.isInBounds(newLocation) && !map.hasAnyWithProperty(newLocation, Names.kPropertyBlock)) {
				// remove from cell
				map.setPlayer(oldLocation, null);
				
				if (move.jump) {
					player.adjustPoints(Soar2D.config.eatersConfig().jump_penalty, "jump penalty");
				}
				players.setLocation(player, newLocation);
				
			} else {
				player.adjustPoints(Soar2D.config.eatersConfig().wall_penalty, "wall collision");
			}
		}
	}

	private void updateMapAndEatFood(EatersMap map, PlayersManager players) {
		Iterator<Player> iter = players.iterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			MoveInfo lastMove = players.getMove(player);
			int [] location = players.getLocation(player);
			
			if (lastMove.move || lastMove.jump) {
				map.setPlayer(location, player);

				ArrayList<CellObject> moveApply = map.getAllWithProperty(location, Names.kPropertyMoveApply);
				if (moveApply != null) {
					for (CellObject object : moveApply) {
						if (object.apply(player)) {
							map.removeObject(location, object.getName());
						}
					}
				}
			}
			
			if (!lastMove.dontEat) {
				eat(player, map, location);
			}
			
			if (lastMove.open) {
				open(player, map, location, lastMove.openCode);
			}
		}
	}
	
	private void open(Player player, EatersMap map, int [] location, int openCode) {
		ArrayList<CellObject> boxes = map.getAllWithProperty(location, Names.kPropertyBox);
		if (boxes == null) {
			logger.warn(player.getName() + " tried to open but there is no box.");
			return;
		}

		// TODO: multiple boxes
		assert boxes.size() <= 1;
		
		CellObject box = boxes.get(0);
		if (box.hasProperty(Names.kPropertyStatus)) {
			if (box.getProperty(Names.kPropertyStatus).equalsIgnoreCase(Names.kOpen)) {
				logger.warn(player.getName() + " tried to open an open box.");
				return;
			}
		}
		if (openCode != 0) {
			box.addProperty(Names.kPropertyOpenCode, Integer.toString(openCode));
		}
		if (box.apply(player)) {
			map.removeObject(location, box.getName());
		}
		
		if (box.getResetApply()) {
			String message = "Max resets achieved.";
			if (Soar2D.control.checkRunsTerminal()) {
				Soar2D.simulation.world.dumpStats(true, message);
			} else {
				restartMessage = message;
			}
		}
	}
	
	private void eat(Player player, EatersMap map, int [] location) {
		ArrayList<CellObject> list = map.getAllWithProperty(location, Names.kPropertyEdible);
		if (list != null) {
			for (CellObject food : list) {
				if (food.apply(player)) {
					// if this returns true, it is consumed
					map.removeObject(location, food.getName());
				}
			}
		}			
	}
	
	private ArrayList<ArrayList<Player>> findCollisions(PlayersManager players) {
		ArrayList<ArrayList<Player>> collisions = new ArrayList<ArrayList<Player>>(players.numberOfPlayers() / 2);

		// Make sure collisions are possible
		if (players.numberOfPlayers() < 2) {
			return collisions;
		}
		
		// Optimization to not check the same name twice
		HashSet<Player> colliding = new HashSet<Player>(players.numberOfPlayers());
		ArrayList<Player> collision = new ArrayList<Player>(players.numberOfPlayers());

		ListIterator<Player> leftIter = players.listIterator();
		while (leftIter.hasNext()) {
			Player left = leftIter.next();
			
			// Check to see if we're already colliding
			if (colliding.contains(left)) {
				continue;
			}
			
			ListIterator<Player> rightIter = players.listIterator(leftIter.nextIndex());
			// Clear collision list now
			collision.clear();
			while (rightIter.hasNext()) {
				// For each player to my right (I've already checked to my left)
				Player right = rightIter.next();

				// Check to see if we're already colliding
				if (colliding.contains(right)) {
					continue;
				}
				
				// If the locations match, we have a collision
				if (players.getLocation(left).equals(players.getLocation(right))) {
					
					// Add to this set to avoid checking same player again
					colliding.add(left);
					colliding.add(right);
					
					// Add the left the first time a collision is detected
					if (collision.size() == 0) {
						collision.add(left);
						
						logger.debug("collision at " + players.getLocation(left));
					}
					// Add each right as it is detected
					collision.add(right);
				}
			}
			
			// Add the collision to the total collisions if there is one
			if (collision.size() > 0) {
				collisions.add(new ArrayList<Player>(collision));
			}
		}

		return collisions;
	}
		
	private void handleEatersCollisions(EatersMap map, PlayersManager players, ArrayList<ArrayList<Player>> collisions) {
		
		// if there are no total collisions, we're done
		if (collisions.size() < 1) {
			return;
		}

		ArrayList<Player> collision = new ArrayList<Player>(players.numberOfPlayers());
		
		Iterator<ArrayList<Player>> collisionIter = collisions.iterator();
		while (collisionIter.hasNext()) {
			collision = collisionIter.next();

			assert collision.size() > 0;
			logger.debug("Processing collision group with " + collision.size() + " collidees.");

			// Redistribute wealth
			int cash = 0;			
			ListIterator<Player> collideeIter = collision.listIterator();
			while (collideeIter.hasNext()) {
				cash += collideeIter.next().getPoints();
			}
			if (cash > 0) {
				int trash = cash % collision.size();
				cash /= collision.size();
				logger.debug("Cash to each: " + cash + " (" + trash + " lost in division)");
				collideeIter = collision.listIterator();
				while (collideeIter.hasNext()) {
					collideeIter.next().setPoints(cash, "collision");
				}
			} else {
				logger.debug("Sum of cash is negative.");
			}
			
			int [] collisionLocation = players.getLocation(collision.get(0));

			// Add the boom on the map
			map.setExplosion(collisionLocation);

			// Remove from former location (only one of these for all players)
			map.setPlayer(collisionLocation, null);
			
			// Move to new cell, consume food
			collideeIter = collision.listIterator();
			while (collideeIter.hasNext()) {
				Player player = collideeIter.next();
				int [] location = Soar2D.simulation.world.putInStartingLocation(player, false);
				assert location != null;
				player.fragged();
				if (!players.getMove(player).dontEat) {
					eat(player, map, location);
				}
			}
		}
	}
	
	public int getMinimumAvailableLocations() {
		return 1;
	}

	public void resetPlayer(GridMap map, Player player, PlayersManager players, boolean resetDuringRun) {
		// This is here because the TOSCA stuff wants to keep around the reward
		// in the beginning of the next phase
		
		if (resetDuringRun) {
			player.mapReset();
		}
		
		player.reset();
	}

	public GridMap newMap() {
		return new EatersMap();
	}
}