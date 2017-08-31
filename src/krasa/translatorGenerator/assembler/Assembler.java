package krasa.translatorGenerator.assembler;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiBuilder;
import krasa.translatorGenerator.PsiFacade;

/**
 * @author Vojtech Krasa
 */
public abstract class Assembler {
	private static final Logger LOG = Logger.getInstance(TranslatorMethodAssembler.class.getName());

	protected PsiFacade psiFacade;
	protected PsiBuilder psiBuilder;
	protected Context context;
	protected Editor editor;
	protected Project project;

	public Assembler(PsiFacade psiFacade, Context context) {
		this.psiBuilder = new PsiBuilder(context, psiFacade.getProject());
		this.context = context;
		editor = context.getEditor();
		project = context.getProject();
		this.psiFacade = psiFacade;
	}

	protected void generateScheduledTranslatorMethods(PsiClass builderClass) {
		for (TranslatorDto translatorDto : context.scheduled) {
			if (translatorDto.processed) {
				continue;
			}
			translatorDto.processed = true;
			PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(builderClass, translatorDto);
			addToClass(builderClass, translatorMethod);
		}
		if (context.hasAnyScheduled()) {
			generateScheduledTranslatorMethods(builderClass);
		}
	}

	protected void addToClass(PsiClass builderClass, PsiMethod methodToAdd) {
		PsiElement added;
		PsiMethod[] childrenOfType = PsiTreeUtil.getChildrenOfType(builderClass, PsiMethod.class);
		PsiMethod last = null;
		if (childrenOfType != null) {
			last = childrenOfType[childrenOfType.length - 1];
		}
		if (context.replaceMethods) {
			PsiMethod[] methodsBySignature = builderClass.findMethodsBySignature(methodToAdd, false);
			if (methodsBySignature.length == 1) {
				added = builderClass.addAfter(methodToAdd, methodsBySignature[0]);
				methodsBySignature[0].delete();
			} else {
				added = builderClass.addAfter(methodToAdd, last);
			}
		} else {
			added = builderClass.addAfter(methodToAdd, last);
		}
		psiFacade.shortenClassReferences(added);
		psiFacade.reformat(added);
	}

	protected void replaceMethod(PsiMethod original, PsiMethod replacement) {
		try {
			original.replace(replacement);
			psiFacade.shortenClassReferences(replacement);
			psiFacade.reformat(replacement);
		} catch (Throwable e) {
			throw new RuntimeException("translatorMethod=" + replacement.getName() + ", text=" + replacement.getText(), e);
		}
	}

	protected void replaceClass(PsiClass original, PsiClass replacement) {
		try {
			original.replace(replacement);
			psiFacade.shortenClassReferences(replacement);
			psiFacade.reformat(replacement);
		} catch (Throwable e) {
			throw new RuntimeException("translatorMethod=" + replacement.getName() + ", text=" + replacement.getText(), e);
		}
	}
}
