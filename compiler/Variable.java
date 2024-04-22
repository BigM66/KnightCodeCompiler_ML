 /**
* The Class that defines our Variable constructor, and allows us to get the attributes of our variable
* such as Variable type, name, location, etc.
* @author Matthew Parsley
* @version 1.0
* Assignment 5
* CS322 - Compiler Construction
* Spring 2024
**/
package compiler;

public class Variable {

    
    public String variableType = ""; // the type of data that our variable can hold
    public String name = ""; //What the variables name is
    public int memoryLocation;//The Location of variable on the stack

    /**
     * Constructor for a variable
     * @param variableType the type of data that our given variable can hold
     * @param name the name of the variable that will be stored
     * @param memoryLocation what memory location on the stack that the varibale is stored
     */
    public Variable(String name, String variableType, int memoryLocation){
        
        this.variableType = variableType;
        this.name = name;
        this.memoryLocation = memoryLocation;

    }//end constructor

    /**
     * Returns the data that the variable can hold
     * @return the variable data type
     */
    public String getType(){
        return variableType;
    }//end getType

    /**
     * Returns the name that the variable is given
     * @return name of variable
     */
    public String getName(){
        return name;
    }//end getName

    /**
     * Returns the memory location on the stack where the variable is located 
     * @return the memory location of the variable on stack
     */
    public int getLocation(){
        return memoryLocation;
    }// end getLocation

    /**
     * prints a string of all the given information of name, typem and location
     * @return a String containing all of the attributes of the Variable
     */
    public String toString(){
        return "Variable Name: " + name + " Variable Type: " + variableType + " Variable's Location: " + memoryLocation;
    }
    
}//end Variable