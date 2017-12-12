package gsoptimizer.Data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GSData extends Data {

    private static final Pattern patternComment = Pattern
            .compile("(/\\*.*?\\*/).*", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern patternOption = Pattern
            .compile("(options\\s*\\{(.*?)}).*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern patternCompUnit = Pattern
            .compile("(parser_begin\\(\\s*(\\w+)\\s*\\)"
                    + ".*?\\(\\s*system\\s*\\.\\s*in\\)\\s*;"
                    + ".*\\.(.*?)\\s*\\(.*?\\)\\s*;.*"
                    + "parser_end\\(.*?\\)).*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern patternToken = Pattern
            .compile("(token\\s*:\\s*\\{.*?>\\s*}).*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern patternBnf = Pattern
            .compile("(public|private|protected??\\s*"
                    + "byte|int|long|float|char|string|void{1}\\s*"
                    + "(\\w*)\\s*\\(.*?\\)\\s*:\\s*\\{.*?\\}\\s*\\{(.*?)\\}"
                    + ").*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern patternNonTerminal = Pattern
            .compile("(.*?)(\\w+)\\s*\\(.*?\\).*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    public GSData() {
        super();
    }

    public GSData(Data data) {
        super(data);
    }

    public GSData(String filepath) {
        super();
        this.filepath = filepath;
        try {
            List<String> rawData = Files.readAllLines(Paths.get(filepath),
                    StandardCharsets.UTF_8);
            rawData.stream().forEach((s) -> {
                printData += s + "\n";
            });

            int stateId = 0;
            String initialStateName = "";
            int index = next(0);

            ArrayList<String[]> transitionList = new ArrayList();

            while (index != -1 && index < printData.length()) {
                Matcher regexMatcher;
                if (printData.regionMatches(index, "/*", 0, "/*".length())) {
                    regexMatcher = patternComment.matcher(printData.substring(index));
                    regexMatcher.matches();
                    index = next(index + regexMatcher.end(1));
                } else if (printData.regionMatches(true, index, "options", 0, "options".length())) {
                    regexMatcher = patternOption.matcher(printData.substring(index));
                    regexMatcher.matches();
                    options = regexMatcher.group(2);
                    index = next(index + regexMatcher.end(1));
                } else if (printData.regionMatches(true, index, "parser_begin", 0, "parser_begin".length())) {
                    regexMatcher = patternCompUnit.matcher(printData.substring(index));
                    regexMatcher.matches();
                    parserName = regexMatcher.group(2);
                    initialStateName = regexMatcher.group(3);
                    index = next(index + regexMatcher.end(1));
                } else if (printData.regionMatches(true, index, "token", 0, "token".length())) {
                    regexMatcher = patternToken.matcher(printData.substring(index));
                    regexMatcher.matches();
                    tokenManager = regexMatcher.group(1);
                    index = next(index + regexMatcher.end(1));
                } else if (printData.regionMatches(true, index, "SKIP", 0, "SKIP".length())) {
                    for (; printData.charAt(index) != '}'; index++) {
                    }
                    index = next(index + 1);
                } else {
                    regexMatcher = patternBnf.matcher(printData.substring(index));
                    regexMatcher.matches();
                    String currentStateName = regexMatcher.group(2);
                    String[] stateTransitions = regexMatcher.group(3).split("\\|");
                    boolean finalState = false;
                    for (String transition : stateTransitions) {
                        transition = transition.trim();
                        Matcher regexMatcher2 = patternNonTerminal.matcher(transition);
                        String nextStateName = "";
                        if (regexMatcher2.matches()) {
                            nextStateName = regexMatcher2.group(2);
                            transition = regexMatcher2.group(1).trim();
                        } else {
                            nextStateName = State.END_STATE;
                            finalState = true;
                        }
                        String[] arTransition = {currentStateName, nextStateName, transition};
                        transitionList.add(arTransition);
                    }

                    State state = new State()
                            .setId(stateId)
                            .setName(currentStateName)
                            .setInitialState(currentStateName.equals(initialStateName))
                            .setFinalState(finalState)
                            .setLocation(stateId * 20, 0);
                    if (state.isInitialState()) {
                        initialStateId = state.getId();
                    }
                    nameToId.put(currentStateName, stateId);
                    idToName.put(stateId, currentStateName);
                    states.add(state);

                    stateId++;
                    index = next(index + regexMatcher.end(1));
                }
            }

            for (String[] tr : transitionList) {
                int toState;
                if (tr[1].equals(State.END_STATE)) {
                    toState = -1;
                } else {
                    toState = nameToId.get(tr[1]);
                }
                Transition transition = new Transition()
                        .setFromState(nameToId.get(tr[0]))
                        .setToState(toState)
                        .setMessage(tr[2]);
                transitions.add(transition);
            }
        } catch (IOException ex) {
            Logger.getLogger(GSData.class.getName()).log(Level.SEVERE, null, ex);
        }
        printData = "";

    }

    private int next(int index) {
        for (; index < printData.length(); index++) {
            if (!Character.isWhitespace(printData.charAt(index))) {
                break;
            }
        }
        return index;
    }

    @Override
    public String toString() {
        if (printData == null || printData.equals("")) {
            if (options != null && !options.equals("")) {
                printData += "options{\n";
                printData += options;
                printData += "}\n\n";
            }

            printData += "PARSER_BEGIN(" + parserName + ")\n"
                    + "    public class " + parserName + "{\n"
                    + "        public static void main(String args[]) throws ParseException {\n"
                    + "            " + parserName + " parser = new " + parserName + "(System.in);\n"
                    + "            parser." + idToName.get(initialStateId) + "();\n"
                    + "        }\n"
                    + "    }\n"
                    + "PARSER_END(" + parserName + ")\n\n";

            if (tokenManager != null && !tokenManager.equals("")) {
                printData += tokenManager + "\n\n";
            }
//            printData += "SKIP:{\" \" | \"\\r\" | \"\\n\" | \"\\t\"}\n";

            ArrayList<ArrayList> transitionLists = new ArrayList<>();

            for (State state : states) {
                ArrayList<Transition> transitionList = new ArrayList<>();
                transitionLists.add(transitionList);
            }
            for (Transition transition : transitions) {
                transitionLists.get(transition.getFromState()).add(transition);
            }

            for (State state : states) {
                String tmpStr = ""
                        + "void " + state.getName() + "():\n"
                        + "{}\n"
                        + "{\n";

                ArrayList<Transition> transitionList = transitionLists.get(state.getId());

                boolean first = true;
                for (Transition transition : transitionList) {
                    if (transition.getToState() == -1) {
                        if (!first) {
                            tmpStr += "    | ";
                        } else {
                            tmpStr += "    ";
                            first = false;
                        }
                        tmpStr += transition.getMessage() + "\n";
                    }
                }
                for (Transition transition : transitionList) {
                    if (transition.getToState() != -1) {
                        if (!first) {
                            tmpStr += "    | ";
                        } else {
                            tmpStr += "    ";
                            first = false;
                        }
                        tmpStr += transition.getMessage() + " ";
                        tmpStr += idToName.get(transition.getToState()) + "()\n";
                    }
                }
                tmpStr += "}\n\n";

                printData += tmpStr;
            }
        }
        return printData;
    }

}
