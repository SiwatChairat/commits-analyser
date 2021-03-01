import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class appMain {
    //Get all the file in the commit
    //find . -type f get all files in a folder
    static String currDir = System.getProperty("user.dir");

    private static ArrayList<String> getAllFileName(String pathToProjectRepo, String commitHead)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        File file = new File(pathToProjectRepo);
        ArrayList<String[]> cmd = new ArrayList<>();
        ArrayList<String> listOfFiles = new ArrayList<>();
        builder.directory(file);
        cmd.add(new String[]{"git", "checkout", commitHead});
        cmd.add(new String[]{"find", ".", "type", "f"});
        for (String[] c : cmd) {
            builder.command(c);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            while ((output = stdInput.readLine()) != null && c[0].compareTo("find") == 0) {
                if (output.contains(".java")) {
                    listOfFiles.add(output);
                }
                //System.out.println(output);
            }
            ssh.waitFor();
        }
        System.out.println(listOfFiles.size());
        return listOfFiles;
    }

    private static void copyFile(ArrayList<String> listOfFile, String pathToProjectRepo, String pathToFolder,
                                 String v) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        File file = new File(pathToProjectRepo);
        File file1 = new File(pathToFolder);
        ArrayList<String[]> cmd = new ArrayList<>();
        builder.directory(file1);
        cmd.add(new String[]{"mkdir", v});
        for (String temp : listOfFile) {
            int lastIndex1 = temp.lastIndexOf("/") + 1;
            int lastIndex2 = temp.lastIndexOf(".");
            String fileName = temp.substring(lastIndex1, lastIndex2);
            cmd.add(new String[]{"cp", "-av", temp, pathToFolder + "/" + v + "/" + fileName + v + ".java"});
        }

        for (String[] c : cmd) {
            builder.command(c);
            Process ssh = builder.start();
            builder.directory(file);
            ssh.waitFor();
        }
    }

    private static void extractFileName(ArrayList<String> list2, ArrayList<String> b) {
        for (String string : list2) {
            int lastIndex1 = string.lastIndexOf("/") + 1;
            int lastIndex2 = string.lastIndexOf(".");
            String fileName = string.substring(lastIndex1, lastIndex2);
            b.add(fileName);
        }
    }

    private static ArrayList<String> extractHeadFromTxt(String fileName) throws IOException {
        BufferedReader bufReader = new BufferedReader(new FileReader(fileName));
        ArrayList<String> listOfLines = new ArrayList<>();
        String line = bufReader.readLine();
        while (line != null) {
            listOfLines.add(line);
            line = bufReader.readLine();
        }
        bufReader.close();
        return listOfLines;
    }

    private static void deleteFolder(String pathToFolder, String v) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        File file = new File(pathToFolder);
        builder.directory(file);
        builder.command("rm", "-rf", v);
        Process ssh = builder.start();
        ssh.waitFor();
    }

    private static ArrayList<ArrayList<String>> getChange(ArrayList<String> list1, ArrayList<String> list2, String v1, String v2,
                                               String pathToFolder)
            throws IOException {
        ArrayList<ArrayList<String>> results = new ArrayList<>();
        ArrayList<String> a = new ArrayList<>();
        ArrayList<String> b = new ArrayList<>();
        extractFileName(list1, a);
        extractFileName(list2, b);

        for (String string : a) {
            if (b.contains(string)) {
                String fileV1 = pathToFolder + "/" + v1 + "/" + string + v1 + ".java";
                String fileV2 = pathToFolder + "/" + v2 + "/" + string + v2 + ".java";

                File left = new File(fileV1);
                File right = new File(fileV2);

                FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
                try {
                    distiller.extractClassifiedSourceCodeChanges(left, right);
                } catch (Exception e) {
                    System.err.println("Warning: error while change distilling. " + e.getMessage());
                }
                List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
                if (changes != null) {
                    for (SourceCodeChange change : changes) {
                        ArrayList<String> list = new ArrayList<>();
                        String changeType = change.getChangeType().toString();
                        String changedStatement = change.getChangedEntity().toString();
                        String rootEntity = change.getRootEntity().toString();
                        String parentEntity = change.getParentEntity().toString();
                        String significanceLevel = change.getSignificanceLevel().toString();
                        list.add(changeType);
                        list.add(changedStatement);
                        list.add(parentEntity);
                        list.add(rootEntity);
                        list.add(significanceLevel);
                        System.out.println(change.getChangeType());
                        System.out.println(change.getRootEntity());
                        System.out.println(change.getParentEntity());
                        System.out.println(change.getChangedEntity());
                        System.out.println(change.getSignificanceLevel());
                        results.add(list);
                    }
                }
            }
        }
        return results;
    }

    private static void addToCsv(String pathToFolder, FileWriter csvWriter, int i, String previousHead, int v2, ArrayList<String> a, ArrayList<String> b) throws IOException {
        ArrayList<ArrayList<String>> results = getChange(a, b, Integer.toString(i), Integer.toString(v2), pathToFolder);
        for (ArrayList<String> strings : results) {
            csvWriter.append(Integer.toString(i + 1));
            csvWriter.append(",");
            csvWriter.append(previousHead);
            for (String string : strings) {
                csvWriter.append(",");
                csvWriter.append("\"");
                csvWriter.append(string);
                csvWriter.append("\"");
            }
            csvWriter.append("\n");
        }
    }

    private static void createCsvReport(String pathToProjectRepo, String pathToFolder, String fileName,
                                        ArrayList<String> listOfHead) throws IOException, InterruptedException {
        int folderToDelete = 0;
        File file = new File(fileName + ".csv");
        FileWriter csvWriter = new FileWriter(file);
        csvWriter.append("NO.");
        csvWriter.append(",");
        csvWriter.append("HEAD");
        csvWriter.append(",");
        csvWriter.append("CHANGE TYPE");
        csvWriter.append(",");
        csvWriter.append("CHANGED STATEMENT");
        csvWriter.append(",");
        csvWriter.append("PARENT ENTITY");
        csvWriter.append(",");
        csvWriter.append("ROOT ENTITY");
        csvWriter.append(",");
        csvWriter.append("SIGNIFICANCE LEVEL");
        csvWriter.append("\n");
        for (int i = 0; i < listOfHead.size(); i++) {
            System.out.println("Interval: " + i);
            String head1 = listOfHead.get(i);
            String head2 = listOfHead.get(i + 1);
            String previousHead = "";
            if(previousHead.compareTo(head1) != 0) {
                int v2 = i + 2;
                System.out.println(head1);
                ArrayList<String> a = getAllFileName(pathToProjectRepo, head1);
                copyFile(a, pathToProjectRepo, pathToFolder, Integer.toString(i));
                ArrayList<String> b = getAllFileName(pathToProjectRepo, head2);
                copyFile(b, pathToProjectRepo, pathToFolder, Integer.toString(v2));
                previousHead = head1;
                addToCsv(pathToFolder, csvWriter, i, head1, v2, a, b);
            } else {
                System.out.println(previousHead);
                int v2 = i + 2;
                ArrayList<String> a = getAllFileName(pathToProjectRepo, previousHead);
                ArrayList<String> b = getAllFileName(pathToProjectRepo, head2);
                copyFile(b, pathToProjectRepo, pathToFolder, Integer.toString(v2));
                addToCsv(pathToFolder, csvWriter, i, previousHead, v2, a, b);
            }
            csvWriter.flush();
            if (i >= folderToDelete + 2) {
                deleteFolder(pathToFolder, Integer.toString(folderToDelete));
                folderToDelete++;
            }
        }
    }

    private static void run() throws IOException, InterruptedException {
        String pathToProjectRepo = "/Users/Siwat/Desktop/UCL/3rd year/CS final project/myApp" +
                "/analyser/gitProjects/fastjson";
        String pathToFolder = "/Users/Siwat/Desktop/Analusis/testProject/testChange";
        ArrayList<String> listOfHead = extractHeadFromTxt(
                "/Users/Siwat/Desktop/Analusis/testProject/testChange/1.txt");
        createCsvReport(pathToProjectRepo, pathToFolder, "test", listOfHead);
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        run();
    }


}
