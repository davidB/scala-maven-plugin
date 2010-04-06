package org_scala_tools_maven_cs;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import org_scala_tools_maven.ScalaMojoSupport;
import org_scala_tools_maven_executions.JavaMainCaller;
import org_scala_tools_maven_executions.JavaMainCallerByFork;
import org_scala_tools_maven_executions.MainHelper;

/**
 * ScalacsClient is a client used to send request to a scalacs running server.
 *
 * @author davidB
 */
public class ScalacsClient {
    public static Pattern linePattern = Pattern.compile("^-(INFO|WARN|ERROR)\t([^\t]*)\t([^\t]*)\t(.*)$");
    public static Pattern locationPattern = Pattern.compile("([^#]*)#(\\d+),(\\d+),(\\d+),(\\d+)");

    public enum Level {INFO, WARN, ERROR};

    public static class LogEvent {
        public Level level = Level.INFO;
        public String category = "";
        public File file = null;
        public int line = 0;
        public int column = 0;
        public int offset = 0;
        public int length = 0;
        public CharSequence text = "";

        @Override
        public String toString() {
            return level + "*" + category + "*" + file + "*" + line + "*" + column + "*" + offset + "*"+ length + "*"+ text+ "*";
        }
    }

    private Log _log;
    private ScalaMojoSupport _mojo;
    private String _csVersion;
    private String[] _jvmArgs;

    public ScalacsClient(ScalaMojoSupport mojo, String csVersion, String[] jvmArgs) {
        _log = mojo.getLog();
        _mojo = mojo;
        _csVersion = csVersion;
        _jvmArgs = jvmArgs;
    }

    public List<LogEvent> parse(String response) throws Exception {
        List<LogEvent> back = new LinkedList<LogEvent>();
        BufferedReader in = new BufferedReader(new StringReader(response));
        try {
            for(String l = in.readLine(); l != null; l =in.readLine()){
                Matcher m = linePattern.matcher(l);
                if (m.matches()) {
                    LogEvent e = new LogEvent();
                    e.level = Level.valueOf(m.group(1).toUpperCase());
                    e.category = m.group(2);
                    e.text = m.group(4).replace('§', '\n');
                    Matcher ml = locationPattern.matcher(m.group(3));
                    if (ml.matches()) {
                        e.file = new File(ml.group(1));
                        e.line = Integer.parseInt(m.group(2));
                        e.column = Integer.parseInt(m.group(3));
                        e.offset = Integer.parseInt(m.group(4));
                        e.length = Integer.parseInt(m.group(5));
                    }
                    back.add(e);
                }
            }
        } finally {
            IOUtil.close(in);
        }
        return back;
    }

    /**
     * request to createOrUpdate one or more project define in the Yaml syntax, each project definition should be separated by "---"
     * @return the output (log) of the request
     * @throws Exception
     */
    public String sendRequestCreateOrUpdate(String yamlDef) throws Exception {
        String back = "";
        try {
            back = sendRequest("createOrUpdate", yamlDef);
        } catch (java.net.ConnectException exc) {
            startNewServer();
            back = sendRequest("createOrUpdate", yamlDef);
        }
        return back;
    }

    /**
     * @return the output (log) of the request
     * @throws Exception
     */
    public String sendRequestRemove(String projectName) throws Exception {
        return sendRequest("remove?p=" + projectName, null);
    }

    /**
     *
     * @return the output (log) of the request
     * @throws Exception
     */
    public String sendRequestCompile(String projectName, boolean withDependencies, boolean withDependent) throws Exception {
        StringBuilder query = new StringBuilder("compile?");
        if (StringUtils.isNotEmpty(projectName)) {
            query.append("p=").append(projectName);
            if (!withDependencies) {
                query.append("&noDependencies=true");
            }
            if (!withDependent) {
                query.append("&noDependent=true");
            }
        }
        return sendRequest(query.toString(), null);
    }

    /**
     *
     * @return the output (log) of the request
     * @throws Exception
     */
    public String sendRequestClean() throws Exception {
        return sendRequest("clean", null);
    }

    /**
     *
     * @return the output (log) of the request
     * @throws Exception
     */
    public String sendRequestStop() throws Exception {
        return sendRequest("stop", null);
    }

    protected String sendRequest(String action, String data) throws Exception {
        URL url = new URL("http://127.0.0.1:27616/" + action);
        traceUrl(url);
        URLConnection cnx = url.openConnection();
        cnx.setDoOutput(StringUtils.isNotEmpty(data));
        cnx.setDoInput(true);
        if (StringUtils.isNotEmpty(data)) {
            OutputStream os = cnx.getOutputStream();
            try {
                IOUtil.copy(new StringReader(data), os);
            } finally {
                IOUtil.close(os);
            }
        }
        InputStream is = cnx.getInputStream();
        try {
            String back = IOUtil.toString(is);
            return back;
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Implementation could override this method if it want to print, log, url requested
     *
     * @throws Exception
     */
    public void traceUrl(URL url) throws Exception {
        _log.debug("request : " + url);
    }

    /**
     * Implementation should provide a way to startNewServer (used if call sendRequestCreateOrUpdate and no server is up)
     *
     * @throws Exception
     */
    public void startNewServer() throws Exception{
        _log.info("start scala-tools-server...");
        Set<String> classpath = new HashSet<String>();
        //_mojo.addToClasspath("net.alchim31", "scalacs", _csVersion, classpath, true);
        //JavaMainCaller jcmd = new JavaMainCallerByFork(_mojo, "net_alchim31_scalacs.HttpServer", MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()])), null, null, false);

        _mojo.addToClasspath("org.scala-tools.sbt", "sbt-launch", "0.7.2", classpath, true);
        File scalaCsBootConf = installConf(new File(System.getProperty("user.home"), ".sbt-launch/scalacs-"+ _csVersion +".boot.properties"));
        JavaMainCaller jcmd = new JavaMainCallerByFork(_mojo, "xsbt.boot.Boot", MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()])), _jvmArgs, new String[]{scalaCsBootConf.getCanonicalPath()}, false);
        jcmd.spawn(_mojo.displayCmd);
        boolean started = false;
        for(int i = 60; i>0 && !started; i--) {
            try {
                System.out.print(".");
                Thread.sleep(1000);
                sendRequest("ping", null);
                started = true;
                System.out.println("\n started");
            } catch (java.net.ConnectException exc) {
                started = false; //useless but more readable
            }
        }
        if (!started) {
            throw new IllegalStateException("can't start and connect to scalacs");
        }
    }

    private File installConf(File scalaCsBootConf) throws Exception {
        if (!scalaCsBootConf.isFile()) {
            scalaCsBootConf.getParentFile().mkdirs();
            InputStream is = null;
            StringWriter sw = new StringWriter();
            try {
                is = this.getClass().getResourceAsStream("scalacs.boot.properties");
                IOUtil.copy(is, sw);
            } finally {
                IOUtil.close(is);
                IOUtil.close(sw);
            }
            Properties p = new Properties(System.getProperties());
            p.setProperty("scalacs.version", _csVersion);
            p.setProperty("scalacs.directory", scalaCsBootConf.getParentFile().getCanonicalPath());
            String cfg = StringUtils.interpolate(sw.toString(), p);
            FileUtils.fileWrite(scalaCsBootConf.getCanonicalPath(), "UTF-8", cfg);
        }
        return scalaCsBootConf;
    }
}
