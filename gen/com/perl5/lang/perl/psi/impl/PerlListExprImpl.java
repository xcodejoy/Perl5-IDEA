// This is a generated file. Not intended for manual editing.
package com.perl5.lang.perl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.perl5.lang.perl.lexer.PerlElementTypes.*;
import com.perl5.lang.perl.psi.*;

public class PerlListExprImpl extends PerlExprImpl implements PerlListExpr {

  public PerlListExprImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PerlVisitor) ((PerlVisitor)visitor).visitListExpr(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PerlArray getArray() {
    return findChildByClass(PerlArray.class);
  }

  @Override
  @Nullable
  public PerlHash getHash() {
    return findChildByClass(PerlHash.class);
  }

  @Override
  @Nullable
  public PerlScalar getScalar() {
    return findChildByClass(PerlScalar.class);
  }

}
