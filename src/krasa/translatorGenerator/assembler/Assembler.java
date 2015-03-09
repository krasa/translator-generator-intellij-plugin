package krasa.translatorGenerator.assembler;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiBuilder;
import krasa.translatorGenerator.PsiFacade;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Vojtech Krasa
 */
public abstract class Assembler {

	protected PsiFacade psiFacade;
	protected PsiBuilder psiBuilder;
	protected Context context;

	public Assembler(PsiFacade psiFacade, Context context) {
		this.psiBuilder = new PsiBuilder(context, psiFacade.getProject());
		this.context = context;
		this.psiFacade = psiFacade;
	}

	protected void generateTranslatorMethods(PsiClass builderClass, PsiClass from, final PsiClass to) {
		PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(builderClass, from, to);
		addToClass(builderClass, translatorMethod);
		generateScheduledTranslators(builderClass);
	}

	protected void generateScheduledTranslators(PsiClass builderClass) {
		for (TranslatorDto translatorDto : context.scheduled) {
			if (translatorDto.processed) {
				continue;
			}
			translatorDto.processed = true;
			PsiClassReferenceType from = (PsiClassReferenceType) translatorDto.getFrom();
			PsiClassReferenceType to = (PsiClassReferenceType) translatorDto.getTo();
			PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(builderClass, from.resolve(), to.resolve());
			addToClass(builderClass, translatorMethod);
		}
		if (context.hasAnyScheduled()) {
			generateScheduledTranslators(builderClass);
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
}
