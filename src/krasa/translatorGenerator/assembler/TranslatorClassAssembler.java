package krasa.translatorGenerator.assembler;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

/**
 * @author Vojtech Krasa
 */
public class TranslatorClassAssembler extends Assembler {

	private PsiClass sourceClass;

	public TranslatorClassAssembler(PsiClass sourceClass, PsiFacade psiFacade, Context context) {
		super(psiFacade, context);
		this.sourceClass = sourceClass;
	}

	public void assemble() {
		PsiClass builderClass = psiBuilder.createTranslatorClass(sourceClass, sourceClass);
		PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(builderClass, sourceClass, sourceClass);
		addToClass(builderClass, translatorMethod);
		generateScheduledTranslatorMethods(builderClass);
		psiFacade.shortenClassReferences(builderClass);
		psiFacade.reformat(builderClass);
		sourceClass.add(builderClass);
	}

}
