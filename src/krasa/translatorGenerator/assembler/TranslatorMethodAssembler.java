package krasa.translatorGenerator.assembler;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;

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
		PsiClass parentClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
		PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
		PsiType fromType = parameters[0].getType();
		PsiType toType = psiMethod.getReturnType();
		boolean jaxbCollection = false;
		if (TypeConversionUtil.isVoidType(toType) && parameters.length == 2) {
			toType = parameters[1].getType();
			jaxbCollection = true;
		}

		PsiMethod translatorMethod = psiBuilder.createTranslatorMethod(parentClass, fromType, toType, jaxbCollection);
		replaceMethod(psiMethod, translatorMethod);

		generateScheduledTranslatorMethods(parentClass);
	}

	private PsiClass getPsiClass(PsiType type) {
		PsiClassReferenceType returnType = (PsiClassReferenceType) type;
		PsiJavaCodeReferenceElement reference = returnType.getReference();
		return (PsiClass) reference.resolve();
	}
}
