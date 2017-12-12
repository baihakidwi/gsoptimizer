package gsoptimizer;

import gsoptimizer.Data.GSData;
import gsoptimizer.Data.JFLAPData;
import gsoptimizer.Data.State;
import gsoptimizer.Data.Transition;
import java.io.File;
import java.util.HashSet;

public class Translator {

    public JFLAPData translateGS(GSData gsData, String jflapPath) {
        if (new File(jflapPath).exists()) {
            return null;
        }
        JFLAPData jflapdata = new JFLAPData(gsData);
        jflapdata.setFilePath(jflapPath);

        for (State state : gsData.getStates()) {
            jflapdata.addState(state.clone());
        }

        int finalStateId = gsData.getStates().size();
        State finalState = new State(finalStateId, "FINAL_STATE")
                .setFinalState(true)
                .setLocation(0, 0);
        jflapdata.addState(finalState);

        for (Transition transition : gsData.getTransitions()) {
            Transition jflapTransition = transition.clone();
            if (jflapTransition.getToState() == -1) {
                jflapTransition.setToState(finalStateId);
            }
            jflapTransition.setMessage(jflapTransition.getMessage().replace("\"", ""));
            jflapdata.addTransition(jflapTransition);
        }
        return jflapdata;
    }

    public GSData translateJFLAP1(JFLAPData jflapData, String gsPath) {
        if (new File(gsPath).exists()) {
            return null;
        }
        GSData gsdata = new GSData(jflapData);
        gsdata.setparserName("GS")
                .setFilePath(gsPath);

        int finalStateId = -1;
        for (State state : jflapData.getStates()) {
            gsdata.addState(state.clone());
            if (state.isFinalState()) {
                Transition finalTransition = new Transition(state.getId(), finalStateId, "\"\"");
                gsdata.addTransition(finalTransition);
            }
        }

        for (Transition transition : jflapData.getTransitions()) {
            Transition gsTransition = transition.clone();
            gsTransition.setMessage("\"" + gsTransition.getMessage() + "\"");
            gsdata.addTransition(gsTransition);
        }

        return gsdata;
    }

    public GSData translateJFLAP2(JFLAPData jflapData, String gsPath) {
        if (new File(gsPath).exists()) {
            return null;
        }

        GSData gsdata = new GSData(jflapData);
        gsdata.setparserName(gsPath)
                .setFilePath(gsPath);

        HashSet<Integer> finalStates = new HashSet<>();

        for (State state : jflapData.getStates()) {
            gsdata.addState(state.clone());
            if (state.isFinalState()) {
                finalStates.add(state.getId());
            }
        }

        int finalStateId = -1;
        for (Transition transition : jflapData.getTransitions()) {
            Transition gsTransition = transition.clone();
            gsTransition.setMessage("\"" + gsTransition.getMessage() + "\"");
            gsdata.addTransition(gsTransition);

            if (finalStates.contains(gsTransition.getToState())) {
                Transition finalTransition = gsTransition.clone();
                finalTransition.setToState(finalStateId);
                gsdata.addTransition(finalTransition);
            }
        }
        return gsdata;
    }

}
