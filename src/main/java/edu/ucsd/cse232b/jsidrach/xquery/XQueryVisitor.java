package edu.ucsd.cse232b.jsidrach.xquery;

import edu.ucsd.cse232b.jsidrach.antlr.XQueryParser;
import edu.ucsd.cse232b.jsidrach.xpath.XPathEvaluator;
import org.w3c.dom.Node;

import java.util.*;

/**
 * XQueryVisitor - Visitor for the context tree generated by ANTLR4
 * <p>
 * The traversal of the context tree has to be done manually, recursively calling visit(ctx)<br>
 * Initially, the root of the grammar is invoked<br>
 * Each method modifies the current list of nodes (nodes) and returns it<br>
 * </p>
 */
public class XQueryVisitor extends edu.ucsd.cse232b.jsidrach.antlr.XQueryBaseVisitor<List<Node>> {

    /**
     * Current map of variables
     */
    private Map<String, List<Node>> vars;

    /**
     * Variables map stack
     */
    private Stack<Map<String, List<Node>>> varsStack;

    /**
     * Current list of nodes (used mainly for XPath)
     */
    private List<Node> nodes;

    /**
     * Public constructor - Initializes the variables
     */
    public XQueryVisitor() {
        this.vars = new HashMap<>();
        this.varsStack = new Stack<>();
        this.nodes = new LinkedList<>();
    }

    /*
     * XPath
     */

    /**
     * Absolute path (children)
     * <pre>
     * [doc(FileName)/rp]
     *   → [rp](root(FileName))
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of nodes resulting of the traversal of the relative path starting from the root of the document
     */
    @Override
    public List<Node> visitApChildren(XQueryParser.ApChildrenContext ctx) {
        visit(ctx.doc());
        visit(ctx.rp());
        this.nodes = XPathEvaluator.unique(this.nodes);
        return this.nodes;
    }

    /**
     * Absolute path (all)
     * <pre>
     * [doc(FileName)//rp]
     *   → [.//rp](root(FileName))
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of nodes resulting of the traversal of the relative path starting at any node in the document
     */
    @Override
    public List<Node> visitApAll(XQueryParser.ApAllContext ctx) {
        visit(ctx.doc());
        this.nodes = XPathEvaluator.descendantsOrSelves(this.nodes);
        visit(ctx.rp());
        this.nodes = XPathEvaluator.unique(this.nodes);
        return this.nodes;
    }

    /**
     * Absolute path (doc)
     * <pre>
     * [doc(FileName)]
     *   → root(FileName)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return Singleton list containing the root element of the XML document
     */
    @Override
    public List<Node> visitApDoc(XQueryParser.ApDocContext ctx) {
        this.nodes = XPathEvaluator.root(ctx.FileName().getText());
        return this.nodes;
    }

