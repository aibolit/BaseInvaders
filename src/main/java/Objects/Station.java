/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.io.Serializable;

/**
 *
 * @author Thomas
 */
public class Station extends GameObject implements Serializable {

    public Station(Point position) {
        super(position);
    }

    public Station(Station station) {
        this(new Point(station.getPosition()));
    }


    @Override
    public void nextRound() {
    }

}
