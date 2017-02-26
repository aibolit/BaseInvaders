/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseinvaders;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 *
 * @author Sasa
 */
public class Configurations {

    private static int port = 17429;
    private static int uiPort = 17428;
    private static int biUiPort = 14739;
    private static String host = "127.0.0.1";
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static long timeout = 20;
    private static long uiTimeout = 20;
    private static int maxConnectionsPerUser = 3;
    private static Long ticksRemaining = 48000L;
    private static Long downtimeTicks = 1200L;
    private static long tickDelay = 25;

    private static double friction = .99;
    private static double brakeFriction = .987;
    private static double speed = .1;
    private static double captureRadius = 5;
    private static double visionRadius = 150;
    private static int maxBombs = 1;
    private static double bombPlacementRadius = 50;
    private static double bombExplosionRadius = 15;
    private static long bombDelay = 20;
    private static long minBombDelay = 20, maxBombDelay = 200;
    private static double bombPower = 15;
    private static double scanRadius = 50;
    private static long scanDelay = 200;

    private static int mineCount = 30;
    private static int mapWidth = 10000, mapHeight = 10000;
    private static boolean useLocalUi = true;

    private static final List<Image> shipImages = new ArrayList<>();

    private static InputStream getResource(String name) {
        return Configurations.class.getClassLoader().getResourceAsStream(name);
    }

