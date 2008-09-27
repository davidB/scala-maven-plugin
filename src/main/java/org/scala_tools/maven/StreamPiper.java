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

import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.plexus.util.IOUtil;

public class StreamPiper extends Thread {

    private InputStream in_;
    private OutputStream out_;

    public StreamPiper(InputStream in, OutputStream out) {
        in_ = in;
        out_ = out;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[512];
            int bytes_read;
            while (true) {
                bytes_read = in_.read(buffer);
                if (bytes_read != -1) {
                    out_.write(buffer, 0, bytes_read);
                    out_.flush();
                }
//                System.err.print("sleep");
                if (bytes_read < 1) {
                    yield();
                    sleep(500l);
                }
            }
        } catch (InterruptedException exc) {
            System.err.print("stop by interrupt");
            return;
        } catch (Exception exc) {
            System.err.print("!!!! exc !!!");
            exc.printStackTrace();
            throw new RuntimeException("wrap: " + exc.getMessage(), exc);
        } finally {
            IOUtil.close(in_);
            IOUtil.close(out_);
        }
    }
}
