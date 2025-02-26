// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._

object Control {
  val Y = true.B
  val N = false.B

  // pc_sel
  val PC_4 = 0.U(2.W)
  val PC_ALU = 1.U(2.W)
  val PC_0 = 2.U(2.W)
  val PC_EPC = 3.U(2.W)

  // A_sel
  val A_XXX = 0.U(1.W)
  val A_PC = 0.U(1.W)
  val A_RS1 = 1.U(1.W)

  // B_sel
  val B_XXX = 0.U(1.W)
  val B_IMM = 0.U(1.W)
  val B_RS2 = 1.U(1.W)

  // imm_sel
  val IMM_X = 0.U(3.W)
  val IMM_I = 1.U(3.W)
  val IMM_S = 2.U(3.W)
  val IMM_U = 3.U(3.W)
  val IMM_J = 4.U(3.W)
  val IMM_B = 5.U(3.W)
  val IMM_Z = 6.U(3.W)

  // br_type
  val BR_XXX = 0.U(3.W)
  val BR_LTU = 1.U(3.W)
  val BR_LT = 2.U(3.W)
  val BR_EQ = 3.U(3.W)
  val BR_GEU = 4.U(3.W)
  val BR_GE = 5.U(3.W)
  val BR_NE = 6.U(3.W)

  // st_type
  val ST_XXX = 0.U(2.W)
  val ST_SW = 1.U(2.W)
  val ST_SH = 2.U(2.W)
  val ST_SB = 3.U(2.W)

  // ld_type
  val LD_XXX = 0.U(3.W)
  val LD_LW = 1.U(3.W)
  val LD_LH = 2.U(3.W)
  val LD_LB = 3.U(3.W)
  val LD_LHU = 4.U(3.W)
  val LD_LBU = 5.U(3.W)

  // wb_sel
  val WB_ALU = 0.U(2.W)
  val WB_MEM = 1.U(2.W)
  val WB_PC4 = 2.U(2.W)
  val WB_CSR = 3.U(2.W)

  // MDU
  val MDU_XXX = false.B
  val MDU_Y = true.B

  import Alu._
  import Instructions._

  import MDU._
  // format: off
  val default =
  //                                                              kill                        wb_en  illegal?
  //             pc_sel  A_sel   B_sel  imm_sel   alu_op   br_type  |  st_type ld_type wb_sel  | csr_cmd |
  //               |       |       |     |          |          |    |     |       |       |    |  |      |
             List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, Y, MDU_NONE, MDU_XXX)
  val map = Array(
    LUI   -> List(PC_4  , A_PC,   B_IMM, IMM_U, ALU_COPY_B, BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    AUIPC -> List(PC_4  , A_PC,   B_IMM, IMM_U, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    JAL   -> List(PC_ALU, A_PC,   B_IMM, IMM_J, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_XXX, WB_PC4, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    JALR  -> List(PC_ALU, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_XXX, WB_PC4, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    BEQ   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_EQ , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    BNE   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_NE , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    BLT   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_LT , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    BGE   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_GE , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    BLTU  -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_LTU, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    BGEU  -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_GEU, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    LB    -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LB , WB_MEM, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    LH    -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LH , WB_MEM, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    LW    -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LW , WB_MEM, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    LBU   -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LBU, WB_MEM, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    LHU   -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LHU, WB_MEM, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SB    -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SB , LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    SH    -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SH , LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    SW    -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SW , LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    ADDI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SLTI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLT   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SLTIU -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLTU  , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    XORI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_XOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    ORI   -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_OR    , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    ANDI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_AND   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SLLI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SRLI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SRL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SRAI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SRA   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    ADD   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SUB   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SUB   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SLL   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SLT   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLT   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SLTU  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLTU  , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    XOR   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_XOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SRL   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SRL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    SRA   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SRA   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    OR    -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_OR    , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    AND   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_AND   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    FENCE -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    FENCEI-> List(PC_0  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    CSRRW -> List(PC_0  , A_RS1,  B_XXX, IMM_X, ALU_COPY_A, BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.W, N, MDU_NONE, MDU_XXX),
    CSRRS -> List(PC_0  , A_RS1,  B_XXX, IMM_X, ALU_COPY_A, BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.S, N, MDU_NONE, MDU_XXX),
    CSRRC -> List(PC_0  , A_RS1,  B_XXX, IMM_X, ALU_COPY_A, BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.C, N, MDU_NONE, MDU_XXX),
    CSRRWI-> List(PC_0  , A_XXX,  B_XXX, IMM_Z, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.W, N, MDU_NONE, MDU_XXX),
    CSRRSI-> List(PC_0  , A_XXX,  B_XXX, IMM_Z, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.S, N, MDU_NONE, MDU_XXX),
    CSRRCI-> List(PC_0  , A_XXX,  B_XXX, IMM_Z, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.C, N, MDU_NONE, MDU_XXX),
    ECALL -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N, MDU_NONE, MDU_XXX),
    EBREAK-> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N, MDU_NONE, MDU_XXX),
    ERET  -> List(PC_EPC, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N, MDU_NONE, MDU_XXX),
    WFI   -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, N, MDU_NONE, MDU_XXX),
    // New Instructions
    // 添加到Control.scala中的map数组
    //PC_4: 执行后PC+4 A_RS1: 使用rs1寄存器作为ALU的A输入 B_XXX: 不使用B输入 IMM_X: 不使用立即数 ALU_CLZ/ALU_POPCNT: 指定ALU操作 WB_ALU: 将ALU结果写回rd寄存器 Y: 启用寄存器写回
    POPCNT-> List(PC_4, A_RS1, B_XXX, IMM_X, ALU_POPCNT, BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_NONE, MDU_XXX),
    // In Control.scala, add to the map Array
    MUL   -> List(PC_4, A_RS1, B_RS2, IMM_X, ALU_XXX, BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N, MDU_MUL, MDU_Y)
    )
  // format: on
}

class ControlSignals extends Bundle {
  val inst = Input(UInt(32.W))
  val pc_sel = Output(UInt(2.W))
  val inst_kill = Output(Bool())
  val A_sel = Output(UInt(1.W))
  val B_sel = Output(UInt(1.W))
  val imm_sel = Output(UInt(3.W))
  val alu_op = Output(UInt(4.W))
  val br_type = Output(UInt(3.W))
  val st_type = Output(UInt(2.W))
  val ld_type = Output(UInt(3.W))
  val wb_sel = Output(UInt(2.W))
  val wb_en = Output(Bool())
  val csr_cmd = Output(UInt(3.W))
  val illegal = Output(Bool())

  val mdu_op = Output(UInt(3.W)) // MDU operation type
  val mdu_sel = Output(Bool())  // Select MDU output (vs ALU)
}

class Control extends Module {
  val io = IO(new ControlSignals)
  val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

  // Control signals for Fetch
  io.pc_sel := ctrlSignals(0)
  io.inst_kill := ctrlSignals(6).asBool

  // Control signals for Execute
  io.A_sel := ctrlSignals(1)
  io.B_sel := ctrlSignals(2)
  io.imm_sel := ctrlSignals(3)
  io.alu_op := ctrlSignals(4)
  io.br_type := ctrlSignals(5)
  io.st_type := ctrlSignals(7)

  // Control signals for Write Back
  io.ld_type := ctrlSignals(8)
  io.wb_sel := ctrlSignals(9)
  io.wb_en := ctrlSignals(10).asBool
  io.csr_cmd := ctrlSignals(11)
  io.illegal := ctrlSignals(12)

  // MDU signals
  io.mdu_op := ctrlSignals(13)
  io.mdu_sel := ctrlSignals(14).asBool
}
