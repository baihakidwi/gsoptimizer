package gsoptimizer.Data;

public class Transition {

    private int fromState;
    private int toState;
    private String message;

    public Transition() {
        fromState = -1;
        toState = -1;
        message = "";
    }

    public Transition(Transition transition) {
        this.fromState = transition.getFromState();
        this.toState = transition.getToState();
        this.message = transition.getMessage();
    }

    public Transition(int fromState, int toState, String message) {
        this.fromState = fromState;
        this.toState = toState;
        this.message = message;
    }

    public Transition setFromState(int fromState) {
        this.fromState = fromState;
        return this;
    }

    public Transition setToState(int toState) {
        this.toState = toState;
        return this;
    }

    public Transition setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getFromState() {
        return fromState;
    }

    public int getToState() {
        return toState;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public Transition clone() {
        return new Transition(this);
    }

    @Override
    public String toString() {
        String s = String.valueOf(fromState) + "," + String.valueOf(toState) + ":" + message;
        return s;
    }

}
