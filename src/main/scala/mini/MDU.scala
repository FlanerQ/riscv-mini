package mini
import chisel3._
import chisel3.util._

object MDU {
  val MDU_NONE = 0.U(3.W)
  val MDU_MUL = 1.U(3.W)
  // other operations ...
}

import MDU._

class MDUIO(width: Int) extends Bundle {
  val op = Input(UInt(3.W))
  val A = Input(UInt(32.W))
  val B = Input(UInt(32.W))
  val out = Output(UInt(32.W))
}
class MDU(width: Int) extends Module {
  val io = IO(new MDUIO(width))
  io.out := 0.U
  when(io.op === MDU.MDU_MUL) {
    io.out := io.A * io.B
  }
}
