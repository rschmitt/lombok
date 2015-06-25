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

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
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
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import org.mangosdk.spi.ProviderFor;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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

		JCTree source = local.vartype;

		handleFlagUsage(localNode, ConfigurationKeys.DESTRUCTURE_FLAG_USAGE, "destructure");

		JavacNode ancestor = localNode.directUp();
		JCTree parentRaw = ancestor.get();
		if (parentRaw instanceof JCForLoop || parentRaw instanceof JCEnhancedForLoop) {
			localNode.addError("'destructure' is not allowed in for loops");
			return;
		}

		if (local.init instanceof JCNewArray && ((JCNewArray)local.init).elemtype == null) {
			localNode.addError("'val' is not compatible with array initializer expressions. Use the full form (new int[] { ... } instead of just { ... })");
			return;
		}

		if (localNode.shouldDeleteLombokAnnotations()) JavacHandlerUtil.deleteImportFromCompilationUnit(localNode, "lombok.destructure");

		local.mods.flags |= Flags.FINAL;

		if (!localNode.shouldDeleteLombokAnnotations()) {
			JCAnnotation valAnnotation = recursiveSetGeneratedBy(localNode.getTreeMaker().Annotation(local.vartype, List.<JCExpression>nil()), source, localNode.getContext());
			local.mods.annotations = local.mods.annotations == null ? List.of(valAnnotation) : local.mods.annotations.append(valAnnotation);
		}

		JCTree blockNode = ancestor.get();
		final List<JCStatement> statements;
		if (blockNode instanceof JCBlock) {
			statements = ((JCBlock)blockNode).stats;
		} else if (blockNode instanceof JCCase) {
			statements = ((JCCase)blockNode).stats;
		} else if (blockNode instanceof JCMethodDecl) {
			statements = ((JCMethodDecl)blockNode).body.stats;
		} else {
			localNode.addError("destructuring is legal only within local variable declarations inside a block.");
			return;
		}

		JCExpression initExpr = null;
		JCExpression lombokVal = JavacHandlerUtil.chainDotsString(localNode, "lombok.val");
		ListBuffer<JCStatement> before = new ListBuffer<JCStatement>();
		ListBuffer<JCStatement> newStatements = new ListBuffer<JCStatement>();
		ListBuffer<JCStatement> after = new ListBuffer<JCStatement>();
		final ArrayList<Name> lhsVars = new ArrayList<Name>();
		boolean seenDeclaration = false;
		for (JCStatement statement : statements) {
			if (initExpr != null) {
				after.append(statement);
				continue;
			}
			if (!seenDeclaration) {
				if (statement == local) {
					seenDeclaration = true;
					if (local.init == null) {
						localNode.getNodeFor(statement).addWarning("This begins a multi-variable destructure");
					} else {
						localNode.getNodeFor(statement).addWarning("This destructures into a single variable");
						initExpr = local.init;
					}
					lhsVars.add(local.getName());
					local.vartype = lombokVal;
				} else {
					before.append(statement);
				}
			} else {
				if (statement instanceof JCVariableDecl) {
					JCVariableDecl decl = (JCVariableDecl) statement;
					if (isDestructure(localNode.getNodeFor(decl), decl)) {
						if (decl.init == null) {
							localNode.getNodeFor(decl).addWarning("This is a continuation of the destructuring");
						} else {
							localNode.getNodeFor(decl).addWarning("This is the end of the destructuring");
							initExpr = decl.init;
						}
						lhsVars.add(decl.getName());
						decl.vartype = lombokVal;
					} else {
						localNode.getNodeFor(statement).addError("'destructure' requires an initializer expression");
						return;
					}
				} else {
					localNode.getNodeFor(statement).addError("'destructure' requires an initializer expression");
					return;
				}
			}
		}

		if (initExpr == null) {
			localNode.addError("'destructure' requires an initializer expression");
			return;
		} else if (initExpr.getKind() == Tree.Kind.NULL_LITERAL) {
			localNode.getNodeFor(initExpr).addError("'destructure' requires a non-null initializer");
			return;
		}
		JavacTreeMaker maker = localNode.getAst().getTreeMaker();
		Name tempvar = localNode.toName(getTempIdent());
		JCVariableDecl jcVariableDecl = maker.VarDef(local.mods, tempvar, lombokVal, initExpr);
		newStatements.append(jcVariableDecl);

		for (Name lhsVar : lhsVars) {
			String suffix = lhsVar.toString().substring(1);
			String methodNameStr = "get" + Character.toUpperCase(lhsVar.charAt(0)) + suffix;
			Name methodName = localNode.toName(methodNameStr);
			JCFieldAccess getterSelect = maker.Select(maker.Ident(tempvar), methodName);
			JCMethodInvocation getterInv = maker.Apply(null, getterSelect, List.<JCTree.JCExpression>nil());
			JCVariableDecl lhsVarDecl = maker.VarDef(local.mods, lhsVar, lombokVal, getterInv);
			newStatements.append(lhsVarDecl);
		}

		ListBuffer<JCStatement> newBlock = before.appendList(newStatements).appendList(after);

		if (blockNode instanceof JCBlock) {
			((JCBlock)blockNode).stats = newBlock.toList();
		} else if (blockNode instanceof JCCase) {
			((JCCase)blockNode).stats = newBlock.toList();
		} else if (blockNode instanceof JCMethodDecl) {
			((JCMethodDecl)blockNode).body.stats = newBlock.toList();
		} else throw new AssertionError("Should not get here");

		ancestor.rebuild();
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
}
