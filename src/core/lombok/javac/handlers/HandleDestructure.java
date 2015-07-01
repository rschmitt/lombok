/*
 * Copyright (C) 2010-2015 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac.handlers;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import lombok.ConfigurationKeys;
import lombok.core.HandlerPriority;
import lombok.destructure;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import org.mangosdk.spi.ProviderFor;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sun.source.tree.Tree.Kind.*;
import static lombok.core.handlers.HandlerUtil.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

@ProviderFor(JavacASTVisitor.class)
@HandlerPriority(65536-1) // 2^16-1; should run immediately before HandleVal
@ResolutionResetNeeded
public class HandleDestructure extends JavacASTAdapter {
	private static final String PREFIX = "$$lombok$destructure$temp";
	private static final AtomicInteger sequenceNumber = new AtomicInteger(0);

	@Override public void visitLocal(JavacNode localNode, JCVariableDecl local) {
		if (!isDestructure(localNode, local)) return;
		handleFlagUsage(localNode, ConfigurationKeys.DESTRUCTURE_FLAG_USAGE, "destructure");

		JavacNode ancestor = localNode.directUp();
		JCTree blockNode = ancestor.get();

		if (illegalDestructure(localNode, local, blockNode)) return;

		if (localNode.shouldDeleteLombokAnnotations()) {
			JavacHandlerUtil.deleteImportFromCompilationUnit(localNode, "lombok.destructure");
		} else {
			JCAnnotation destructureAnnotation = recursiveSetGeneratedBy(localNode.getTreeMaker().Annotation(local.vartype, List.<JCExpression>nil()), local.vartype, localNode.getContext());
			local.mods.annotations = local.mods.annotations == null ? List.of(destructureAnnotation) : local.mods.annotations.append(destructureAnnotation);
		}

		final List<JCStatement> statements = getStatements(blockNode);

		if (statements == null) {
			localNode.addError("destructuring is legal only within local variable declarations inside a block.");
			return;
		}

		visitStatements(localNode, local, statements);
	}

	private List<JCStatement> getStatements(JCTree blockNode) {
		final List<JCStatement> statements;
		if (blockNode instanceof JCBlock) {
			statements = ((JCBlock)blockNode).stats;
		} else if (blockNode instanceof JCCase) {
			statements = ((JCCase)blockNode).stats;
		} else if (blockNode instanceof JCMethodDecl) {
			statements = ((JCMethodDecl)blockNode).body.stats;
		} else {
			statements = null;
		}
		return statements;
	}

	private void visitStatements(JavacNode localNode, JCVariableDecl local, List<JCStatement> statements) {
		JavacNode ancestor = localNode.directUp();
		JCTree blockNode = ancestor.get();

		JCExpression initExpr = null;
		ListBuffer<JCStatement> before = new ListBuffer<JCStatement>();
		ListBuffer<JCStatement> after = new ListBuffer<JCStatement>();
		final ArrayList<Name> lhsVars = new ArrayList<Name>();
		boolean seenDeclaration = false;
		for (JCStatement statement : statements) {
			if (initExpr != null) {
				after.append(statement);
			} else if (!seenDeclaration) {
				if (statement == local) {
					seenDeclaration = true;
					if (local.init == null) {
//						localNode.getNodeFor(statement).addWarning("This begins a multi-variable destructure");
					} else {
//						localNode.getNodeFor(statement).addWarning("This destructures into a single variable");
						initExpr = local.init;
					}
					lhsVars.add(local.getName());
					local.vartype = JavacResolution.createJavaLangObject(localNode.getAst());
				} else {
					before.append(statement);
				}
			} else {
				if (statement instanceof JCVariableDecl) {
					JCVariableDecl decl = (JCVariableDecl) statement;
					if (isDestructure(localNode.getNodeFor(decl), decl)) {
						if (decl.init == null) {
//							localNode.getNodeFor(decl).addWarning("This is a continuation of the destructuring");
						} else {
//							localNode.getNodeFor(decl).addWarning("This is the end of the destructuring");
							initExpr = decl.init;
						}
						lhsVars.add(decl.getName());
						decl.vartype = JavacResolution.createJavaLangObject(localNode.getAst());
					} else {
//						localNode.getNodeFor(statement).addError("'destructure' requires an initializer expression");
						return;
					}
				} else {
//					localNode.getNodeFor(statement).addError("'destructure' requires an initializer expression");
					return;
				}
			}
		}

		if (initExpr == null) {
			localNode.addError("'destructure' requires an initializer expression");
			return;
		} else if (initExpr.getKind() == NULL_LITERAL) {
			localNode.getNodeFor(initExpr).addError("'destructure' requires a non-null initializer");
			return;
		}

		ListBuffer<JCStatement> newStatements = desugarVars(localNode, local, initExpr, lhsVars);
		ListBuffer<JCStatement> newBlock = before.appendList(newStatements).appendList(after);

		installNewBlock(blockNode, newBlock);

		ancestor.rebuild();
	}

	private void installNewBlock(JCTree blockNode, ListBuffer<JCStatement> newBlock) {
		if (blockNode instanceof JCBlock) {
			((JCBlock)blockNode).stats = newBlock.toList();
		} else if (blockNode instanceof JCCase) {
			((JCCase)blockNode).stats = newBlock.toList();
		} else if (blockNode instanceof JCMethodDecl) {
			((JCMethodDecl)blockNode).body.stats = newBlock.toList();
		} else throw new AssertionError("Should not get here");
	}

	private ListBuffer<JCStatement> desugarVars(JavacNode localNode, JCVariableDecl local, JCExpression initExpr, ArrayList<Name> lhsVars) {
		ListBuffer<JCStatement> newStatements = new ListBuffer<JCStatement>();
		JCExpression lombokVal = JavacHandlerUtil.chainDotsString(localNode, "lombok.val");
		JavacTreeMaker maker = localNode.getAst().getTreeMaker();
		Name tempvar = localNode.toName(getTempIdent());
		JCVariableDecl jcVariableDecl = maker.VarDef((JCModifiers) local.mods.clone(), tempvar, lombokVal, initExpr);
		newStatements.append(jcVariableDecl);

		for (Name lhsVar : lhsVars) {
			Name methodName = localNode.toName(toGetterName(lhsVar.toString()));
			JCFieldAccess getterSelect = maker.Select(maker.Ident(tempvar), methodName);
			JCMethodInvocation getterInv = maker.Apply(null, getterSelect, List.<JCExpression>nil());
			JCVariableDecl lhsVarDecl = maker.VarDef((JCModifiers) local.mods.clone(), lhsVar, lombokVal, getterInv);
			newStatements.append(lhsVarDecl);
		}
		return newStatements;
	}

	private boolean illegalDestructure(JavacNode localNode, JCVariableDecl local, JCTree parentRaw) {
		if (parentRaw instanceof JCForLoop || parentRaw instanceof JCEnhancedForLoop) {
			localNode.addError("'destructure' is not allowed in for loops");
			return true;
		}

		if (local.init instanceof JCNewArray && ((JCNewArray)local.init).elemtype == null) {
			localNode.addError("'val' is not compatible with array initializer expressions. Use the full form (new int[] { ... } instead of just { ... })");
			return true;
		}
		return false;
	}

	private boolean isDestructure(JavacNode localNode, JCVariableDecl local) {
		if (local.vartype == null || (!local.vartype.toString().equals("destructure") && !local.vartype.toString().equals("lombok.destructure")))
			return false;
		if (typeMatches(destructure.class, localNode, local.vartype)) return true;
		return true;
	}

	private static String getTempIdent() {
		return PREFIX + sequenceNumber.getAndIncrement();
	}

	private static String toGetterName(String varName) {
		if (varName.length() >= 3 && varName.startsWith("is") && Character.isUpperCase(varName.charAt(2))) {
			return varName;
		}
		String suffix = varName.substring(1);
		String methodNameStr = "get" + Character.toUpperCase(varName.charAt(0)) + suffix;
		return methodNameStr;
	}
}
