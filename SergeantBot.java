/**
 * A: Gaige Johnston 000869398
 * This is my decorated sergeant bot that has been through thousands of nautical combats
 * This bot is used for the game of battleship, it initializes a new game,
 *  shoots at the enemy, and reports the author(s)
 * Some code from ExampleBot.java by Mark Yendt
 */

import battleship.BattleShip2;
import battleship.BattleShipBot;
import java.util.ArrayList;
import java.awt.Point;
import java.util.Comparator;


/**
 * This handles finding the most likely space for a ship to occupy
 * and then fires at it. If hit, it continues hunting that
 * ship until it is sunk
 */
public class SergeantBot implements BattleShipBot {
    private int gameSize;
    private BattleShip2 battleShip;
    // these 2 are from Marks starter code

    private int[][] comboMap;
    // shot map of the board

    private ArrayList<Map> allMaps;
    // list of ship heat maps

    private int sunkShipCount;
    // number of ships sunk so far this game

    private int[] firstHitLocation;
    // tracks where first contact of current ship was, for turning around shot pattern during hunting

    private int[][] hitSpots;
    // map of known ship locations - 1 is hit, 0 not hit

    private int recursiveCounter;
    // tracks number of recursion loops, used to break out of recursion if
    //   more loops done than the largest remaining ship size

    private static int hitsToSinkThisShip;
    // used to find the size of the ship that has been sunk

    private ArrayList<Integer> remainingShips;
    // tracks ships to be sunk with largest at the front
    //   used in combo with recursiveCounter to break out of infinite recursion issue
    //   I get with some games


    /**
     * create the battleship firing bot
     * @param battleShip2 the battleship bot
     */
    @Override
    public void initialize(BattleShip2 battleShip2) {
        battleShip = battleShip2;
        gameSize = battleShip2.BOARD_SIZE;
        sunkShipCount = 0;
        hitSpots = new int[gameSize][gameSize];
        remainingShips = new ArrayList<>();

        // make list of maps, each of which are actually 2 layer maps
        //   for horizontal and vertical ship alignments
        int[] shipSizes = battleShip2.getShipSizes();
        allMaps = new ArrayList<>();
        for (int c = 0; c < battleShip2.getShipSizes().length; c++) {
            allMaps.add(new Map(gameSize, shipSizes[c]));
            remainingShips.add(shipSizes[c]);
        }
        // put largest remaining ship at front
        remainingShips.sort(Comparator.reverseOrder());

        // create the heat map
        updateBigMap();
    }


    /**
     * Makes the probability map by combining all
     * layers of each ship's heat map
     */
    private void updateBigMap() {
        comboMap = new int[gameSize][gameSize];
        // make / update the huge combo of all maps
        for (Map map : allMaps) {
            int[][] item = map.getShotMap();
            for (int r = 0; r < item.length; r++) {
                for (int c = 0; c < item[r].length; c++) {
                    comboMap[r][c] += item[r][c];
                }
            }
        }
    }


    /**
     * Updates the map layers by shooting at the given point
     * @param x column number to shoot
     * @param y row number to shoot
     */
    private void updateMaps(int x, int y) {
        // shoot at that point on every map layer to adjust probabilities
        for(Map map : allMaps) {
            map.shootAtPoint(x, y);
        }
        updateBigMap();
    }


    /**
     * Fire a shot at a specific coordinate
     * If hit, start hunting that ship
     */
    @Override
    public void fireShot() {
        // get coords
        int[] coords = findPoint();
        int x = coords[0];
        int y = coords[1];

        // shoot at point in game and check for hit
        boolean isHit = battleShip.shoot(new Point(x, y));
        updateMaps(x, y);
        if (isHit && !battleShip.allSunk()) {
            recursiveCounter = 0;
            hitsToSinkThisShip = 1;
            hitSpots[y][x] = 1;

            shipFound(x, y, true);

            // remove sunk ship from list of ships to sink
            if (!remainingShips.isEmpty()) {
                if (remainingShips.contains(hitsToSinkThisShip)) {
                    remainingShips.remove(Integer.valueOf(hitsToSinkThisShip));
                }
            }
        }
    }


