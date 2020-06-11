package org.ehrbase.aql.containment;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ehrbase.aql.containment.Containment;
import org.ehrbase.aql.containment.ContainmentSet;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.parser.AqlParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEval {
    private final ParserRuleContext parserRuleContext;
    private final IdentifierMapper identifierMapper;

    public ExpressionEval(ParserRuleContext parserRuleContext, IdentifierMapper identifierMapper) {
        this.parserRuleContext = parserRuleContext;
        this.identifierMapper = identifierMapper;
    }

    public boolean isSingleChain(){
        if (!(parserRuleContext instanceof AqlParser.ContainsExpressionContext))
            return false;
        else
        return ((AqlParser.ContainsExpressionContext)parserRuleContext).AND() == null
                && ((AqlParser.ContainsExpressionContext)parserRuleContext).OR() == null
                && ((AqlParser.ContainsExpressionContext)parserRuleContext).XOR() == null;
    }

    public Containment getEmbeddedContainment(){
        try {
            AqlParser.ArchetypedClassExprContext archetypedClassExprContext =
                    (AqlParser.ArchetypedClassExprContext) parserRuleContext.getChild(0).getChild(0).getChild(0).getChild(0);

            return (Containment) identifierMapper.getContainer(archetypedClassExprContext.getChild(1).toString());
        } catch (NullPointerException e){
            return null;
        }
    }

    public List<Object> developedExpression(Map<String, ContainsCheck> containsCheckMap){
        List<Object> objects = new ArrayList<>();
        for (ParseTree child: parserRuleContext.children){
            if (child instanceof TerminalNode)
                objects.add(new ContainOperator(child.getText()));
            else {
                String expressionLabel = child.getText();
                objects.add(containsCheckMap.get(expressionLabel));
            }
        }
        return objects;
    }


    public List<Containment> getChainedContainments(){
        //traverse the containment list upward
        List<Containment> containments = new ArrayList<>();

        ParserRuleContext parentContainment = parserRuleContext.getParent();

        while (!(parentContainment instanceof AqlParser.FromContext)) {
            parentContainment = parentContainment.getParent();

            if (parentContainment instanceof AqlParser.ContainsContext){
                //it reads from right to left!
                //check for contains keyword
                AqlParser.ContainsContext containsContext = (AqlParser.ContainsContext)parentContainment;
                if (containsContext.getChildCount() > 1){
                    if (!containsContext.getChild(1).getText().equals("contains"))
                    throw new IllegalStateException("Invalid contains context:"+containsContext.getText());

                    if (containsContext.getChild(0) instanceof AqlParser.SimpleClassExprContext){
                        AqlParser.SimpleClassExprContext simpleClassExprContext = (AqlParser.SimpleClassExprContext)containsContext.getChild(0);
                        if (!simpleClassExprContext.IDENTIFIER().isEmpty()){ //form: 1
                            containments.add((Containment)identifierMapper.getContainer(simpleClassExprContext.IDENTIFIER(1).getText()));
                        }
                        else {
                            String identifier = ((AqlParser.ArchetypedClassExprContext)simpleClassExprContext.getChild(0)).IDENTIFIER(1).getText();
                            containments.add((Containment)identifierMapper.getContainer(identifier));
                        }

                    }
                }
            }
        }

        return containments;
    }

    public ContainmentSet containmentSet(Containment containment){
        ContainmentSet containmentSet = new ContainmentSet(0, null);
        containmentSet.add(containment);
        List<Containment> containmentList = getChainedContainments();
        containmentSet.addAll(containmentList);

        return containmentSet;
    }
}
