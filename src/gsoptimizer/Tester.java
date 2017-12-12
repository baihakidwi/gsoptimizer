package gsoptimizer;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellNotAvailableException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tester {

    private final int JAVA_FILE = 0;
    private final int CLASS_FILE = 1;

    private final String javaCCPath;

    private final String OUT_FILE;

    PowerShell powerShell;

    public Tester(String javaCCPath) {
        this.javaCCPath = javaCCPath;
        try {
            powerShell = PowerShell.openSession();
            HashMap<String, String> myConfig = new HashMap<>();
            myConfig.put("waitPause", "10");
            myConfig.put("maxWait", "5000");
            powerShell.configuration(myConfig);
        } catch (PowerShellNotAvailableException ex) {
            Logger.getLogger(Tester.class.getName()).log(Level.SEVERE, null, ex);
        }
        OUT_FILE = System.getProperty("user.dir") + "\\" + "output.txt";
    }

    public void setMaxWait(int maxWaitMilli) {
        HashMap<String, String> myConfig = new HashMap<>();
        myConfig.put("waitPause", "5");
        myConfig.put("maxWait", String.valueOf(maxWaitMilli));
        powerShell.configuration(myConfig);
    }

    public void buildParser(String gsDir) {
        String gsPath = gsDir + "gs.jj";
        if (!new File(gsPath).exists()) {
            return;
        }

        String command = "cd " + gsDir;
        powerShell.executeCommand(command);

        if (!checkFiles(gsDir, JAVA_FILE)) {
            command = javaCCPath + " gs.jj";
            while (!checkFiles(gsDir, JAVA_FILE)) {
                powerShell.executeCommand(command);
                for (int sleep = 0; sleep < 9000; sleep++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                    if (checkFiles(gsDir, JAVA_FILE)) {
                        break;
                    }
                }
            }
        }
        if (!checkFiles(gsDir, CLASS_FILE)) {
            command = "javac *.java";
            while (!checkFiles(gsDir, CLASS_FILE)) {
                powerShell.executeCommand(command);
                for (int sleep = 0; sleep < 9000; sleep++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                    if (checkFiles(gsDir, CLASS_FILE)) {
                        break;
                    }
                }
            }
        }
    }

    public int[][] runTest(String gsDir, int numberOfTestCase) {
        int[][] data = new int[numberOfTestCase][];
        for (int testCase = 0; testCase < numberOfTestCase; testCase++) {
            data[testCase] = runSingleTest(gsDir, testCase);
        }
        return data;
    }

    public int[] runSingleTest(String groupDir, int testCaseNumber) {
        String[] gsTypeDir = {"\\1_nfa\\", "\\2_dfa\\", "\\3_mindfa\\"};
        int[] data = new int[gsTypeDir.length];
        for (int i = 0; i < gsTypeDir.length; i++) {
            String gsdir = groupDir + gsTypeDir[i];
            if (!new File(gsdir + "gs.jj").exists()) {
                data[i] = -1;
                continue;
            }
            try {
                String command = "Measure-Command{ "
                        + "Get-Content " + groupDir + "\\4_testcase\\" + testCaseNumber + ".txt"
                        + " | java -cp " + gsdir + " GS }"
                        + " | select @{n=\"t\";e={$_.Minutes,\";\",$_.Seconds,"
                        + "\";\",$_.Milliseconds,\"\" -join \"\"}} "
                        + "| Out-File -filepath " + OUT_FILE;
                data[i] = getMilli(command);
            } catch (Exception ex) {
            }
        }
        return data;
    }

    private int getMilli(String command) {
        File file = new File(System.getProperty("user.dir") + "\\" + "output.txt");
        for (int tries = 0; tries < 5 && file.exists(); tries++) {
            file.delete();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        file = new File(System.getProperty("user.dir") + "\\" + "output.txt");
        do {
            powerShell.executeCommand(command);
            for (int wait = 0; wait < 5 && !file.exists(); wait++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
        } while (!file.exists());

        BufferedReader reader = null;
        int milli = Integer.MIN_VALUE;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file)));
            String s;
            while ((s = reader.readLine()) != null) {
                s = s.replace((char) 0, ' ');
                s = s.replace(" ", "");
                if (s.contains(";")) {
                    String[] split = s.split(";");
                    milli = Integer.parseInt(split[0].trim()) * 60 * 1000
                            + Integer.parseInt(split[1].trim()) * 1000
                            + Integer.parseInt(split[2].trim());
                    break;
                }
            }
        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(Tester.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex1) {
                Logger.getLogger(Tester.class.getName()).log(Level.SEVERE, null, ex1);
            }
            file.delete();
        }
        return milli;
    }

    private boolean checkFiles(String gsPath, int type) {
        String ext = "";
        switch (type) {
            case JAVA_FILE:
                ext = ".java";
                break;
            case CLASS_FILE:
                ext = ".class";
                break;
        }
        if (!new File(gsPath + "GS" + ext).exists()) {
            return false;
        }
        if (!new File(gsPath + "GSConstants" + ext).exists()) {
            return false;
        }
        if (!new File(gsPath + "GSTokenManager" + ext).exists()) {
            return false;
        }
        if (!new File(gsPath + "ParseException" + ext).exists()) {
            return false;
        }
        if (!new File(gsPath + "SimpleCharStream" + ext).exists()) {
            return false;
        }
        if (!new File(gsPath + "Token" + ext).exists()) {
            return false;
        }
        if (!new File(gsPath + "TokenMgrError" + ext).exists()) {
            return false;
        }
        return true;
    }

    public void close() {
        powerShell.close();
    }
}
