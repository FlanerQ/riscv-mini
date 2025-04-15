// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._
import mini.Control._

// Basic Branch Predictor interface
class BranchPredictorIO(xlen: Int) extends Bundle {
  // Inputs for prediction stage
  val fetch_pc = Input(UInt(xlen.W))
  val inst = Input(UInt(32.W))

  // Inputs for update stage
  val exec_pc = Input(UInt(xlen.W))
  val exec_inst = Input(UInt(32.W))
  val exec_br_type = Input(UInt(3.W))
  val exec_taken = Input(Bool())
  val exec_target = Input(UInt(xlen.W))
  val update = Input(Bool())

  // Outputs for prediction
  val predict_taken = Output(Bool())
  val predict_target = Output(UInt(xlen.W))
}

// 2-bit saturating counter states
object BranchCounterState {
  val STRONGLY_NOT_TAKEN = 0.U(2.W)
  val WEAKLY_NOT_TAKEN = 1.U(2.W)
  val WEAKLY_TAKEN = 2.U(2.W)
  val STRONGLY_TAKEN = 3.U(2.W)
}

// Branch Predictor implementation with BTB and 2-bit saturating counter PHT
class BranchPredictor(val xlen: Int, val entries: Int = 32) extends Module {
  val io = IO(new BranchPredictorIO(xlen))

  // Branch Target Buffer
  // Contains target address for predicted taken branches
  class BTBEntry extends Bundle {
    val valid = Bool()
    val pc = UInt(xlen.W)
    val target = UInt(xlen.W)
  }

  // Pattern History Table
  // Contains 2-bit saturating counters for branch prediction
  val btb = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(new BTBEntry))))
  val pht = RegInit(VecInit(Seq.fill(entries)(BranchCounterState.WEAKLY_NOT_TAKEN)))

  // Compute index for fetch and execute stages
  val fetch_idx = io.fetch_pc(log2Ceil(entries) + 1, 2)
  val exec_idx = io.exec_pc(log2Ceil(entries) + 1, 2)

  // Is the fetched instruction a branch?
  val fetch_is_branch = io.inst(6, 0) === "b1100011".U

  // Check if BTB entry is valid and matches current PC
  val btb_hit = btb(fetch_idx).valid && btb(fetch_idx).pc === io.fetch_pc

  // Make branch prediction
  import BranchCounterState._
  val counter = pht(fetch_idx)
  val predict_taken = fetch_is_branch && btb_hit && (counter === WEAKLY_TAKEN || counter === STRONGLY_TAKEN)

  io.predict_taken := predict_taken
  io.predict_target := Mux(predict_taken, btb(fetch_idx).target, 0.U)

  // Update branch predictor
  when(io.update && io.exec_br_type =/= BR_XXX) {
    // Update BTB
    when(io.exec_taken) {
      btb(exec_idx).valid := true.B
      btb(exec_idx).pc := io.exec_pc
      btb(exec_idx).target := io.exec_target
    }

    // Update PHT counter using 2-bit saturating counter
    when(io.exec_taken) {
      pht(exec_idx) := MuxCase(
        pht(exec_idx),
        Array(
          (pht(exec_idx) === STRONGLY_NOT_TAKEN) -> WEAKLY_NOT_TAKEN,
          (pht(exec_idx) === WEAKLY_NOT_TAKEN) -> WEAKLY_TAKEN,
          (pht(exec_idx) === WEAKLY_TAKEN) -> STRONGLY_TAKEN,
          (pht(exec_idx) === STRONGLY_TAKEN) -> STRONGLY_TAKEN
        ).toIndexedSeq
      ) // Convert Array to IndexedSeq explicitly
    }.otherwise {
      pht(exec_idx) := MuxCase(
        pht(exec_idx),
        Array(
          (pht(exec_idx) === STRONGLY_TAKEN) -> WEAKLY_TAKEN,
          (pht(exec_idx) === WEAKLY_TAKEN) -> WEAKLY_NOT_TAKEN,
          (pht(exec_idx) === WEAKLY_NOT_TAKEN) -> STRONGLY_NOT_TAKEN,
          (pht(exec_idx) === STRONGLY_NOT_TAKEN) -> STRONGLY_NOT_TAKEN
        ).toIndexedSeq
      ) // Convert Array to IndexedSeq explicitly
    }
  }
}
