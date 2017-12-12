package gsoptimizer;

import automata.Automaton;
import automata.fsa.FiniteStateAutomaton;
import automata.fsa.MinimizeTreeNode;
import automata.fsa.Minimizer;
import automata.fsa.NFAToDFA;
import file.XMLCodec;
import gsoptimizer.Data.JFLAPData;
import gsoptimizer.Data.State;
import gsoptimizer.Data.Transition;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.tree.DefaultTreeModel;

public class JFLAPOperator extends Minimizer {

    NFAToDFA transformer;
    Minimizer minimizer;
    private int timeOut = 600000;

    public JFLAPOperator() {
        transformer = new NFAToDFA();
        minimizer = new Minimizer();
    }

    public JFLAPData transformToDFA(String nfaPath, String dfaPath) {
        if (!new File(nfaPath).exists() || new File(dfaPath).exists()) {
            return null;
        }
        File input = new File(nfaPath);
        FiniteStateAutomaton nfa = (FiniteStateAutomaton) new XMLCodec()
                .decode(input, null);

        FiniteStateAutomaton dfa = transformer.convertToDFA(nfa);

        JFLAPData jflapData = toJFLAPData(dfa);
        jflapData.setFilePath(dfaPath);
        return jflapData;
    }

    public JFLAPData minimizeDFA(String dfaPath, String minDFAPath) {
        if (new File(minDFAPath).exists()) {
            return null;
        }

        File input = new File(dfaPath);
        FiniteStateAutomaton dfa = (FiniteStateAutomaton) new XMLCodec().decode(input, null);
        initializeMinimizer();
        FiniteStateAutomaton minDfa = (FiniteStateAutomaton) dfa.clone();

        DefaultTreeModel tree;
        minimizer.initializeMinimizer();

        minDfa = (FiniteStateAutomaton) getMinimizeableAutomaton(minDfa);
        tree = getDistinguishableGroupsTree(minDfa);
        if (tree == null) {
            return null;
        }
        minDfa = minimizer.getMinimumDfa(minDfa, tree);
        if ((minDfa.getStates().length < dfa.getStates().length)) {
            JFLAPData minJflapData = toJFLAPData(minDfa);
            minJflapData.setFilePath(minDFAPath);
            return minJflapData;
        }
        return null;
    }

    private JFLAPData toJFLAPData(FiniteStateAutomaton fsa) {
        JFLAPData jflapData = new JFLAPData();
        jflapData.setInitialState(fsa.getInitialState().getID());

        HashSet<Integer> finalStates = new HashSet<>();
        for (automata.State state : fsa.getFinalStates()) {
            finalStates.add(state.getID());
        }

        for (automata.State state : fsa.getStates()) {
            State jflapState = new State(state.getID(), state.getName())
                    .setLabel(state.getLabel())
                    .setLocation((float) state.getPoint().getX(),
                            (float) state.getPoint().getY())
                    .setFinalState(finalStates.contains(state.getID()))
                    .setInitialState(state.getID() == fsa.getInitialState().getID());
            jflapData.addState(jflapState);
        }

        for (automata.Transition transition : fsa.getTransitions()) {
            Transition jflapTransition = new Transition()
                    .setFromState(transition.getFromState().getID())
                    .setToState(transition.getToState().getID())
                    .setMessage(transition.getDescription());
            jflapData.addTransition(jflapTransition);
        }
        return jflapData;
    }

    @Override
    public DefaultTreeModel getDistinguishableGroupsTree(Automaton automaton) {
        DefaultTreeModel tree = minimizer.getInitializedTree(automaton);
        // DefaultMutableTreeNode root =
        // (DefaultMutableTreeNode) tree.getRoot();
        MinimizeTreeNode root = (MinimizeTreeNode) tree.getRoot();
        long time = System.currentTimeMillis();
        while (!minimizer.isMinimized(automaton, tree)) {
            if (System.currentTimeMillis() - time > timeOut) {
                return null;
            }
            automata.State[] group = minimizer.getDistinguishableGroup(automaton, tree);
            ArrayList children = new ArrayList();
            String terminal = minimizer.getTerminalToSplit(group, automaton, tree);
            children.addAll(minimizer.splitOnTerminal(group, terminal, automaton, tree));
            // children.addAll(split(group, automaton));
            MinimizeTreeNode parent = minimizer.getTreeNodeForObject(tree, root, group);
            parent.setTerminal(terminal);
            // DefaultMutableTreeNode parent =
            // getTreeNodeForObject(tree, root, group);
            minimizer.addChildrenToParent(children, parent, tree);
        }

        return tree;
    }

    public void setMinimizerTimeout(int timeOutMilli) {
        this.timeOut = timeOutMilli;
    }
}
