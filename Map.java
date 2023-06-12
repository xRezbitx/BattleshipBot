/**
 * A: Gaige Johnston 000869398
 * This creates a map of the probabilities of the ship of given size to be in each coordinate
 */

/**
 * Heat map of a battleship ship that is actually 2 map layers
 *   where one layer is odds of horizontal ship, and other
 *   layer is odds of vertical ship
 */
public class Map {
    private int gameSize; // size of game board
    private int shipSize; // number of units the ship occupies
    private int[][] shotMap; // combo of vertical and horizontal heat maps
    private int[][] maph; // horizontally aligned ship heat map
    private int[][] mapv; // vertically aligned ship heat map
    private int ratioTop = 101; // best found 101 - used for probability mapping
    private int ratioBottom = 50; // best found 50 - used for parity adjusting probability


    /**
     * Maps probability of ship of given size to be in each coordinate
     * @param gameSize size of battleship game board
     * @param shipSize number of squares ship occupies
     */
    public Map(int gameSize, int shipSize) {
        this.gameSize = gameSize;
        this.shipSize = shipSize;
        shotMap = new int[gameSize][gameSize];
        maph = new int[gameSize][gameSize];
        mapv = new int[gameSize][gameSize];
        makeShotMap();
    }


    /**
     * This fills in the heat map for where the ship is likely to be
     */
    private void makeShotMap() {
        // horizontal layout
        makeHorizontalMap();
        // vertical layout
        makeVerticalMap();
        // the h + v heat maps combined
        updateShotMap();
    }


    /**
     * For probability - Marks all locations the ship could be horizontally
     */
    private void makeHorizontalMap() {
        // row based
        for (int r = 0; r < gameSize; r++) {
            int startIndex = 0;
            // walk right until ship size away from right wall
            while (startIndex < gameSize - shipSize+1) {
                // number of times to loop this part of the row
                int shipSizeLoop = shipSize + startIndex;
                for (int i = startIndex; i < shipSizeLoop; i++) {
                    maph[r][i]+=ratioTop;
                }
                startIndex++;
            }
        }
    }


    /**
     * For probability - Marks all locations the ship could be vertically
     */
    private void makeVerticalMap() {
        // column based
        for (int c = 0; c < gameSize; c++) {
            int startIndex = 0;
            while (startIndex < gameSize - shipSize+1) {
                int shipSizeLoop = shipSize + startIndex;
                for (int i = startIndex; i < shipSizeLoop; i++) {
                    mapv[i][c]+=ratioTop;
                }
                startIndex++;
            }
        }
    }


    /**
     * This shows the vertical and horizontal maps together for the overall probability
     * @return combo heat map of vertical and horizontal alignments of the ship
     */
    public int[][] getShotMap() {
        updateShotMap();
        return shotMap;
    }


    /**
     * Combines horizontal and vertical probabilities together
     * for total chance a ship is in a certain square
     */
    private void updateShotMap() {
        for (int r = 0; r < mapv.length; r++) {
            for (int c = 0; c < mapv[r].length; c++) {
                shotMap[r][c] = mapv[r][c] + maph[r][c];
            }
        }

    }


