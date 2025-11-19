import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

object SeedManager {
  private val fileName = "seed.txt"
  private val defaultSeed = 42

  def loadSeed(): Long = {
    val file = new File(fileName)
    if (!file.exists()) saveSeed(defaultSeed)

    Try {
      val src = Source.fromFile(file)
      val seed = src.getLines().next().toLong
      src.close()
      seed
    }.getOrElse(defaultSeed)
  }

  def saveSeed(seed: Long): Unit = {
    val writer = new PrintWriter(new File(fileName))
    writer.println(seed)
    writer.close()
  }

  def nextSeed(current: Long): Long = {
    val (_, next) = MyRandom(current).nextInt
    next.seed
  }

  def getNextRandom(): MyRandom = {
    val current = loadSeed()
    val next = nextSeed(current)
    saveSeed(next)
    MyRandom(current)
  }
}
