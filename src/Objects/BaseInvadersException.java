/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Objects;

/**
 *
 * @author Sasa
 */
public class BaseInvadersException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>ExchangeException</code> without detail
     * message.
     */
    public BaseInvadersException() {
    }

    /**
     * Constructs an instance of <code>ExchangeException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public BaseInvadersException(String msg) {
        super(msg);
    }
}
