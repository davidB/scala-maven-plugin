package org.scala_tools.maven;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VersionNumber /*implements Serializable*/ implements Comparable<VersionNumber>{
    private static Pattern regexp_ = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?(-\\w+)?");

    public int major;
    public int minor;
    public int bugfix;
    public String modifier;

    public VersionNumber(){
    }

    public VersionNumber(String s) {
        Matcher match = regexp_.matcher(s);
        if (!match.find()) {
            throw new IllegalArgumentException("invalid versionNumber : major.minor(.bugfix)(-modifier) :" + s);
        }
        major = Integer.parseInt(match.group(1));
        minor = Integer.parseInt(match.group(2));
        if ((match.group(3) != null) && (match.group(3).length() > 1)){
            bugfix = Integer.parseInt(match.group(3).substring(1));
        }
        if ((match.group(4) != null) && (match.group(4).length() > 1)){
            modifier = match.group(4).substring(1);
        }
    }

    @Override
    public String toString() {
         StringBuilder str = new StringBuilder();
         str.append(major)
                 .append('.')
                 .append(minor)
                 .append('.')
                 .append(bugfix)
                 ;
         if ((modifier != null) && (modifier.length() > 0)){
             str.append('-').append(modifier);
         }
         return str.toString();
     }

    /**
     * Doesn't compare modifier
     */
    public int compareTo(VersionNumber o) {
        int back = 0;
        if ((back == 0) && (major > o.major)) {
            back = 1;
        }
        if ((back == 0) && (major < o.major)) {
            back = -1;
        }
        if ((back == 0) && (minor > o.minor)) {
            back = 1;
        }
        if ((back == 0) && (minor < o.minor)) {
            back = -1;
        }
        if ((back == 0) && (bugfix > o.bugfix)) {
            back = 1;
        }
        if ((back == 0) && (bugfix < o.bugfix)) {
            back = -1;
        }
        return back;
    }


}
