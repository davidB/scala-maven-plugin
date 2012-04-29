package sbt_inc;

import java.io.File;
import sbinary.DefaultProtocol$;
import sbinary.Format;
import sbt.CompileOptions;
import sbt.CompileSetup;
import sbt.inc.Analysis;
import static sbt.inc.AnalysisFormats.analysisFormat;
import static sbt.inc.AnalysisFormats.apisFormat;
import static sbt.inc.AnalysisFormats.existsFormat;
import static sbt.inc.AnalysisFormats.fileFormat;
import static sbt.inc.AnalysisFormats.hashStampFormat;
import static sbt.inc.AnalysisFormats.infoFormat;
import static sbt.inc.AnalysisFormats.infosFormat;
import static sbt.inc.AnalysisFormats.lastModFormat;
import static sbt.inc.AnalysisFormats.optsFormat;
import static sbt.inc.AnalysisFormats.orderFormat;
import static sbt.inc.AnalysisFormats.positionFormat;
import static sbt.inc.AnalysisFormats.problemFormat;
import static sbt.inc.AnalysisFormats.relationFormat;
import static sbt.inc.AnalysisFormats.relationsFormat;
import static sbt.inc.AnalysisFormats.setupFormat;
import static sbt.inc.AnalysisFormats.severityFormat;
import static sbt.inc.AnalysisFormats.sourceFormat;
import static sbt.inc.AnalysisFormats.stampFormat;
import static sbt.inc.AnalysisFormats.stampsFormat;
import sbt.inc.AnalysisStore;
import sbt.inc.APIs;
import sbt.inc.FileBasedStore;
import sbt.inc.Relations;
import sbt.inc.SourceInfo;
import sbt.inc.SourceInfos;
import sbt.inc.Stamp;
import sbt.inc.Stamps;
import sbt.Relation;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;
import scala.Option;
import scala.Tuple2;
import xsbti.api.Source;
import xsbti.Maybe;
import xsbti.Problem;

public class SbtAnalysis {

    public static final Analysis EMPTY_ANALYSIS = sbt.inc.Analysis$.MODULE$.Empty();

    public static final Maybe<Analysis> JUST_EMPTY_ANALYSIS = Maybe.just(EMPTY_ANALYSIS);

    public static Maybe<Analysis> analysisMap(File file, File classesDirectory) {
        if (file.getName().endsWith("jar")) {
            return JUST_EMPTY_ANALYSIS;
        } else if (file.equals(classesDirectory)) {
            return JUST_EMPTY_ANALYSIS;
        } else if (file.exists() && file.isDirectory()) {
            File analysisFile = cacheLocation(file);
            if (analysisFile.exists()) {
                return Maybe.just(SbtAnalysis.readAnalysis(cacheLocation(file)));
            } else {
                return JUST_EMPTY_ANALYSIS;
            }
        } else {
            return JUST_EMPTY_ANALYSIS;
        }
    }

    public static Analysis readAnalysis(File file) {
        AnalysisStore store = analysisStore(file);
        Option<Tuple2<Analysis, CompileSetup>> result = store.get();
        if (result.isDefined()) {
            return result.get()._1();
        } else {
            return EMPTY_ANALYSIS;
        }
    }

    public static AnalysisStore analysisStore(File file) {
        Format<String> formatString = DefaultProtocol$.MODULE$.StringFormat();
        Format<File> formatFile = fileFormat();
        Format<Stamp> formatStamp = stampFormat(hashStampFormat(), lastModFormat(), existsFormat());
        Format<Map<File, Stamp>> formatMapFileStamp = DefaultProtocol$.MODULE$.immutableMapFormat(formatFile, formatStamp);
        Format<Map<File, String>> formatMapFileString = DefaultProtocol$.MODULE$.immutableMapFormat(formatFile, formatString);
        Format<Stamps> formatStamps = stampsFormat(formatMapFileStamp, formatMapFileStamp, formatMapFileStamp, formatMapFileString);
        Format<Map<File, Source>> formatMapFileSource = DefaultProtocol$.MODULE$.immutableMapFormat(formatFile, sourceFormat());
        Format<Map<String, Source>> formatMapStringSource = DefaultProtocol$.MODULE$.immutableMapFormat(formatString, sourceFormat());
        Format<APIs> formatAPIs = apisFormat(formatMapFileSource, formatMapStringSource);
        Format<Set<File>> formatSetFile = DefaultProtocol$.MODULE$.immutableSetFormat(formatFile);
        Format<Set<String>> formatSetString = DefaultProtocol$.MODULE$.immutableSetFormat(formatString);
        Format<Map<File, Set<File>>> formatMapFileSetFile = DefaultProtocol$.MODULE$.immutableMapFormat(formatFile, formatSetFile);
        Format<Map<File, Set<String>>> formatMapFileSetString = DefaultProtocol$.MODULE$.immutableMapFormat(formatFile, formatSetString);
        Format<Map<String, Set<File>>> formatMapStringSetFile = DefaultProtocol$.MODULE$.immutableMapFormat(formatString, formatSetFile);
        Format<Relation<File, File>> formatRFF = relationFormat(formatMapFileSetFile, formatMapFileSetFile);
        Format<Relation<File, String>> formatRFS = relationFormat(formatMapFileSetString, formatMapStringSetFile);
        Format<Relations> formatRelations = relationsFormat(formatRFF, formatRFF, formatRFF, formatRFS, formatRFS);
        Format<Problem> formatProblem = problemFormat(positionFormat(), formatString, severityFormat());
        Format<SourceInfo> formatSourceInfo = infoFormat(formatProblem);
        Format<Map<File, SourceInfo>> formatMapFileSourceInfo = DefaultProtocol$.MODULE$.immutableMapFormat(formatFile, formatSourceInfo);
        Format<SourceInfos> formatSourceInfos = infosFormat(formatMapFileSourceInfo);
        Format<Analysis> formatAnalysis = analysisFormat(formatStamps, formatAPIs, formatRelations, formatSourceInfos);
        Format<CompileOptions> formatOptions = optsFormat(formatString);
        Format<CompileSetup> formatSetup = setupFormat(formatFile, formatOptions, formatString, orderFormat());

        return FileBasedStore.apply(file, formatAnalysis, formatSetup);
    }

    public static File cacheLocation(File file) {
        return new File(new File(file.getParent(), "cache"), file.getName());
    }
}
