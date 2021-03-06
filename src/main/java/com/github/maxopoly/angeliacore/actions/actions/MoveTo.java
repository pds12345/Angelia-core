package com.github.maxopoly.angeliacore.actions.actions;

import com.github.maxopoly.angeliacore.actions.AbstractAction;
import com.github.maxopoly.angeliacore.actions.ActionLock;
import com.github.maxopoly.angeliacore.connection.ServerConnection;
import com.github.maxopoly.angeliacore.connection.play.packets.out.PlayerPositionPacket;
import com.github.maxopoly.angeliacore.model.PlayerStatus;
import com.github.maxopoly.angeliacore.model.location.Location;
import java.io.IOException;

public class MoveTo extends AbstractAction {

	private Location destination;
	private double movementSpeed;
	private double ticksPerSecond;
	private final static double errorMargin = 0.01;
	public static final double SLOW_SPEED = 1.0;
	public static final double WALKING_SPEED = 4.317;
	public static final double SPRINTING_SPEED = 5.612;
	public static final double FALLING = 20.0;

	public MoveTo(ServerConnection connection, Location desto, double movementSpeed) {
		super(connection);
		this.destination = desto;
		this.movementSpeed = movementSpeed;
	}

	/**
	 * Calculates the location to which the player should move on the next client tick
	 *
	 * @param current
	 *          The players current location
	 * @param movementSpeed
	 *          The speed at which the player is supposed to move
	 * @return
	 */
	public Location getNextLocation(Location current, double movementSpeed, double ticksPerSecond) {
		double xDiff = destination.getX() - current.getX();
		double yDiff = destination.getY() - current.getY();
		double zDiff = destination.getZ() - current.getZ();
		connection.getPlayerStatus().setMidAir(yDiff > errorMargin);
		double distance = Math.sqrt((xDiff * xDiff) + (yDiff * yDiff) + (zDiff * zDiff));
		double timeTakenSeconds = distance / movementSpeed;
		double timeTakenTicks = timeTakenSeconds * ticksPerSecond;
		if (timeTakenTicks < 1.0) {
			timeTakenTicks = 1.0;
		}

		return new Location(current.getX() + (xDiff / timeTakenTicks), current.getY() + (yDiff / timeTakenTicks),
				current.getZ() + (zDiff / timeTakenTicks), current.getYaw(), current.getPitch());
	}

	public boolean hasReachedDesto(Location current) {
		return Math.abs(current.getX() - destination.getX()) < errorMargin
				&& Math.abs(current.getZ() - destination.getZ()) < errorMargin;
	}

	public Location getDestination() {
		return destination;
	}

	public void sendLocationPacket() {
		Location playerLoc = connection.getPlayerStatus().getLocation();
		try {
			connection.sendPacket(new PlayerPositionPacket(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), !connection
					.getPlayerStatus().isMidAir()));
		} catch (IOException e) {
			connection.getLogger().error("Failed to update location", e);
		}
	}

	@Override
	public void execute() {
		this.ticksPerSecond = connection.getTicksPerSecond();
		PlayerStatus status = connection.getPlayerStatus();
		status.updateLocation(getNextLocation(status.getLocation(), movementSpeed, ticksPerSecond));
		sendLocationPacket();
		if (isDone()) {
			connection.getPlayerStatus().setMidAir(false);
		}
	}

	@Override
	public boolean isDone() {
		return hasReachedDesto(connection.getPlayerStatus().getLocation());
	}

	@Override
	public ActionLock[] getActionLocks() {
		return new ActionLock[] { ActionLock.MOVEMENT };
	}

}
