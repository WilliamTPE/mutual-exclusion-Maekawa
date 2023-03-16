#!/bin/bash
echo "Please compile all files first, then initiating servers and clients"
echo "Please select: "
echo " 0 -> Compile all files"
echo " 1 -> Server"
echo " 2 -> Client"
read VAR

if [[ $VAR == 0 ]]; then
	echo -e "\nCleaning previous .class files..."
	make clean
	echo -e "\nCompiling java files..."
	javac *.java
elif [[ $VAR == 1 ]]; then
  echo -e "\nPlease choose server number from 0~2"
  read SERVER
  java Server $SERVER
elif [[ $VAR == 2 ]]; then
  echo -e "\nPlease choose client number from 0~4"
  read CLIENT
  java Client $CLIENT
else
	echo "Invalid input"
fi
