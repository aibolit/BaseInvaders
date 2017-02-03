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
public class Bomb extends GameObject implements Serializable {

    private long lifetime = 0;
    private final Player player;
    private final long delay;

    public Bomb(Player player, Point position, long delay) {
        super(position);
        this.player = player;
        this.delay = delay;
    }

    public Bomb(Bomb bomb) {
        super(bomb.getPosition());
        this.player = new Player(bomb.getPlayer());
        this.delay = bomb.getDelay();
        this.lifetime = bomb.getLifetime();
    }

    public long getLifetime() {
        return lifetime;
    }

    boolean isExploded() {
        return lifetime >= delay;
    }

    public Player getPlayer() {
        return player;
    }

    public long getDelay() {
        return delay;
    }

    @Override
    public void nextRound() {
        lifetime++;
    }

}
