package gsoptimizer;

import gsoptimizer.Data.GSData;
import gsoptimizer.Data.JFLAPData;
import gsoptimizer.Data.Transition;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class Main {

    public static void main(String[] args) {

        int endTransitionProb = 5;
        int[][] numberOfState = {{2, 70}};
        int[][] transitionMultiplier = {{2, 8}, {8, 8}, {32, 32}};
        int[] lookaheadMultiplier = {30, 80};
        int[] lookaheadLength = {4, 8};
        int totalNumberOfGS = 500;
        int numberOfTestCase = 5;
        int[] testCaseSize = {11000, 12000};
        int minimizerTimeOut = 3000000;

        for (int i = 0; i < numberOfState.length; i++) {
            String workingDir = "E:\\DATA\\KULIAH\\OTHER\\RUNNING_DATA\\"
                    + "S" + numberOfState[i][0] + "-" + numberOfState[i][1] + "-"
                    + "T" + transitionMultiplier[i][0] + "-" + transitionMultiplier[i][1] + "-"
                    + "L" + ((float) lookaheadMultiplier[0] / 100) + "-"
                    + ((float) lookaheadMultiplier[1] / 100) + "-"
                    + "Ln" + lookaheadLength[0] + "-" + lookaheadLength[1] + "\\";
            String javaCCPath = "E:\\TEMP\\javacc-6.0\\bin\\javacc.bat";

            Main main = new Main(workingDir, javaCCPath)
                    .setNumberOfState(numberOfState[i])
                    .setTransitionMultiplier(transitionMultiplier[i])
                    .setLookaheadMultiplier(lookaheadMultiplier)
                    .setLookaheadLength(lookaheadLength)
                    .setEndTransProb(endTransitionProb)
                    .setNumberOfTestCase(numberOfTestCase)
                    .setTestCaseSize(testCaseSize)
                    .setTotalNumberOfGS(totalNumberOfGS)
                    .setMinimizerTimeout(minimizerTimeOut);

            main.generateNFA();
//            main.generateDFA();
//            main.minimizeDFA();
//            main.generateTestCase();
            main.buildParser();
            main.runTest();
            main.processData();
        }

        int endTransitionProb2 = 5;
        int[][] numberOfState2 = {{4, 4}, {4, 4}, {4, 4}, {4, 4}};
        int[][] transitionMultiplier2 = {{2, 2}, {32, 32}, {64, 64}, {128, 128}};
        int[] lookaheadMultiplier2 = {0, 0};
        int[] lookaheadLength2 = {4, 4};
        int totalNumberOfGS2 = 150;
        int numberOfTestCase2 = 5;
        int[] testCaseSize2 = {11000, 12000};
        int minimizerTimeOut2 = 3000000;

        for (int i = 0; i < numberOfState2.length; i++) {
            String workingDir = "E:\\DATA\\KULIAH\\OTHER\\RUNNING_DATA\\"
                    + "S" + numberOfState2[i][0] + "-" + numberOfState2[i][1] + "-"
                    + "T" + transitionMultiplier2[i][0] + "-" + transitionMultiplier2[i][1] + "-"
                    + "L" + ((float) lookaheadMultiplier2[0] / 100) + "-"
                    + ((float) lookaheadMultiplier2[1] / 100) + "-"
                    + "Ln" + lookaheadLength2[0] + "-" + lookaheadLength2[1] + "\\";
            String javaCCPath = "E:\\TEMP\\javacc-6.0\\bin\\javacc.bat";

            Main main = new Main(workingDir, javaCCPath)
                    .setNumberOfState(numberOfState2[i])
                    .setTransitionMultiplier(transitionMultiplier2[i])
                    .setLookaheadMultiplier(lookaheadMultiplier2)
                    .setLookaheadLength(lookaheadLength2)
                    .setEndTransProb(endTransitionProb2)
                    .setNumberOfTestCase(numberOfTestCase2)
                    .setTestCaseSize(testCaseSize2)
                    .setTotalNumberOfGS(totalNumberOfGS2)
                    .setMinimizerTimeout(minimizerTimeOut2);

            main.generateNFA();
//            main.generateDFA();
//            main.minimizeDFA();
            main.generateTestCase();
            main.buildParser();
            main.runTest();
            main.processData();

        }
        playSound(Main.FINISH);
    }

    public static final int ERROR = 0;
    public static final int WARNING = 1;
    public static final int FINISH = 2;

    private final String WORKING_DIR;
    private final String JAVACC_PATH;

    private final String[] GS_TYPE;

    private int endTransProb;
    private int[] numberOfState;
    private int[] transitionMultiplier;
    private int[] lookaheadMultiplier;
    private int[] lookaheadLength;
    private int totalNumberOfGS;
    private int numberOfTestCase;
    private int[] testCaseSize;

    private int timeOutMilli;

    private final ArrayList<String> parserNameList;

    private static Random randomGenerator;

    private static final String NFA_DIR = "\\1_nfa\\";
    private static final String DFA_DIR = "\\2_dfa\\";
    private static final String MIN_DFA_DIR = "\\3_mindfa\\";
    private static final String TEST_DIR = "\\4_testcase\\";

    private static int totalProgress;
    private static int taskProgress;

    private static DecimalFormat decimalFormat;

    public Main(String workingDir, String javaCCPath) {
        this.GS_TYPE = new String[]{"\\1_nfa\\", "\\2_dfa\\", "\\3_mindfa\\"};
        this.WORKING_DIR = workingDir;
        this.JAVACC_PATH = javaCCPath;
        parserNameList = new ArrayList<>();
        randomGenerator = new Random(System.currentTimeMillis());
        decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(2);
    }

    private void loadParserNameList() {
        String parserNamePath = WORKING_DIR + "parsername.txt";
        File parserNameFile = new File(parserNamePath);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(parserNameFile)));
            String s;
            while ((s = reader.readLine()) != null) {
                s = s.replace((char) 0, ' ');
                s = s.replace(" ", "");
                parserNameList.add(s.trim());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        java.util.Collections.sort(parserNameList);
        totalProgress = parserNameList.size();
        System.out.println(parserNameList.size() + " parser loaded");
    }

    private void generateNFA() {
        taskProgress = -1;
        progressing("generating gs", "");

        Generator generator = new Generator();
        Translator translator = new Translator();
        String parserNamePath = WORKING_DIR + "parsername.txt";
        if (!new File(parserNamePath).exists()) {
            for (int gs = 0; gs < totalNumberOfGS; gs++) {
                int state, transition, lookahead, lookaheadLen;
                do {
                    state = random(numberOfState[0], numberOfState[1]);
                    transition = state * random(transitionMultiplier[0], transitionMultiplier[1]);
                    lookahead = (int) Math.round((double) transition
                            * ((double) random(lookaheadMultiplier[0], lookaheadMultiplier[1]) / 100));
                    lookaheadLen = random(lookaheadLength[0], lookaheadLength[1]);
                    if (lookaheadMultiplier[0] <= 1) {
                        break;
                    }
                } while (lookahead <= 1);
                String parserName = WORKING_DIR + "S" + state + "-"
                        + "T" + transition + "-"
                        + "L" + lookahead + "-"
                        + "Ln" + lookaheadLen;
                if (parserNameList.contains(parserName)) {
                    for (int i = 1;; i++) {
                        if (!parserNameList.contains(parserName + "_" + i)) {
                            parserName = parserName + "_" + i;
                            break;
                        }
                    }
                }
                parserNameList.add(parserName);
                System.out.println(parserName);
                String gsNfaPath = parserName + NFA_DIR + "gs.jj";
                String nfaPath = parserName + NFA_DIR + "gs.jff";

                GSData gsdata = generator.generateGS(state, transition, lookahead,
                        lookaheadLen, endTransProb, gsNfaPath);
                if (gsdata != null) {
                    gsdata.setTokenManager("SKIP: {\" \" | \"\\r\" | \"\\n\"}");
                    gsdata.print();
                }

                JFLAPData nfa = translator.translateGS(gsdata, nfaPath);
                if (nfa != null) {
                    nfa.print();
                }

                progressing("generating gs", parserName);
            }

            java.util.Collections.sort(parserNameList);

            String parserNames = "";
            for (String parserName : parserNameList) {
                parserNames += parserName + "\n";
            }
            print(parserNames, WORKING_DIR + "parsername.txt");
        } else {
            loadParserNameList();
        }
    }

    private void generateDFA() {
        taskProgress = -1;
        progressing("generating dfa", "");

        JFLAPOperator operator = new JFLAPOperator();
        Translator translator = new Translator();

        for (String parserName : parserNameList) {
            String nfaPath = parserName + NFA_DIR + "gs.jff";
            String dfaPath = parserName + DFA_DIR + "gs.jff";
            String gsDfaPath = parserName + DFA_DIR + "gs.jj";

            JFLAPData dfa = operator.transformToDFA(nfaPath, dfaPath);
            if (dfa != null) {
                dfa.print();

                GSData gsdata = translator.translateJFLAP1(dfa, gsDfaPath);
                if (gsdata != null) {
                    gsdata.print();
                }
            }
            progressing("generating dfa", parserName);
        }
    }

    private void minimizeDFA() {
        taskProgress = -1;
        progressing("minimizing DFA", "");

        JFLAPOperator operator = new JFLAPOperator();
        operator.setMinimizerTimeout(timeOutMilli);

        for (String parserName : parserNameList) {
            String dfaPath = parserName + DFA_DIR + "gs.jff";
            String MinDfaPath = parserName + MIN_DFA_DIR + "gs.jff";
            String gsMinDfaPath = parserName + MIN_DFA_DIR + "gs.jj";

            JFLAPData mindfa = operator.minimizeDFA(dfaPath, MinDfaPath);
            if (mindfa != null) {
                mindfa.print();

                Translator translator = new Translator();
                GSData gsdata = translator.translateJFLAP1(mindfa, gsMinDfaPath);
                if (gsdata != null) {
                    gsdata.print();
                }
            }

            progressing("minimizing DFA", parserName);
        }
    }

    private void generateTestCase() {
        taskProgress = -1;
        progressing("generating test case", "");

        Generator generator = new Generator();
        for (String parserName : parserNameList) {
            String gspath = parserName + NFA_DIR + "gs.jj";
            GSData gsdata = new GSData(gspath);
            for (int testCase = 0; testCase < numberOfTestCase; testCase++) {
                String testCaseData = generator.generateTestCase(gsdata,
                        testCaseSize[0], testCaseSize[1]);
                String testcasepath = parserName + TEST_DIR + testCase + ".txt";
                print(testCaseData, testcasepath);
            }
            progressing("generating test case", parserName);
        }
    }

    private void buildParser() {
        taskProgress = -1;
        progressing("building parser", "");
        Tester tester = new Tester(JAVACC_PATH);
        tester.setMaxWait(250);
        for (String parserName : parserNameList) {
            String[] gss = {parserName + NFA_DIR,
                parserName + DFA_DIR,
                parserName + MIN_DFA_DIR};

            for (String gsdir : gss) {
                tester.buildParser(gsdir);
            }
            progressing("building parser", parserName);
        }
        tester.close();
    }

    private void runTest() {
        taskProgress = -1;
        progressing("running test", "");
        for (String parserName : parserNameList) {
            Tester tester = new Tester(JAVACC_PATH);
            tester.setMaxWait(400);

            int[][] data = tester.runTest(parserName, numberOfTestCase);

            boolean error;
            do {
                error = false;
                for (int testCase = 0; testCase < numberOfTestCase; testCase++) {
                    if (data[testCase][0] == Integer.MIN_VALUE
                            || data[testCase][1] == Integer.MIN_VALUE
                            || data[testCase][2] == Integer.MIN_VALUE //
                            || data[testCase][0] >= 400
                            || data[testCase][1] >= 400
                            || data[testCase][2] >= 400) {
                        error = true;
                        data[testCase] = tester.runSingleTest(parserName, testCase);
                    }
                }
            } while (error);

            tester.close();

            String printData = "group;ori;dfa;min;\n";
            for (int line = 0; line < data.length; line++) {
                for (int column = 0; column < data[0].length; column++) {
                    printData += data[line][column] + ";";
                }
                printData += "\n";
            }

            String csvpath = parserName + ".csv";

            print(printData, csvpath);

            progressing("running test", parserName);
        }
    }

    private void processData() {
        totalProgress = 4;
        taskProgress = -1;
        progressing("processing data", "");
        progressing("collecting time data", "");
        combineParserTimeData();
        progressing("collectiong parser state and transition data", "");
        combineParserStateData();
        progressing("collectiong parser message data", "");
        collectParserMessageData();
        progressing("processing data", "");
    }

    private void combineParserTimeData() {
        String allParserTime = "group;ori;dfa;min;\n";
        String allParserTimeCsvPath = WORKING_DIR + "allParserTime.csv";
        String minDfaParserTime = "group;ori;dfa;min;\n";
        String minDfaParserTimeCsvPath = WORKING_DIR + "minDfaParserTime.csv";
        for (String parserName : parserNameList) {
            String dataPath = parserName + ".csv";
            File dataFile = new File(dataPath);
            if (dataFile.exists()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)));
                    String s;
                    while (null != (s = reader.readLine())) {
                        s = s.replace((char) 0, ' ');
                        s = s.replace(" ", "");
                        s = s.trim();

                        if (!s.contains("ori")) {
                            allParserTime += parserName + ";" + s + "\n";

                            if (!s.contains("-1")) {
                                minDfaParserTime += parserName + ";" + s + "\n";
                            }
                        }
                    }
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
        print(allParserTime, allParserTimeCsvPath);
        print(minDfaParserTime, minDfaParserTimeCsvPath);
    }

    private void combineParserStateData() {
        String stateData = "group;"
                + "ori_state;mean_ori_tr_per_state;sd_ori_tr_per_state;"
                + "dfa_state;mean_dfa_tr_per_state;sd_dfa_tr_per_state;"
                + "min_state;mean_min_tr_per_state;sd_min_tr_per_state;"
                + "\n";
        String stateDataCsvPath = WORKING_DIR + "stateData.csv";

        String[] gsTypes = {"\\1_nfa\\", "\\2_dfa\\", "\\3_mindfa\\"};
        for (String parserName : parserNameList) {
            stateData += parserName + ";";
            for (String gsType : gsTypes) {
                String gsdataPath = parserName + gsType + "gs.jj";
                if (!new File(gsdataPath).exists()) {
                    stateData += ";;;";
                    continue;
                }
                GSData gsdata = new GSData(parserName + gsType + "gs.jj");

                int[] stateTransCount = new int[gsdata.getStates().size()];
                for (Transition transition : gsdata.getTransitions()) {
                    stateTransCount[transition.getFromState()]++;
                }
                SummaryStatistics stateStatistic = new SummaryStatistics();
                for (int count : stateTransCount) {
                    stateStatistic.addValue(count);
                }

                stateData += gsdata.getStates().size() + ";";
                stateData += decimalFormat.format(stateStatistic.getMean()) + ";";
                stateData += decimalFormat.format(stateStatistic.getStandardDeviation()) + ";";
            }
            stateData += "\n";
        }
        print(stateData, stateDataCsvPath);
    }

    private void collectParserMessageData() {
        String messageData = "group;"
                + "ori_trans_count;mean_ori_trans_len;sd_ori_trans_len;"
                + "dfa_trans_count;mean_dfa_trans_len;sd_dfa_trans_len;"
                + "min_trans_count;mean_min_trans_len;sd_min_trans_len;"
                + "\n";
        String messageDataCsvPath = WORKING_DIR + "transitionData.csv";

        String[] gsTypes = {"\\1_nfa\\", "\\2_dfa\\", "\\3_mindfa\\"};
        for (String parserName : parserNameList) {
            messageData += parserName + ";";
            for (String gsType : gsTypes) {
                if (!new File(parserName + gsType + "gs.jj").exists()) {
                    messageData += ";;;";
                    continue;
                }
                GSData gsdata = new GSData(parserName + gsType + "gs.jj");

                SummaryStatistics messageStatistic = new SummaryStatistics();
                for (Transition transition : gsdata.getTransitions()) {
                    messageStatistic.addValue(transition.getMessage().length() - 2);
                }
                messageData += gsdata.getTransitions().size() + ";";
                messageData += decimalFormat.format(messageStatistic.getMean()) + ";";
                messageData += decimalFormat.format(messageStatistic.getStandardDeviation()) + ";";
            }
            messageData += "\n";
        }
        print(messageData, messageDataCsvPath);
    }

    public static void print(String data, String filepath) {
        try {
            Path path = Paths.get(filepath);
            Files.createDirectories(path.getParent());
            Files.write(Paths.get(filepath), data.getBytes(StandardCharsets.UTF_8));

        } catch (IOException ex) {
            Logger.getLogger(Generator.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void progressing(String step, String message) {
        taskProgress++;
        if (!message.equals("")) {
            System.out.println("Progress    : " + LocalDateTime.now()
                    + ": " + message);
        }
        System.out.println("Progress    : " + LocalDateTime.now()
                + ": " + step
                + ": " + decimalFormat.format(100f * taskProgress / totalProgress) + "%"
        );
    }

    public static int random(int bound) {
        if (randomGenerator == null) {
            randomGenerator = new Random(System.currentTimeMillis());
        }
        if (bound == 0) {
            return 0;
        }
        return randomGenerator.nextInt(bound);
    }

    public static int random(int lowerBound, int upperBound) {
        if (randomGenerator == null) {
            randomGenerator = new Random(System.currentTimeMillis());
        }
        if (lowerBound == upperBound) {
            return lowerBound;
        }
        return lowerBound + randomGenerator.nextInt(upperBound - lowerBound);
    }

    private static void playSound(int status) {
        Clip clip = null;
        File soundFile = null;
        String soundpath = "E:\\DATA\\KULIAH\\OTHER\\PROGRAM\\GSOptimizer\\";
        switch (status) {
            case ERROR:
                soundFile = new File(soundpath + "error.wav");
                break;
            case WARNING:
                soundFile = new File(soundpath + "warning.wav");
                break;
            case FINISH:
                soundFile = new File(soundpath + "done.wav");
                break;
        }
        if (soundFile != null) {
            try {
                AudioInputStream audioInputStream = AudioSystem
                        .getAudioInputStream(soundFile.getAbsoluteFile());
                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
                Thread.sleep(clip.getMicrosecondLength() / 1000);
            } catch (UnsupportedAudioFileException | IOException |
                    LineUnavailableException | InterruptedException ex) {
                Logger.getLogger(Main.class
                        .getName()).log(Level.SEVERE, null, ex);
            } finally {
                clip.close();
            }
        }
    }

    public Main setEndTransProb(int endTransProb) {
        this.endTransProb = endTransProb;
        return this;
    }

    public Main setNumberOfState(int[] numberOfState) {
        this.numberOfState = numberOfState;
        return this;
    }

    public Main setTransitionMultiplier(int[] transitionMultiplier) {
        this.transitionMultiplier = transitionMultiplier;
        return this;
    }

    public Main setLookaheadMultiplier(int[] lookaheadMultiplier) {
        this.lookaheadMultiplier = lookaheadMultiplier;
        return this;
    }

    public Main setLookaheadLength(int[] lookaheadLength) {
        this.lookaheadLength = lookaheadLength;
        return this;
    }

    public Main setNumberOfTestCase(int numberOfTestCase) {
        this.numberOfTestCase = numberOfTestCase;
        return this;
    }

    public Main setTestCaseSize(int[] testCaseSize) {
        this.testCaseSize = testCaseSize;
        return this;
    }

    public Main setTotalNumberOfGS(int totalNumberOfGS) {
        this.totalNumberOfGS = totalNumberOfGS;
        totalProgress = totalNumberOfGS;
        return this;
    }

    public Main setMinimizerTimeout(int timeOutMilli) {
        this.timeOutMilli = timeOutMilli;
        return this;
    }
}
