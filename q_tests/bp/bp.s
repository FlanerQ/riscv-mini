.text
.global _start
_start:
    # 初始化寄存器
    li x1, 0           # 循环计数器
    li x2, 1024          # 循环上限
    li x3, 512           # 行为改变阈值
    li x4, 0           # 工作寄存器
    
loop_start:
    addi x1, x1, 1     # 计数器增加
    
    # 模式1：前8次迭代：taken，后8次：not taken
    blt x1, x3, pattern1_taken
    j pattern1_not_taken
pattern1_taken:
    addi x4, x4, 1     # 一些工作
    j pattern1_end
pattern1_not_taken:
    addi x4, x4, 2     # 不同的工作
pattern1_end:
    
    # 模式2：前8次迭代：not taken，后8次：taken
    bge x1, x3, pattern2_taken
    j pattern2_not_taken
pattern2_taken:
    addi x4, x4, 4     # 一些工作
    j pattern2_end
pattern2_not_taken:
    addi x4, x4, 8     # 不同的工作
pattern2_end:
    
    # 循环控制 - 大部分时间应该被预测为taken
    blt x1, x2, loop_start
    
exit:
    csrw    mtohost, 1
    j exit
.end
