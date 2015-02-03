package krasa.translatorGenerator.assembler;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;

import com.intellij.psi.PsiClass;

public class TranslatorClassAssembler extends Assembler {

	private PsiClass sourceClass;

	public TranslatorClassAssembler(PsiClass sourceClass, PsiFacade psiFacade, Context context) {
		super(psiFacade, context);
		this.sourceClass = sourceClass;
	}

	public void assemble() {
		PsiClass builderClass = assembleTranslatorClass();
		psiFacade.shortenClassReferences(builderClass);
		psiFacade.reformat(builderClass);
		sourceClass.add(builderClass);
	}

	private PsiClass assembleTranslatorClass() {
		PsiClass builderClass = builderFactory.createTranslatorClass(sourceClass, sourceClass);
		generateTranslatorMethods(builderClass, sourceClass, sourceClass);
		return builderClass;
	}

}
