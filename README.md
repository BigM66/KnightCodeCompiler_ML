# KnightCodeCompiler_MLP
The main goal of this code is to compile a language that we created called knight code. Knight code is a very simple language, as the only data types that it supports are strings and integers.
The language is able to take the strings and integers and do simple arithmetic and logical opperations on them. This limited to be the 4 main  operations of addition, subtraction, multiplication, and division.
It also allows us to comapre 2 integers and print out the result of the comparison. The language also has the capabilites to do simple if and while loops.

The compiler requires 2 libraries in order to work correctly. Those are ANTLR and the ASM ByteCode library. you can install them using the code below

ANTLR: antlr-4.13.1-complete.jar
ASM Bytecode Library: asm-9.6.jar

In order to use ANTLR you must take the grammar file, called KnightCode.g4 and run it through ANTLR. WHen you do this, ANTLR creates all of the needed files in order to parse a tree and compile the grammar.
This includes but is not limited to a Lexer, a Parser, and a base Visitor and Base Listener. you need all of this files in order to determine how you want to traverse the tree that gets created. You can do this one of 2 ways.
The first option is through a listener, which is easier to use, but does not give you as much control as to how the tree is parsed.
the second option is through a visitor, which is the one i opted to use. It is a little harder to get set up, but gives you a lot more control on how the tree gets parsed as compared to the listener.

You can generate evewrything using the code below in the command line

ant build-grammar
ant compile-grammar
ant compile

doing this will generate all of the files that you need into a file called lexparse, which was used to create my visitor. using ANTLR makes it to where we did not have to write all of those files by hand, and
it allowed us to be able to parse the tree easier.

Once you have everything set up and actually want to use the compiler, you must then go and run the kcc.java file using command line.
To do this our command line takes 2 arguments, the first is the name of the file that you want to compile, 
the second is what you want to name the output file, and the location you want to store it in.
both of the arguments require the file path of the files you want.

for example, if you would want to run program 5 using our compiler it would look like this:

java Compiler/kcc tests/program5.kc output/Program5

Now ideally that would then create a .class file that can then be run by 

java output/Program5

however sadly, I was not able to get my compiler to generate the file. The compiler still does the job of generating a parse tree and allowing us to see the fully parsed out tree, however
When it tries to generate the file into the output file it returns with a null Pointer Exception, which I have not been able to figure out why it does this. 

So the compiler still does most of its job, as it generates and parses through the tree correctly, but it does not fully work as the .class file can not be generated yet. 

The tree can be viewed and it is done correctly, I just could not figure out why I am getting a null pointer exception when it tries to create the .class file.
