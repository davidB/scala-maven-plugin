import scala.tools.nsc
import nsc._
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

import java.{io => jio}
/**
 * Our privatesetter plugin class that contributes a new phase to the compiler.
 */
class TestPlugin(val global : Global) extends Plugin {

  val name = "test-plugin"
  val description = "allows vars to have private setters"
  val components = List[PluginComponent](VarAccessChanger)

  
  /** Plugin component to complete dependency analysis after a build and write out dependnecies for next build */
  private object VarAccessChanger extends PluginComponent {
    val global = TestPlugin.this.global
    val runsAfter = "typer"
    val phaseName = "test-phase"
    def newPhase(prev: Phase) = new MakeSettersPrivatePhase(prev)
    def name = phaseName
        
    /** The actual phase the removes units from being compiled that are up-to-date */
    class MakeSettersPrivatePhase(prev: Phase) extends Phase(prev) {
      
      override def name = VarAccessChanger.this.name
      import global._
      /**
       * Called when our plugin is running.
       */
      override def run {
		val outdir = new jio.File(global.settings.outdir.value)
		if(!outdir.isDirectory) {
        	outdir.mkdirs()
        }
		val file = new jio.File(outdir, "test-plugin.out")
		Console.println("Writing file: " + file)      
		if(!file.exists()) {
        	file.createNewFile()
        }
      }
      
    }
  }
}
