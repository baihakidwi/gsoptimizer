package gsoptimizer.Data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Data {

    HashMap<String, Integer> nameToId;
    HashMap<Integer, String> idToName;
    ArrayList<State> states;
    ArrayList<Transition> transitions;
    int initialStateId;
    String filepath;
    String parserName;
    String options;
    String tokenManager;
    String printData;

    public Data() {
        nameToId = new HashMap<>();
        idToName = new HashMap<>();
        states = new ArrayList<>();
        transitions = new ArrayList<>();
        initialStateId = -1;
        filepath = "";
        parserName = "";
        options = "";
        tokenManager = "";
        printData = "";
    }

    public Data(Data data) {
        nameToId = new HashMap<>();
        idToName = new HashMap<>();
        states = new ArrayList<>();
        transitions = new ArrayList<>();
        this.initialStateId = data.getInitialStateId();
        this.filepath = data.getFilepath();
        this.parserName = data.getParserName();
        this.options = data.getOptions();
        this.tokenManager = data.getTokenManager();
        printData = "";
    }

    public Data setInitialState(int initialStateId) {
        this.initialStateId = initialStateId;
        return this;
    }

    public Data addState(State state) {
        states.add(state);
        nameToId.put(state.getName(), state.getId());
        idToName.put(state.getId(), state.getName());
        return this;
    }

    public Data addTransition(Transition transition) {
        transitions.add(transition);
        return this;
    }

    public Data setFilePath(String filepath) {
        this.filepath = filepath;
        return this;
    }

    public Data setOptions(String options) {
        this.options = options;
        return this;
    }

    public Data setTokenManager(String tokenManager) {
        this.tokenManager = tokenManager;
        return this;
    }

    public Data setparserName(String parserName) {
        this.parserName = parserName;
        return this;
    }

    public ArrayList<State> getStates() {
        return states;
    }

    public ArrayList<Transition> getTransitions() {
        return transitions;
    }

    public int getInitialStateId() {
        return initialStateId;
    }

    public String getFilepath() {
        return filepath;
    }

    public String getOptions() {
        return options;
    }

    public String getTokenManager() {
        return tokenManager;
    }

    public int getStateId(String stateName) {
        return nameToId.get(stateName);
    }

    public String getStateName(int stateId) {
        return idToName.get(stateId);
    }

    public String getParserName() {
        return parserName;
    }

    public void print() {
        try {
            Path path = Paths.get(filepath);
            Files.createDirectories(path.getParent());
            Files.write(Paths.get(filepath), toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
