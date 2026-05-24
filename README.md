### About Minimal
Minimal is a *high-level* programming language focused on simplicity in all aspects, designed for people with little to no programming experience, while still teaching advanced programming techniques.

Minimal is compiled with *Java*.

We aim to make it an open-source tool for the entire programming community who want to experiment with Minimal.

This is a project still under constant development, so regular changes, adjustments, and improvements are expected.

Any comments and suggestions are always welcome. The Minimal team is constantly working to bring you the best while staying true to our philosophy.

Because anyone can (and should) program.

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

Our extension is .min All files of Minimal need the extension .min

### Typing
Typing. Our typing is **_STATIC_**, But we used a Typing similar to Dynamic.
Inside the Typing Behaves is Static, let v = 2; Is a Integer  and let h = "Hi"; is a String

### Variables
The variables work like this:
We used the reservated word *let*

#### Sintax
let «var_name» = «Value»;
Por example

’’’
let num1 = 2;
let Fword = "Hi";
let isTrue = true;
let decim = 3.1415;
let arr = [];
'''

You can see the file *vars.min* in the folder _examples_

### Operations 

’’’
let num1 = 5;
let num2 = 5;
let sum = num1 + num2;
let text1 = "===Sumes===";
print text1;
print sum;
print num1, " + ", num2, " = ", num1 + num2;

let decim1 = 2.234;
let decim2 = 3.1416;
let multi = decim1 * decim2;
let text2 = "===Multiplications===";
print text2;
print decim1, " * ", decim2, " = ", decim1 * decim2;

print "===Divicion===";
print num1, " / ", decim2, " = ", num1 / decim2;

print "===Rest===";
print num1, " - ", decim1, " = ", num1 - decim1;
'''

### structure 

### Matrix Library

Matrix is a Library to calculate of Matrix acelerated

Functions of Matrix:
```
matrix.int(rows, cols);
matrix.zeros(rows, cols);
matrix.ones(rows, cols);
matrix.random(rows, cols);

matrix.fill(m, value);
matrix.fill_parallel(m, hilos);

matrix.add(a, b);
matrix.multiply(a, b);
matrix.matmul(a, b);
matrix.matmul_parallel(a, b, hilos);
matrix.transpose(m);

matrix.sum(m);
matrix.mean(m);

matrix.get(m, row, col);
matrix.set(m, row, col, value);

matrix.rows(m);
matrix.cols(m);
matrix.print_sample(m, rows, cols);
```