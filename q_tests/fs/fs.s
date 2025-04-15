.text
.global _start

_start:
    li t1, 5         
    li t1, 10   # 写后写冒险      
    add t3, t1, t1 # 相邻冒险
    sw t1, 2(t1) # 间隔一条
    lw t4, 2(t1) # 间隔两条
    add t5, t4, t1 # load_use冒险
    
exit:
    csrw mtohost, 1
    j exit
.end 

