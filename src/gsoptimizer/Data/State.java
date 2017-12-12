package gsoptimizer.Data;

public class State {

    private int id;
    private String name;
    private String label;
    private float x;
    private float y;
    private boolean initialState;
    private boolean finalState;

    public static final String END_STATE = "END_STATE";

    public State() {
        name = "";
        label = "";
        initialState = false;
        finalState = false;
    }

    public State(State state) {
        this.id = state.getId();
        this.name = state.getName();
        this.label = state.getLabel();
        this.x = state.getY();
        this.y = state.getX();
        this.initialState = state.isInitialState();
        this.finalState = state.isFinalState();
    }

    public State(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public State setId(int id) {
        this.id = id;
        return this;
    }

    public State setName(String name) {
        this.name = name;
        return this;
    }

    public State setLabel(String label) {
        this.label = label;
        return this;
    }

    public State setLocation(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public State setInitialState(boolean initialState) {
        this.initialState = initialState;
        return this;
    }

    public State setFinalState(boolean finalState) {
        this.finalState = finalState;
        return this;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isInitialState() {
        return initialState;
    }

    public boolean isFinalState() {
        return finalState;
    }

    @Override
    public State clone() {
        return new State(this);
    }

    @Override
    public String toString() {
        String s = name + ":" + String.valueOf(id);
        if (label != null && !label.equals("")) {
            s += "(" + label + ")";
        }
        s += "@(" + x + "," + y + ")";
        if (initialState) {
            s += "(INITIAL)";
        }
        if (finalState) {
            s += "(FINAL)";
        }
        return s;
    }

}
