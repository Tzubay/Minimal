### How to compile?
```
rm -rf out                            
javac -d out src/Main.java src/lexer/*.java src/parser/*.java src/ast/*.java src/interpreter/*.java

```

### How lo execute? 
First, i recommend create a command to execute. 
You can execute yours codes with the command form **"minimal file.min"**
But, to can make it you need create a file call "minimal" with content: 
```
#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd)"
java -cp "/Users/rute/to/file/out" Main "$@"
```

Copy and paste with
```
sudo cp minimal /usr/local/bin/minimal
```

And, this is already to execute with the command 

```
minimal file.min
```

### Extension

Our extension is .min. All files of Minimal need the extension .min

### Typing
Typing. Our typing is **_STATIC_**, But we used a Typing similar to Dynamic.
Inside the Typing Behaves is Static, let v = 2; Is a Integer  and let h = "Hi"; is a String