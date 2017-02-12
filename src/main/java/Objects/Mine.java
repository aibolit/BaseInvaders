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

    public Mine(Point position) {
        super(position);
    }

    public Mine(Mine mine) {
        this(new Point(mine.getPosition()));
        if (mine.owner != null) {
            owner = new Player(mine.owner);
        }
    }

    private Player owner = null;

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    @Override
    public void nextRound() {
    }

}