    /**
     * Hit or Miss the square is shot, sets probability to 0 to avoid revisiting square again later
     * @param x column
     * @param y row
     */
    public void shootAtPoint(int x, int y) {
        // horizontal update and check wall distance
        maph[y][x] = 0;
        boolean isCloseToSideWall = false;
        // close enough to left wall to rule out spaces up to left wall
        //  NOTE unsure why but setting to 0 causes issues, reducing odds by 1 gave best results
        if (x < shipSize) {
            isCloseToSideWall = true;
            for (int c = x; c >= 0; c--) {
                maph[y][c] -= 1;
            }
        }
        // close enough to right wall to rule out spaces between
        if ((x + shipSize) >= gameSize) {
            isCloseToSideWall = true;
            for (int c = x; c < gameSize; c++) {
                maph[y][c] -= 1;
            }
        }
        // close enough to already guessed space on left to rule out between
        LinkedQueue<Integer> xLeftQueue = new LinkedQueue<>();
        if (!isCloseToSideWall) {
            int counter = 0;
            int xcheck = x;
            while (counter < shipSize) {
                xcheck--;
                xLeftQueue.enqueue(xcheck);
                if (maph[y][xcheck] == 0) {
                    while (!xLeftQueue.isEmpty()) {
                        maph[y][xLeftQueue.dequeue()] = 0;
                    }
                    break;
                }
                counter++;
            }
        }
        // close enough to miss / hit on right side
        LinkedQueue<Integer> xRightQueue = new LinkedQueue<>();
        if (!isCloseToSideWall) {
            int counter = 0;
            int xcheck = x;
            while (counter < shipSize) {
                xcheck++;
                xRightQueue.enqueue(xcheck);
                if (maph[y][xcheck] == 0) {
                    while (!xRightQueue.isEmpty()) {
                        maph[y][xRightQueue.dequeue()] = 0;
                    }
                    break;
                }
                counter++;
            }
        }

        // vertical update and check wall distance
        mapv[y][x] = 0;
        boolean isCloseToTopBottom = false;
        // close enough to top wall to rule out spaces between
        if (y < shipSize) {
            isCloseToTopBottom = true;
            for (int c = y; c >= 0; c--) {
                mapv[c][x] -= 1;
            }
        }
        // close enough to bottom wall to rule out spaces between
        if ((y + shipSize) >= gameSize) {
            isCloseToTopBottom = true;
            for (int c = y; c < gameSize; c++) {
                mapv[c][x] -= 1;
            }
        }
        // close to miss / hit at top
        if (!isCloseToTopBottom) {
            LinkedQueue<Integer> yTopQueue = new LinkedQueue<>();
            if (!isCloseToSideWall) {
                int counter = 0;
                int ycheck = y;
                while (counter < shipSize) {
                    ycheck--;
                    yTopQueue.enqueue(ycheck);
                    if (mapv[ycheck][x] == 0) {
                        while (!yTopQueue.isEmpty()) {
                            mapv[yTopQueue.dequeue()][x] = 0;
                        }
                        break;
                    }
                    counter++;
                }
            }
        }
        // close to miss / hit at bottom
        if (!isCloseToTopBottom) {
            LinkedQueue<Integer> yBottomQueue = new LinkedQueue<>();
            if (!isCloseToSideWall) {
                int counter = 0;
                int ycheck = y;
                while (counter < shipSize) {
                    ycheck++;
                    yBottomQueue.enqueue(ycheck);
                    if (mapv[ycheck][x] == 0) {
                        while (!yBottomQueue.isEmpty()) {
                            mapv[yBottomQueue.dequeue()][x] = 0;
                        }
                        break;
                    }
                    counter++;
                }
            }
        }


        // N E S W less likely following parity logic
        // along top
        if (y == 0) {
            // left top corner
            if (x == 0) {
                // right of top left
                moveRight(x, y);
                // down from top left
                moveDown(x, y);
            }
            // right top corner
            else if (x == gameSize-1) {
                // left of top right
                moveLeft(x, y);
                // down from top right
                moveDown(x, y);
            }
            // not corner
            else {
                // right
                moveRight(x, y);
                // down
                moveDown(x, y);
                // left
                moveLeft(x, y);
            }
        }
        // along bottom
        else if (y == gameSize-1) {
            // left bottom
            if (x == 0) {
                // right of bottom left
                moveRight(x, y);
                // up from bottom left
                moveUp(x, y);
            }
            // right bottom
            else if (x == gameSize - 1) {
                // up
                moveUp(x, y);
                // left
                moveLeft(x, y);
            }
            // bottom not corner
            else {
                // right
                moveRight(x, y);
                // up
                moveUp(x, y);
                // left
                moveLeft(x, y);
            }
        }
        // along left wall
        else if (x == 0) {
            // already checked corners
            // up
            moveUp(x, y);
            // right
            moveRight(x, y);
            // down
            moveDown(x, y);
        }
        // along right wall
        else if (x == gameSize-1) {
            // up
            moveUp(x, y);
            // left
            moveLeft(x, y);
            // down
            moveDown(x, y);
        }
        // not along wall
        else {
            // up
            moveUp(x, y);
            // right
            moveRight(x, y);
            // down
            moveDown(x, y);
            // left
            moveLeft(x, y);
        }

        updateShotMap();
    }


    /**
     * Lowers odds of ship being above the hit location
     * @param x column
     * @param y row
     */
    private void moveUp(int x, int y) {
        if (maph[y-1][x] >= ratioBottom)
            maph[y-1][x]-=ratioBottom;
        else
            maph[y-1][x] = 0;
        if (mapv[y-1][x] >= ratioBottom)
            mapv[y-1][x]-=ratioBottom;
        else
            mapv[y-1][x] = 0;
    }


    /**
     * Lowers odds of ship being east of the hit location
     * @param x column
     * @param y row
     */
    private void moveRight(int x, int y) {
        if (maph[y][x+1] >= ratioBottom)
            maph[y][x+1]-=ratioBottom;
        else
            maph[y][x+1] = 0;
        if (mapv[y][x+1] >= ratioBottom)
            mapv[y][x+1]-=ratioBottom;
        else
            mapv[y][x+1] = 0;
    }


    /**
     * Lowers odds of ship being below the hit location
     * @param x column
     * @param y row
     */
    private void moveDown(int x, int y) {
        if (maph[y+1][x] >= ratioBottom)
            maph[y+1][x]-=ratioBottom;
        else
            maph[y+1][x] = 0;
        if (mapv[y+1][x] >= ratioBottom)
            mapv[y+1][x]-=ratioBottom;
        else
            mapv[y+1][x] = 0;
    }


    /**
     * Lowers odds of ship being west of the hit location
     * @param x column
     * @param y row
     */
    private void moveLeft(int x, int y) {
        if (maph[y][x-1] >= ratioBottom)
            maph[y][x-1]-=ratioBottom;
        else
            maph[y][x-1] = 0;
        if (mapv[y][x-1] >= ratioBottom)
            mapv[y][x-1]-=ratioBottom;
        else
            mapv[y][x-1] = 0;
    }


    /**
     * Shows the total probability heat map of the ship
     * @return string representation of the odds of ship being in
     *          any given square
     */
    @Override
    public String toString() {
        updateShotMap();
        StringBuilder heatMap = new StringBuilder("[");
        for (int[] row : shotMap) {
            heatMap.append("[");
            for (int item : row)
                heatMap.append(String.format("%d, ", item));
            heatMap.append("]\n");
        }
        heatMap.append("]");
        return heatMap.toString();
    }
}
