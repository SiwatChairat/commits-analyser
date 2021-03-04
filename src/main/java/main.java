import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class main {
    //Get all the file in the commit
    //find . -type f get all files in a folder
    static String currDir = System.getProperty("user.dir");

    private static ArrayList<String> getAllFileName (String pathToProjectRepo, String commitHead) throws IOException, InterruptedException {
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
            while ((output= stdInput.readLine()) != null && c[0].compareTo("find") == 0) {
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

    private static void copyFile (ArrayList<String> listOfFile, String pathToProjectRepo, String pathToFolder,
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
            ssh.waitFor();
            builder.directory(file);
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

    private static void getChange(ArrayList<String> list1, ArrayList<String> list2, String v1, String v2,
                                 String pathToFolder) {
        ArrayList<String> a = new ArrayList<>();
        ArrayList<String> b = new ArrayList<>();
        extractFileName(list1, a);
        extractFileName(list2, b);

        for (String string : a) {
            if (b.contains(string)) {
                String fileV1 = pathToFolder + string + v1 + ".java";
                String fileV2 = pathToFolder + string + v2 + ".java";
                System.out.println(fileV1);
                System.out.println(fileV2);
            }
        }

        String file1 = "a";
        String file2 = "b";
        File left = new File("/Users/Siwat/Desktop/Analusis/JSONReader 2.java");
        File right = new File("/Users/Siwat/Desktop/Analusis/JSONReader.java");

        FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
        try {
            distiller.extractClassifiedSourceCodeChanges(left, right);
        } catch(Exception e) {
    /* An exception most likely indicates a bug in ChangeDistiller. Please file a
       bug report at https://bitbucket.org/sealuzh/tools-changedistiller/issues and
       attach the full stack trace along with the two files that you tried to distill. */
            System.err.println("Warning: error while change distilling. " + e.getMessage());
        }
        List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
        if(changes != null) {
            for(SourceCodeChange change : changes) {
                System.out.println(change.getLabel());
                System.out.println(change.getChangedEntity());
                System.out.println(change.getSignificanceLevel());
            }
        }
    }

    private static void run () throws IOException, InterruptedException {
        String pathToProjectRepo = "/Users/Siwat/Desktop/UCL/3rd year/CS final project/myApp" +
                "/analyser/gitProjects/fastjson";
        String pathToFolder = "/Users/Siwat/Desktop/Analusis/testProject/testChange";
        ArrayList<String> a = getAllFileName(pathToProjectRepo, "62787d9bfa0c7c674ebba69db5cdbeb85e9893d6");
        ArrayList<String> b = getAllFileName(pathToProjectRepo, "1a4fb9bb29188ba8474a20868ff8eda6d9b48317");
        copyFile(a, pathToProjectRepo, pathToFolder, "1");
        copyFile(b, pathToProjectRepo, pathToFolder, "2");
        getChange(a, b, "1", "2", pathToFolder);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        run();
    }


}
