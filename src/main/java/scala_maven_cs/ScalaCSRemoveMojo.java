package scala_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist, request compile of dependencies,...)
 * @goal cs-remove
 */
public class ScalaCSRemoveMojo extends ScalaCSMojoSupport {

    @Override
    protected CharSequence doRequest() throws Exception {
        return scs.sendRequestRemove(projectNamePattern());
    }
}
