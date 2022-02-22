import antlr.*;
import assembly.Instruction;
import ast.Program;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Path;

import static java.lang.System.exit;

public class Compiler {

  public static CommonTokenStream tokenize(CharStream input) {
    BasicLexer lexer = new BasicLexer(input);
    return new CommonTokenStream(lexer);
  }

  public static String parse(CommonTokenStream tokens) {
    BasicParser parser = new BasicParser(tokens);
    ParseTree tree = parser.prog();
    return tree.toStringTree(parser);
  }
  
  public static String lexAnalyse(String program) {
    CharStream input = CharStreams.fromString(program);
    return parse(tokenize(input));
  }

  public static void main(String[] args) throws IOException {

    Path filename = Path.of(args[0]);

    CharStream input = CharStreams.fromPath(filename);

    BasicLexer lexer = new BasicLexer(input);

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    BasicParser parser = new BasicParser(tokens);

    ParseTree tree = parser.prog();
    
    if (parser.getNumberOfSyntaxErrors() > 0) {
      exit(100);
    }

    ASTBuilder astBuilder = new ASTBuilder();
    Program ast = (Program) astBuilder.visit(tree);

    SemanticAnalysis semanticAnalysis = new SemanticAnalysis();
    semanticAnalysis.traverse(ast);

    if (semanticAnalysis.getNumberOfErrors() > 0) {
      for (String errorMsg : semanticAnalysis.getErrorMsgs()) {
        System.out.println(errorMsg);
      }
      exit(200);
    }

    Converter converter = new Converter();
    List<Instruction> instructions = converter.visitProgram(ast);

    String fileNameWithExtension = filename.getFileName().toString();
    String fileName = fileNameWithExtension.substring(0,fileNameWithExtension.lastIndexOf("."))
        + ".s";
    FileWriter binFileWriter = new FileWriter(fileName);
    for (Instruction instruction: instructions){
      binFileWriter.write(instruction.toString());
    }
    binFileWriter.close();

  }

}
