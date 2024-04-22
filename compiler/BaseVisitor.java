

 /**
* This is a Class that overrides some of the methods of the ANTLR generated BaseVisitor class that will be responsible for performing bytecode operations for the grammar rules
* of our given grammar of KnightCode. This is where we can give the rules on how we want to go about parsing our tree, and how we can go about our buisness of making sure that
* everything runs smoothly with the Compiler.
* @author Matthew Parsley
* @version 1.0
* Assignment 5
* CS322 - Compiler Construction
* Spring 2024
**/
package compiler;

import lexparse.*;
import org.objectweb.asm.*;  //classes for generating bytecode


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class BaseVisitor extends KnightCodeBaseVisitor<Object>{

    private ClassWriter cw;  //ClassWriter for a KnightCode class
	private MethodVisitor mainVisitor; //global MethodVisitor
	private String programName; //name of the output file
    private Map<String, Variable> symbolTable; //map that will store the name of the variable along with its corresponding Variable object which will contain some of its attributes
    private int memPointer;//where our compiler is going to be pointed and so we know where we are at with compiling


    /**
     * Constructor for BaseVisitor
     * @param programName the name of the program we want to use 
     */
    public BaseVisitor(String programName){
        this.programName = programName;
        
    }//end constructor

    /**
     * Method that we will use to remove parenthesis from our strings when we print strings
     * @param str the string that will be modified
     * @return the string without the parenthesis
     */
    public String removeParentheses(String str){
        return str.substring(1, str.length() -1);
    }//end removeParentheses


    /*
     * Method that we will use to print our symbol table we created
     */
    public void printSymbolTable(){
        System.out.println("SymbolTable");
        for (Map.Entry<String, Variable> entry : symbolTable.entrySet()){
            System.out.println("Key: " + entry.getKey() + " Value: " + entry.getValue().toString());
        }
    }// end PrintSymbolTable

    /**
     * Method that will set up the ClassWriter and starts the constructor that will be used so our Compiler knows how to use it
     * @param name the name of the program that we want to create
     */
    public void startClass(String name){
        
        // Sets up our ClassWriter
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,programName, null, "java/lang/Object",null);
        
        // Creating Constructor for the class
        {
			MethodVisitor mv=cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0); //load the first local variable: this
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V",false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(1,1);
			mv.visitEnd();
		}

    }//end startClass

    public  void writeFile(byte[] bytearray, String fileName){

        try{
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(bytearray);
            out.close();
        }
        catch(IOException e){
        System.out.println(e.getMessage());
        }
        
    }
    /**
     * closes the main method and writes the ClassWriter data into the output destination, with the name we give it
     */
    public void EndClass(){

            mainVisitor.visitInsn(Opcodes.RETURN);
            mainVisitor.visitMaxs(0, 0);
            mainVisitor.visitEnd();
    
            cw.visitEnd();
    
            byte[] b = cw.toByteArray();
    
            writeFile(b,this.programName+".class");
    
            System.out.println("\nCompiling Finished, class file was generated");
        
    }//end EndClass

    @Override
    /**
     * Calls our startClass method which creates the ClassWriter and constructor for the KnightCode class so our Compiler can work through it.
     */
    public Object visitFile(KnightCodeParser.FileContext ctx){
        System.out.println("Entering File");

        startClass(programName);
        return super.visitFile(ctx);
    }//end visitFile

    @Override
    /**
     * Once our Declare is visited for our Variables, a HashMap for the symbol table will be initialized and the stack memory pointer will be set to zero
     * and our compiler can use this to parse through a tree 
     */
    public Object visitDeclare(KnightCodeParser.DeclareContext ctx){
        //Debug
        System.out.println("Visiting Declare");
        
        symbolTable = new HashMap<>();
        memPointer = 0;

        return super.visitDeclare(ctx);
    }//end visitDeclare

    @Override
    /**
     * Once the variables are visited, the name and type of each Variable will be used to instantiate a new Variable object 
     * using the attributes given by the declaration and puts them into the symbol table
     */
    public Object visitVariable(KnightCodeParser.VariableContext ctx){
        //Debug
        System.out.println("Visiting Variable");
        
        String type = ctx.vartype().getText();

        // Check if declared type is unsupported (for our code if it is not a string or integer)
        if (!type.equals("INTEGER") && !type.equals("STRING")){
            System.err.println("Compilation ERROR: the entered type is not supported, please enter a string or integer!");
            System.exit(1);
        }

        // Creates our variable and adds it to our symbol table we created
        String name = ctx.identifier().getText();
        Variable v = new Variable(name, type, memPointer++);
        symbolTable.put(name, v);

        printSymbolTable();

        return super.visitVariable(ctx);
    }//end visitVariable

    @Override
    /**
     * Method that visits the body and initializes the main method
     * @param ctx The file that the compiler is running
     */
    public Object visitBody(KnightCodeParser.BodyContext ctx){
        //Debug
        System.out.println("Enter file");
        
        // Start MethodVisitor for main method of program
        
        mainVisitor=cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mainVisitor.visitCode();

        return super.visitBody(ctx);
    }//end visitBody
    
    /**
     * Method that will evaluate an expression depending on what type of context it is an instance of. 
     * It uses recursion so that once the compiler reaches a Terminal, it can be reevaluated so operations can be performed
     * @param ctx the context of the expr that is to be evaluated
     */
    public void evalExpr(KnightCodeParser.ExprContext ctx){
        
        // If the expr is just a number, our compiler reads and parses the text as an int and loads it to constant pool for use later
        if (ctx instanceof KnightCodeParser.NumberContext){
            int value = Integer.parseInt(ctx.getText());
            
            //debug
            System.out.println(value + " was added onto stack");
            mainVisitor.visitLdcInsn(value);
        }//number

        // If the expr is an identifier our grammar supports
        else if (ctx instanceof KnightCodeParser.IdContext){
            String id = ctx.getText();
            Variable var = symbolTable.get(id);
            
            //debug
            System.out.println("expr id " + id + "\nvar: " + var.toString());

            // If type of the variable is INTEGER
            if (var.getType().equals("INTEGER")){
                mainVisitor.visitVarInsn(Opcodes.ILOAD, var.getLocation());
                System.out.println(id+ " is on stack");
            }
            // If the type of the varibale is a STRING
            else if (var.getType().equals("STRING")){
                mainVisitor.visitVarInsn(Opcodes.ALOAD, var.getLocation());
            } 
            
        }//id   

        //Subtraction of 2 INTEGERS in opcdoe
        else if (ctx instanceof KnightCodeParser.SubtractionContext){
            
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.SubtractionContext)ctx).expr()){
                evalExpr(expr);
            }//for
        System.out.println("subtracting");
        mainVisitor.visitInsn(Opcodes.ISUB);
            
        }
        //Addition of 2 INTEGERS in opcdoe
        else if (ctx instanceof KnightCodeParser.AdditionContext){
            
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.AdditionContext)ctx).expr()){
                evalExpr(expr);
            }//for
        System.out.println("adding");
        mainVisitor.visitInsn(Opcodes.IADD);
            
        }
        //Multiplication of 2 INTEGERS in opcdoe
        else if (ctx instanceof KnightCodeParser.MultiplicationContext){
            
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.MultiplicationContext)ctx).expr()){
                evalExpr(expr);
            }//for
        System.out.println("Multiplying");
        mainVisitor.visitInsn(Opcodes.IMUL);
            
        }
        //Division of 2 INTEGERS in opcode
        else if (ctx instanceof KnightCodeParser.DivisionContext){
            
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.DivisionContext)ctx).expr()){
                evalExpr(expr);
            }//for
        System.out.println("dividing");
        mainVisitor.visitInsn(Opcodes.IDIV);
            
        }

    }//end evalExpr


    @Override
    /**
     * Method that visits comparison,and will perform the comparison operation and if the comparison is  true load one, if it is false load 0
     * @param the file that the compiler is running
     */
    public Object visitComparison(KnightCodeParser.ComparisonContext ctx){
        
        Label trueLabel = new Label();
        Label endLabel = new Label();

        String oper = ctx.comp().getText();

        evalExpr(ctx.expr(0));
        evalExpr(ctx.expr(1));

        switch (oper) {
            case "GT":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, trueLabel);
                break;
        
            case "LT":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
                break;

            case "EQ":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, trueLabel);
                break;
            case "NE":
            mainVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, trueLabel);
                break;
        }

        //If false, load 0 and jump to end
        mainVisitor.visitLdcInsn(0);
        mainVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        //If true load 1 into the stack
        mainVisitor.visitLabel(trueLabel);
        mainVisitor.visitLdcInsn(1);

        mainVisitor.visitLabel(endLabel);

        return super.visitComparison(ctx);
    }//end visitComparison


    /**
     * Method that will check if a string is either a number or an identifier in the symbol table and will load its stored value from our symbol table
     * @param str the string with the ID or value to be loaded
     */
    public void loadInteger(String str){
        int location;
        
        //If the string is a key of the symbol table, ( if it is the key of one of our key value pairs in the hashmap)
        if (symbolTable.containsKey(str)){
            Variable var = symbolTable.get(str);
            location = var.getLocation();
            mainVisitor.visitVarInsn(Opcodes.ILOAD, location);
        }
        //If it's a number we must parse it so it van be used in our compiler 
        else {
            mainVisitor.visitLdcInsn(Integer.parseInt(str));
        }
    }//end loadInteger
    
    @Override
    /**
     * Method that handles the logic for a simple IF THEN ELSE loop based off of a comparison using jumps
     */
    public Object visitDecision(KnightCodeParser.DecisionContext ctx){
        
        //Labels used for jumping
        Label trueLabel = new Label();
        Label endLabel = new Label();
        
        
        //Load the children to be compared from our main branch, from left to right
        String num1 = ctx.getChild(1).getText();
        String num2 = ctx.getChild(3).getText();

        loadInteger(num1);
        loadInteger(num2);

        //Decide which comparison was used and will be done by the program
        String oper = ctx.comp().getText();
        
        //Handles whether or not it will jump to the IF THEN block based on the conditions
        switch (oper) {
            case ">":
                System.out.println("GT");
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, trueLabel);
                break;
        
            case "<":
                System.out.println("LT");
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
                break;

            case "=":
                System.out.println("EQ");  
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, trueLabel);
                break;
            case "<>":
                System.out.println("NEQ");
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, trueLabel);
                break;
            default:
                System.err.println("ERROR: not one of the known Comparisons.");
                System.exit(1);
        }

        //Check to see if there is an else statement or not 
        boolean hasElse = false;
        int endLocation = 6; //sets us up so we can find our endif statement

         //Loop that determines where our End if statement is 
         while (!ctx.getChild(endLocation).getText().equals("ENDIF")){
            endLocation++;
        }  
        
        //Checks to see if an else statement is included after the if 
        for(int i = 0; i<ctx.children.size(); i++){
            if(ctx.getChild(i).getText().equals("ELSE")){
                hasElse = true;
                break;
            }
        }

        int elseLocation = 6; // least possible child index for else location (IF x comp y THEN stat ELSE)
        
        //Handles else block if there is an else block
        if(hasElse){
    
            //Loop that figures out how many children there are in the if block
            while (!ctx.getChild(elseLocation).getText().equals("ELSE")){
                elseLocation++;
            }  
            
            //ELSE
            //Loop that runs all of the children within the else block
            for(int i = elseLocation+1; i<ctx.getChildCount(); i++){
                visit(ctx.getChild(i));
            }
        }

        //Jump to end after the loop has executed
        mainVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        //IF THEN
        // Go here when comparison is true
        mainVisitor.visitLabel(trueLabel);

        //handles the if, when there is an else
        if(hasElse){
            //Starts at location of first stat of the then block and visits children until it reaches the location of the else
            for (int i = 5; i< elseLocation;i++){
                visit(ctx.getChild(i));
            }
        }
        //Handles the if loop when there is no else
        else{
            //Starts at location of first branch in the if block and visits children until it reaches the TERMINAL of ENDIF
            for (int i = 5; i< endLocation;i++){
                visit(ctx.getChild(i));
            }
        }

        //End label
        mainVisitor.visitLabel(endLabel);

        return null;

    }//end visitDecision

    @Override
    /**
     * Method that will allow us to perform while loops
     * @param ctx the file our compiler is running
     */
    public Object visitLoop(KnightCodeParser.LoopContext ctx){
        //Labels used for jumping
        
        Label beginLabel = new Label(); //beginning of loop
        Label endLoop = new Label(); //leaves the loop
        
        //Begin loop Label
        mainVisitor.visitLabel(beginLabel);

        //Load the children to be compared in our main branch, from left to right
        String num1 = ctx.getChild(1).getText();
        String num2 = ctx.getChild(3).getText();

        loadInteger(num1);
        loadInteger(num2);

        //Decide which comparison to use
        String oper = ctx.comp().getText();
        
        //Handles whether or not it will jump to endLoop
        switch (oper) {
            case ">":
                System.out.println("LE");
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, endLoop);
                break;
        
            case "<":
                System.out.println("GE");
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, endLoop);
                break;

            case "=":
                System.out.println("NEQ");  
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, endLoop);
                break;
            case "<>":
                System.out.println("EQ");
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, endLoop);
                break;
            default:
                System.err.println("ERROR: not one of the known comparisons.");
                System.exit(1);
        }//end switch

        //Loop that runs all of the children contained within the while loop until the need is met
        for(int i = 5; i<ctx.getChildCount(); i++){
            visit(ctx.getChild(i));
        }
        
        //Jumps back to top if loop requirement is not met
        mainVisitor.visitJumpInsn(Opcodes.GOTO, beginLabel);
        
        //End label
        mainVisitor.visitLabel(endLoop);

        return null;

    }//end visitLoop

    @Override
    /**
     * Is triggered when Setvar is entered and will define a previously declared variable
     * @param ctx the file compiler is running
     */
    public Object visitSetvar(KnightCodeParser.SetvarContext ctx){
        
        //Name of variable to be created
        String varName = ctx.ID().getText(); 

        //Debug
        System.out.println("Enter Variable: " + varName);

        //Creates object for the variable
        Variable var = symbolTable.get(varName);
        
        // If the variable was not previously declared
        if (var == null){
            System.err.println("ERROR: " + varName + "Variable does not exist");
            System.exit(1);
        }
        //Evaluates any expressions before storing our created variable
        else if(ctx.expr() != null){
            evalExpr(ctx.expr());

            //Defines the variable if it is an INTEGER
            if (var.getType().equals("INTEGER")){
                System.out.println("Storing the Var " + varName);
                mainVisitor.visitVarInsn(Opcodes.ISTORE, var.getLocation());
            }
            
        }
        //Defines the variable if it is an STRING
        else if (var.getType().equals("STRING") && ctx.STRING() != null){
            String str = removeParentheses(ctx.STRING().getText());
            mainVisitor.visitLdcInsn(str);
            mainVisitor.visitVarInsn(Opcodes.ASTORE, var.getLocation());
        } 
        
        printSymbolTable();

        return super.visitSetvar(ctx);
        
    }//end visitSetvar

    @Override
    /**
     * Is triggered whenever print is encountered and will either print out and integer or a string, whichever is specified
     * @param ctx the file that the compiler is running
     */
    public Object visitPrint(KnightCodeParser.PrintContext ctx){
        //Debug
        System.out.println("please enter Print");
        
        mainVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

        // If the varibale that needs printing is an ID then it searches the stack and finds its location so it can be loaded and printed
        if(ctx.ID() != null){   
            String varID = ctx.ID().getText();
            Variable var = symbolTable.get(varID);
            int location = var.getLocation(); //location of the variable

            if (var.getType().equals("INTEGER")){
                mainVisitor.visitVarInsn(Opcodes.ILOAD, location);
                mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
            }
            else{
                mainVisitor.visitVarInsn(Opcodes.ALOAD, location);
                mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }
        }

        //If the variable is a String, it will load the string to the constant pool so it can grabbed and printed 
        else if(ctx.STRING()!=null){
            String str = removeParentheses(ctx.STRING().getText());
            mainVisitor.visitLdcInsn(str);
            mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
        return super.visitPrint(ctx);
    }//end visitPrint

    
    @Override
    /**
     * Method that will read an input from the user and store the input in the variable whose identifier follows the read call so our compiler can use it later
     */
    public Object visitRead(KnightCodeParser.ReadContext ctx){
        //debug
        System.out.println("Entering Read");
        
        //Initializes the variable that will store the value inputted by the user so it can used later by the compiler 
        Variable var = symbolTable.get(ctx.ID().getText());
        int scanLocation = memPointer++;

        // Initializes the Scanner object for use later
        mainVisitor.visitTypeInsn(Opcodes.NEW, "java/util/Scanner"); // Creates Scanner and pushes it to the stack
        mainVisitor.visitInsn(Opcodes.DUP); // Duplicates the Scanner reference which will be used in initializing and storing the scanner
        mainVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;"); // System.in
        mainVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false); // Initializes Scanner
        mainVisitor.visitVarInsn(Opcodes.ASTORE, scanLocation); // Stores Scanner

        //Handles the variable if it is an integer
        if (var.getType().equals("INTEGER")){

            // Reads the users inputed integer
            mainVisitor.visitVarInsn(Opcodes.ALOAD, scanLocation); // Loads scanner
            mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false); // Scan.nextLong()
            mainVisitor.visitVarInsn(Opcodes.ISTORE, var.getLocation()); // Store the int value in a variable
        }
        
        //Handles if the variable the user inputted is a string
        else if (var.getType().equals("STRING")){
            

            // Reads the users inputted string
            mainVisitor.visitVarInsn(Opcodes.ALOAD, scanLocation); // Loads scanner
            mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Scanner", "nextLine", "()Ljava/lang/String;", false); // Scan.nextLong()
            mainVisitor.visitVarInsn(Opcodes.ASTORE, var.getLocation()); // Store the String value in a variable
        }

        return super.visitRead(ctx);
    }//end visitRead
    
}//end MyBaseVisitor
 
