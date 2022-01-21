/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.io.Serializable;

/**
 *
 * @author Aleks
 */
public class Mine extends GameObject implements Serializable {

    public Mine(Point position, long resources, long maxResources) {
        super(position);
        this.resources = resources;
        this.maxResources = maxResources;
    }

    public Mine(Mine mine) {
        this(new Point(mine.getPosition()), mine.getResources(), mine.maxResources);
        if (mine.owner != null) {
            owner = new Player(mine.owner);
        }
    }

    private long resources;
    private final long maxResources;

    private Player owner = null;

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void setResources(long resources) {
        this.resources = resources;
    }

    public long getResources() {
        return this.resources;
    }

    /**
     * Decrease resources by the given amount.  Returns effective amount of resources used.
     * @param usage amount of resources to be attempted to mine
     * @return amount of resources actually used
     */
    public long mineResources(long resource) {
        long decreasedResources = Math.min(this.resources, resource);
        this.resources -= decreasedResources;
        return decreasedResources;
    }

    /**
     * Adds resources up to maxResources
     * @param replenish amount to try to add
     */
    public void replenishResources(long replenish) {
        this.resources = Math.max(this.maxResources, this.resources + replenish); 
    }

    @Override
    public void nextRound() {
    }

}
