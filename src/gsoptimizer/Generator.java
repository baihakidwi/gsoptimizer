package gsoptimizer;

import gsoptimizer.Data.GSData;
import gsoptimizer.Data.State;
import gsoptimizer.Data.Transition;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class Generator {

    private final ArrayList<String> normalMessageList;
    private final ArrayList<String> lookaheadMessageList;
    private final HashSet<String> usedLookaheadMessage;
    private final HashSet<String> usedNormalMessage;
    private final String ALFABET = "abcdefghijklmnopqrstuvwxys0123456789";
    private int separatorSize;
    private GSData gsdata;
    private ArrayList<ArrayList> transitionList;
    int lowerBound;
    int upperBound;

    public Generator() {
        normalMessageList = new ArrayList<>();
        lookaheadMessageList = new ArrayList<>();
        usedLookaheadMessage = new HashSet<>();
        usedNormalMessage = new HashSet<>();
    }

    private int calculateSeparatorLength(int numberOfNormalTransition) {
        int separator = 5;
        int ct;
        int messageLength = 3;
        do {
            separator++;
            ct = 1;
            for (int i = 0; i < messageLength; i++) {
                ct = ct * separator;
            }
        } while (ct < numberOfNormalTransition * 5);

        return separator;
    }

    private String generateLookaheadMessage(String message, int length, int lowBound, int upBound) {
        if (message.length() == length) {
            if (lookaheadMessageList.contains(message)) {
                return null;
            }
            return message;
        }
        for (int tries = 0; tries < 5; tries++) {
            String s = generateLookaheadMessage(
                    message + ALFABET.charAt(Main.random(lowBound, upBound)),
                    length, lowBound, upBound);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    private String generateMessage(String message, int length, int lowBound, int upBound) {
        if (message.length() == length) {
            return message;
        }
        return generateMessage(message + ALFABET.charAt(Main.random(lowBound, upBound)),
                length, lowBound, upBound);
    }

    private void setGSData(GSData gsdata, int lowerBound, int upperBound) {
        this.gsdata = gsdata;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (transitionList == null) {
            transitionList = new ArrayList<>();
        } else {
            transitionList.clear();
        }
        for (int stateId = 0; stateId < gsdata.getStates().size(); stateId++) {
            ArrayList<Transition> transitions = new ArrayList<>();
            transitionList.add(transitions);
        }
        for (Transition transition : gsdata.getTransitions()) {
            transitionList.get(transition.getFromState()).add(transition);
        }
    }

    private String traverse(int stateId, String message) {
        if (stateId == -1 && message.length() > lowerBound && message.length() < upperBound) {
            return message;
        }
        if (stateId == -1 || message.length() > upperBound) {
            return null;
        }

        ArrayList transitions = transitionList.get(stateId);
        for (int tries = 0; tries < transitions.size(); tries++) {
            Transition transition = (Transition) transitions.get(Main.random(transitions.size()));
            String s = traverse(transition.getToState(),
                    message + transition.getMessage().replace("\"", ""));
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public void initialize(int numberOfState, int numberOfTransition,
            int numberOfLookahead, int lookaheadLength) {
        normalMessageList.clear();
        lookaheadMessageList.clear();
        usedLookaheadMessage.clear();

        int numberOfNormalTransition = numberOfTransition - numberOfLookahead;
        separatorSize = calculateSeparatorLength(numberOfNormalTransition);
        int messageLength = 3;
        for (int i = 0; i < numberOfNormalTransition * 5;) {
            String message = generateMessage("", messageLength, 0, separatorSize);
            if (!normalMessageList.contains(message)) {
                normalMessageList.add(message);
                i++;
            }
        }
        for (int i = 0; i < numberOfLookahead * 5;) {
            String message = generateLookaheadMessage("", lookaheadLength, separatorSize, ALFABET.length());
            if (!lookaheadMessageList.contains(message)) {
                lookaheadMessageList.add(message);
                i++;
            }
        }
    }

    public GSData generateGS(int numberOfState, int numberOfTransition,
            int numberOfLookahead, int lookaheadLength, int endTransProb, String gsPath) {
        if (new File(gsPath).exists()) {
            return null;
        }

        initialize(numberOfState, numberOfTransition, numberOfLookahead,
                lookaheadLength);

        while (true) {
            usedLookaheadMessage.clear();
            usedNormalMessage.clear();

            gsdata = new GSData();
            gsdata.setInitialState(0)
                    .setparserName("GS")
                    .setFilePath(gsPath);
            if (numberOfLookahead != 0) {
                gsdata.setOptions("    lookahead = " + lookaheadLength + ";\n");
            }

            for (int stateId = 0; stateId < numberOfState; stateId++) {
                State state = new State(stateId, "q" + stateId)
                        .setInitialState(stateId == gsdata.getInitialStateId())
                        .setFinalState(false)
                        .setLocation(0, 0)
                        .setLabel("q" + stateId);
                gsdata.addState(state);
            }

            int normalTransition = numberOfTransition - numberOfLookahead;
            for (int stateId = 0; stateId < numberOfState; stateId++) {
                while (true) {
                    int choice = Main.random(20);
                    if (choice % 2 == 0 && normalTransition != 0) {
                        int to = (stateId + 1) % numberOfState;
                        String message;
                        do {
                            message = "\"" + normalMessageList.get(Main.random(normalMessageList.size())) + "\"";
                        } while (usedNormalMessage.contains(message));
                        usedNormalMessage.add(message);
                        Transition transition = new Transition(stateId, to, message);
                        gsdata.addTransition(transition);
                        normalTransition--;
                        break;
                    } else if (numberOfLookahead != 0) {
                        String message;
                        do {
                            message = "\"" + lookaheadMessageList
                                    .get(Main.random(lookaheadMessageList.size())) + "\"";
                        } while (usedLookaheadMessage.contains(message));
                        usedLookaheadMessage.add(message);

                        int to, to2, to3;
                        to = (stateId + 1) % numberOfState;
                        do {
                            if (Main.random(endTransProb) == 0) {
                                to2 = -1;
                            } else {
                                to2 = Main.random(numberOfState);
                            }
                        } while (to2 == to);
                        do {
                            if (Main.random(endTransProb) == 0) {
                                to3 = -1;
                            } else {
                                to3 = Main.random(numberOfState);
                            }
                        } while (to3 == to || to3 == to2);

                        Transition transition = new Transition(stateId, to, message);
                        Transition transition2 = new Transition(stateId, to2, message);
                        gsdata.addTransition(transition);
                        gsdata.addTransition(transition2);
                        numberOfLookahead -= 2;
                        if (numberOfLookahead % 2 == 1) {
                            Transition transition3 = new Transition(stateId, to3, message);
                            gsdata.addTransition(transition3);
                            numberOfLookahead--;
                        }
                        break;
                    }
                }
            }

            while (numberOfLookahead > 0) {
                int from = Main.random(numberOfState);
                String message;
                do {
                    message = "\"" + lookaheadMessageList
                            .get(Main.random(lookaheadMessageList.size())) + "\"";
                } while (usedLookaheadMessage.contains(message));
                usedLookaheadMessage.add(message);

                int to, to2, to3;
                if (Main.random(endTransProb) == 0) {
                    to = -1;
                } else {
                    to = Main.random(numberOfState);
                }
                do {
                    if (Main.random(endTransProb) == 0) {
                        to2 = -1;
                    } else {
                        to2 = Main.random(numberOfState);
                    }
                } while (to2 == to);
                do {
                    if (Main.random(endTransProb) == 0) {
                        to3 = -1;
                    } else {
                        to3 = Main.random(numberOfState);
                    }
                } while (to3 == to || to3 == to2);

                Transition transition = new Transition(from, to, message);
                Transition transition2 = new Transition(from, to2, message);
                gsdata.addTransition(transition);
                gsdata.addTransition(transition2);
                numberOfLookahead -= 2;
                if (numberOfLookahead % 2 == 1) {
                    Transition transition3 = new Transition(from, to3, message);
                    gsdata.addTransition(transition3);
                    numberOfLookahead--;
                }

            }

            while (normalTransition != 0) {
                int from = Main.random(numberOfState);
                int to;
                if (Main.random(endTransProb) == 0) {
                    to = -1;
                } else {
                    to = Main.random(numberOfState);
                }
                String message;
                do {
                    message = "\"" + normalMessageList.get(Main.random(normalMessageList.size())) + "\"";
                } while (usedNormalMessage.contains(message));
                usedNormalMessage.add(message);
                Transition transition = new Transition(from, to, message);
                gsdata.addTransition(transition);
                normalTransition--;
            }
            boolean end = false;
            for (Transition t : gsdata.getTransitions()) {
                if (t.getToState() == -1) {
                    end = true;
                    break;
                }
            }
            if (end) {
                break;
            }
        }
        return gsdata;
    }

    public String generateTestCase(GSData gsdata, int lowerBound, int upperBound) {
        if (this.gsdata == null
                || !this.gsdata.getFilepath().equals(gsdata.getFilepath())) {
            setGSData(gsdata, lowerBound, upperBound);
        }
        String testCaseData;
        do {
            testCaseData = traverse(gsdata.getInitialStateId(), "");
        } while (testCaseData == null);
        return testCaseData;
    }
}
