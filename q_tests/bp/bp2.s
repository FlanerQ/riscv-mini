.text
.global _start
_start:
    # 初始化寄存器
    li x1, 0        # 循环计数器
    li x2, 16       # 循环总迭代次数
    li x3, 8        # 分支模式切换阈值
    li x4, 0        # 累加结果
    
loop_start:
    addi x1, x1, 1   # 计数器加1
    blt x1, x3, first_path
    
second_path:         # 后8次迭代走此路径
    addi x4, x4, 2
    j path_end
    
first_path:          # 前8次迭代走此路径
    addi x4, x4, 1
    
path_end:
    blt x1, x2, loop_start

exit:
    csrw    mtohost, 1
    j exit
.end
