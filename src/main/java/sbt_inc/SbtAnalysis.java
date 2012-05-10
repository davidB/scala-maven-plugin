package sbt_inc;

import java.io.File;
import sbt.compiler.IC;
import sbt.inc.Analysis;
import xsbti.Maybe;

public class SbtAnalysis {

    public static final Analysis EMPTY_ANALYSIS = sbt.inc.Analysis$.MODULE$.Empty();

    public static final Maybe<Analysis> JUST_EMPTY_ANALYSIS = Maybe.just(EMPTY_ANALYSIS);

    public static Maybe<Analysis> getAnalysis(File file, File classesDirectory) {
        if (file.getName().endsWith("jar")) {
            return JUST_EMPTY_ANALYSIS;
        } else if (file.equals(classesDirectory)) {
            return JUST_EMPTY_ANALYSIS;
        } else if (file.exists() && file.isDirectory()) {
            File analysisFile = cacheLocation(file);
            if (analysisFile.exists()) {
                return Maybe.just(IC.readAnalysis(cacheLocation(file)));
            } else {
                return JUST_EMPTY_ANALYSIS;
            }
        } else {
            return JUST_EMPTY_ANALYSIS;
        }
    }

    public static File cacheLocation(File file) {
        return new File(new File(file.getParent(), "cache"), file.getName());
    }
}
