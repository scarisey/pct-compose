package dev.carisey.pctcompose

import os.{ProcGroup, ProcessOutput, SubProcess}
import scala.util.chaining.*
import os.ProcessInput

type OsCommands = List[OsCommand]
trait Executor {
  def apply(osCommand: OsCommands): List[String]
}
final case class OsCommand private (line: List[String], f: List[String] => List[String] = identity) {
  def execute(): Unit = OsCommand.execute(this)
  def map(f: List[String] => List[String]): OsCommand = OsCommand(line, this.f.andThen(f))
  def andPrint(): OsCommand = this.map(_.tapEach(println))
  override def equals(x: Any): Boolean = x match
    case x: OsCommand => x.line == this.line
    case _            => false
}
object OsCommand {
  private val pctCreate = List("pct", "create")
  private val iptables = List("iptables")
  def apply(args: String*): OsCommand = new OsCommand(args.toList)
  def apply(line: List[String]): OsCommand = new OsCommand(line)

  def pctCreate(args: String*): OsCommand = new OsCommand(pctCreate ::: args.toList)
  def iptables(args: String*): OsCommand = new OsCommand(iptables ::: args.toList)

  def execute(osCommand: OsCommand): List[String] = simpleExecutor(osCommand)
  def execute(osCommands: OsCommands): List[String] = chainableExecutor(osCommands)
  def forEach(osCommands: OsCommands): List[String] = forEachExecutor(osCommands)

  private val simpleExecutor: OsCommand => List[String] = cmd => {
    val result = scala.collection.mutable.Queue.empty[String]
    os.proc(cmd.line).call(stdout = ProcessOutput.Readlines(s => result.enqueue(s)))
    cmd.f(result.dequeueAll(_ => true).toList)
  }

  private val forEachExecutor: Executor = cmds => cmds.flatMap(cmd => simpleExecutor(cmd).tapEach(println))

  private val exectuteWithStdIn: (OsCommand, List[String]) => List[String] = (cmd, in) => {
    val result = scala.collection.mutable.Queue.empty[String]
    os.proc(cmd.line)
      .call(stdin = ProcessInput.makeSourceInput(in.mkString("\n")), stdout = ProcessOutput.Readlines(s => result.enqueue(s)))
    cmd.f(result.dequeueAll(_ => true).toList)
  }

  private val chainableExecutor: Executor = {
    case first :: second :: tail =>
      val result = scala.collection.mutable.Queue.empty[String]
      tail.foldLeft {
        val res1 = simpleExecutor(first)
        exectuteWithStdIn(second, res1)
      } { case (in, cmd) => exectuteWithStdIn(cmd, in) }
    case head :: Nil => simpleExecutor(head)
    case _           => List.empty
  }
}
