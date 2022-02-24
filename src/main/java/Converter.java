import assembly.Instruction;
import assembly.Instruction.InstrType;
import assembly.Operand2;
import assembly.Register;
import ast.Expression;
import ast.Function;
import ast.Program;
import ast.Statement;

import ast.Type;
import ast.Type.EType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Converter extends ASTVisitor<List<Instruction>> {

  List<Instruction> instructions = new ArrayList<>();
  List<Register> generalRegisters = initialiseGeneralRegisters();
  private Register sp = new Register(13);
  private Register pc = new Register(15);
  SymbolTable currentST;

  private List<Instruction> getInstructionFromExpression(Expression expr) {

    if (expr == null) {
      return null;
    }

    switch (expr.getExprType()) {

      case INTLITER:
      case NEG:
      case ORD:
      case LEN:
      case DIVIDE:
      case MULTIPLY:
      case MODULO:
      case PLUS:
      case MINUS:
        return visitIntLiterExp(expr);

      case BOOLLITER:
      case NOT:
      case GT:
      case GTE:
      case LT:
      case LTE:
      case EQ:
      case NEQ:
      case AND:
      case OR:
        return visitBoolLiterExp(expr);

      case CHARLITER:
      case CHR:
        return visitCharLiterExp(expr);

      case STRINGLITER:
        return visitStringLiterExp(expr);

      case IDENT:
        switch (currentST.getType(expr.getIdent()).getType()){
          case INT:
            return visitIntLiterExp(expr);
          case BOOL:
            return visitBoolLiterExp(expr);
          case CHAR:
            return visitCharLiterExp(expr);
          case STRING:
            return visitStringLiterExp(expr);
          case PAIR:
            List<Instruction> pairInstructions = new ArrayList<>();
            pairInstructions.addAll(getInstructionFromExpression(expr.getExpression1()));
            pairInstructions.addAll(getInstructionFromExpression(expr.getExpression2()));
            return pairInstructions;
          case ARRAY:
            List<Instruction> arrayInstructions = new ArrayList<>();
            for (Expression arrayExp: expr.getArrayElem().getExpression()){
              arrayInstructions.addAll(getInstructionFromExpression(arrayExp));
            }
            return arrayInstructions;
        }

      case ARRAYELEM:
        List<Instruction> arrayInstructions = new ArrayList<>();
        for (Expression arrayExp: expr.getArrayElem().getExpression()){
          arrayInstructions.addAll(getInstructionFromExpression(arrayExp));
        }
        return arrayInstructions;

      case BRACKETS:
        return getInstructionFromExpression(expr.getExpression1());

    }
    return null;
  }



  private List<Register> initialiseGeneralRegisters(){
    List<Register> regs = new ArrayList<>();
    for(int i = 0; i != 13; i++){
      regs.add(new Register(i));
    }
    return regs;
  }


  public void translateDeclaration(Statement statement) {

  }

  @Override
  public List<Instruction> visitProgram(Program program) {
    List<Instruction> instructions = new ArrayList<>();

    //TODO: ADD CONSTRUCTOR FOR DIRECTIVES
    instructions.add(new Instruction(InstrType.DATA, ""));

    //TODO: ADD VARIABLE INSTRUCTIONS HERE

    instructions.add(new Instruction(InstrType.TEXT, ""));

    instructions.add(new Instruction(InstrType.GLOBAL_MAIN, ""));

    /* Generate the assembly instructions for each function. */
    for (Function function : program.getFunctions()) {
      instructions.addAll(visitFunction(function));
    }

    //TODO: ADD ENUM FOR LABEL
    instructions.add(new Instruction(InstrType.DATA, "main"));

    /* Generate the assembly instructions for the program body. */
    instructions.addAll(visitStatement(program.getStatement()));

    instructions.add(new Instruction(InstrType.LTORG, ""));

    return instructions;
  }

  //TODO: ADD FUNCTION PARAMETERS TO SYMBOL TABLE
  @Override
  public List<Instruction> visitFunction(Function function) {
    return visitStatement(function.getStatement());
  }

  @Override
  public List<Instruction> visitConcatStatement(Statement statement) {
    List<Instruction> instructions = new ArrayList<>();

    /* Generate the assembly code for each statement in the concatenated statement. */
    instructions.addAll(visitStatement(statement.getStatement1()));
    instructions.addAll(visitStatement(statement.getStatement2()));

    return instructions;
  }

  @Override
  public List<Instruction> visitSkipStatement(Statement statement) {
    /* Generate instructions when a skip statement is found, that is, no instructions. */
    return Collections.emptyList();
  }

  @Override
  public List<Instruction> visitIntLiterExp(Expression expression) {
    return new ArrayList<>(List.of(new Instruction(InstrType.MOV, generalRegisters.get(2),
        expression.getIntLiter())));
  }

  @Override
  public List<Instruction> visitBoolLiterExp(Expression expression) {
    long boolVal = expression.getBoolLiter() ? 1 : 0;
    return new ArrayList<>(List.of(new Instruction(InstrType.MOV, generalRegisters.get(2), boolVal)));
  }

  @Override
  public List<Instruction> visitCharLiterExp(Expression expression) {
    long charVal = Character.getNumericValue(expression.getCharLiter());
    return new ArrayList<>(List.of(new Instruction(InstrType.MOV, generalRegisters.get(2), charVal)));
  }

  @Override
  public List<Instruction> visitStringLiterExp(Expression expression) {
    //TODO Generate initial message label
    List<Instruction> instructions = new ArrayList<>();
      instructions.add(new Instruction(InstrType.LDR, generalRegisters.get(1), "msg_0" ));
      instructions.add(new Instruction(InstrType.PUSH, generalRegisters.get(1)));
    return instructions;
  }



  private List<Instruction> translateBinaryExpression(Expression expression) {
    List<Instruction> instructions = new ArrayList<>();

    /* Generate assembly instructions for the first expression. */
    instructions.addAll(visitExpression(expression.getExpression1())); //Assume expression value is stored in r1

    /* Generate assembly instructions for the second expression. */
    instructions.addAll(visitExpression(expression.getExpression2())); //Assume expression value is stored in r2

    return instructions;
  }

  @Override
  public List<Instruction> visitGreaterExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    // CMP r1, r2
    instructions.add(new Instruction(InstrType.CMP, generalRegisters.get(1),
        new Operand2(generalRegisters.get(2))));

    // MOV r1, #0
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 0));

    // MOVGT r1, #0
    //TODO: CREATE CONDITION CODE ENUMS
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 1));

    return instructions;
  }

  @Override
  public List<Instruction> visitGreaterEqExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    // CMP r1, r2
    instructions.add(new Instruction(InstrType.CMP, generalRegisters.get(1),
        new Operand2(generalRegisters.get(2))));

    // MOV r1, #0
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 0));

    // MOVGE r1, #0
    //TODO: CREATE CONDITION CODE ENUMS
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 1));

    return instructions;
  }

  @Override
  public List<Instruction> visitLessExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    // CMP r1, r2
    instructions.add(new Instruction(InstrType.CMP, generalRegisters.get(1),
        new Operand2(generalRegisters.get(2))));

    // MOV r1, #0
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 0));

    // MOVLT r1, #0
    //TODO: CREATE CONDITION CODE ENUMS
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 1));

    return instructions;
  }

  @Override
  public List<Instruction> visitLessEqExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    // CMP r1, r2
    instructions.add(new Instruction(InstrType.CMP, generalRegisters.get(1),
        new Operand2(generalRegisters.get(2))));

    // MOV r1, #0
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 0));

    // MOVLE r1, #0
    //TODO: CREATE CONDITION CODE ENUMS
    instructions.add(new Instruction(InstrType.MOV, generalRegisters.get(1), 1));

    return instructions;
  }
}
