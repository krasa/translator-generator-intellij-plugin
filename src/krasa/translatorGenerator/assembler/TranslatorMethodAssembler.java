package krasa.translatorGenerator.assembler;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;

/**
 * @author Vojtech Krasa
 */
public class TranslatorMethodAssembler extends Assembler {

	private static final Logger LOG = Logger.getInstance(TranslatorMethodAssembler.class.getName());

	private PsiMethod psiMethod;

	public TranslatorMethodAssembler(PsiMethod psiMethod, PsiFacade psiFacade, Context context) {
		super(psiFacade, context);
		this.psiMethod = psiMethod;
	}

	public void assemble() {
		PsiClass topLevelClass = PsiUtil.getTopLevelClass(psiMethod);
		PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
		PsiType fromType = parameters[0].getType();
		PsiType toType = psiMethod.getReturnType();

		PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(topLevelClass, fromType, toType);
		replaceMethod(translatorMethod);

		generateScheduledTranslatorMethods(topLevelClass);
	}

	private void replaceMethod(PsiMethod translatorMethod) {
		try {
			psiMethod.replace(translatorMethod);
			psiFacade.shortenClassReferences(translatorMethod);
			psiFacade.reformat(translatorMethod);
		} catch (Throwable e) {
			throw new RuntimeException("translatorMethod=" + translatorMethod.getName() + ", text="
					+ translatorMethod.getText(), e);
		}
	}

	private PsiClass getPsiClass(PsiType type) {
		PsiClassReferenceType returnType = (PsiClassReferenceType) type;
		PsiJavaCodeReferenceElement reference = returnType.getReference();
		return (PsiClass) reference.resolve();
	}
}
