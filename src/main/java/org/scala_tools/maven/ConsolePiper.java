/*
 * Copyright 2007 scala-tools.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
 package org.scala_tools.maven;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;

import jline.ClassNameCompletor;
import jline.ConsoleReader;
import jline.FileNameCompletor;
import jline.SimpleCompletor;

import org.codehaus.plexus.util.IOUtil;

/**
 * Use Jline to manage input stream, to provide arrow support (history and line navigation).
 *
 * @author David Bernard (dwayne)
 * @created 2007
 */
public class ConsolePiper extends Thread {
    private ConsoleReader console_;
    private PrintWriter processInput_;
    private InputStream processOutput_;

    public ConsolePiper(Process p) throws Exception {
        console_ = new ConsoleReader();
        File histoFile = new File(System.getProperty("user.home"), ".m2/scala-console.histo");
        histoFile.getParentFile().mkdirs();
        console_.getHistory().setHistoryFile(histoFile);
        console_.addCompletor(new FileNameCompletor());
        console_.addCompletor(new ClassNameCompletor());
        console_.addCompletor(new SimpleCompletor(new String [] {
                "abstract", "case", "catch", "class", "def",
                "do", "else", "extends", "false", "final",
                "finally", "for", "if", "implicit", "import", "lazy",
                "match", "new", "null", "object", "override",
                "package", "private", "protected", "requires", "return",
                "sealed", "super", "this", "throw", "trait",
                "try", "true", "type", "val", "var",
                "while", "with", "yield"
                }
            )
        );
        processInput_ = new PrintWriter(p.getOutputStream());
//        processOutput_ = p.getInputStream();
//        if (StringUtils.isNotEmpty(firstInput)) {
//            firstInput_ = firstInput;
//        }
    }

    @Override
    public void run() {
        try {
            while (true) {
//                // wait for prompt from process
//                do {
//                    bytes_read = processOutput_.read(buffer);
//                    if (bytes_read != -1) {
//                        System.out.write(buffer, 0, bytes_read);
//                        System.out.flush();
//                    }
//                } while (processOutput_.available() > 0);
//                if ((bytes_read > 1) && new String(buffer, 0, bytes_read).startsWith("scala>") && (firstInput_ != null)) {
//                    console_.putString(firstInput_);
//                    console_.printNewline();
//                    firstInput_ = null;
//                }
                processInput_.println(console_.readLine());
                processInput_.flush();
                yield();
                sleep(500l);
            }
        } catch (InterruptedException exc) {
            System.err.print("stop by interrupt");
            return;
        } catch (Exception exc) {
            System.err.print("!!!! exc !!!");
            exc.printStackTrace();
            throw new RuntimeException("wrap: " + exc.getMessage(), exc);
        } finally {
            IOUtil.close(processInput_);
            IOUtil.close(processOutput_);
        }
    }

}
