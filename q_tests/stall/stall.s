.section .text
.globl _start

_start:
    # 初始化内存地址
    addi a0, zero, 0x100    # a0 = 0x100 (基址)

    # 准备两个值
    addi t0, zero, 42       # t0 = 42 
    addi t1, zero, 10       # t1 = 10

    # 存储测试
    sw t0, 0(a0)            # 将 42 存储到内存 0x100
    lw t2, 0(a0)            # 从同一地址加载 - 这里会产生存储-加载停顿

    # 无关指令，避免连续停顿影响观察
    addi t3, zero, 5        # t3 = 5 (不会导致停顿)

    # 加载-使用测试
    lw t4, 0(a0)            # 从内存加载值到 t4
    add t5, t4, t1          # 立即使用 t4 - 这里会产生加载-使用停顿

    # 结束
    li      a1, 1
    csrw    mtohost, a1
    
infinite:
    j       infinite
