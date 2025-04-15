// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

object Consts {
  val PC_START = 0x200
  val PC_EVEC = 0x100
}

class DatapathIO(xlen: Int) extends Bundle {
  val host = new HostIO(xlen)
  val icache = Flipped(new CacheIO(xlen, xlen))
  val dcache = Flipped(new CacheIO(xlen, xlen))
  val ctrl = Flipped(new ControlSignals)
}

class FetchExecutePipelineRegister(xlen: Int) extends Bundle {
  val inst = chiselTypeOf(Instructions.NOP)
  val pc = UInt(xlen.W)
  val predicted_taken = Bool()
  val predicted_target = UInt(xlen.W)
}

class ExecuteWritebackPipelineRegister(xlen: Int) extends Bundle {
  val inst = chiselTypeOf(Instructions.NOP)
  val pc = UInt(xlen.W)
  val alu = UInt(xlen.W)
  val csr_in = UInt(xlen.W)
}

class Datapath(val conf: CoreConfig) extends Module {
  val io = IO(new DatapathIO(conf.xlen))
  val csr = Module(new CSR(conf.xlen))
  val regFile = Module(new RegFile(conf.xlen))
  val alu = Module(conf.makeAlu(conf.xlen))
  val immGen = Module(conf.makeImmGen(conf.xlen))
  val brCond = Module(conf.makeBrCond(conf.xlen))
  val branchPredictor = Module(new BranchPredictor(conf.xlen, conf.branchPredictorEntries))

  import Control._

  /** Pipeline State Registers * */

  /** *** Fetch / Execute Registers ****
    */
  val fe_reg = RegInit(
    (new FetchExecutePipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U,
      _.predicted_taken -> false.B,
      _.predicted_target -> 0.U
    )
  )

  /** *** Execute / Write Back Registers ****
    */
  val ew_reg = RegInit(
    (new ExecuteWritebackPipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U,
      _.alu -> 0.U,
      _.csr_in -> 0.U
    )
  )

  /** **** Control signals ****
    */
  val st_type = Reg(io.ctrl.st_type.cloneType)
  val ld_type = Reg(io.ctrl.ld_type.cloneType)
  val wb_sel = Reg(io.ctrl.wb_sel.cloneType)
  val wb_en = Reg(Bool())
  val csr_cmd = Reg(io.ctrl.csr_cmd.cloneType)
  val illegal = Reg(Bool())
  val pc_check = Reg(Bool())

  /** **** Fetch ****
    */
  val started = RegNext(reset.asBool)
  val stall = !io.icache.resp.valid || !io.dcache.resp.valid
  val pc = RegInit(Consts.PC_START.U(conf.xlen.W) - 4.U(conf.xlen.W))
  
  // 连接分支预测器的取指接口
  branchPredictor.io.fetch_pc := pc
  branchPredictor.io.inst := io.icache.resp.bits.data
  // 检测分支预测错误
  val is_branch_inst = io.ctrl.br_type =/= BR_XXX
  val branch_taken = brCond.io.taken
  val branch_target = (alu.io.sum >> 1.U << 1.U)
  val mispredict = is_branch_inst && 
                  ((branch_taken =/= fe_reg.predicted_taken) || 
                   (branch_taken && (branch_target =/= fe_reg.predicted_target)))

  // Next Program Counter
  val next_pc = MuxCase(
    Mux(branchPredictor.io.predict_taken, branchPredictor.io.predict_target, pc + 4.U),
    IndexedSeq(
      stall -> pc,
      csr.io.expt -> csr.io.evec,
      mispredict -> Mux(branch_taken, branch_target, fe_reg.pc + 4.U), // 提高优先级，确保预测错误时能及时纠正
      (io.ctrl.pc_sel === PC_EPC) -> csr.io.epc,
      // 只在非分支指令或JAL/JALR类指令时使用PC_ALU，避免与分支预测冲突
      ((io.ctrl.pc_sel === PC_ALU) && !is_branch_inst) -> branch_target,
      (io.ctrl.pc_sel === PC_0) -> pc
    )
  )
  // 当处理分支指令时，使用预测结果决定是否插入NOP
  val inst =
    Mux(started || io.ctrl.inst_kill || mispredict || csr.io.expt, Instructions.NOP, io.icache.resp.bits.data)
  pc := next_pc
  io.icache.req.bits.addr := next_pc
  io.icache.req.bits.data := 0.U
  io.icache.req.bits.mask := 0.U
  io.icache.req.valid := !stall
  io.icache.abort := false.B

  // Pipelining
  when(!stall) {
    fe_reg.pc := pc
    fe_reg.inst := inst
    fe_reg.predicted_taken := branchPredictor.io.predict_taken
    fe_reg.predicted_target := branchPredictor.io.predict_target
  }

  /** **** Execute ****
    */
  io.ctrl.inst := fe_reg.inst

  // regFile read
  val rd_addr = fe_reg.inst(11, 7)
  val rs1_addr = fe_reg.inst(19, 15)
  val rs2_addr = fe_reg.inst(24, 20)
  regFile.io.raddr1 := rs1_addr
  regFile.io.raddr2 := rs2_addr

  // gen immdeates
  immGen.io.inst := fe_reg.inst
  immGen.io.sel := io.ctrl.imm_sel

