package org.monolith.exp.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessWatcher {

    private Logger logger;
    private String filename;
    private String filepath;

    public ProcessWatcher(String filepath, String filename, Logger logger) {
        this.filepath = filepath;
        this.filename = filename;
        this.logger = logger;
    }

    public static boolean isUnix() {
        return SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC;
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private Boolean checkIfRun() throws IOException, InterruptedException {
        if (isUnix()) {
            System.out.println("Unix detected!");
            ProcessBuilder pb = new ProcessBuilder("ps aux | grep " + filename);
            Process proc = pb.start();
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            Boolean isRun = false;
            while((line = br.readLine()) != null) {
                if (line.contains(filename + ".exe")) {
                    isRun = true;
                };
            }
            return isRun;
        } else if (isWindows()) {
            System.out.println("Windows detected!");
            return readInThread("wmic", "process where name=\'" + filename + "\' list brief");
        }

        return false;
    }

    public class ReadOutputThread extends Thread {

        private InputStream is;
        private Boolean status;

        final String emptyString = "No Instance(s) Available.";
        final String successString = "Method execution successful.";

        public ReadOutputThread(InputStream inputStream) {
            is = inputStream;
        }

        public Boolean getStatus() {
            return status;
        }

        @Override
        public void run() {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            Boolean end = false;
            try {
                br.readLine();
                br.readLine();
                br.readLine();
                br.readLine();
                while ((line = br.readLine()) != null) {
                    System.out.println("read line=" + line);
                    if (line.trim().contains(filename)) {
                        System.out.println("Process exist=" + line);
                        logger.log(Level.INFO, "Process exist=" + line);
                        status = true;
                        end = true;
                    }
                    //процесс не найден
                    if (line.trim().contains(emptyString)) {
                        logger.log(Level.INFO, "Process not found.");
                        status = false;
                        end = true;
                    }

                    //успешное выполнение команды
                    if ( line.trim().contains(successString)) {
                        logger.log(Level.INFO, "Command executed successfully.");
                        status = true;
                        end = true;
                    }

                    if (end) {
                        isr.close();
                        br.close();
                        return;
                    }

                }
                isr.close();
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, e.toString());
            } finally {
                try {
                    isr.close();
                    br.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Process Watcher error:" + e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    public Boolean readInThread(String utilName, String command) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder("cmd");
        pb.redirectErrorStream(true);
        pb.directory(new File("C:\\"));
        Process proc = pb.start();

        ReadOutputThread t = new ReadOutputThread(proc.getInputStream());
        t.setDaemon(true);
        t.start();

        OutputStream os = proc.getOutputStream();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        System.out.println("run cmd=" + utilName + " " + command);
        writer.write(utilName + " " + command);
        writer.newLine();
        writer.flush();
        writer.write("EXIT");
        proc.waitFor();
        System.out.println("end=" + t.getStatus());
        return t.getStatus();
    }

    public void killProcess() throws InterruptedException, IOException {
        if (isUnix()) {
            //TODO
        } else if (isWindows()) {
            readInThread("wmic", "process where name=\"" + filename + "\" call terminate");
            System.out.println("kill process->" + filename);
        }
    }

    public void runProcess() throws IOException, InterruptedException {

        Thread waitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Check if process already launched!");
                    if (!checkIfRun()) {
                        System.out.println("Process not launched, try to run=" + filepath + filename);
                        File testFile = new File(filepath + filename);
                        if (testFile.exists()) {
                            ProcessBuilder pb = new ProcessBuilder(filepath + filename);
                            pb.directory(new File(filepath));
                            Process p = pb.start();
                            System.out.println("process started->" + filename);
                            p.waitFor();
                            System.out.println("process terminated->" + filename + ", rerun!");
                            runProcess();
                        } else {
                            logger.log(Level.SEVERE, "Chrome Web Driver launcher is not found!");
                            System.out.println("Chrome Web Driver launcher is not found!");
                        }
                    } else {
                        System.out.println("Chrome Web Driver already launched!");
                        killProcess();
                        runProcess();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.log(Level.SEVERE, "Chrome Web Driver launch error!");
                } catch (InterruptedException e) {
                    System.out.println("Watch thread interrupted!");
                    e.printStackTrace();
                    try {
                        runProcess();
                    } catch (IOException | InterruptedException ex) {
                        e.printStackTrace();
                        System.out.println("Watch thread interrupted!");
                        logger.log(Level.SEVERE, "IO Exception");
                    }
                }
            }
        });

        System.out.println("run process->" + filepath + filename);
        waitThread.start();
        System.out.println("run process complete->" + filepath + filename);
    }
}
