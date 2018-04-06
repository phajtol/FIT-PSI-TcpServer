public enum Orientation {
    UNDEFINED,
    UP,
    DOWN,
    LEFT,
    RIGHT;

    static Orientation left(Orientation o){
        switch(o){
            case UP: return LEFT;
            case DOWN: return RIGHT;
            case LEFT: return DOWN;
            case RIGHT: return UP;
        }
        return UNDEFINED;
    }

    static Orientation right(Orientation o){
        switch(o){
            case UP: return RIGHT;
            case DOWN: return LEFT;
            case LEFT: return UP;
            case RIGHT: return DOWN;
        }
        return UNDEFINED;
    }
}
