
.section .text
.globl _start

_start:
    li a0, 0xFFFFFFFF    # 全1
    li a1, 0x0000000F    # 4个连续1位
    li a2, 0x10101010    # 4个分散1位

    # 0000010 00000 01010 001 00101 0101011
    # 0000010 00000 01011 001 00110 0101011
    # 0000010 00000 01100 001 00111 0101011

    .word 0x040512AB    # POPCNT t0, a0 → 32
    .word 0x0405932B    # POPCNT t1, a1 → 4
    .word 0x040613AB    # POPCNT t2, a2 → 4

exit:
    csrw    mtohost, 1
    j exit

.end
