package frontend;

import IR.BasicBlock;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ivan on 1/31/2015.
 */
public class Parser {

    private int in; //the current currToken on the input
    private Scanner s;

    private HashMap<String, List<Integer>> du;

    private int tokenCount = 0;

    public Parser(String path) throws Exception {
        s = new Scanner(path);
       //initialize the first token
        next();
        //computation();
        //System.out.println("Finished compiling "+s.getLineNumber()+" lines in "+path+".");
    }

    private void next() throws IOException {
        try {
            in = s.getSym();
            tokenCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // BEGIN RULES FOR PL241

    public void relOp() throws Exception {
//        switch(in) {
//            // relation operators
//        }
        if(in >= 20 && in <=25){
            next();
        }
    }

    public void ident() throws Exception {
        if(accept(Token.ident)) {
            next();
        } else {
            error("Missing identifier");
        }
    }

    public void number() throws Exception {
        if(accept(Token.number)) {
            next();
        } else {
            error("Missing number");
        }
    }


    public void designator() throws Exception {
        if(accept(Token.ident)) {
            ident();
            while (accept(Token.openbracketToken)) {
                next();
                expression();
                if(accept(Token.closebracketToken)) {
                    next();
                } else {
                    error("Missing close bracket in designator");
                }
            }
        } else {
            error("Missing identifier from designator");
        }
    }

    public BasicBlock factor() throws Exception {
        BasicBlock b = new BasicBlock();
        if(accept(Token.ident)) {
            designator();
        }
        else if(accept(Token.number)) {
            number();
        }
        else if(accept(Token.openparenToken)) {
            next();
            expression();
            if(accept(Token.closeparenToken)) {
                next();
            }
        }
        else if(accept(Token.callToken)) {
            funcCall();
        }
        else {
            error("Invalid factor call");
        }
        b.exit = b;
        return b;
    }

    public BasicBlock term() throws Exception {
        BasicBlock b = new BasicBlock();
        factor();
        while(accept(Token.timesToken) || accept(Token.divToken)) {
            next();
            factor();
        }
        b.instruction = "term";
        b.exit = b;
        return b;
    }

    public BasicBlock expression() throws Exception {
        BasicBlock b = new BasicBlock();
        term();
        while(accept(Token.plusToken) || accept(Token.minusToken)) {
            next();
            term();
        }
        b.instruction = "expression";
        b.exit = b;
        return b;
    }

    public void relation() throws Exception {
        expression();
        relOp();
        expression();
    }

    public BasicBlock assignment() throws Exception {
        BasicBlock b = new BasicBlock();
        if(accept(Token.letToken)) {
            next();
            designator();
            if(accept(Token.becomesToken)) {
                next();
                expression();
            } else {
                error("Missing becomes token during assignment");
            }
        } else {
            error("Missing let token during assignment");
        }
        b.instruction = "assignment";
        b.exit = b;
        return b;
    }

    public BasicBlock funcCall() throws Exception {
        BasicBlock b = new BasicBlock();
        if(accept(Token.callToken)) {
            next();
            ident();
            if(accept(Token.openparenToken)) {
                next();
                if(!accept(Token.closeparenToken)) {
                    expression();
                    while(accept(Token.commaToken)) {
                        next();
                        expression();
                    }
                }
                if(accept(Token.closeparenToken)) {
                    next();
                } else {
                    error("Missing close paren in func call");
                }
            } else {
//                error("Missing open paren in func call");
            }
        }
        b.instruction = "calling something";
        b.exit = b;
        return b;
    }

    public BasicBlock ifStatement() throws Exception {
        BasicBlock b = new BasicBlock();
        b.instruction = "if";
        BasicBlock join = new BasicBlock();
        join.instruction = "fi";
        b.right = join;
        if(accept(Token.ifToken)) {
            next();
            relation();
            if(accept(Token.thenToken)) {
                next();
                b.left = statSequence();
                if (accept(Token.elseToken)) {
                    next();
                    b.right = statSequence();
                    b.right.exit.left = join;
                }
                if(accept(Token.fiToken)) {
                    next();
                    b.left.exit.left = join;
                    b.exit = join;
                } else {
                    error("Missing fi token");
                }
            } else {
                error("Missing then token");
            }
        } else {
            error("Missing if token");
        }
        return b;
    }

    public BasicBlock whileStatement() throws Exception {
        BasicBlock b = new BasicBlock();
        b.instruction = "while";
        BasicBlock j = new BasicBlock();
        j.instruction = "od";
        b.right = j;            //exiting the while loop
        b.exit = j;
        if(accept(Token.whileToken)) {
            next();
            relation();
            if(accept(Token.doToken)) {
                next();
                BasicBlock leftSide = statSequence();
                leftSide.exit.left = j;
                b.left = leftSide;
                // TODO: patch this back to the first b block later
                if(accept(Token.odToken)) {
                    next();
                } else {
                    error("Missing od token");
                }
            } else {
                error("Missing do token");
            }
        } else {
            error("Missing while token");
        }
        return b;
    }

    public BasicBlock returnStatement() throws Exception {
        BasicBlock b = new BasicBlock();
        if(accept(Token.returnToken)) {
            next();
            expression();
            b.instruction = "return something";
        } else {
            error("Missing return statement");
        }
        b.exit = b;
        return b;
    }

    public BasicBlock statement() throws Exception {
        if(accept(Token.letToken)) {
            return assignment();
        }
        else if(accept(Token.callToken)) {
            return funcCall();
        }
        else if(accept(Token.ifToken)) {
            return ifStatement();
        }
        else if(accept(Token.whileToken)) {
            return whileStatement();
        }
        else if(accept(Token.returnToken)) {
            return returnStatement();
        } else {
            error("Statement is invalid");
        }
        return null;
    }

    public BasicBlock statSequence() throws Exception {
        BasicBlock start = statement();
        BasicBlock last = start;
        while(accept(Token.semiToken)) {
            next();
            last.left = statement();
            last = last.left.exit;
            start.exit = last;
        }
        return start;
    }

    public BasicBlock typeDecl() throws Exception {
        BasicBlock b = new BasicBlock();
        if(accept(Token.varToken) || accept(Token.arrToken)) {
            next();
            while(accept(Token.openbracketToken)) {
                next();
                number();
                if(accept(Token.closebracketToken)) {
                    next();
                } else {
                    error("Missing close parenthesis in type declaration");
                }
            }
        }
        b.instruction = "typeDecl";
        b.exit = b;
        return b;
    }

    public BasicBlock varDecl() throws Exception {
        BasicBlock b = new BasicBlock();
        typeDecl();
        ident();
        while(accept(Token.commaToken)) {
            next();
            ident();
        }
        if(accept(Token.semiToken)) {
            next();
        } else {
            error("Missing semicolon for var declaration");
        }
        b.instruction = "varDecl";
        b.exit = b;
        return b;
    }

    public BasicBlock funcDecl() throws Exception {
        BasicBlock b = new BasicBlock();
        b.instruction = "funcDecl";
        if(accept(Token.funcToken) || accept(Token.procToken)) {
            next();
            if(accept(Token.ident)) {
                next();
                //if not semiToken, then formalParams MUST be following
                if(!accept(Token.semiToken)) {
                    formalParam();
                }
                if(accept(Token.semiToken)) {
                    next();
                    b.left = funcBody();
                    b.exit = b.left.exit;
                    if(accept(Token.semiToken)) {
                        next();
                    } else {
                        error("Missing ; after function body");
                    }
                } else {
                    error("Missing ; after formal parameters");
                }
            } else {
                error("Missing identifier for function declaration");
            }
        } else {
            error("Missing function or procedure heading");
        }
        return b;
    }

    public void formalParam() throws Exception {
        if(accept(Token.openparenToken)) {
            next();
            while(!accept(Token.closeparenToken)) {
                ident();
                if(accept(Token.commaToken)) {
                    next();
                }
            }
            if(accept(Token.closeparenToken)) {
                next();
            } else {
                error("Missing close paren for formal params");
            }
        } else {
            error("Missing open paren for formal params");
        }
    }

    public BasicBlock funcBody() throws Exception {
        BasicBlock b = new BasicBlock();
        while(!accept(Token.beginToken)){
            if(accept(Token.varToken) || accept(Token.arrToken)) {
                varDecl();
            } else {
                error("Invalid var declaration in func body");
            }
        }
        if(accept(Token.beginToken)) {
            next();
            b.left = statSequence();
            b.exit = b.left.exit;
            if(accept(Token.endToken)) {
                next();
            } else {
                error("Misisng closing bracket for func body");
            }
        } else {
            error("Missing open bracket for func body");
        }
        return b;
    }

    public BasicBlock computation() throws Exception {
        BasicBlock b = new BasicBlock();
        BasicBlock current = b;
        if(accept(Token.mainToken)) {
            next();
            while (accept(Token.varToken) || accept(Token.arrToken)) {
                current.left = varDecl();
                current = current.left;
            }
            while (accept(Token.funcToken) || accept(Token.procToken)) {
                current.left = funcDecl();
                current = current.left;
            }
            if (accept(Token.beginToken)){
                next();
                //if token is not }, must be statSequence option.
                if(!accept(Token.endToken)){
                    current.left = statSequence();
                    current = current.left;
                }
                if(accept(Token.endToken)) {
                    next();
                } else {
                    error("Missing closing bracket for main");
                }
            } else {
                error("Missing open bracket for main");
            }
        } else {
            error("Missing main");
        }
        b.instruction = "main";
        return b;
    }

    private boolean accept(Token t) {
        return in == t.value;
    }


    private void emit(String s) {
        System.out.println(s);
    }

    public void error(String e) throws Exception {
        throw new Exception("Parser encountered error "+e+" on line "+s.getLineNumber()+" near tokenNum:"
                +tokenCount+" ="+Token.getRepresentation(in));
    }
}
