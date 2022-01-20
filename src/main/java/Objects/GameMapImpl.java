package Objects;

import baseinvaders.Configurations;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Collections;
import javafx.util.Pair;  

/**
 *
 * @author Sasa
 */
public class GameMapImpl implements Runnable, Serializable, GameMap {

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userUpdates = new ConcurrentHashMap<>();
    private final Map<String, Double> userAcceleration = new ConcurrentHashMap<>();
    private final Map<String, Double> userAngle = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userBrakes = new ConcurrentHashMap<>();
    private final Map<String, Pair<Point, Double>> userDestinations = new ConcurrentHashMap<>();
    private final Map<String, Double> userArrivingDeceleration = new ConcurrentHashMap<>();
    private final Set<Mine> mines = new CopyOnWriteArraySet<>();
    private final Set<Station> stations = new CopyOnWriteArraySet<>();
    private final Map<String, Long> userScores = new ConcurrentHashMap<>();
    private final Map<String, Long> userMinerals = new ConcurrentHashMap<>();
    private final Map<String, Long> userCapacity = new ConcurrentHashMap<>();
    private final Set<Bomb> bombs = new CopyOnWriteArraySet<>();
    private final Map<String, Set<Bomb>> userBombs = new ConcurrentHashMap<>();
    private final Map<String, Long> userLastScan = new ConcurrentHashMap<>();
    private final Set<WormHole> wormHoles = new CopyOnWriteArraySet<>();
    private long ticks = 0, downtimeTicks = 0;
    private double mineTimer = 0.;

    private volatile boolean isRunning = true;

    public GameMapImpl() throws BaseInvadersException {
        Configurations.getUsers().forEach((player) -> {
            players.put(player,
                    new Player(player, new Point(Configurations.getMapWidth() / 2, Configurations.getMapHeight() / 2)));
        });
        reset();
    }

    public List<WormHole> getPossibleWormHoles() {
        List<WormHole> wormHoles = new ArrayList<WormHole>();
        int mapWidth = Configurations.getMapWidth();
        int mapHeight = Configurations.getMapHeight();
        double screenDivisions = 4;
        double xDistance = mapWidth / screenDivisions;
        double yDistance = mapHeight / screenDivisions;
        for (double i = xDistance / 2; i < mapWidth; i += xDistance) {
            for (double j = yDistance / 2; j < mapHeight; j += yDistance) {
                double radius = Math.random()
                        * (Configurations.getMaxWormHoleRadius() - Configurations.getMinWormHoleRadius())
                        + Configurations.getMinWormHoleRadius();
                Point destination;
                while (true) {
                    destination = new Point(Math.random() * Configurations.getMapWidth(),
                            Math.random() * Configurations.getMapHeight());
                    boolean valid = true;
                    for (WormHole w : wormHoles) {
                        if (w.getPosition().distanceTo(destination) < w.getRadius()) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        break;
                    }
                }
                double xOffset = xDistance / (Math.random() > .5 ? 4 : -4);
                double yOffset = yDistance / (Math.random() > .5 ? 4 : -4);
                Point center = new Point(i + xOffset, j + yOffset);
                wormHoles.add(new WormHole(center, radius, destination));
            }
        }
        return wormHoles;
    }