    /**
     * Finds the most likely position to contain a ship
     * @return coordinates of first most likely space to have a ship
     */
    private int[] findPoint() {
        int x = 0;
        int y = 0;
        int highest = 0;
        for (int r = 0; r < comboMap.length; r++)
            for (int c = 0; c < comboMap[r].length; c++) {
                if (highest < comboMap[r][c]) {
                    highest = comboMap[r][c];
                    x = c;
                    y = r;
                }
            }
        return new int[] {x, y};
    }


    /**
     * If a shot is successful, start hunting to sink ship
     */
    private void shipFound(int x, int y, boolean isFirstHit) {
        // for turning around if end of ship reached but not yet sunk
        if (isFirstHit)
            firstHitLocation = new int[] {x, y};

        // check if sunk another ship
        int shipsSunk = battleShip.numberOfShipsSunk();
        if (sunkShipCount < shipsSunk) {
            sunkShipCount = shipsSunk;
            return;
        }

        // issue in some games I can't figure out how to fix, brute force out of recursion
        // to keep hunting for the ship
        recursiveCounter++;
        if (recursiveCounter > remainingShips.get(0)) {
            fallbackSearch();
            return;
        }

        // check N E S W that aren't out of bounds and have > 0 chance of having a ship
        // make sure can go north
        if (y > 0) {
            // check up hasn't been hit yet
            if (comboMap[(y-1)][x] > 0) {
                boolean isHit = battleShip.shoot(new Point(x, y-1));
                updateMaps(x, y-1);
                if (isHit) {
                    hitsToSinkThisShip++;
                    hitSpots[y-1][x] = 1;
                    // vertical ship, nothing to east and west
                    // update east
                    if (x < gameSize-1) {
                        updateMaps(x+1, y);
                        updateMaps(x+1, y-1);
                    }
                    // update west
                    if (x > 0) {
                        updateMaps(x-1, y);
                        updateMaps(x-1, y-1);
                    }
                    // keep hunting
                    shipFound(x, y-1, false);
                    return;
                }
                else
                    hitSpots[y-1][x] = -1;
            }
        }
        // make sure can go east
        if (x < gameSize-1) {
            // check east hasn't been hit yet
            if (comboMap[y][(x+1)] > 0) {
                boolean isHit = battleShip.shoot(new Point(x+1, y));
                updateMaps(x+1,y);
                if (isHit) {
                    hitsToSinkThisShip++;
                    hitSpots[y][x+1] = 1;
                    // horizontal ship, nothing north or south
                    // update north
                    if (y > 0) {
                        updateMaps(x,y-1);
                        updateMaps(x+1,y-1);
                    }
                    // update south
                    if (y < gameSize-1){
                        updateMaps(x, y+1);
                        updateMaps(x+1, y+1);
                    }
                    shipFound(x+1, y, false);
                    return;
                }
                else
                    hitSpots[y][x+1] = -1;
            }
        }
        // make sure can go south
        if (y < gameSize-1) {
            // check south not hit yet
            if (comboMap[(y+1)][x] > 0) {
                boolean isHit = battleShip.shoot(new Point(x, y+1));
                updateMaps(x,y+1);
                if (isHit) {
                    hitsToSinkThisShip++;
                    hitSpots[y+1][x] = 1;
                    // update east
                    if (x < gameSize - 1) {
                        updateMaps(x+1, y);
                        updateMaps(x+1, y+1);
                    }
                    // update west
                    if (x > 0) {
                        updateMaps(x-1, y);
                        updateMaps(x-1, y+1);
                    }
                    shipFound(x, y+1, false);
                    return;
                }
                else
                    hitSpots[y+1][x] = -1;
            }
        }
        // make sure can go west
        if (x > 0) {
            // check west not hit yet
            if (comboMap[y][(x-1)] > 0) {
                boolean isHit = battleShip.shoot(new Point(x-1, y));
                updateMaps(x-1,y);
                if (isHit) {
                    hitsToSinkThisShip++;
                    hitSpots[y][x-1] = 1;
                    // update north
                    if (y > 0) {
                        updateMaps(x, y-1);
                        updateMaps(x-1, y-1);
                    }
                    // update south
                    if (y < gameSize-1) {
                        updateMaps(x, y+1);
                        updateMaps(x-1, y+1);
                    }
                    shipFound(x-1, y, false);
                    return;
                }
                else
                    hitSpots[y][x-1] = -1;
            }
        }
        // ship not sunk and no more shots found in direction, go back to first hit location and rescan
        shipFound(firstHitLocation[0], firstHitLocation[1], false);
    }


