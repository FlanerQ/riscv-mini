#!/bin/zsh

set -e

ASM_FILE="$1"
FILENAME=$(basename "$ASM_FILE" .s)

if [ "$1" = "clean" ]; then
  rm -f *.dump *.hex *.vcd
  exit 0
fi

riscv32-unknown-elf-gcc -nostdlib -Ttext=0x200 -o $FILENAME "$FILENAME.s"

riscv32-unknown-elf-objdump -S $FILENAME > $FILENAME.dump

elf2hex 16 4096 $FILENAME > $FILENAME.hex

/home/flanerq/now/riscv-mini/VTile $FILENAME.hex $FILENAME.vcd