  // bypass
  val wb_rd_addr = ew_reg.inst(11, 7)
  val rs1hazard = wb_en && rs1_addr.orR && (rs1_addr === wb_rd_addr)
  val rs2hazard = wb_en && rs2_addr.orR && (rs2_addr === wb_rd_addr)
  val rs1 = Mux(wb_sel === WB_ALU && rs1hazard, ew_reg.alu, regFile.io.rdata1)
  val rs2 = Mux(wb_sel === WB_ALU && rs2hazard, ew_reg.alu, regFile.io.rdata2)

  // ALU operations
  alu.io.A := Mux(io.ctrl.A_sel === A_RS1, rs1, fe_reg.pc)
  alu.io.B := Mux(io.ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  alu.io.alu_op := io.ctrl.alu_op

  // Branch condition calc
  brCond.io.rs1 := rs1
  brCond.io.rs2 := rs2
  brCond.io.br_type := io.ctrl.br_type

  // D$ access
  val daddr = Mux(stall, ew_reg.alu, alu.io.sum) >> 2.U << 2.U
  val woffset = (alu.io.sum(1) << 4.U).asUInt | (alu.io.sum(0) << 3.U).asUInt
  io.dcache.req.valid := !stall && (io.ctrl.st_type.orR || io.ctrl.ld_type.orR)
  io.dcache.req.bits.addr := daddr
  io.dcache.req.bits.data := rs2 << woffset
  io.dcache.req.bits.mask := MuxLookup(Mux(stall, st_type, io.ctrl.st_type), "b0000".U)(
    Seq(ST_SW -> "b1111".U, ST_SH -> ("b11".U << alu.io.sum(1, 0)), ST_SB -> ("b1".U << alu.io.sum(1, 0)))
  )

  // 添加分支预测性能计数器
  val hitCounter = RegInit(0.U(32.W)) 
  val missCounter = RegInit(0.U(32.W))
  
  when(is_branch_inst && !stall) {
    when(fe_reg.predicted_taken === branch_taken && 
         (!branch_taken || fe_reg.predicted_target === branch_target)) {
      hitCounter := hitCounter + 1.U
    }.otherwise {
      missCounter := missCounter + 1.U
    }
  }
  
  // 打印分支预测统计信息
  when(io.host.tohost =/= 0.U) {
    printf("Branch prediction stats - Hits: %d, Misses: %d, Accuracy: %d%%\n", 
           hitCounter, missCounter, 
           (hitCounter * 100.U) / (hitCounter + missCounter + 1.U))
  }
  
  // 连接分支预测器的更新接口
  branchPredictor.io.exec_pc := fe_reg.pc
  branchPredictor.io.exec_inst := fe_reg.inst
  branchPredictor.io.exec_br_type := io.ctrl.br_type
  branchPredictor.io.exec_taken := branch_taken
  branchPredictor.io.exec_target := branch_target
  branchPredictor.io.update := !stall && io.ctrl.br_type =/= BR_XXX

  // Pipelining
  when(reset.asBool || !stall && csr.io.expt) {
    st_type := 0.U
    ld_type := 0.U
    wb_en := false.B
    csr_cmd := 0.U
    illegal := false.B
    pc_check := false.B
  }.elsewhen(!stall && !csr.io.expt) {
    ew_reg.pc := fe_reg.pc
    ew_reg.inst := fe_reg.inst
    ew_reg.alu := alu.io.out
    ew_reg.csr_in := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    st_type := io.ctrl.st_type
    ld_type := io.ctrl.ld_type
    wb_sel := io.ctrl.wb_sel
    wb_en := io.ctrl.wb_en
    csr_cmd := io.ctrl.csr_cmd
    illegal := io.ctrl.illegal
    pc_check := io.ctrl.pc_sel === PC_ALU
  }

  // Load
  val loffset = (ew_reg.alu(1) << 4.U).asUInt | (ew_reg.alu(0) << 3.U).asUInt
  val lshift = io.dcache.resp.bits.data >> loffset
  val load = MuxLookup(ld_type, io.dcache.resp.bits.data.zext)(
    Seq(
      LD_LH -> lshift(15, 0).asSInt,
      LD_LB -> lshift(7, 0).asSInt,
      LD_LHU -> lshift(15, 0).zext,
      LD_LBU -> lshift(7, 0).zext
    )
  )

  // CSR access
  csr.io.stall := stall
  csr.io.in := ew_reg.csr_in
  csr.io.cmd := csr_cmd
  csr.io.inst := ew_reg.inst
  csr.io.pc := ew_reg.pc
  csr.io.addr := ew_reg.alu
  csr.io.illegal := illegal
  csr.io.pc_check := pc_check
  csr.io.ld_type := ld_type
  csr.io.st_type := st_type
  io.host <> csr.io.host

  // Regfile Write
  val regWrite =
    MuxLookup(wb_sel, ew_reg.alu.zext)(
      Seq(WB_MEM -> load, WB_PC4 -> (ew_reg.pc + 4.U).zext, WB_CSR -> csr.io.out.zext)
    ).asUInt

  regFile.io.wen := wb_en && !stall && !csr.io.expt
  regFile.io.waddr := wb_rd_addr
  regFile.io.wdata := regWrite

  // Abort store when there's an excpetion
  io.dcache.abort := csr.io.expt

  // TODO: re-enable through AOP
//  if (p(Trace)) {
//    printf(
//      "PC: %x, INST: %x, REG[%d] <- %x\n",
//      ew_reg.pc,
//      ew_reg.inst,
//      Mux(regFile.io.wen, wb_rd_addr, 0.U),
//      Mux(regFile.io.wen, regFile.io.wdata, 0.U)
//    )
//  }
}
