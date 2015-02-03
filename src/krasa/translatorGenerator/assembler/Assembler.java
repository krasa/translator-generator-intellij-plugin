package krasa.translatorGenerator.assembler;

import krasa.translatorGenerator.BuilderFactory;
import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiClassReferenceType;

/**
 * @author Vojtech Krasa
 */
public abstract class Assembler {
	protected PsiFacade psiFacade;
	protected BuilderFactory builderFactory;
	protected Context context;

	public Assembler(PsiFacade psiFacade, Context context) {
		this.builderFactory = new BuilderFactory(psiFacade, context);
		this.context = context;
		this.psiFacade = psiFacade;
	}

	protected void generateTranslatorMethods(PsiClass builderClass, PsiClass from, final PsiClass to) {
		PsiMethod translatorMethod = builderFactory.createTranslatorMethod(builderClass, from, to);
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
			PsiMethod translatorMethod = builderFactory.createTranslatorMethod(builderClass, from.resolve(),
					to.resolve());
			addToClass(builderClass, translatorMethod);
		}
		if (context.hasAnyScheduled()) {
			generateScheduledTranslators(builderClass);
		}
	}

	protected void addToClass(PsiClass builderClass, PsiMethod methodToAdd) {
		PsiElement added;
		if (context.replaceMethods) {
			PsiMethod[] methodsBySignature = builderClass.findMethodsBySignature(methodToAdd, false);
			if (methodsBySignature.length == 1) {
				added = methodsBySignature[0].replace(methodToAdd);
			} else {
				added = builderClass.add(methodToAdd);
			}
		} else {
			added = builderClass.add(methodToAdd);
		}
		psiFacade.shortenClassReferences(added);
		psiFacade.reformat(added);
	}
}
