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

    private static final long serialVersionUID = 1L;

    private Player owner = null;
    private long index = 0;
    private Point velocity;

    public Mine(Point position, Point velocity) {
        super(position);

        this.velocity = velocity;
    }

    public Mine(Mine mine) {
        this(new Point(mine.getPosition()), new Point(mine.velocity));
        if (mine.owner != null) {
            owner = new Player(mine.owner);
        }
        if (mine.index != 0) {
            index = mine.index;
        }
        else {
            index = mine.getId();
        }
    }

    public Player getOwner() {
        return owner;
    }

    public long getIndex() {
        return index;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public Point getVelocity() {
        return velocity;
    }

    @Override
    public void nextRound() {
    }

}
