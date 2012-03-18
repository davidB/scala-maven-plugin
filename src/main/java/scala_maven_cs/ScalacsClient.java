package scala_maven_cs;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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

import scala_maven.ScalaMojoSupport;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerByFork;
import scala_maven_executions.MainHelper;
import scala_maven_executions.SpawnMonitor;

/**
 * ScalacsClient is a client used to send request to a scalacs running server.
 *
 * @author davidB
 */
public class ScalacsClient {
    public static final String BOOT_PROP_RSRC = "scalacs.boot.properties";
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
    private String[] _jvmArgs;
    private String _csGroupId;
    private String _csArtifactId;
    private String _csVersion;

    public ScalacsClient(ScalaMojoSupport mojo, String csGroupId, String csArtifactId, String csVersion, String[] jvmArgs) {
        _log = mojo.getLog();
        _mojo = mojo;
        _csGroupId = csGroupId;
        _csArtifactId = csArtifactId;
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
                    e.text = m.group(4).replace('$', '\n');
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
        StringBuilder query = new StringBuilder("compile");
        if (StringUtils.isNotEmpty(projectName)) {
            query.append("?p=").append(projectName);
            if (!withDependencies) {
                query.append("&noDependencies=true");
            }
            // not supported by scalacs 0.2
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
        String msg = "request : " + url;
        if (_mojo.displayCmd) {
            _log.info(msg);
        } else {
            _log.debug(msg);
        }
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
        String[] jvmArgs = new String[(_jvmArgs == null)?1:_jvmArgs.length + 1];
        File installDir = new File(System.getProperty("user.home"), ".sbt-launch");
        jvmArgs[0] = "-Dsbt.boot.properties="+ installConf(new File(installDir, _csArtifactId + "-"+ _csVersion +".boot.properties")).getCanonicalPath();
        if (_jvmArgs != null) {
            System.arraycopy(_jvmArgs, 0, jvmArgs, 1, _jvmArgs.length);
        }
        FileTailer tailer = new FileTailer(new File(installDir, "update.log"));
        boolean started = false;
        try {
            JavaMainCaller jcmd = new JavaMainCallerByFork(_mojo, "xsbt.boot.Boot", MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()])), jvmArgs, null, false);
            SpawnMonitor mon = jcmd.spawn(_mojo.displayCmd);
            for(int i = 60; i>0 && !started && mon.isRunning(); i--) {
                try {
                    if (_mojo.displayCmd) {
                        System.out.print(tailer.whatNew());
                    } else {
                        System.out.print(".");
                    }
                    Thread.sleep(1000);
                    sendRequest("ping", null);
                    started = true;
                } catch (java.net.ConnectException exc) {
                    started = false; //useless but more readable
                }
            }
            if (_mojo.displayCmd) {
                System.out.print(tailer.whatNew());
            }
            System.out.println("");
        } finally {
            tailer.close();
        }
        if (!started) {
            throw new IllegalStateException("can't start and connect to scalacs");
        }
        _mojo.getLog().info("scalacs connected");
    }

    private File installConf(File scalaCsBootConf) throws Exception {
        if (!scalaCsBootConf.isFile()) {
            scalaCsBootConf.getParentFile().mkdirs();
            InputStream is = null;
            StringWriter sw = new StringWriter();
            try {
                is = this.getClass().getResourceAsStream(BOOT_PROP_RSRC);
                if (is == null) {
                    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(BOOT_PROP_RSRC);
                }
                if (is == null) {
                    String abspath = "/" + this.getClass().getPackage().getName().replace('.', '/') + "/" + BOOT_PROP_RSRC;
                    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(abspath);
                    if (is == null) {
                        throw new IllegalStateException("can't find " + abspath + " in the classpath");
                    }
                }
                IOUtil.copy(is, sw);
            } finally {
                IOUtil.close(is);
                IOUtil.close(sw);
            }
            Properties p = new Properties(System.getProperties());
            p.setProperty("scalacs.groupId", _csGroupId);
            p.setProperty("scalacs.artifactId", _csArtifactId);
            p.setProperty("scalacs.version", _csVersion);
            p.setProperty("scalacs.directory", scalaCsBootConf.getParentFile().getCanonicalPath());
            String cfg = StringUtils.interpolate(sw.toString(), p);
            FileUtils.fileWrite(scalaCsBootConf.getCanonicalPath(), "UTF-8", cfg);
        }
        return scalaCsBootConf;
    }

    private static class FileTailer {
        private long _filePointer;
        private RandomAccessFile _raf;
        private File _file;
        public FileTailer(File f) throws Exception {
            _file = f;
            _filePointer = f.length();
            _raf = null;
        }

        public CharSequence whatNew() throws Exception {
            StringBuilder back = new StringBuilder();
            if (_raf == null && _file.isFile()) {
                _raf = new RandomAccessFile(_file, "r" );
            }
            if (_raf != null) {
                // Compare the length of the file to the file pointer
                long fileLength = _file.length();
                if( fileLength < _filePointer ) {
                  // Log file must have been rotated or deleted;
                  // reopen the file and reset the file pointer
                  close();
                  _raf = new RandomAccessFile(_file, "r" );
                  _filePointer = 0;
                }

                if( fileLength > _filePointer ) {
                  // There is data to read
                  _raf.seek( _filePointer );
//                  back = _raf.readUTF();

                  String line =  null;
                  while( (line = _raf.readLine())!= null ) {
                    back.append( line ).append('\n');
                  }
                  _filePointer = _raf.getFilePointer();
                }
            }
            return back;
        }
        public void close() {
            try {
                if (_raf != null) {
                    _raf.close();
                    _raf = null;
                }
            } catch(Exception e) {
                // ignore
            }
        }
    }
}
