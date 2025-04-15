.text
.global _start
_start: 
    addi x1,x0,1
    addi x2,x0,1
    beq x1,x2,label0  # x1与x2相等，则跳转label0，这里需要跳转
    addi x3,x0,3
    addi x4,x0,4
    addi x5,x0,5
    addi x6,x0,6
label0: 
    addi x7,x0,7
    addi x8,x0,8
    addi x9,x0,9

exit:
    csrw mtohost, 1
    j exit
.end 
