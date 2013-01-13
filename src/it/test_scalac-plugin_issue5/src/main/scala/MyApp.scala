import edu.berkeley.cs.avro.marker._
import edu.berkeley.cs.avro.runtime._
 
 
 
 
object MyApp extends App {
  case class IntRec(var f1: Int) extends AvroRecord
  val outfile = AvroOutFile[IntRec](new java.io.File("ints.avro"))
  (1 to 1024).foreach(i => outfile.append(IntRec(i)))
  outfile.close
}
