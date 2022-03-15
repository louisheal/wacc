package assembly.instructions;

import assembly.Conditionals;
import assembly.Flags;
import assembly.Operand2;
import assembly.Register;

public class Instruction {
  Register dest;
  long immValue;
  Register operand1;
  Operand2 operand2;
  String extraInformation = "";
  String label;

  Register rdLo;
  Register rdHi;
  Register rn;
  Register rm;
}
