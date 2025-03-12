.section .text
.globl _start

_start:
    li      a0, 0x100
    addi    t0, a0, 5

    li      a0, 1
    csrw    mtohost, a0
    
infinite:
    j       infinite
