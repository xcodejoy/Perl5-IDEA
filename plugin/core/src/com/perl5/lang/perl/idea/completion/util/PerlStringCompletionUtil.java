/*
 * Copyright 2015-2019 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.completion.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.Language;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.perl5.PerlIcons;
import com.perl5.lang.perl.extensions.packageprocessor.PerlPackageOptionsProvider;
import com.perl5.lang.perl.extensions.packageprocessor.PerlPackageParentsProvider;
import com.perl5.lang.perl.extensions.packageprocessor.PerlPackageProcessor;
import com.perl5.lang.perl.idea.PerlElementPatterns;
import com.perl5.lang.perl.idea.completion.PerlStringCompletionCache;
import com.perl5.lang.perl.idea.intellilang.PerlInjectionMarkersService;
import com.perl5.lang.perl.psi.*;
import com.perl5.lang.perl.psi.impl.PerlStringContentElementImpl;
import com.perl5.lang.perl.psi.impl.PerlUseStatementElement;
import com.perl5.lang.perl.psi.utils.PerlPsiUtil;
import com.perl5.lang.perl.util.PerlPackageUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.perl5.lang.perl.parser.PerlParserUtil.IDENTIFIER_PATTERN;


public class PerlStringCompletionUtil implements PerlElementPatterns {
  public static final String[] REF_TYPES = new String[]{
    "SCALAR",
    "ARRAY",
    "HASH",
    "CODE",
    "REF",
    "GLOB",
    "LVALUE",
    "FORMAT",
    "IO",
    "VSTRING",
    "Regexp"
  };

  public static void fillWithHashIndexes(final @NotNull PsiElement element, @NotNull final CompletionResultSet result) {
    Set<String> hashIndexesCache = PerlStringCompletionCache.getInstance(element.getProject()).getHashIndexesCache();

    for (String text : hashIndexesCache) {
      result.addElement(LookupElementBuilder.create(text));
    }

    PsiFile file = element.getContainingFile();

    file.accept(
      new PerlRecursiveVisitor() {
        @Override
        public void visitStringContentElement(@NotNull PerlStringContentElementImpl o) {
          if (o != element && SIMPLE_HASH_INDEX.accepts(o)) {
            processStringElement(o);
          }
          super.visitStringContentElement(o);
        }

        @Override
        public void visitCommaSequenceExpr(@NotNull PsiPerlCommaSequenceExpr o) {
          if (o.getParent() instanceof PsiPerlAnonHash) {
            PsiElement sequenceElement = o.getFirstChild();
            boolean isKey = true;

            while (sequenceElement != null) {
              ProgressManager.checkCanceled();
              IElementType elementType = sequenceElement.getNode().getElementType();
              if (isKey && sequenceElement instanceof PerlString) {
                for (PerlStringContentElement stringElement : PerlPsiUtil.collectStringElements(sequenceElement)) {
                  processStringElement(stringElement);
                }
              }
              else if (elementType == COMMA || elementType == FAT_COMMA) {
                isKey = !isKey;
              }

              sequenceElement = PerlPsiUtil.getNextSignificantSibling(sequenceElement);
            }
          }
          super.visitCommaSequenceExpr(o);
        }

        protected void processStringElement(PerlStringContentElement stringContentElement) {
          String text = stringContentElement.getText();
          if (StringUtil.isNotEmpty(text) && !hashIndexesCache.contains(text) && IDENTIFIER_PATTERN.matcher(text).matches()) {
            hashIndexesCache.add(text);
            result.addElement(LookupElementBuilder.create(stringContentElement, text));
          }
        }
      });
  }

  public static void fillWithExportableEntities(@NotNull PsiElement element, @NotNull final CompletionResultSet result) {
    final String contextPackageName = PerlPackageUtil.getContextNamespaceName(element);

    if (contextPackageName == null) {
      return;
    }

    element.getContainingFile().accept(
      new PerlRecursiveVisitor() {

        @Override
        public void visitSubDeclarationElement(@NotNull PerlSubDeclarationElement o) {
          if (contextPackageName.equals(o.getNamespaceName())) {
            result.addElement(LookupElementBuilder.create(o, o.getSubName()));
          }
          super.visitSubDeclarationElement(o);
        }

        @Override
        protected boolean shouldVisitLightElements() {
          return true;
        }

        @Override
        public void visitPerlSubDefinitionElement(@NotNull PerlSubDefinitionElement o) {
          if (contextPackageName.equals(o.getNamespaceName())) {
            result.addElement(LookupElementBuilder.create(o, o.getSubName()));
          }
          super.visitPerlSubDefinitionElement(o);
        }
      }
    );
  }

  public static void fillWithUseParameters(final @NotNull PsiElement stringContentElement, @NotNull final CompletionResultSet resultSet) {
    @SuppressWarnings("unchecked")
    PerlUseStatementElement useStatement =
      PsiTreeUtil.getParentOfType(stringContentElement, PerlUseStatementElement.class, true, PsiPerlStatement.class);

    if (useStatement == null) {
      return;
    }

    List<String> typedParameters = useStatement.getImportParameters();
    Set<String> typedStringsSet = typedParameters == null ? Collections.emptySet() : new THashSet<>(typedParameters);

    PerlPackageProcessor packageProcessor = useStatement.getPackageProcessor();
    // fixme we should allow lookup elements customization by package processor
    if (packageProcessor instanceof PerlPackageOptionsProvider) {
      Map<String, String> options = ((PerlPackageOptionsProvider)packageProcessor).getOptions();

      for (Map.Entry<String, String> option : options.entrySet()) {
        if (!typedStringsSet.contains(option.getKey())) {
          resultSet.addElement(LookupElementBuilder
                                 .create(option.getKey())
                                 .withTypeText(option.getValue(), true)
                                 .withIcon(PerlIcons.PERL_OPTION)
          );
        }
      }

      options = ((PerlPackageOptionsProvider)packageProcessor).getOptionsBundles();

      for (Map.Entry<String, String> option : options.entrySet()) {
        if (!typedStringsSet.contains(option.getKey())) {
          resultSet.addElement(LookupElementBuilder
                                 .create(option.getKey())
                                 .withTypeText(option.getValue(), true)
                                 .withIcon(PerlIcons.PERL_OPTIONS)
          );
        }
      }
    }

    if (packageProcessor instanceof PerlPackageParentsProvider &&
        ((PerlPackageParentsProvider)packageProcessor).hasPackageFilesOptions()) {
      PerlPackageUtil.processPackageFilesForPsiElement(stringContentElement, (packageName, file) -> {
        if (!typedStringsSet.contains(packageName)) {
          resultSet.addElement(PerlPackageCompletionUtil.getPackageLookupElement(file, packageName, null));
        }
        return true;
      });
    }

    Set<String> export = new HashSet<>();
    Set<String> exportOk = new HashSet<>();
    packageProcessor.addExports(useStatement, export, exportOk);
    exportOk.removeAll(export);

    for (String subName : export) {
      if (!typedStringsSet.contains(subName)) {
        resultSet.addElement(LookupElementBuilder
                               .create(subName)
                               .withIcon(PerlIcons.SUB_GUTTER_ICON)
                               .withTypeText("default", true)
        );
      }
    }
    for (String subName : exportOk) {
      if (!typedStringsSet.contains(subName)) {
        resultSet.addElement(LookupElementBuilder
                               .create(subName)
                               .withIcon(PerlIcons.SUB_GUTTER_ICON)
                               .withTypeText("optional", true)
        );
      }
    }
  }

  public static void fillWithRefTypes(@NotNull final CompletionResultSet resultSet) {
    for (String refType : REF_TYPES) {
      resultSet.addElement(LookupElementBuilder.create(refType));
    }
  }

  public static void fillWithInjectableMarkers(@NotNull PsiElement element, @NotNull final CompletionResultSet resultSet) {
    // injectable markers
    PerlInjectionMarkersService injectionService = PerlInjectionMarkersService.getInstance(element.getProject());
    for (String marker : injectionService.getSupportedMarkers()) {
      Language language = injectionService.getLanguageByMarker(marker);
      if (language == null) {
        continue;
      }

      LookupElementBuilder newItem = LookupElementBuilder
        .create(marker)
        .withTypeText("inject with " + language.getDisplayName(), true);

      if (language.getAssociatedFileType() != null) {
        newItem = newItem.withIcon(language.getAssociatedFileType().getIcon());
      }

      resultSet.addElement(newItem);
    }
  }

  public static void fillWithHeredocOpeners(@NotNull PsiElement element, @NotNull final CompletionResultSet resultSet) {
    Set<String> heredocOpenersCache = PerlStringCompletionCache.getInstance(element.getProject()).getHeredocOpenersCache();
    // cached values
    for (String marker : heredocOpenersCache) {
      resultSet.addElement(LookupElementBuilder.create(marker));
    }

    PerlInjectionMarkersService injectionService = PerlInjectionMarkersService.getInstance(element.getProject());

    // collect new values
    PsiFile file = element.getContainingFile();
    if (file != null) {
      file.accept(new PerlRecursiveVisitor() {
        @Override
        public void visitHeredocOpener(@NotNull PsiPerlHeredocOpener o) {
          String openerName = o.getName();
          if (StringUtil.isNotEmpty(openerName) &&
              !heredocOpenersCache.contains(openerName) &&
              injectionService.getLanguageByMarker(openerName) == null) {
            heredocOpenersCache.add(openerName);
            resultSet.addElement(LookupElementBuilder.create(o, openerName));
          }
          super.visitHeredocOpener(o);
        }
      });
    }
  }
}