    /**
     * Relative path (tag)
     * <pre>
     * [Identifier](n)
     *   → { c | c ← [∗](n) if tag(c) = Identifier }
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of children nodes that have the given identifier
     */
    @Override
    public List<Node> visitRpTag(XQueryParser.RpTagContext ctx) {
        List<Node> nodes = new LinkedList<>();
        String tag = ctx.Identifier().getText();
        for (Node n : this.nodes) {
            List<Node> children = XPathEvaluator.children(n);
            for (Node c : children) {
                if (XPathEvaluator.tag(c).equals(tag)) {
                    nodes.add(c);
                }
            }
        }
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Relative path (wildcard)
     * <pre>
     * [∗](n)
     *   → children(n)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of children nodes
     */
    @Override
    public List<Node> visitRpWildcard(XQueryParser.RpWildcardContext ctx) {
        List<Node> nodes = new LinkedList<>();
        for (Node n : this.nodes) {
            nodes.addAll(XPathEvaluator.children(n));
        }
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Relative path (current)
     * <pre>
     * [.](n)
     *   → { n }
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes
     */
    @Override
    public List<Node> visitRpCurrent(XQueryParser.RpCurrentContext ctx) {
        return this.nodes;
    }

    /**
     * Relative path (parent)
     * <pre>
     * [..](n)
     *   → parent(n)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of parent nodes
     */
    @Override
    public List<Node> visitRpParent(XQueryParser.RpParentContext ctx) {
        List<Node> nodes = new LinkedList<>();
        for (Node n : this.nodes) {
            nodes.addAll(XPathEvaluator.parent(n));
        }
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Relative path (text)
     * <pre>
     * [text()](n)
     *   → txt(n)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of text nodes
     */
    @Override
    public List<Node> visitRpText(XQueryParser.RpTextContext ctx) {
        List<Node> nodes = new LinkedList<>();
        for (Node n : this.nodes) {
            nodes.addAll(XPathEvaluator.txt(n));
        }
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Relative path (attribute)
     * <pre>
     * [@Identifier](n)
     *   → attrib(n, Identifier)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of attribute nodes that have the given attribute name
     */
    @Override
    public List<Node> visitRpAttribute(XQueryParser.RpAttributeContext ctx) {
        List<Node> nodes = new LinkedList<>();
        String attId = ctx.Identifier().getText();
        for (Node n : this.nodes) {
            nodes.addAll(XPathEvaluator.attrib(n, attId));
        }
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Relative path (parentheses)
     * <pre>
     * [(rp)](n)
     *   → [rp](n)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of nodes returned by the relative path inside the parentheses
     */
    @Override
    public List<Node> visitRpParentheses(XQueryParser.RpParenthesesContext ctx) {
        return visit(ctx.rp());
    }

    /**
     * Relative path (children)
     * <pre>
     * [rp1/rp2](n)
     *   → unique({ y | x ← [rp1](n), y ← [rp2](x) })
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of distinct nodes obtained by the first relative path concatenated with the second relative path
     */
    @Override
    public List<Node> visitRpChildren(XQueryParser.RpChildrenContext ctx) {
        List<Node> nodes = new LinkedList<>();
        List<Node> children = visit(ctx.rp(0));
        for (Node c : children) {
            this.nodes = new LinkedList<>();
            this.nodes.add(c);
            nodes.addAll(visit(ctx.rp(1)));
        }
        this.nodes = XPathEvaluator.unique(nodes);
        return this.nodes;
    }

    /**
     * Relative path (all)
     * <pre>
     * [rp1//rp2](n)
     *   → unique([rp1/rp2](n), [rp1/∗//rp2](n))
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of distinct nodes obtained by the first relative path concatenated with the second relative path,
     * union the list of nodes obtained by the first relative path concatenated with the second relative path,
     * skipping any number of descendants
     */
    @Override
    public List<Node> visitRpAll(XQueryParser.RpAllContext ctx) {
        visit(ctx.rp(0));
        this.nodes = XPathEvaluator.descendantsOrSelves(this.nodes);
        visit(ctx.rp(1));
        this.nodes = XPathEvaluator.unique(this.nodes);
        return this.nodes;
    }

    /**
     * Relative path (filter)
     * <pre>
     * [rp[f]](n)
     *   → { x | x ← [rp](n) if [f](x) }
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of nodes by preserving only the relative paths that satisfy the filter
     */
    @Override
    public List<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) {
        List<Node> nodes = new LinkedList<>();
        List<Node> rp = visit(ctx.rp());
        for (Node n : rp) {
            this.nodes = new LinkedList<>();
            this.nodes.add(n);
            if (!visit(ctx.f()).isEmpty()) {
                nodes.add(n);
            }

        }
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Relative path (pair)
     * <pre>
     * [rp1, rp2](n)
     *   → [rp1](n), [rp2](n)
     * </pre>
     *
     * @param ctx Current parse tree context
     * @return List of nodes resulting of the union of the lists of nodes produced by both relative paths
     */
    @Override
    public List<Node> visitRpPair(XQueryParser.RpPairContext ctx) {
        List<Node> nodes = new LinkedList<>();
        List<Node> original = this.nodes;
        nodes.addAll(visit(ctx.rp(0)));
        this.nodes = original;
        nodes.addAll(visit(ctx.rp(1)));
        this.nodes = nodes;
        return this.nodes;
    }

    /**
     * Filter (relative path)
     * <pre>
     * [rp](n)
     *   → [rp](n) ≠ { }
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes if the relative path is not empty
     * - an empty list otherwise
     */
    @Override
    public List<Node> visitFRelativePath(XQueryParser.FRelativePathContext ctx) {
        List<Node> nodes = this.nodes;
        List<Node> filter = visit(ctx.rp());
        this.nodes = nodes;
        return filter;
    }

    /**
     * Filter (value equality)
     * <pre>
     * [rp1 = rp2](n)
     * [rp1 eq rp2](n)
     *   → ∃ x ∈ [rp1](n) ∃ y ∈ [rp2](n) / x eq y
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes if there exists some x in the first relative path
     * and some y in the second relative path that are equal
     * - an empty list otherwise
     */
    @Override
    public List<Node> visitFValueEquality(XQueryParser.FValueEqualityContext ctx) {
        List<Node> nodes = this.nodes;
        List<Node> l = visit(ctx.rp(0));
        this.nodes = nodes;
        List<Node> r = visit(ctx.rp(1));
        this.nodes = nodes;
        for (Node nl : l) {
            for (Node nr : r) {
                if (nl.isEqualNode(nr)) {
                    return this.nodes;
                }
            }
        }
        return new LinkedList<>();
    }

    /**
     * Filter (identity equality)
     * <pre>
     * [rp1 == rp2](n)
     * [rp1 is rp2](n)
     *   → ∃ x ∈ [rp1](n) ∃ y ∈ [rp2](n) / x is y
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes if there exists some x in the first relative path
     * and some y in the second relative path that reference the same node
     * - an empty list otherwise
     */
    @Override
    public List<Node> visitFIdentityEquality(XQueryParser.FIdentityEqualityContext ctx) {
        List<Node> nodes = this.nodes;
        List<Node> l = visit(ctx.rp(0));
        this.nodes = nodes;
        List<Node> r = visit(ctx.rp(1));
        this.nodes = nodes;
        for (Node nl : l) {
            for (Node nr : r) {
                if (nl.isSameNode(nr)) {
                    return this.nodes;
                }
            }
        }
        return new LinkedList<>();
    }

    /**
     * Filter (parentheses)
     * <pre>
     * [(f)](n)
     *   → [f](n)
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return List of nodes returned by the filter inside the parentheses
     */
    @Override
    public List<Node> visitFParentheses(XQueryParser.FParenthesesContext ctx) {
        return visit(ctx.f());
    }

    /**
     * Filter (and)
     * <pre>
     * [f1 and f2](n)
     *   → [f1](n) ∧ [f2](n)
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes if both of the filters evaluate to true
     * - an empty list otherwise
     */
    @Override
    public List<Node> visitFAnd(XQueryParser.FAndContext ctx) {
        if ((visit(ctx.f(0)).isEmpty()) || (visit(ctx.f(1)).isEmpty())) {
            return new LinkedList<>();
        }
        return this.nodes;
    }

    /**
     * Filter (or)
     * <pre>
     * [f1 or f2](n)
     *   → [f1](n) ∨ [f2](n)
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes if any of the filters evaluates to true
     * - an empty list otherwise
     */
    @Override
    public List<Node> visitFOr(XQueryParser.FOrContext ctx) {
        if ((visit(ctx.f(0)).isEmpty()) && (visit(ctx.f(1)).isEmpty())) {
            return new LinkedList<>();
        }
        return this.nodes;
    }

    /**
     * Filter (not)
     * <pre>
     * [not f](n)
     *   → ¬[f](n)
     * </pre>
     * Note: filter functions should not change the current list of nodes
     *
     * @param ctx Current parse tree context
     * @return Current list of nodes if the filter evaluates to false
     * - an empty list otherwise
     */
    @Override
    public List<Node> visitFNot(XQueryParser.FNotContext ctx) {
        if (visit(ctx.f()).isEmpty()) {
            return this.nodes;
        }
        return new LinkedList<>();
    }
}
