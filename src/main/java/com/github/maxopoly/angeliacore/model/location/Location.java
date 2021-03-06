package com.github.maxopoly.angeliacore.model.location;

import java.text.DecimalFormat;

public final class Location {

	private static DecimalFormat format = new DecimalFormat("#.##");

	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	public Location(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public Location(Vector v) {
		this(v.getX(), v.getY(), v.getZ(), 0.0f, 0.0f);
	}

	public Location(int x, int y, int z) {
		this(x, y, z, 0.0f, 0.0f);
	}

	public Location addVector(Vector v) {
		return new Location(this.x + v.getX(), this.y + v.getY(), this.z + v.getZ(), yaw, pitch);
	}

	/**
	 * @return x coordinate
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return y coordinate
	 */
	public double getY() {
		return y;
	}

	/**
	 * @return z coordinate
	 */
	public double getZ() {
		return z;
	}

	/**
	 * @return x coordinate rounded down to an int
	 */
	public int getBlockX() {
		return (int) Math.floor(x);
	}

	/**
	 * @return y coordinate rounded down to an int
	 */
	public int getBlockY() {
		return (int) Math.floor(y);
	}

	/**
	 * @return z coordinate rounded down to an int
	 */
	public int getBlockZ() {
		return (int) Math.floor(z);
	}

	/**
	 * Drops yaw and pitch and rounds all coordinates to integers
	 *
	 * @return
	 */
	public Location toBlockLocation() {
		return new Location(Math.floor(x), Math.floor(y), Math.floor(z), 0.0f, 0.0f);
	}

	/**
	 * Turns this location into a vector, which represents the locations offset from 0, 0, 0
	 *
	 * @return Vector representing this location
	 */
	public Vector toVector() {
		return new Vector(x, y, z);
	}

	/**
	 * Returns a copy of this location with each coordinate modified by the given amount
	 *
	 * @param x
	 *          Number to add to x coord
	 * @param y
	 *          Number to add to y coord
	 * @param z
	 *          Number to add to z coord
	 * @return Modified location
	 */
	public Location relativeBlock(double x, double y, double z) {
		return new Location(this.x + x, this.y + y, this.z + z, this.yaw, this.pitch);
	}

	/**
	 * If this location represents a block (using full integer coords), then this method will return the center of the
	 * block
	 *
	 * @return Block center
	 */
	public Location getBlockCenter() {
		return new Location(x + 0.5, y + 0.5, z + 0.5, yaw, pitch);
	}

	/**
	 * If this location represents a block (using full integer coords), then this method will return the center of the
	 * block, while leaving y unchanged
	 *
	 * @return Block center only for the x and z coordinate
	 */
	public Location getBlockCenterXZ() {
		return new Location(x + 0.5, y, z + 0.5, yaw, pitch);
	}

	/**
	 * @return current yaw, used to calculate the direction the entity is facing
	 */
	public float getYaw() {
		return yaw;
	}

	/**
	 * @return current pitch, used to calculate the direction the entity is facing
	 */
	public float getPitch() {
		return pitch;
	}

	/**
	 * Calculates the middle between this location and the given one based on their euclidian distance
	 *
	 * @param other
	 *          Second location to use
	 * @return The middle between the locations
	 */
	public Location getMiddle(Location other) {
		return new Location((x + other.x) / 2, (y + other.y) / 2, (z + other.z) / 2, yaw, pitch);
	}

	/**
	 * Gets a point on the line between this location and the given one. How far this point is from the starting location
	 * is determined by the factor, where a factor of 0 is the starting point (this instance) and a factor of 1 is the
	 * second location given as parameter. The range of the factor is not limited to [0,1] though, any double is allowed.
	 *
	 * @param other Location to base line on
	 * @param factor
	 * @return
	 */
	public Location getScaledMiddle(Location other, double factor) {
		return new Location(x + (other.x - x) * factor, y + (other.y - y) * factor, z + (other.z - z) * factor, yaw, pitch);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Loc: " + format.format(x) + " " + format.format(y) + " " + format.format(z));
		sb.append(", Yaw: " + format.format(yaw));
		sb.append(", Pitch: " + format.format(pitch));
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Location)) {
			return false;
		}
		Location loc = (Location) o;
		double accuracy = 0.001;
		// view and pitch dont really matter for location comparison
		return Math.abs(loc.x - x) < accuracy && Math.abs(loc.y - y) < accuracy && Math.abs(loc.z - z) < accuracy;
	}

	public static float translateAngleFrom256Step(byte step) {
		return (step) * 180f;
	}

}