    private static void init() {
        try {
            BufferedImage spaceships = ImageIO.read(getResource("spaceships.png"));
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 3; j++) {
                    BufferedImage subimage = spaceships.getSubimage(48 * j, i * 24, 24, 24);
                    for (int k = 0; k < 24; k++) {
                        for (int l = 0; l < 24; l++) {
                            if (subimage.getRGB(k, l) == -1 || subimage.getRGB(k, l) == -921103) {
                                subimage.setRGB(k, l, 0x00000000);
                            }
                        }
                    }
                    shipImages.add(subimage);
                }
            }
            Collections.shuffle(shipImages, new Random(14729));
        } catch (IOException ex) {
        }
        //INIT PARAMS HERE
    }

    public static Image getPlayerImage(int id) {
        return shipImages.get(id % shipImages.size());
    }

    public static int getPort() {
        return port;
    }

    public static String getHost() {
        return host;
    }

    public static int getUiPort() {
        return uiPort;
    }

    public static int getBiUiPort() {
        return biUiPort;
    }

    public static Set<String> getUsers() {
        return users.keySet();
    }

    public static String getUserPassword(String user) {
        if (!users.containsKey(user)) {
            throw new IllegalArgumentException("No Such User");
        }
        return users.get(user);
    }

    public static long getTimeout() {
        return timeout;
    }

    public static long getUITimeout() {
        return uiTimeout;
    }

    public static int getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    public static Long getTicksRemaining() {
        return ticksRemaining;
    }

    public static double getFriction() {
        return friction;
    }

    public static double getSpeed() {
        return speed;
    }

    public static double getCaptureRadius() {
        return captureRadius;
    }

    public static int getMapWidth() {
        return mapWidth;
    }

    public static int getMapHeight() {
        return mapHeight;
    }

    public static double getBrakeFriction() {
        return brakeFriction;
    }

    public static int getMineCount() {
        return mineCount;
    }

    public static double getVisionRadius() {
        return visionRadius;
    }

    public static int getMaxBombs() {
        return maxBombs;
    }

    public static long getBombDelay() {
        return bombDelay;
    }

    public static long getMinBombDelay() {
        return minBombDelay;
    }

    public static long getMaxBombDelay() {
        return maxBombDelay;
    }

    public static double getBombPower() {
        return bombPower;
    }

    public static double getBombPlacementRadius() {
        return bombPlacementRadius;
    }

    public static double getBombExplosionRadius() {
        return bombExplosionRadius;
    }

    public static double getScanRadius() {
        return scanRadius;
    }

    public static long getScanDelay() {
        return scanDelay;
    }

    public static Long getDowntimeTicks() {
        return downtimeTicks;
    }

    public static long getTickDelay() {
        return tickDelay;
    }

    public static String getConfigData() {
        return mapWidth + " " + mapHeight + " " + mineCount + " " + captureRadius + " " + speed + " " + friction + " " + brakeFriction + " " + scanRadius + " " + scanDelay + " " + bombPlacementRadius + " " + bombExplosionRadius + " " + bombPower + " " + bombDelay;
    }

    public static boolean getUseLocalUI() {
        return useLocalUi;
    }

    public static void readCongfigs(String file) throws IOException {
        init();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(file)))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (!st.hasMoreTokens()) {
                    continue;
                }
                switch (st.nextToken()) {
                    case "port":
                        port = Integer.parseInt(st.nextToken());
                        break;
                    case "uiport":
                        uiPort = Integer.parseInt(st.nextToken());
                        break;
                    case "biuiport":
                        biUiPort = Integer.parseInt(st.nextToken());
                        break;
                    case "host":
                        host = st.nextToken();
                        break;
                    case "users":
                        while (st.hasMoreTokens()) {
                            users.put(st.nextToken(), st.nextToken());
                        }
                        break;
                    case "timeout":
                        timeout = Long.parseLong(st.nextToken());
                        break;
                    case "uitimeout":
                        uiTimeout = Long.parseLong(st.nextToken());
                        break;
                    case "ticks-remaining":
                        ticksRemaining = Long.parseLong(st.nextToken());
                        break;
                    case "downtime-ticks":
                        downtimeTicks = Long.parseLong(st.nextToken());
                        break;
                    case "tick-delay":
                        tickDelay = Long.parseLong(st.nextToken());
                        break;
                    case "max-connections":
                        maxConnectionsPerUser = Integer.parseInt(st.nextToken());
                        break;
                    case "map":
                        mapWidth = Integer.parseInt(st.nextToken());
                        mapHeight = Integer.parseInt(st.nextToken());
                        break;
                    case "friction":
                        friction = Double.parseDouble(st.nextToken());
                        break;
                    case "brake":
                        brakeFriction = Double.parseDouble(st.nextToken());
                        break;
                    case "speed":
                        speed = Double.parseDouble(st.nextToken());
                        break;
                    case "capture-radius":
                        captureRadius = Double.parseDouble(st.nextToken());
                        break;
                    case "mines":
                        mineCount = Integer.parseInt(st.nextToken());
                        break;
                    case "vision-radius":
                        visionRadius = Double.parseDouble(st.nextToken());
                        break;
                    case "max-bombs":
                        maxBombs = Integer.parseInt(st.nextToken());
                        break;
                    case "bomb-palacement-radius":
                        bombPlacementRadius = Double.parseDouble(st.nextToken());
                        break;
                    case "bomb-explosion-radius":
                        bombExplosionRadius = Double.parseDouble(st.nextToken());
                        break;
                    case "bomb-delay":
                        bombDelay = Long.parseLong(st.nextToken());
                        break;
                    case "bomb-power":
                        bombPower = Double.parseDouble(st.nextToken());
                        break;
                    case "scan-radius":
                        scanRadius = Double.parseDouble(st.nextToken());
                        break;
                    case "scan-delay":
                        scanDelay = Long.parseLong(st.nextToken());
                        break;
                    case "use-local-ui":
                        switch (st.nextToken()) {
                            case "true":
                                useLocalUi = true;
                                break;
                            case "false":
                                useLocalUi = false;
                                break;
                            default:
                                System.out.println("use-local-ui must be {true, false}, defaulting true");
                                break;
                        }
                        break;
                    default:
                        if (line.charAt(0) != '#') {
                            System.out.println("Oops no such setting " + line);
                        }
                        break;
                }
            }
        }
    }

    public static void saveConfigurations(String file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new File(file))) {
            pw.print(getConfigString());
        }
    }

    public static String getConfigString() {
        StringBuilder out = new StringBuilder();
        out.append("port ").append(port).append("\nhost ").append(host).append("\n");
        out.append("users ");
        users.entrySet().forEach((user) -> {
            out.append(user.getKey()).append(" ").append(user.getValue());
        });
        out.append("\n");
        out.append("timeout ").append(timeout).append("\n");
        out.append("ticks-remaining ").append(ticksRemaining).append("\n");
        return out.toString();
    }

    public static String getPlayerConfigString() {
        StringBuilder out = new StringBuilder();
        out.append("MAPWIDTH ").append(mapWidth);
        out.append(" MAPHEIGHT ").append(mapHeight);
        out.append(" CAPTURERADIUS ").append(captureRadius);
        out.append(" VISIONRADIUS ").append(visionRadius);
        out.append(" FRICTION ").append(friction);
        out.append(" BRAKEFRICTION ").append(brakeFriction);
        out.append(" BOMBPLACERADIUS ").append(bombPlacementRadius);
        out.append(" BOMBEFFECTRADIUS ").append(bombExplosionRadius);
        out.append(" BOMBDELAY ").append(bombDelay);
        out.append(" BOMBPOWER ").append(bombPower);
        out.append(" SCANRADIUS ").append(scanRadius);
        out.append(" SCANDELAY ").append(scanDelay);
        return out.toString();
    }

    private Configurations() {

    }
}
