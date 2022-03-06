#!/bin/bash
# https://stackoverflow.com/questions/44427355/how-to-convert-jar-to-linux-executable-file/56469496#56469496
echo "#!/usr/bin/java -jar" > "$2"
cat "$1" >> "$2"
chmod +x "$2"
