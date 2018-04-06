public class Position {
    public IntPair pos;
    public Orientation orientation;

    Position(int x_, int y_, Orientation orientation_){
        this(new IntPair(x_, y_), orientation_);
    }

    Position(IntPair pos_, Orientation orientation_){
        pos = pos_;
        orientation = orientation_;
    }

}