    private synchronized void reset() {
        ticks = 0;
        mines.clear();
        for (int i = 0; i < Configurations.getMineCount(); i++) {
            final Point minePosition = new Point(Math.random() * Configurations.getMapWidth(), Math.random() * Configurations.getMapHeight());
            mines.add(new Mine(minePosition, Configurations.getMineMaxResources(), Configurations.getMineMaxResources()));
        }

        stations.clear();
        for (int i = 0; i < Configurations.getStationCount(); i++) {

            // Center-ish
            if(Configurations.getStationCount() == 1) {
                stations.add(new Station(new Point( Configurations.getMapWidth()    * 0.4 + Math.random() * Configurations.getMapWidth()   * 0.2,
                                                    Configurations.getMapHeight()   * 0.4 + Math.random() * Configurations.getMapHeight()  * 0.2)));
            }
            // Top left / bottom right
            else if(Configurations.getStationCount() == 2) {
                // Quadrant 1
                if(i == 0) {
                    stations.add(new Station(new Point( Configurations.getMapWidth()    * 0 + Math.random() * Configurations.getMapWidth()   * 0.5,
                                                        Configurations.getMapHeight()   * 0 + Math.random() * Configurations.getMapHeight()  * 0.5)));
                }
                else {
                // Quadrant 2
                    stations.add(new Station(new Point( Configurations.getMapWidth()    * 0.5 + Math.random() * Configurations.getMapWidth()   * 0.5,
                                                        Configurations.getMapHeight()   * 0.5 + Math.random() * Configurations.getMapHeight()  * 0.5)));
                }
            }
            // Complete random
            else {
                stations.add(new Station(new Point( Configurations.getMapWidth()    * 0 + Math.random() * Configurations.getMapWidth()   * 1,
                                                    Configurations.getMapHeight()   * 0 + Math.random() * Configurations.getMapHeight()  * 1)));
            }
        }

        wormHoles.clear();
        List<WormHole> availableWormHoles = getPossibleWormHoles();
        Collections.shuffle(availableWormHoles);
        for (int i = 0; i < Configurations.getWormHoleCount(); i++) {
            if (i >= availableWormHoles.size()) {
                break;
            }
            WormHole w = availableWormHoles.get(i);
            wormHoles.add(w);
        }
        System.out.println(wormHoles);
        System.out.println(players);

        userScores.clear();
        userMinerals.clear();
        userCapacity.clear();
        userBombs.clear();
        bombs.clear();

        Configurations.getUsers().stream().forEach(player -> {
            userScores.put(player, 0L);
            userMinerals.put(player, 0L);
            userCapacity.put(player, Configurations.getMineralCapacity());
            userBombs.put(player, new CopyOnWriteArraySet<>());
        });

        players.values().stream().forEach(player -> {
            player.getPosition().setX(Configurations.getMapWidth() / 2).setY(Configurations.getMapHeight() / 2);
            player.getVelocity().setX(0).setY(0);
        });

        userAcceleration.clear();
        userAngle.clear();
        userBrakes.clear();
        userDestinations.clear();
        userArrivingDeceleration.clear();
        userLastScan.clear();
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void setRunning(boolean run) {
        this.isRunning = run;
    }

    private boolean isNearingDestination(String user,  double remainingDistance, double slowDownDistance) {
        return (remainingDistance < slowDownDistance && !userArrivingDeceleration.containsKey(user));
    }

    private void slowForArrival(String user, double remainingDistance, double currentSpeedPerSecond) {
        double decelTime = 2 * remainingDistance / currentSpeedPerSecond;
        double decel = -currentSpeedPerSecond / decelTime;
        userArrivingDeceleration.put(user, decel);
    }

    private boolean isArrived(double remainingDistance) {
        return remainingDistance < Configurations.getArrivalDistance();
    }

    private double getNextSpeed(double currentSpeedPerSecond, double accelPerSecond, double percent) {
        return Math.min(Configurations.getMaxSpeed(), currentSpeedPerSecond + accelPerSecond * Configurations.getFixedDelta() * percent); // per second
    }

    private void updatePlayerVelocity(Player player, double currentSpeedPerSecond, Point direction) {
        player.getVelocity().
            setX(direction.getX() * currentSpeedPerSecond * Configurations.getFixedDelta()).
            setY(direction.getY() * currentSpeedPerSecond * Configurations.getFixedDelta());
    }

    private void updatePlayerPosition(Player player, Point position) {
        player.getPosition().setX(position.getX() % Configurations.getMapWidth()).setY(position.getY()  % Configurations.getMapHeight());
    }

    private synchronized void nextRound() {
        if (!isRunning) {
            return;
        }

        Set<String> usersDestinationsToClear = new CopyOnWriteArraySet<>();

        players.entrySet().stream().parallel().forEach(entry -> {
            String user = entry.getKey();
            Player player = entry.getValue();
            if (userBrakes.containsKey(user) && userBrakes.get(user)) {
                player.getVelocity().multiply(Configurations.getBrakeFriction());
                Point p = player.getPosition();
                p.add(player.getVelocity())
                        .setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth())
                        .setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());
                player.getVelocity().multiply(Configurations.getFriction());
            }else if(userDestinations.containsKey(user)) {
                Pair<Point, Double> userDestination = userDestinations.get(user);
                Point destination = userDestination.getKey();
                Double slowDownDistance = userDestination.getValue();
                double distance = player.getPosition().distanceTo(destination);
                double currentSpeed = player.getVelocity().getMagnitude() / Configurations.getFixedDelta(); // per second

                // Do we need to slowdown for arrival?
                if(isNearingDestination(user, distance, slowDownDistance)) {
                    slowForArrival(user, distance, currentSpeed);
                }

                // Get direction
                double angle = player.getPosition().directionTo(destination);
                Point direction = new Point(Math.cos(angle), Math.sin(angle));

                double accel = userArrivingDeceleration.containsKey(user) ? userArrivingDeceleration.get(user) : Configurations.getDefaultAcceleration(); // per second

                // System.err.println("accel: " + accel + " current speed: " + currentSpeed / Configurations.getFixedDelta() + " rem dist: " + distance);

                // Snap to position upon arrival
                if(isArrived(distance)) {
                    //System.err.println("Arrived!!!: " + distance);
                    updatePlayerVelocity(player, 0, new Point(0, 0));
                    updatePlayerPosition(player, destination);
                    userArrivingDeceleration.remove(user);
                    userAcceleration.remove(user);
                    usersDestinationsToClear.add(user);
                }
                // Keep moving
                else {
                    // Apply half vel now
                    currentSpeed = getNextSpeed(currentSpeed, accel, 0.5);
                    updatePlayerVelocity(player, currentSpeed, direction);

                    // Update position
                    updatePlayerPosition(player, new Point(player.getPosition()).add(player.getVelocity()));

                    // Apply half vel later
                    currentSpeed = getNextSpeed(currentSpeed, accel, 0.5);
                    updatePlayerVelocity(player, currentSpeed, direction);
                }
            }
             else {
                double acceleration = 0;

                if (!player.isDisabled()) {
                    acceleration = Configurations.getSpeed()
                            * (userAngle.containsKey(user) && userAcceleration.containsKey(user)
                            ? userAcceleration.get(user)
                            : 0);

                }
                double angle = userAngle.containsKey(user) ? userAngle.get(user) : 0;

                double x = acceleration * Math.cos(angle);
                double y = acceleration * Math.sin(angle);

                player.getVelocity().add(x * .5, y * .5);

                Point p = player.getPosition();
                p.add(player.getVelocity())
                        .setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth())
                        .setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());

                player.getVelocity().add(x * .5, y * .5);
                player.getVelocity().multiply(Configurations.getFriction());
            }