    /**
     * Some games get stuck in recursion and I'm not sure why
     * This gets out of it and fires around known ships to try to finish sinking one
     */
    private void fallbackSearch() {
        int shipsSunk = battleShip.numberOfShipsSunk();

        // find a known ship, and fire around it if recursive hunting messed up
        for (int row = 0; row < hitSpots.length; row++)
            for (int col = 0; col < hitSpots[row].length; col++) {
                if (hitSpots[row][col] == 1) {
                    // fire north
                    if (row > 0) {
                        // tried checking orientation before firing for north and
                        //   east but that resulted in worse score
                        if (hitSpots[row-1][col] == 0) {
                            boolean hit = battleShip.shoot(new Point(col, row - 1));
                            if (!hit) {
                                hitSpots[row-1][col] = -1;
                            }
                            else {
                                updateMaps(col, row-1);
                                if (sunkShipCount < shipsSunk) {
                                    sunkShipCount = shipsSunk;
                                    return;
                                }
                            }
                        }
                    }
                    // fire east
                    if (col < gameSize-1) {
                        if (hitSpots[row][col+1] == 0) {
                            boolean hit = battleShip.shoot(new Point(col + 1, row));
                            if (!hit) {
                                hitSpots[row][col+1] = -1;
                            }
                            else {
                                updateMaps(col+1, row);
                                if (sunkShipCount < shipsSunk) {
                                    sunkShipCount = shipsSunk;
                                    return;
                                }
                            }
                        }
                    }
                    // fire south
                    if (row < gameSize-1) {
                        if (hitSpots[row+1][col] == 0) {
                            boolean hit = battleShip.shoot(new Point(col, row + 1));
                            if (!hit)
                                hitSpots[row+1][col] = -1;
                            else {
                                updateMaps(col, row+1);
                                if (sunkShipCount < shipsSunk) {
                                    sunkShipCount = shipsSunk;
                                    return;
                                }
                            }
                        }
                    }
                    // fire west
                    if (col > 0) {
                        if (hitSpots[row][col-1] == 0) {
                            boolean hit = battleShip.shoot(new Point(col - 1, row));
                            if (!hit)
                                hitSpots[row][col-1] = -1;
                            else {
                                updateMaps(col-1, row);
                                if (sunkShipCount < shipsSunk) {
                                    sunkShipCount = shipsSunk;
                                    return;
                                }
                            }
                        }
                    }
                }
            }

        // still not all sunk
        if (battleShip.numberOfShipsSunk() != allMaps.size())
            fireEverywhereAtEverything();
    }


    /**
     * If the fallback didn't sink a ship, this brute force fires
     *   at any location there isn't a known ship in sequence
     */
    private void fireEverywhereAtEverything() {
        int shipsSunk = battleShip.numberOfShipsSunk();

        for (int row = 0; row < hitSpots.length; row++)
            for (int col = 0; col < hitSpots[row].length; col++)
                if (hitSpots[row][col] == 0) {
                    battleShip.shoot(new Point(col, row));
                    if (sunkShipCount < shipsSunk) {
                        sunkShipCount = shipsSunk;
                        return;
                    }
                }
    }


    /**
     * shows who made this
     * @return string stating author name, student number, and month + year
     */
    @Override
    public String getAuthors() {
        return "Gaige Johnston, 000869398, April 2023";
    }
}
