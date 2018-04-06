#!/bin/bash

if [ $# -ne 2 ]; then
	echo "Usage: ./bouda.bash <source directory> <target file>"
	exit;
fi

if ! [ -d "$1" ]; then
	echo "Error: Source directory is not a directory!"
	exit;
fi

echo -n "" > "$2"

for i in "$1"/*; do
        echo -e "\n//----------------------------------------\n//	$i\n//----------------------------------------\n" >> "$2"
	cat $i >> "$2"
done
