/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility for file-based CRUD operations
 */
public class FileUtil {

    public static void deleteRecursively(File file) {
        File[] children = file.listFiles();

        for (File child : children) {
            if (child.isDirectory()) {
                deleteRecursively(child);
            } else {
                child.delete();
            }
        }

        file.delete();
    }

    public static void copyDirectoryRecursively(File sourceFolder, File destinationFolder) throws IOException {
        if (sourceFolder.isDirectory())
        {
            if (!destinationFolder.exists())
            {
                destinationFolder.mkdir();
            }

            for (String file : sourceFolder.list())
            {
                File srcFile = new File(sourceFolder, file);
                File destFile = new File(destinationFolder, file);

                //Recursive function call
                copyDirectoryRecursively(srcFile, destFile);
            }
        } else {
            //Copy the file content from one place to another
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    //Copies the specifed directory from within the provided jar file to the destination
    public static void copyDirectoryFromJar(JarFile sourceJar, String dirName, String destDir) throws IOException {
        //The root directory of the jar does not begin with a slash
        if (dirName.startsWith("/")) {
            dirName = dirName.replaceFirst("/", "");
        }

        Enumeration<JarEntry> entries = sourceJar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().startsWith(dirName + "/") && !entry.isDirectory()) {
                File destination = new File(destDir + "/" + entry.getName().substring(dirName.length() + 1));
                destination.getParentFile().mkdirs();
                try (InputStream in = sourceJar.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(destination)){
                    copyStream(in, out);
                }

            }
        }
    }

    public static void findAndReplaceInFile(File in, String pattern, String replacement) throws IOException {
        String content = new String(Files.readAllBytes(in.toPath()));
        content = content.replaceAll(pattern, replacement);
        Files.write(in.toPath(), content.getBytes());
    }

    public static void removeEmptyLinesFromFile(File in) throws IOException {
        File tempCopy = new File("tempCopy.txt");

        try (Scanner scanner = new Scanner(in); PrintWriter writer = new PrintWriter("tempCopy.txt")) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (!line.trim().isEmpty()) {
                    writer.write(line);
                    writer.write("\n");
                }
            }
        }

        in.delete();
        tempCopy.renameTo(in);
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        int read;
        byte[] bytes = new byte[1024];

        while ((read = in.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
    }
}