            player.nextRound();
        });

        usersDestinationsToClear.forEach(user -> {
            if(userDestinations.containsKey(user)) {
                userDestinations.remove(user);
            }
        });

        mines.stream().parallel().forEach((Mine mine) -> {

            Player[] ps = players.values().stream().filter(
                    player -> player.distanceTo(mine) < Configurations.getCaptureRadius() && !player.isDisabled())
                    .toArray(Player[]::new);
            if (ps.length == 1) {
                mine.setOwner(ps[0]);
            }
        });

        players.entrySet().stream().parallel().forEach(playerEntry -> {
            Player player = playerEntry.getValue();
            WormHole currentWormHole = null;
            for (WormHole wormHole : wormHoles) {
                if (wormHole.getPosition().distanceTo(player.getPosition()) < wormHole.getRadius()) {
                    currentWormHole = wormHole;
                    break;
                }
            }

            if (currentWormHole != null) {
                player.setDisabled(true);

                // Set velocity towards center of worm hole
                if (player.getPosition().distanceTo(currentWormHole.getPosition()) > Configurations
                        .getWormHoleCenterRadius()) {
                    double direction = player.getPosition().directionTo(currentWormHole.getPosition());
                    double gravity = Configurations.getWormHoleGravity();
                    double x = gravity * Math.cos(direction);
                    double y = gravity * Math.sin(direction);
                    player.getVelocity().add(x, y);

                    // Transport players in center of worm hole
                } else {
                    Point destination = currentWormHole.getDestination();
                    player.getPosition().setX(destination.getX());
                    player.getPosition().setY(destination.getY());
                    player.getVelocity().setX(0).setY(0);
                }
            } else {
                player.setDisabled(false);
            }
        });

        // Compute how often a mine awards minerals
        mineTimer += Configurations.getFixedDelta();
        boolean awardMineral = false;
        if(mineTimer >= Configurations.getMineralPerSecondTime()) {
            awardMineral = true;
            mineTimer = 0.;
        }
        final boolean captureAwardMineral = awardMineral;
        mines.stream().filter((mine) -> (mine.getOwner() != null)).forEach((mine) -> {
            if(captureAwardMineral) {
                final long amountMined = mine.mineResources(Configurations.getMineResourceAmount());
                userMinerals.put(mine.getOwner().getName(), Math.min(userCapacity.get(mine.getOwner().getName()), amountMined + userMinerals.get(mine.getOwner().getName())));
            }
        });
        mines.stream().forEach((mine) -> {
            mine.replenishResources(Configurations.getMineResourceReplenishAmount());
        });

        Set<Bomb> removeBombs = new CopyOnWriteArraySet<>();
        bombs.stream().parallel().forEach(bomb -> {
            if (bomb.isExploded()) {
                removeBombs.add(bomb);
                players.values().stream()
                        .filter((player) -> (player.distanceTo(bomb) < Configurations.getBombExplosionRadius()))
                        .forEach((player) -> {
                            wipeExistingDestination(player.getName());
                            double angle = bomb.directionTo(player);
                            double acceleration = Configurations.getBombPower()
                                    * Math.sqrt((Configurations.getBombExplosionRadius() - bomb.distanceTo(player))
                                            / Configurations.getBombExplosionRadius());

                            double x = acceleration * Math.cos(angle);
                            double y = acceleration * Math.sin(angle);

                            player.getVelocity().add(x, y);
                        });
            }

            bomb.nextRound();
        });
        removeBombs.stream().forEach(bomb -> {
            bombs.remove(bomb);
            userBombs.get(bomb.getPlayer().getName()).remove(bomb);
        });

        System.gc();
    }

    private synchronized void addSubscriptionUpdate(Map<String, List<String>> data, String user, String update) {
        if (!data.containsKey(user)) {
            data.put(user, new CopyOnWriteArrayList<>());
        }
        data.get(user).add(update);
    }

    public synchronized void setAcceleration(String user, double angle, double acceleration)
            throws BaseInvadersException {
        if (acceleration < 0 || acceleration > 1) {
            throw new BaseInvadersException("Acceleration must be between 0 and 1");
        }
        userBrakes.put(user, false);
        userAngle.put(user, angle);
        userAcceleration.put(user, acceleration);
    }

    public synchronized void teleport(String user, double x, double y) {
        Point dest = new Point(x  % Configurations.getMapWidth(), y  % Configurations.getMapHeight());

        updatePlayerPosition(players.get(user), dest);
    }

    public synchronized boolean sellMineral(String user) {

        Player player = players.get(user);
        boolean isNearStation = false;

        for(Station station : stations) {
            if(station.distanceTo(player) < Configurations.getCaptureRadius() && !player.isDisabled()) {
                isNearStation = true;
                break;
            }
        }

        if (!isNearStation) {
            return false;
        }

        // Allow sell to go through
        Long currentMinerals = userMinerals.get(user);
        userScores.put(user, currentMinerals + userScores.get(user));
        userMinerals.put(user, 0L);

        return true;
    }

    private synchronized void wipeExistingDestination(String user) {
        userBrakes.put(user, false);
        userAcceleration.remove(user);
        userArrivingDeceleration.remove(user);
        userDestinations.remove(user);
    }
    public synchronized void setDestination(String user, double x, double y) {
        Point dest = new Point(x  % Configurations.getMapWidth(), y  % Configurations.getMapHeight());

        wipeExistingDestination(user);

        Player player = players.get(user);
        double distance = player.getPosition().distanceTo(dest);
        Double slowDownDistance = distance * Configurations.getPercentOfDestinationToSlowdown();
        userDestinations.put(user, new Pair<Point, Double>(dest, slowDownDistance));
    }

    public synchronized void setBrake(String user) {
        userBrakes.put(user, true);
        userAngle.put(user, 0.0);
        userAcceleration.put(user, 0.0);
    }

    public synchronized void placeBomb(String user, double x, double y, long delay) throws BaseInvadersException {
        Point p = new Point(x, y);
        Player player = players.get(user);
        if (player.distanceTo(p) > Configurations.getBombPlacementRadius()) {
            throw new BaseInvadersException("Cannot Place Bomb that far away.");
        }
        if (userBombs.get(user).size() >= Configurations.getMaxBombs()) {
            throw new BaseInvadersException("Unable to place this many bombs at a time");
        }
        if (delay > Configurations.getMaxBombDelay() || delay < Configurations.getMinBombDelay()) {
            throw new BaseInvadersException("Bomb delay is out of allowable range");
        }

        Bomb b = new Bomb(player, p, delay);
        bombs.add(b);
        userBombs.get(user).add(b);
    }

    @Override
    public synchronized Collection<Player> getPlayers() {
        return players.values();
    }

    @Override
    public synchronized long getTicks() {
        return ticks;
    }

    @Override
    public synchronized Collection<Mine> getMines() {
        return mines;
    }
    
    @Override
    public synchronized Collection<Station> getStations() {
        return stations;
    }

    @Override
    public synchronized long getUserScore(String user) {
        if (!userScores.containsKey(user)) {
            return 0;
        }
        return userScores.get(user);
    }

    @Override
    public synchronized long getUserMinerals(String user) {
        if (!userMinerals.containsKey(user)) {
            return 0;
        }
        return userMinerals.get(user);
    }

    @Override
    public synchronized long getUserCapacity(String user) {
        if (!userCapacity.containsKey(user)) {
            return 0;
        }
        return userCapacity.get(user);
    }

    @Override
    public synchronized Map<String, Set<Mine>> getUserMines() {
        Map<String, Set<Mine>> userMines = new HashMap<>();
        mines.stream().filter((mine) -> !(mine.getOwner() == null)).map((mine) -> {
            if (!userMines.containsKey(mine.getOwner().getName())) {
                userMines.put(mine.getOwner().getName(), new HashSet<>());
            }
            return mine;
        }).forEach((mine) -> {
            userMines.get(mine.getOwner().getName()).add(mine);
        });
        return userMines;
    }

    @Override
    public synchronized Player getPlayer(String user) {
        return players.get(user);
    }

    @Override
    public synchronized long getPlayerScore(String user) {
        return userScores.get(user);
    }

    @Override
    public synchronized Set<Bomb> getBombs() {
        return bombs;
    }

    public synchronized void doScan(String user, Point p) throws BaseInvadersException {
        if (p.getX() < 0 || p.getX() > Configurations.getMapWidth() || p.getY() < 0
                || p.getY() > Configurations.getMapHeight()) {
            throw new BaseInvadersException("Sight location out of range");
        }
        if (userLastScan.containsKey(user) && userLastScan.get(user) + Configurations.getScanDelay() > ticks) {
            throw new BaseInvadersException("Scanning too soon");
        }
        userLastScan.put(user, ticks);
    }

    @Override
    public synchronized String toString() {
        return "GameMap{" + "players=" + players + ", userUpdates=" + userUpdates + ", userAcceleration="
                + userAcceleration + ", userAngle=" + userAngle + ", userBrakes=" + userBrakes + ", userDestinations=" + userDestinations + ", mines=" + mines
                + ", stations=" + stations + ", userScores=" + userScores + ", userMinerals=" + userMinerals + ", isRunning=" + isRunning + '}';
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(
                        Configurations.getTickDelay() - (System.currentTimeMillis() % Configurations.getTickDelay()));
                if (isRunning) {
                    if (Configurations.getTicksRemaining() != null && ticks < Configurations.getTicksRemaining()) {
                        ticks++;
                        try {
                            nextRound();
                        } catch (Exception ex) {
                            System.err.println(
                                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            ex.printStackTrace();
                        }
                    } else {
                        isRunning = false;
                        downtimeTicks = 0;
                    }
                } else {
                    if (Configurations.getDowntimeTicks() != null
                            && downtimeTicks < Configurations.getDowntimeTicks()) {
                        downtimeTicks++;
                    } else {
                        ticks = 0;
                        reset();
                        isRunning = true;
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public synchronized Collection<WormHole> getWormHoles() {
        return wormHoles;
    }

    class GameMapMessage implements GameMap, Serializable {

        private final Map<String, Player> players;
        private final Map<String, List<String>> userUpdates;
        private final Set<Mine> mines;
        private final Set<Station> stations;
        private final Set<WormHole> wormHoles;
        private final Map<String, Long> userScores;
        private final Set<Bomb> bombs;
        private final long ticks, downtimeTicks;
        private final boolean isRunning;

        @Override
        public Collection<Player> getPlayers() {
            return players.values();
        }

        @Override
        public long getTicks() {
            return ticks;
        }

        @Override
        public Collection<Mine> getMines() {
            return mines;
        }

        @Override
        public Collection<Station> getStations() {
            return stations;
        }

        @Override
        public long getUserScore(String user) {
            if (!userScores.containsKey(user)) {
                return 0;
            }
            return userScores.get(user);
        }

        @Override
        public long getUserMinerals(String user) {
            if (!userMinerals.containsKey(user)) {
                return 0;
            }
            return userMinerals.get(user);
        }

        @Override
        public long getUserCapacity(String user) {
            if (!userCapacity.containsKey(user)) {
                return 0;
            }
            return userCapacity.get(user);
        }

        @Override
        public Map<String, Set<Mine>> getUserMines() {
            Map<String, Set<Mine>> userMines = new HashMap<>();
            mines.stream().filter((mine) -> !(mine.getOwner() == null)).map((mine) -> {
                if (!userMines.containsKey(mine.getOwner().getName())) {
                    userMines.put(mine.getOwner().getName(), new HashSet<>());
                }
                return mine;
            }).forEach((mine) -> {
                userMines.get(mine.getOwner().getName()).add(mine);
            });
            return userMines;
        }

        @Override
        public Player getPlayer(String user) {
            return players.get(user);
        }

        @Override
        public long getPlayerScore(String user) {
            return userScores.get(user);
        }

        @Override
        public Set<Bomb> getBombs() {
            return bombs;
        }

        public GameMapMessage(Map<String, Player> players, Map<String, List<String>> userUpdates, Set<Mine> mines, Set<Station> stations,
                Map<String, Long> userScores, Set<Bomb> bombs, Set<WormHole> wormHoles, long ticks, long downtimeTicks,
                boolean isRunning) {
            this.players = new HashMap<>();
            players.entrySet().stream().forEach((player) -> {
                this.players.put(player.getKey(), new Player(player.getValue()));
            });
            this.userUpdates = new HashMap<>(userUpdates);
            this.mines = new HashSet<>();
            mines.stream().forEach((mine) -> {
                this.mines.add(new Mine(mine));
            });
            this.stations = new HashSet<>();
            stations.stream().forEach((station) -> {
                this.stations.add(new Station(station));
            });
            this.bombs = new HashSet<>(bombs);
            this.wormHoles = new HashSet<>(wormHoles);
            this.userScores = new HashMap<>(userScores);
            this.ticks = ticks;
            this.downtimeTicks = downtimeTicks;
            this.isRunning = isRunning;

        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }

        @Override
        public Collection<WormHole> getWormHoles() {
            return wormHoles;
        }

    }

    private GameMap lastMap = null;
    private long lastMapTick = -1;

    public synchronized GameMap makeCopy() {
        if (lastMapTick == ticks && lastMap != null) {
            return lastMap;
        }
        lastMap = new GameMapMessage(players, userUpdates, mines, stations, userScores, bombs, wormHoles, ticks, downtimeTicks,
                isRunning);
        lastMapTick = ticks;
        return lastMap;
    }
}
