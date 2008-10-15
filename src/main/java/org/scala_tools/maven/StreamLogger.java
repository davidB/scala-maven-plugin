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

import com.sun.org.apache.xalan.internal.xsltc.dom.MatchingIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

public class StreamLogger extends Thread {
	private static final String LS = System.getProperty("line.separator");
	private static final boolean emacsMode = StringUtils.isNotEmpty(System.getProperty("emacsMode"));
	private static final boolean javaMode = StringUtils.isNotEmpty(System.getProperty("javaMode"));

    private InputStream in_;
    private Log log_;
    private boolean isErr_;
    private PrintWriter out_;
    private Pattern pattern_ = Pattern.compile("\\s*((/|\\w:).+?):(\\d+):\\s*(?:Error|error|Warning|warning|Caution|caution):\\s(.+)");

    public StreamLogger(InputStream in, Log log, boolean isErr) {
        in_ = in;
        log_ = log;
        isErr_ = isErr;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in_));
            String line = null;
            StringBuilder sb = null;
            while ((line = reader.readLine()) != null) {
                if (isErr_) {
                	if (!emacsMode && !javaMode) {
                        log_.warn(line);
                	} else {
                		if (sb == null) {
                			sb = new StringBuilder("Compilation failure"+ LS);
                		}
                        if (javaMode) {
                            Matcher matcher = pattern_.matcher(line);
                            if (matcher.matches()) {
                                line = matcher.group(1)+":["+matcher.group(3) +",1] " + matcher.group(4);
                            }
                        }
                    	sb.append(LS + line);
                	}
                } else {
                    log_.info(line);
                }
            }
            if (sb != null) {
            	log_.warn(sb.toString());
            }
        } catch(IOException exc) {
            throw new RuntimeException("wrap: " + exc.getMessage(), exc);
        } finally {
            IOUtil.close(reader);
            IOUtil.close(in_);
            IOUtil.close(out_);
        }
    }
}
