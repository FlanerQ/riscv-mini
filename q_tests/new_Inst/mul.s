
.section .text
.globl _start

_start:
    li x1, 5         # x1 = 5
    li x2, 7         # x2 = 7
    mul x3, x1, x2   # x3 = 35
    
    li x4, -3        # x4 = -3
    li x5, 10        # x5 = 10
    mul x6, x4, x5   # x6 = -30
    
    li x7, 0x7FFFFFFF # x7 = 0x7FFFFFFF
    li x8, 2          # x8 = 2
    mul x9, x7, x8    # x9 should be 0xFFFFFFFE

    li x10, 0x7FFFFFFF  # x7 = 0x7FFFFFFF
    li x11, 0x7777      # x11 =0x7777 
    mul x12, x10, x11   # res = 3BBB7FFF8889, x12 should be 0xFFFFFFFE (lower 32 bits)

exit:
    csrw    mtohost, 1
    j exit

.end
